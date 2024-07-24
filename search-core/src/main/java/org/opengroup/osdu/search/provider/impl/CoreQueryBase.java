// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.provider.impl;

import static org.opengroup.osdu.core.common.Constants.COLLABORATIONS_FEATURE_NAME;
import static org.opengroup.osdu.core.common.model.search.RecordMetaAttribute.COLLABORATION_ID;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.ContentTooLongException;
// import org.elasticsearch.ElasticsearchStatusException;
// import org.elasticsearch.action.search.SearchRequest;
// import org.elasticsearch.action.search.SearchResponse;
// import org.elasticsearch.client.RequestOptions;
// import org.elasticsearch.client.RestHighLevelClient;
// import org.elasticsearch.common.text.Text;
// import org.elasticsearch.common.unit.TimeValue;
// import org.elasticsearch.index.query.BoolQueryBuilder;
// import org.elasticsearch.index.query.QueryBuilder;
// import org.elasticsearch.index.query.QueryBuilders;
// import org.elasticsearch.index.query.TermsQueryBuilder;
// import org.elasticsearch.index.query.WrapperQueryBuilder;
// import org.elasticsearch.rest.RestStatus;
// import org.elasticsearch.search.SearchHit;
// import org.elasticsearch.search.SearchHits;
// import org.elasticsearch.search.aggregations.Aggregation;
// import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
// import org.elasticsearch.search.aggregations.bucket.terms.Terms;
// import org.elasticsearch.search.builder.SearchSourceBuilder;
// import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
// import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
// import org.elasticsearch.search.sort.FieldSortBuilder;
// import org.elasticsearch.search.suggest.SuggestBuilder;
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
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.util.*;
import org.springframework.beans.factory.annotation.Autowired;

abstract class CoreQueryBase {

  @Inject DpsHeaders dpsHeaders;
  @Inject private JaxRsDpsLog log;
  @Inject private IProviderHeaderService providerHeaderService;
  @Inject private CrossTenantUtils crossTenantUtils;

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
  @Autowired public IFeatureFlag collaborationFeatureFlag;
  @Autowired private CollaborationContextFactory collaborationContextFactory;

  // if returnedField contains property matching from excludes than query result will NOT include
  // that property
  private final Set<String> excludes =
      new HashSet<>(Arrays.asList(RecordMetaAttribute.X_ACL.getValue()));

  // queryableExcludes properties can be returned by query results
  private final Set<String> queryableExcludes =
      new HashSet<>(Arrays.asList(RecordMetaAttribute.INDEX_STATUS.getValue()));

  BoolQuery.Builder buildQuery(String simpleQuery, SpatialFilter spatialFilter, boolean asOwner)
      throws AppException, IOException {

    var queryBuilder = new BoolQuery.Builder();
    if (!Strings.isNullOrEmpty(simpleQuery)) {
      var textQueryBuilder = queryParserUtil.buildQueryBuilderFromQueryString(simpleQuery);
      if (textQueryBuilder != null) {
        queryBuilder.must(textQueryBuilder.build()._toQuery());
      }
    }

    // use only one of the spatial request
    //        if (spatialFilter != null) {
    //            QueryBuilder spatialQueryBuilder =
    // this.geoQueryBuilder.getGeoQuery(spatialFilter);
    //            if (spatialQueryBuilder != null) {
    //                queryBuilder.filter().add(spatialQueryBuilder);
    //            }
    //        }

    if (collaborationFeatureFlag.isFeatureEnabled(COLLABORATIONS_FEATURE_NAME)) {
      Optional<CollaborationContext> collaborationContext =
          collaborationContextFactory.create(dpsHeaders.getCollaboration());
      if (collaborationContext.isPresent()) {
        TermQuery.Builder termQueryBuilder =
            QueryBuilders.term()
                .field(COLLABORATION_ID.getValue())
                .value(collaborationContext.get().getId());
        queryBuilder.must(termQueryBuilder.build()._toQuery());
      } else {
        ExistsQuery.Builder existsQueryBuilder =
            QueryBuilders.exists().field(COLLABORATION_ID.getValue());
        queryBuilder.mustNot(existsQueryBuilder.build()._toQuery());
      }
    }

    if (this.iPolicyService != null) {
      String compiledESPolicy = this.iPolicyService.getCompiledPolicy(providerHeaderService);
      WrapperQuery.Builder wrapperQueryBuilder = QueryBuilders.wrapper().query(compiledESPolicy);
      return queryBuilder.must(wrapperQueryBuilder.build()._toQuery());
    } else {
      return getQueryBuilderWithAuthorization(queryBuilder, asOwner);
    }
  }

