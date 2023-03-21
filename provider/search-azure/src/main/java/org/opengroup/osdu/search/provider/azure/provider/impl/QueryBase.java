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

package org.opengroup.osdu.search.provider.azure.provider.impl;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.CoordinatesBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.GeometryCollectionBuilder;
import org.elasticsearch.common.geo.builders.MultiPolygonBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
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
import org.locationtech.jts.geom.Coordinate;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AclRole;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.AggregationResponse;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.provider.azure.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.provider.azure.utils.DependencyLogger;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.AggregationParserUtil;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.index.query.QueryBuilders.geoBoundingBoxQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoDistanceQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoIntersectionQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoPolygonQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoWithinQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.opengroup.osdu.search.provider.azure.utils.DependencyLogger.CURSOR_QUERY_DEPENDENCY_NAME;
import static org.opengroup.osdu.search.provider.azure.utils.DependencyLogger.QUERY_DEPENDENCY_NAME;

abstract class QueryBase {

    @Inject
    DpsHeaders dpsHeaders;
    @Inject
    private JaxRsDpsLog log;
    @Inject
    private IProviderHeaderService providerHeaderService;
    @Inject
    private CrossTenantUtils crossTenantUtils;
    @Inject
    private IFieldMappingTypeService fieldMappingTypeService;
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
    @Qualifier("azureUtilsDependencyLogger")
    private DependencyLogger dependencyLogger;

    private static final String GEO_SHAPE_INDEXED_TYPE = "geo_shape";
    private static final int MINIMUM_POLYGON_POINTS_SIZE = 4;

