/*
 *  Copyright 2017-2019 © Schlumberger
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.provider.impl;

import static org.opengroup.osdu.core.common.Constants.COLLABORATIONS_FEATURE_NAME;
import static org.opengroup.osdu.core.common.model.search.RecordMetaAttribute.COLLABORATION_ID;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.POLICY_FEATURE_NAME;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.http.CollaborationContextFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AclRole;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.CollaborationContext;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.AggregationResponse;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.context.UserContext;
import org.opengroup.osdu.search.util.*;
import org.springframework.beans.factory.annotation.Autowired;

abstract class CoreQueryBase {

  @Inject DpsHeaders dpsHeaders;
  @Inject private JaxRsDpsLog log;
  @Inject private CrossTenantUtils crossTenantUtils;
  @Inject private UserContext userContext;

  private final Time REQUEST_TIMEOUT = Time.of(t -> t.time("1m"));

  @Autowired(required = false)
  private IPolicyService iPolicyService;

  @Autowired private IQueryParserUtil queryParserUtil;
  @Autowired private ISortParserUtil sortParserUtil;
  @Autowired private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil;
  @Autowired private ElasticLoggingConfig elasticLoggingConfig;
  @Autowired private IQueryPerformanceLogger tracingLogger;
  @Autowired private GeoQueryBuilder geoQueryBuilder;
  @Autowired private SuggestionsQueryUtil suggestionsQueryUtil;
  @Autowired public IFeatureFlag featureFlag;
  @Autowired private CollaborationContextFactory collaborationContextFactory;
  @Autowired private SearchConfigurationProperties searchConfigurationProperties;

  // if returnedField contains property matching from excludes than query result will NOT include
  // that property
  private final Set<String> excludes =
      new HashSet<>(Collections.singletonList(RecordMetaAttribute.X_ACL.getValue()));

  // queryableExcludes properties can be returned by query results
  private final Set<String> queryableExcludes =
      new HashSet<>(Collections.singletonList(RecordMetaAttribute.INDEX_STATUS.getValue()));

  BoolQuery.Builder buildQuery(String simpleQuery, SpatialFilter spatialFilter, boolean asOwner)
      throws AppException, IOException {

    BoolQuery.Builder queryBuilder = new BoolQuery.Builder();
    if (!Strings.isNullOrEmpty(simpleQuery)) {
      BoolQuery.Builder textQueryBuilder =
          queryParserUtil.buildQueryBuilderFromQueryString(simpleQuery);
      if (textQueryBuilder != null) {
        queryBuilder.must(textQueryBuilder.build()._toQuery());
      }
    }

    // use only one of the spatial request
    if (Objects.nonNull(spatialFilter)) {
      var spatialQuery = this.geoQueryBuilder.getGeoQuery(spatialFilter);
      if (spatialQuery != null) {
        queryBuilder.filter(spatialQuery);
      }
    }

    if (featureFlag.isFeatureEnabled(COLLABORATIONS_FEATURE_NAME)) {
      Optional<CollaborationContext> collaborationContext =
          collaborationContextFactory.create(dpsHeaders.getCollaboration());
      if (collaborationContext.isPresent()) {
        TermQuery.Builder termQueryBuilder =
            QueryBuilders.term()
                .field(COLLABORATION_ID.getValue())
                .value(collaborationContext.get().getId())
                .boost(1.0F);
        queryBuilder.must(termQueryBuilder.build()._toQuery()).boost(1.0F);
      } else {
        ExistsQuery.Builder existsQueryBuilder =
            QueryBuilders.exists().field(COLLABORATION_ID.getValue()).boost(1.0F);
        queryBuilder.mustNot(existsQueryBuilder.build()._toQuery()).boost(1.0F);
      }
    }

    if (featureFlag.isFeatureEnabled(POLICY_FEATURE_NAME)) {
      String compiledESPolicy = this.iPolicyService.getCompiledPolicy();
      WrapperQuery.Builder wrapperQueryBuilder =
          QueryBuilders.wrapper()
              .query(Base64.getEncoder().encodeToString(compiledESPolicy.getBytes()));
      return queryBuilder.must(wrapperQueryBuilder.build()._toQuery()).boost(1.0F);
    } else {
      return getQueryBuilderWithAuthorization(queryBuilder, asOwner);
    }
  }

  private BoolQuery.Builder getQueryBuilderWithAuthorization(
      BoolQuery.Builder queryBuilder, boolean asOwner) {
    if (userHasFullDataAccess()) {
      return queryBuilder;
    }

    List<String> groupsList = userContext.getDataGroups();
    if (groupsList != null && !groupsList.isEmpty()) {
      TermsQueryField groupArray =
          new TermsQueryField.Builder()
              .value(groupsList.stream().map(FieldValue::of).toList())
              .build();

      if (asOwner) {
        queryBuilder.filter(
            new TermsQuery.Builder()
                .field(AclRole.OWNERS.getPath())
                .terms(groupArray)
                    .boost(1.0F)
                .build()
                ._toQuery());
      } else {
        queryBuilder.filter(
            new TermsQuery.Builder()
                .field(RecordMetaAttribute.X_ACL.getValue())
                .terms(groupArray).boost(1.0F)
                .build()
                ._toQuery());
      }
    }
    return queryBuilder;
  }

  String getIndex(Query request) {
    return this.crossTenantUtils.getIndexName(request);
  }

  List<Map<String, Object>> getHitsFromSearchResponse(
      ResponseBody<Map<String, Object>> searchResponse) {
    List<Map<String, Object>> results = new ArrayList<>();
    HitsMetadata<Map<String, Object>> searchHits = searchResponse.hits();

    if (searchHits.hits() != null && !searchHits.hits().isEmpty()) {
      for (Hit<Map<String, Object>> hit : searchHits.hits()) {
        Map<String, Object> hitFields = hit.source();
        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
          Map<String, List<String>> highlights = new HashMap<>();
          for (Map.Entry<String, List<String>> entry : hit.highlight().entrySet()) {
            String fieldName = entry.getKey();
            if (!fieldName.equalsIgnoreCase(RecordMetaAttribute.X_ACL.getValue())) {
              highlights.put(fieldName, entry.getValue().stream().map(String::toString).toList());
            }
          }
          hitFields.put("highlight", highlights);
        }
        results.add(hitFields);
      }
    }
    return results;
  }

  List<AggregationResponse> getAggregationFromSearchResponse(
      SearchResponse<Map<String, Object>> searchResponse) {
    List<AggregationResponse> results = null;
    if (searchResponse.aggregations() != null) {
      TermsAggregateBase kindAgg = null;
      Aggregate aggregate =
          searchResponse.aggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
      if (Objects.nonNull(aggregate)) {
        kindAgg = getTermsAggregationFromNested(aggregate.nested());
      } else {
        aggregate = searchResponse.aggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME);
        if (Objects.nonNull(aggregate)) {
          kindAgg =
              (TermsAggregateBase)
                  searchResponse
                      .aggregations()
                      .get(AggregationParserUtil.TERM_AGGREGATION_NAME)
                      ._get();
        }
      }
      if (Objects.nonNull(kindAgg)
          && Objects.nonNull(kindAgg.buckets())
          && kindAgg.buckets().isArray()) {
        results = new ArrayList<>();

        if (kindAgg instanceof LongTermsAggregate longTermsAggregate) {
          processBuckets(
              longTermsAggregate.buckets().array(),
              results,
              LongTermsBucket::keyAsString,
              LongTermsBucket::docCount);
        } else if (kindAgg instanceof StringTermsAggregate stringTermsAggregate) {
          processBuckets(
              stringTermsAggregate.buckets().array(),
              results,
              bucket -> bucket.key().stringValue(),
              StringTermsBucket::docCount);
        } else if (kindAgg instanceof DoubleTermsAggregate doubleTermsAggregate) {
          processBuckets(
              doubleTermsAggregate.buckets().array(),
              results,
              DoubleTermsBucket::keyAsString,
              DoubleTermsBucket::docCount);
        }
      }
    }
    return results;
  }

  private TermsAggregateBase getTermsAggregationFromNested(NestedAggregate parsedNested) {
    Aggregate nested =
        parsedNested.aggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
    if (Objects.nonNull(nested)) {
      return getTermsAggregationFromNested(nested.nested());
    } else {
      return (TermsAggregateBase)
          parsedNested.aggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME)._get();
    }
  }

  SearchRequest.Builder createSearchSourceBuilder(Query request) throws IOException {
    SearchRequest.Builder sourceBuilder = new SearchRequest.Builder();

    // build query: set query options and query
    BoolQuery.Builder queryBuilder =
        buildQuery(request.getQuery(), request.getSpatialFilter(), request.isQueryAsOwner());

    Suggester suggestBuilder = suggestionsQueryUtil.getSuggestions(request.getSuggestPhrase());

    sourceBuilder.size(QueryUtils.getResultSizeForQuery(request.getLimit()));
    sourceBuilder.query(queryBuilder.build()._toQuery());
    sourceBuilder.timeout(REQUEST_TIMEOUT.time());

    // set suggester
    if (!Objects.isNull(suggestBuilder)) {
      sourceBuilder.suggest(suggestBuilder);
    }

    if (request.isTrackTotalCount()) {
      sourceBuilder.trackTotalHits(tth -> tth.enabled(true));
    }

    // set highlighter
    if (!Objects.isNull(request.getHighlightedFields())) {
      Map<String, HighlightField> map = new HashMap<>();
      for (String field : request.getHighlightedFields()) {
        HighlightField highlightField =
            HighlightField.of(hf -> hf.fragmentSize(200).numberOfFragments(5));
        map.put(field, highlightField);
      }
      Highlight highlight = Highlight.of(h -> h.fields(map));
      sourceBuilder.highlight(highlight);
    }

    // set the return fields
    List<String> returnedFields = request.getReturnedFields();
    if (returnedFields == null) {
      returnedFields = new ArrayList<>();
    }
    Set<String> returnedFieldsSet = new HashSet<>(returnedFields);

    // remove all matching returnedField and queryable from excludes
    Set<String> requestQueryableExcludes = new HashSet<>(queryableExcludes);
    Set<String> requestExcludes = new HashSet<>(excludes);
    requestQueryableExcludes.removeAll(returnedFields);
    requestExcludes.addAll(requestQueryableExcludes);
    sourceBuilder.source(
        SourceConfig.of(
            sc ->
                sc.filter(
                    f ->
                        f.includes(returnedFieldsSet.stream().toList())
                            .excludes(requestExcludes.stream().toList()))));

    return sourceBuilder;
  }

  SearchResponse<Map<String, Object>> makeSearchRequest(
      Query searchRequest, ElasticsearchClient client) {
    long startTime = 0L;
    SearchRequest elasticSearchRequest = null;

    SearchResponse<Map<String, Object>> searchResponse = null;
    int statusCode = 0;

    try {
      String index = this.getIndex(searchRequest);
      SearchRequest.Builder elasticSearchRequestBuilder =
          createElasticRequest(searchRequest, index);
      if (searchRequest.getSort() != null) {
        List<SortOptions> sortBuilders =
            this.sortParserUtil.getSortQuery(client, searchRequest.getSort(), index);
        elasticSearchRequestBuilder.sort(sortBuilders);
      }

      elasticSearchRequest = elasticSearchRequestBuilder.build();

      startTime = System.currentTimeMillis();
      searchResponse = client.search(elasticSearchRequest, (Type) Map.class);
      statusCode = 200;
      return searchResponse;
    } catch (ElasticsearchException e) {
      statusCode = e.status();
      switch (e.status()) {
        case 404:
          throw new AppException(
              HttpServletResponse.SC_NOT_FOUND,
              "Not Found",
              "Resource you are trying to find does not exists",
              e);
        case 400:
          throw new AppException(
              HttpServletResponse.SC_BAD_REQUEST,
              "Bad Request",
              detailedBadRequestMessageUtil.getDetailedBadRequestMessage(elasticSearchRequest, e),
              e);
        case 503:
          throw new AppException(
              HttpServletResponse.SC_SERVICE_UNAVAILABLE,
              "Search error",
              "Please re-try search after some time.",
              e);
        case 429:
          throw new AppException(
              429, "Too many requests", "Too many requests, please re-try after some time", e);
        default:
          statusCode = 500;
          throw new AppException(
              HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Search error",
              "Error processing search request",
              e);
      }
    } catch (AppException e) {
        throw e;
    } catch (Exception e) {
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        String message = "Error processing search request";
        String reason = "Search error";

        if (isTimeout(e)) {
            status = HttpServletResponse.SC_GATEWAY_TIMEOUT;
            message = String.format("Request timed out after waiting for %s", REQUEST_TIMEOUT.time());
        } else if (isJacksonSizeLimitReached(e)) {
            status = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
            reason = "Response is too long";
            message = String.format("Elasticsearch response is too long, max is %dMb",
                    searchConfigurationProperties.getElasticMaxResponseSizeMb());
        }

        throw new AppException(status, reason, message, e);
    } finally {
      Long latency = System.currentTimeMillis() - startTime;
      if (elasticLoggingConfig.getEnabled() || latency > elasticLoggingConfig.getThreshold()) {
        String request =
            elasticSearchRequest != null
                ? elasticSearchRequest.query().toString()
                : searchRequest.toString();
        this.log.debug(String.format("Elastic request-payload: %s", request));
      }
      this.tracingLogger.log(searchRequest, latency, statusCode);
      this.auditLog(searchRequest, searchResponse);
    }
  }

  abstract SearchRequest.Builder createElasticRequest(Query request, String index)
      throws AppException, IOException;

  abstract void querySuccessAuditLogger(Query request);

  abstract void queryFailedAuditLogger(Query request);

  private void auditLog(Query searchRequest, SearchResponse searchResponse) {
    if (searchResponse != null) {
      this.querySuccessAuditLogger(searchRequest);
      return;
    }
    this.queryFailedAuditLogger(searchRequest);
  }

  private boolean isTimeout(Throwable e) {
      String msg = e.getMessage();
      return e instanceof SocketTimeoutException ||
        (msg != null && (msg.contains("listener timeout") || msg.contains("timeout on connection")));
  }

  private boolean isJacksonSizeLimitReached(Throwable t) {
      int depth = 0;
      // Limit to 10 iterations to prevent infinite loops in case of circular references
      while (t != null && depth < 10) {
          if (t instanceof StreamConstraintsException) {
              return true;
          }
          t = t.getCause();
          depth++;
      }
      return false;
  }

  private boolean userHasFullDataAccess() {
    return userContext.isRootUser();
  }

  <T> void processBuckets(
      List<T> buckets,
      List<AggregationResponse> results,
      Function<T, String> keyExtractor,
      ToLongFunction<T> docCountExtractor) {
    for (T bucket : buckets) {
      results.add(
          AggregationResponse.builder()
              .key(keyExtractor.apply(bucket))
              .count(docCountExtractor.applyAsLong(bucket))
              .build());
    }
  }
}