  private BoolQuery.Builder getQueryBuilderWithAuthorization(
      BoolQuery.Builder queryBuilder, boolean asOwner) {
    if (userHasFullDataAccess()) {
      return queryBuilder;
    }

    String groups = dpsHeaders.getHeaders().get(providerHeaderService.getDataGroupsHeader());
    if (groups != null) {
      TermsQueryField groupArray =
          new TermsQueryField.Builder()
              .value(Arrays.stream(groups.trim().split("\\s*,\\s*")).map(FieldValue::of).toList())
              .build();

      if (asOwner) {
        queryBuilder.filter(
            new TermsQuery.Builder()
                .field(AclRole.OWNERS.getPath())
                .terms(groupArray)
                .build()
                ._toQuery());
      } else {
        queryBuilder.filter(
            new TermsQuery.Builder()
                .field(RecordMetaAttribute.X_ACL.getValue())
                .terms(groupArray)
                .build()
                ._toQuery());
      }
    }
    return queryBuilder;
  }

  String getIndex(Query request) {
    return this.crossTenantUtils.getIndexName(request);
  }

  List<Map<String, Object>> getHitsFromSearchResponse(ResponseBody<Map<String, Object>> searchResponse) {
    List<Map<String, Object>> results = new ArrayList<>();
    HitsMetadata<Map<String, Object>> searchHits = searchResponse.hits();

    if (searchHits.hits() != null && !searchHits.hits().isEmpty()){
      for(Hit<Map<String, Object>> hit: searchHits.hits()){
        Map<String, Object> hitFields = hit.source();
        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
          Map<String, List<String>> highlights = new HashMap<>();
          for (Map.Entry<String, List<String>> entry : hit.highlight().entrySet()) {
            String fieldName = entry.getKey();
            if (!fieldName.equalsIgnoreCase(RecordMetaAttribute.X_ACL.getValue())) {
              highlights.put(fieldName, entry.getValue().stream()
                      .map(String::toString)
                      .collect(Collectors.toList()));
            }
          }
          hitFields.put("highlight", highlights);
        }
        results.add(hitFields);
      }
    }
    return results;
  }

  List<AggregationResponse> getAggregationFromSearchResponse(SearchResponse<Map<String, Object>> searchResponse) {
    List<AggregationResponse> results = null;
    if (searchResponse.aggregations() != null) {
      StringTermsAggregate kindAgg = null;
      NestedAggregate nestedAggregate = searchResponse.aggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME).nested();

      if(Objects.nonNull(nestedAggregate)){
        kindAgg = getTermsAggregationFromNested(nestedAggregate);
      }else {
        kindAgg = searchResponse.aggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME).sterms();
      }
      if(Objects.nonNull(kindAgg.buckets()) && kindAgg.buckets().isArray()){
        results = new ArrayList<>();

        for (StringTermsBucket bucket : kindAgg.buckets().array()) {
          results.add(AggregationResponse.builder().key(bucket.key().stringValue()).count(bucket.docCount()).build());
        }
      }
    }
    return results;
  }

    private StringTermsAggregate getTermsAggregationFromNested(NestedAggregate parsedNested) {
    Aggregate nested = parsedNested.aggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
    if (nested != null) {
      return getTermsAggregationFromNested(nested.nested());
    } else {
      return parsedNested.aggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME).sterms();
    }
  }

  SearchRequest.Builder createSearchSourceBuilder(Query request) throws IOException {
    // SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    var sourceBuilder = new SearchRequest.Builder();
    // build query: set query options and query
    var queryBuilder =
        buildQuery(request.getQuery(), request.getSpatialFilter(), request.isQueryAsOwner());
    //        SuggestBuilder suggestBuilder =
    // suggestionsQueryUtil.getSuggestions(request.getSuggestPhrase());

    sourceBuilder.size(QueryUtils.getResultSizeForQuery(request.getLimit()));
    sourceBuilder.query(queryBuilder.build()._toQuery());
    sourceBuilder.timeout(REQUEST_TIMEOUT.time());

    //        // set suggester
    //        if (!Objects.isNull(suggestBuilder)) {
    //           sourceBuilder.suggest(suggestBuilder);
    //        }

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
    String[] includesArr = returnedFieldsSet.toArray(new String[returnedFieldsSet.size()]);

    // remove all matching returnedField and queryable from excludes
    Set<String> requestQueryableExcludes = new HashSet<>(queryableExcludes);
    Set<String> requestExcludes = new HashSet<>(excludes);
    requestQueryableExcludes.removeAll(returnedFields);
    requestExcludes.addAll(requestQueryableExcludes);
    String[] excludesArr = requestExcludes.toArray(new String[requestExcludes.size()]);
    sourceBuilder.source(
        SourceConfig.of(
            sc ->
                sc.filter(
                    f ->
                        f.includes(returnedFieldsSet.stream().toList())
                            .excludes(requestExcludes.stream().toList()))));

    return sourceBuilder;
  }

  SearchResponse<Map<String, Object>> makeSearchRequest(Query searchRequest, ElasticsearchClient client) {
    long startTime = 0L;
    SearchRequest elasticSearchRequest = null;
    SearchResponse<Map<String, Object>> searchResponse = null;
    int statusCode = 0;

    try {
      String index = this.getIndex(searchRequest);
      elasticSearchRequest = createElasticRequest(searchRequest, index);
      if (searchRequest.getSort() != null) {
        List<SortOptions> sortBuilders =
            this.sortParserUtil.getSortQuery(client, searchRequest.getSort(), index);
        for (SortOptions fieldSortBuilder : sortBuilders) {
          elasticSearchRequest.sort().add(fieldSortBuilder);
        }
      }

      startTime = System.currentTimeMillis();
      searchResponse = client.search(elasticSearchRequest, (Type) Map.class);
      return searchResponse;
    } catch (ElasticsearchException e) {
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
    } catch (SocketTimeoutException e) {
      if (e.getMessage().startsWith("60,000 milliseconds timeout on connection")) {
        throw new AppException(
            HttpServletResponse.SC_GATEWAY_TIMEOUT,
            "Search error",
            String.format("Request timed out after waiting for %sm", REQUEST_TIMEOUT.time()),
            e);
      }
      throw new AppException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Search error",
          "Error processing search request",
          e);
    } catch (IOException e) {
      if (e.getMessage().startsWith("listener timeout after waiting for")) {
        throw new AppException(
            HttpServletResponse.SC_GATEWAY_TIMEOUT,
            "Search error",
            String.format("Request timed out after waiting for %sm", REQUEST_TIMEOUT.time()),
            e);
      } else if (e.getCause() instanceof ContentTooLongException) {
        throw new AppException(
            HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
            "Response is too long",
            "Elasticsearch response is too long, max is 100Mb",
            e);
      }
      throw new AppException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Search error",
          "Error processing search request",
          e);
    } catch (Exception e) {
      throw new AppException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Search error",
          "Error processing search request",
          e);
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

  private int getSearchResponseStatusCode(SearchResponse searchResponse) {
    //        if (searchResponse == null)
    //            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search
    // error", "Search returned null or empty response");
    //        else
    //            return searchResponse.
    return 200;
  }

  abstract SearchRequest createElasticRequest(Query request, String index)
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

  private boolean userHasFullDataAccess() {
    String dataRootUser =
        dpsHeaders
            .getHeaders()
            .getOrDefault(providerHeaderService.getDataRootUserHeader(), "false");
    return Boolean.parseBoolean(dataRootUser);
  }
}