    // if returnedField contains property matching from excludes than query result will NOT include that property
    private final Set<String> excludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.X_ACL.getValue()));

    // queryableExcludes properties can be returned by query results
    private final Set<String> queryableExcludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.INDEX_STATUS.getValue()));

    private final TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    private boolean useGeoShapeQuery = false;

    QueryBuilder buildQuery(String simpleQuery, SpatialFilter spatialFilter, boolean asOwner) throws AppException, IOException {

        QueryBuilder textQueryBuilder = null;
        QueryBuilder spatialQueryBuilder = null;
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        if (!Strings.isNullOrEmpty(simpleQuery)) {
            textQueryBuilder = queryParserUtil.buildQueryBuilderFromQueryString(simpleQuery);
        }

        // use only one of the spatial request
        if (spatialFilter != null) {
            if (useGeoShapeQuery) {
                if (spatialFilter.getByBoundingBox() != null) {
                    spatialQueryBuilder = getGeoShapeBoundingBoxQuery(spatialFilter);
                } else if (spatialFilter.getByDistance() != null) {
                    spatialQueryBuilder = getGeoShapeDistanceQuery(spatialFilter);
                } else if (spatialFilter.getByGeoPolygon() != null) {
                    spatialQueryBuilder = getGeoShapePolygonQuery(spatialFilter);
                } else if (spatialFilter.getByIntersection() != null) {
                    spatialQueryBuilder = getGeoShapeIntersectionQuery(spatialFilter);
                }
            } else {
                if (spatialFilter.getByBoundingBox() != null) {
                    spatialQueryBuilder = getBoundingBoxQuery(spatialFilter);
                } else if (spatialFilter.getByDistance() != null) {
                    spatialQueryBuilder = getDistanceQuery(spatialFilter);
                } else if (spatialFilter.getByGeoPolygon() != null) {
                    spatialQueryBuilder = getGeoPolygonQuery(spatialFilter);
                } else if (spatialFilter.getByIntersection() != null) {
                    spatialQueryBuilder = getGeoShapeIntersectionQuery(spatialFilter);
                }
            }
        }

        if (textQueryBuilder != null) {
            queryBuilder.must(textQueryBuilder);
        }
        if (spatialQueryBuilder != null) {
            queryBuilder.must(spatialQueryBuilder);
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

    private QueryBuilder getSimpleQuery(String searchQuery) {

        // if query is empty , then put *
        String query = StringUtils.isNotBlank(searchQuery) ? searchQuery : "*";
        return queryStringQuery(query).allowLeadingWildcard(false);
    }

    private QueryBuilder getBoundingBoxQuery(SpatialFilter spatialFilter) throws AppException {

        GeoPoint topLeft = new GeoPoint(spatialFilter.getByBoundingBox().getTopLeft().getLatitude(), spatialFilter.getByBoundingBox().getTopLeft().getLongitude());
        GeoPoint bottomRight = new GeoPoint(spatialFilter.getByBoundingBox().getBottomRight().getLatitude(), spatialFilter.getByBoundingBox().getBottomRight().getLongitude());
        return geoBoundingBoxQuery(spatialFilter.getField()).setCorners(topLeft, bottomRight).ignoreUnmapped(true);
    }

    private QueryBuilder getDistanceQuery(SpatialFilter spatialFilter) throws AppException {

        return geoDistanceQuery(spatialFilter.getField())
                .point(spatialFilter.getByDistance().getPoint().getLatitude(), spatialFilter.getByDistance().getPoint().getLongitude())
                .distance(spatialFilter.getByDistance().getDistance(), DistanceUnit.METERS).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoPolygonQuery(SpatialFilter spatialFilter) throws AppException {

        List<GeoPoint> points = new ArrayList<>();
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new GeoPoint(point.getLatitude(), point.getLongitude()));
        }
        return geoPolygonQuery(spatialFilter.getField(), points).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoShapePolygonQuery(SpatialFilter spatialFilter) throws IOException {

        List<Coordinate> points = new ArrayList<>();
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new Coordinate(point.getLongitude(), point.getLatitude()));
        }
        CoordinatesBuilder cb = new CoordinatesBuilder().coordinates(points);
        return geoWithinQuery(spatialFilter.getField(), new PolygonBuilder(cb)).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoShapeBoundingBoxQuery(SpatialFilter spatialFilter) throws IOException {

        Coordinate topLeft = new Coordinate(spatialFilter.getByBoundingBox().getTopLeft().getLongitude(), spatialFilter.getByBoundingBox().getTopLeft().getLatitude());
        Coordinate bottomRight = new Coordinate(spatialFilter.getByBoundingBox().getBottomRight().getLongitude(), spatialFilter.getByBoundingBox().getBottomRight().getLatitude());
        return geoWithinQuery(spatialFilter.getField(), new EnvelopeBuilder(topLeft, bottomRight)).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoShapeDistanceQuery(SpatialFilter spatialFilter) throws IOException {
        Coordinate center = new Coordinate(spatialFilter.getByDistance().getPoint().getLongitude(), spatialFilter.getByDistance().getPoint().getLatitude());
        CircleBuilder circleBuilder = new CircleBuilder().center(center).radius(spatialFilter.getByDistance().getDistance(), DistanceUnit.METERS);
        return geoWithinQuery(spatialFilter.getField(), circleBuilder).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoShapeIntersectionQuery(SpatialFilter spatialFilter) throws IOException {
        MultiPolygonBuilder multiPolygonBuilder = new MultiPolygonBuilder();
        for (Polygon polygon : spatialFilter.getByIntersection().getPolygons()) {
            List<Coordinate> coordinates = new ArrayList<>();
            for (Point point : polygon.getPoints()) {
                coordinates.add(new Coordinate(point.getLongitude(), point.getLatitude()));
            }

            checkPolygon(coordinates);

            CoordinatesBuilder cb = new CoordinatesBuilder().coordinates(coordinates);
            multiPolygonBuilder.polygon(new PolygonBuilder(cb));
        }

        GeometryCollectionBuilder geometryCollection = new GeometryCollectionBuilder();
        geometryCollection.shape(multiPolygonBuilder);
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection.buildGeometry()).ignoreUnmapped(true);
    }

    private void checkPolygon(List<Coordinate> coordinates) {
        if (coordinates.size() < MINIMUM_POLYGON_POINTS_SIZE ||
                (
                        coordinates.get(0).x != coordinates.get(coordinates.size() - 1).x
                                || coordinates.get(0).y != coordinates.get(coordinates.size() - 1).y
                )
        ) {
            throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                    String.format(
                            "Polygons must have at least %s points and the first point must match the last point",
                            MINIMUM_POLYGON_POINTS_SIZE));
        }
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
                    for (HighlightField hf : searchHitFields.getHighlightFields().values()) {
                        if (!hf.getName().equalsIgnoreCase(RecordMetaAttribute.X_ACL.getValue())) {
                            Text[] fragments = hf.getFragments();
                            if (fragments.length > 0) {
                                hitFields.put(hf.getName(), fragments[0].toString());
                            }
                        }
                    }
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
        if (request.isReturnHighlightedFields()) {
            HighlightBuilder highlightBuilder = new HighlightBuilder().field("*", 200, 5);
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
            if (searchRequest.getSpatialFilter() != null) {
                useGeoShapeQuery = this.useGeoShapeQuery(client, searchRequest, index);
            }
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
            String dependencyName = searchRequest instanceof QueryRequest ? QUERY_DEPENDENCY_NAME : CURSOR_QUERY_DEPENDENCY_NAME;
            dependencyLogger.logDependency(dependencyName, searchRequest.getQuery(), String.valueOf(searchRequest.getKind()), latency, statusCode, statusCode == HttpStatus.SC_OK);
            this.auditLog(searchRequest, searchResponse);
        }
    }

    private int getSearchResponseStatusCode(SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.status() == null)
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Search returned null or empty response");
        else
            return searchResponse.status().getStatus();
    }

    private boolean useGeoShapeQuery(RestHighLevelClient client, Query searchRequest, String index) throws IOException {
        Set<String> indexedTypes = this.fieldMappingTypeService.getFieldTypes(client, searchRequest.getSpatialFilter().getField(), index);
        // fallback to geo_point search if mixed type found for spatialFilter.field
        if (indexedTypes.isEmpty() || indexedTypes.size() > 1) return false;
        return indexedTypes.contains(GEO_SHAPE_INDEXED_TYPE);
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
