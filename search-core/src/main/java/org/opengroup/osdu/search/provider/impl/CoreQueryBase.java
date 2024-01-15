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

import com.google.common.base.Strings;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.ContentTooLongException;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AclRole;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.AggregationResponse;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.util.AggregationParserUtil;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.GeoQueryBuilder;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.IPerfLogger;
import org.springframework.beans.factory.annotation.Autowired;

abstract class CoreQueryBase {

    @Inject
    DpsHeaders dpsHeaders;
    @Inject
    private JaxRsDpsLog log;
    @Inject
    private IProviderHeaderService providerHeaderService;
    @Inject
    private CrossTenantUtils crossTenantUtils;
    @Autowired(required = false)
    private IPolicyService iPolicyService;
    @Autowired
    private IQueryParserUtil queryParserUtil;
    @Autowired
    private ISortParserUtil sortParserUtil;
    @Autowired
    private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil;
    @Autowired
    private ElasticLoggingConfig elasticLoggingConfig;
    @Autowired
    private IPerfLogger tracingLogger;
    @Autowired
    private GeoQueryBuilder geoQueryBuilder;

    // if returnedField contains property matching from excludes than query result will NOT include that property
    private final Set<String> excludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.X_ACL.getValue()));

    // queryableExcludes properties can be returned by query results
    private final Set<String> queryableExcludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.INDEX_STATUS.getValue()));

    private final TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    QueryBuilder buildQuery(String simpleQuery, SpatialFilter spatialFilter, boolean asOwner) throws AppException, IOException {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        if (!Strings.isNullOrEmpty(simpleQuery)) {
            QueryBuilder textQueryBuilder = queryParserUtil.buildQueryBuilderFromQueryString(simpleQuery);
            if (textQueryBuilder != null) {
                queryBuilder.must(textQueryBuilder);
            }
        }

        // use only one of the spatial request
        if (spatialFilter != null) {
            QueryBuilder spatialQueryBuilder = this.geoQueryBuilder.getGeoQuery(spatialFilter);
            if (spatialQueryBuilder != null) {
                queryBuilder.filter().add(spatialQueryBuilder);
            }
        }

        if (this.iPolicyService != null) {
            String compiledESPolicy = this.iPolicyService.getCompiledPolicy(providerHeaderService);
            WrapperQueryBuilder wrapperQueryBuilder = new WrapperQueryBuilder(compiledESPolicy);
            return queryBuilder.must(wrapperQueryBuilder);
        } else {
            return getQueryBuilderWithAuthorization(queryBuilder, asOwner);
        }
    }

    private QueryBuilder getQueryBuilderWithAuthorization(BoolQueryBuilder queryBuilder, boolean asOwner) {
        if (userHasFullDataAccess()) {
            return queryBuilder;
        }

        String groups = dpsHeaders.getHeaders().get(providerHeaderService.getDataGroupsHeader());
        if (groups != null) {
            String[] groupArray = groups.trim().split("\\s*,\\s*");
            List<QueryBuilder> authFilterClauses = queryBuilder.filter();
            if (asOwner) {
                authFilterClauses.add(new TermsQueryBuilder(AclRole.OWNERS.getPath(), Arrays.asList(groupArray)));
            } else {
                authFilterClauses.add(new TermsQueryBuilder(RecordMetaAttribute.X_ACL.getValue(), Arrays.asList(groupArray)));
            }
        }
        return queryBuilder;
    }

    String getIndex(Query request) {
        return this.crossTenantUtils.getIndexName(request);
    }

    List<Map<String, Object>> getHitsFromSearchResponse(SearchResponse searchResponse) {
        List<Map<String, Object>> results = new ArrayList<>();
        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.getHits().length != 0) {
            for (SearchHit searchHitFields : searchHits.getHits()) {
                Map<String, Object> hitFields = searchHitFields.getSourceAsMap();
                if (!searchHitFields.getHighlightFields().isEmpty()) {
                    Map<String, List<String>> highlights = new HashMap<>();
                    for (HighlightField hf : searchHitFields.getHighlightFields().values()) {
                        if (!hf.getName().equalsIgnoreCase(RecordMetaAttribute.X_ACL.getValue())) {
                            Text[] fragments = hf.getFragments();
                            highlights.put(
                                hf.getName(), 
                                Arrays.asList(fragments).stream().map(x -> x.toString()).collect(Collectors.toList())
                            );
                        }
                    }
                    hitFields.put("highlight", highlights);
                }
                results.add(hitFields);
            }
            return results;
        }

        return null;
    }

    List<AggregationResponse> getAggregationFromSearchResponse(SearchResponse searchResponse) {
        List<AggregationResponse> results = null;

        if (searchResponse.getAggregations() != null) {
            Terms kindAgg = null;
            ParsedNested nested = searchResponse.getAggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
            if (nested != null) {
                kindAgg = (Terms) getTermsAggregationFromNested(nested);
            } else {
                kindAgg = searchResponse.getAggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME);
            }
            if (kindAgg.getBuckets() != null) {
                results = new ArrayList<>();
                for (Terms.Bucket bucket : kindAgg.getBuckets()) {
                    results.add(AggregationResponse.builder().key(bucket.getKeyAsString()).count(bucket.getDocCount()).build());
                }
            }
        }

        return results;
    }

    private Aggregation getTermsAggregationFromNested(ParsedNested parsedNested) {
        ParsedNested nested = parsedNested.getAggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
        if (nested != null) {
            return getTermsAggregationFromNested(nested);
        } else {
            return parsedNested.getAggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME);
        }
    }

    SearchSourceBuilder createSearchSourceBuilder(Query request) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // build query: set query options and query
        QueryBuilder queryBuilder = buildQuery(request.getQuery(), request.getSpatialFilter(), request.isQueryAsOwner());
        sourceBuilder.size(QueryUtils.getResultSizeForQuery(request.getLimit()));
        sourceBuilder.query(queryBuilder);
        sourceBuilder.timeout(REQUEST_TIMEOUT);
        if (request.isTrackTotalCount()) {
            sourceBuilder.trackTotalHits(request.isTrackTotalCount());
        }

        // set highlighter
        if (!Objects.isNull(request.getHighlightedFields())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder = request.getHighlightedFields().stream().reduce(
                highlightBuilder, (builder, fieldName) -> builder.field(fieldName, 200, 5), (a, b) -> a
            );
            sourceBuilder.highlighter(highlightBuilder);
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

        sourceBuilder.fetchSource(includesArr, excludesArr);
        return sourceBuilder;
    }

    SearchResponse makeSearchRequest(Query searchRequest, RestHighLevelClient client) {
        Long startTime = 0L;
        SearchRequest elasticSearchRequest = null;
        SearchResponse searchResponse = null;
        int statusCode = 0;

        try {
            String index = this.getIndex(searchRequest);
            elasticSearchRequest = createElasticRequest(searchRequest, index);
            if (searchRequest.getSort() != null) {
                List<FieldSortBuilder> sortBuilders = this.sortParserUtil.getSortQuery(client, searchRequest.getSort(), index);
                for (FieldSortBuilder fieldSortBuilder : sortBuilders) {
                    elasticSearchRequest.source().sort(fieldSortBuilder);
                }
            }

            startTime = System.currentTimeMillis();
            searchResponse = client.search(elasticSearchRequest, RequestOptions.DEFAULT);
            statusCode = getSearchResponseStatusCode(searchResponse);
            return searchResponse;
        } catch (ElasticsearchStatusException e) {
            switch (e.status()) {
                case NOT_FOUND:
                    statusCode = e.status().getStatus();
                    throw new AppException(HttpServletResponse.SC_NOT_FOUND, "Not Found", "Resource you are trying to find does not exists", e);
                case BAD_REQUEST:
                    statusCode = e.status().getStatus();
                    throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request", detailedBadRequestMessageUtil.getDetailedBadRequestMessage(elasticSearchRequest, e), e);
                case SERVICE_UNAVAILABLE:
                    statusCode = e.status().getStatus();
                    throw new AppException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Search error", "Please re-try search after some time.", e);
                case TOO_MANY_REQUESTS:
                    statusCode = e.status().getStatus();
                    throw new AppException(429, "Too many requests", "Too many requests, please re-try after some time", e);
                default:
                    statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
            }
        } catch (AppException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            if (e.getMessage().startsWith("60,000 milliseconds timeout on connection")) {
                throw new AppException(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Search error", String.format("Request timed out after waiting for %sm", REQUEST_TIMEOUT.getMinutes()), e);
            }
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
        } catch (IOException e) {
            if (e.getMessage().startsWith("listener timeout after waiting for")) {
                throw new AppException(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Search error", String.format("Request timed out after waiting for %sm", REQUEST_TIMEOUT.getMinutes()), e);
            } else if (e.getCause() instanceof ContentTooLongException) {
                throw new AppException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Response is too long", "Elasticsearch response is too long, max is 100Mb", e);
            }
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
        } catch (Exception e) {
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
        } finally {
            Long latency = System.currentTimeMillis() - startTime;
            if (elasticLoggingConfig.getEnabled() || latency > elasticLoggingConfig.getThreshold()) {
                String request = elasticSearchRequest != null ? elasticSearchRequest.source().toString() : searchRequest.toString();
                this.log.debug(String.format("Elastic request-payload: %s", request));
            }
            this.tracingLogger.log(searchRequest, latency, statusCode);
            this.auditLog(searchRequest, searchResponse);
        }
    }

    private int getSearchResponseStatusCode(SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.status() == null)
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Search returned null or empty response");
        else
            return searchResponse.status().getStatus();
    }

    abstract SearchRequest createElasticRequest(Query request, String index) throws AppException, IOException;

    abstract void querySuccessAuditLogger(Query request);

    abstract void queryFailedAuditLogger(Query request);

    private void auditLog(Query searchRequest, SearchResponse searchResponse) {
        if (searchResponse != null && searchResponse.status() == RestStatus.OK) {
            this.querySuccessAuditLogger(searchRequest);
            return;
        }
        this.queryFailedAuditLogger(searchRequest);
    }

    private boolean userHasFullDataAccess() {
        String dataRootUser = dpsHeaders.getHeaders().getOrDefault(providerHeaderService.getDataRootUserHeader(), "false");
        return Boolean.parseBoolean(dataRootUser);
    }
}
