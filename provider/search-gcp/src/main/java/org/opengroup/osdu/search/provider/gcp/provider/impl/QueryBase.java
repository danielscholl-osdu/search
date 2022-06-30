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

package org.opengroup.osdu.search.provider.gcp.provider.impl;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoBoundingBoxQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoDistanceQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoIntersectionQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoPolygonQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoWithinQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
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
import org.elasticsearch.common.geo.builders.MultiPointBuilder;
import org.elasticsearch.common.geo.builders.MultiPolygonBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.index.query.QueryBuilder;
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
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.policy.service.PartitionPolicyStatusService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.AggregationParserUtil;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.springframework.beans.factory.annotation.Autowired;

abstract class QueryBase {

    public static final String SEARCH_ERROR_MSG = "Search error";
    public static final String ERROR_PROCESSING_SEARCH_REQUEST_MSG = "Error processing search request";
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
    @Inject
    private PartitionPolicyStatusService statusService;
    @Autowired
    private IQueryParserUtil queryParserUtil;
    @Autowired
    private ISortParserUtil sortParserUtil;
    @Autowired
    private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil;

    private static final String GEO_SHAPE_INDEXED_TYPE = "geo_shape";
    private static final int MINIMUM_POLYGON_POINTS_SIZE = 4;

    // if returnedField contains property matching from excludes than query result will NOT include that property
    private final Set<String> excludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.X_ACL.getValue()));

    // queryableExcludes properties can be returned by query results
    private final Set<String> queryableExcludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.INDEX_STATUS.getValue()));

    private final TimeValue requestTimeout = TimeValue.timeValueMinutes(1);

    private boolean useGeoShapeQuery = false;

    QueryBuilder buildQuery(String simpleQuery, SpatialFilter spatialFilter, boolean asOwner) throws IOException {
        QueryBuilder textQueryBuilder = null;
        QueryBuilder spatialQueryBuilder = null;
        QueryBuilder queryBuilder = null;

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
                } else if (spatialFilter.getByWithinPolygon() != null) {
                    spatialQueryBuilder = getWithinPolygonQuery(spatialFilter);
                }
            } else {
                if (spatialFilter.getByBoundingBox() != null) {
                    spatialQueryBuilder = getBoundingBoxQuery(spatialFilter);
                } else if (spatialFilter.getByDistance() != null) {
                    spatialQueryBuilder = getDistanceQuery(spatialFilter);
                } else if (spatialFilter.getByGeoPolygon() != null) {
                    spatialQueryBuilder = getGeoPolygonQuery(spatialFilter);
                }
            }
        }

        if (textQueryBuilder != null) {
            queryBuilder = boolQuery().must(textQueryBuilder);
        }
        if (spatialQueryBuilder != null) {
            queryBuilder = queryBuilder != null ? boolQuery().must(queryBuilder).must(spatialQueryBuilder) : boolQuery().must(spatialQueryBuilder);
        }

        if(this.iPolicyService != null && this.statusService.policyEnabled(this.dpsHeaders.getPartitionId())) {
            return queryBuilder;
        } else {
            return getQueryBuilderWithAuthorization(queryBuilder, asOwner);
        }
    }

    private QueryBuilder getQueryBuilderWithAuthorization(QueryBuilder queryBuilder, boolean asOwner) {
        QueryBuilder authorizationQueryBuilder = null;
        // apply authorization filters
        //bypass for BYOC implementation only.
        String groups = dpsHeaders.getHeaders().get(providerHeaderService.getDataGroupsHeader());
        if (groups != null) {
            String[] groupArray = groups.trim().split("\\s*,\\s*");
            if (asOwner) {
                authorizationQueryBuilder = boolQuery().minimumShouldMatch("1").should(termsQuery(
                        AclRole.OWNERS.getPath(), groupArray));
            } else {
                authorizationQueryBuilder = boolQuery().minimumShouldMatch("1").should(termsQuery(RecordMetaAttribute.X_ACL.getValue(), groupArray));
            }
        }
        if (authorizationQueryBuilder != null) {
            queryBuilder = queryBuilder != null ? boolQuery().must(queryBuilder).must(authorizationQueryBuilder) : boolQuery().must(authorizationQueryBuilder);
        }
        return queryBuilder;
    }

    private QueryBuilder getSimpleQuery(String searchQuery) {

        // if query is empty , then put *
        String query = StringUtils.isNotBlank(searchQuery) ? searchQuery : "*";
        return queryStringQuery(query).allowLeadingWildcard(false);
    }

    private QueryBuilder getBoundingBoxQuery(SpatialFilter spatialFilter) {

        GeoPoint topLeft = new GeoPoint(spatialFilter.getByBoundingBox().getTopLeft().getLatitude(), spatialFilter.getByBoundingBox().getTopLeft().getLongitude());
        GeoPoint bottomRight = new GeoPoint(spatialFilter.getByBoundingBox().getBottomRight().getLatitude(), spatialFilter.getByBoundingBox().getBottomRight().getLongitude());
        return geoBoundingBoxQuery(spatialFilter.getField()).setCorners(topLeft, bottomRight);
    }

    private QueryBuilder getDistanceQuery(SpatialFilter spatialFilter) {

        return geoDistanceQuery(spatialFilter.getField())
                .point(spatialFilter.getByDistance().getPoint().getLatitude(), spatialFilter.getByDistance().getPoint().getLongitude())
                .distance(spatialFilter.getByDistance().getDistance(), DistanceUnit.METERS);
    }

    private QueryBuilder getGeoPolygonQuery(SpatialFilter spatialFilter) {

        List<GeoPoint> points = new ArrayList<>();
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new GeoPoint(point.getLatitude(), point.getLongitude()));
        }
        return geoPolygonQuery(spatialFilter.getField(), points);
    }

    private QueryBuilder getGeoShapePolygonQuery(SpatialFilter spatialFilter) throws IOException {

        List<Coordinate> points = new ArrayList<>();
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new Coordinate(point.getLongitude(), point.getLatitude()));
        }
        CoordinatesBuilder cb = new CoordinatesBuilder().coordinates(points);
        Geometry geometry = new PolygonBuilder(cb).buildGeometry();
        return geoWithinQuery(spatialFilter.getField(), geometry);
    }

    private QueryBuilder getGeoShapeBoundingBoxQuery(SpatialFilter spatialFilter) throws IOException {

        Coordinate topLeft = new Coordinate(spatialFilter.getByBoundingBox().getTopLeft().getLongitude(), spatialFilter.getByBoundingBox().getTopLeft().getLatitude());
        Coordinate bottomRight = new Coordinate(spatialFilter.getByBoundingBox().getBottomRight().getLongitude(), spatialFilter.getByBoundingBox().getBottomRight().getLatitude());
        Rectangle rectangle = new EnvelopeBuilder(topLeft, bottomRight).buildGeometry();
        return geoWithinQuery(spatialFilter.getField(), rectangle);
    }

    private QueryBuilder getGeoShapeDistanceQuery(SpatialFilter spatialFilter) throws IOException {

        Coordinate center = new Coordinate(spatialFilter.getByDistance().getPoint().getLongitude(), spatialFilter.getByDistance().getPoint().getLatitude());
        CircleBuilder circleBuilder = new CircleBuilder().center(center).radius(spatialFilter.getByDistance().getDistance(), DistanceUnit.METERS);
        return geoWithinQuery(spatialFilter.getField(), circleBuilder);
    }

    private QueryBuilder getGeoShapeIntersectionQuery(SpatialFilter spatialFilter)
        throws IOException {
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
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection.buildGeometry());
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

    private QueryBuilder getWithinPolygonQuery(SpatialFilter spatialFilter) throws IOException {
        MultiPointBuilder multiPointBuilder = new MultiPointBuilder();
        for (Point point : spatialFilter.getByWithinPolygon().getPoints()) {
            multiPointBuilder.coordinate(new Coordinate(point.getLongitude(), point.getLatitude()));
        }

        GeometryCollectionBuilder geometryCollection = new GeometryCollectionBuilder();
        geometryCollection.multiPoint(multiPointBuilder);
        // geoWithinQuery doesn't work with a polygon as the field to search on
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection.buildGeometry());
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
            if(nested != null){
                kindAgg = (Terms) getTermsAggregationFromNested(nested);
            }else {
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

    private Aggregation getTermsAggregationFromNested(ParsedNested parsedNested){
        ParsedNested nested = parsedNested.getAggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
        if(nested != null){
            return getTermsAggregationFromNested(nested);
        }else {
            return parsedNested.getAggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME);
        }
    }

    SearchSourceBuilder createSearchSourceBuilder(Query request) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // build query: set query options and query
        QueryBuilder queryBuilder = buildQuery(request.getQuery(), request.getSpatialFilter(), request.isQueryAsOwner());
        sourceBuilder.size(QueryUtils.getResultSizeForQuery(request.getLimit()));
        sourceBuilder.query(queryBuilder);
        sourceBuilder.timeout(requestTimeout);
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
        try {
            String index = this.getIndex(searchRequest);
            if (searchRequest.getSpatialFilter() != null) {
                useGeoShapeQuery = this.useGeoShapeQuery(client, searchRequest, index);
            }
            elasticSearchRequest = createElasticRequest(searchRequest);
            if (searchRequest.getSort() != null) {
                List<FieldSortBuilder> sortBuilders = this.sortParserUtil.getSortQuery(client, searchRequest.getSort(), index);
                for (FieldSortBuilder fieldSortBuilder : sortBuilders) {
                    elasticSearchRequest.source().sort(fieldSortBuilder);
                }
            }

            startTime = System.currentTimeMillis();
            searchResponse = client.search(elasticSearchRequest, RequestOptions.DEFAULT);
            return searchResponse;
        } catch (ElasticsearchStatusException e) {
            switch (e.status()) {
                case NOT_FOUND:
                    throw new AppException(HttpServletResponse.SC_NOT_FOUND, "Not Found", "Resource you are trying to find does not exists", e);
                case BAD_REQUEST:
                    throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request", detailedBadRequestMessageUtil.getDetailedBadRequestMessage(elasticSearchRequest, e), e);
                case SERVICE_UNAVAILABLE:
                    throw new AppException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, SEARCH_ERROR_MSG, "Please re-try search after some time.", e);
                default:
                    throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SEARCH_ERROR_MSG, ERROR_PROCESSING_SEARCH_REQUEST_MSG, e);
            }
        } catch (AppException e){
            throw e;
        } catch (IOException e) {
            if (e.getMessage().startsWith("listener timeout after waiting for")) {
                throw new AppException(HttpServletResponse.SC_GATEWAY_TIMEOUT, SEARCH_ERROR_MSG, String.format("Request timed out after waiting for %sm", requestTimeout.getMinutes()), e);
            }
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SEARCH_ERROR_MSG, ERROR_PROCESSING_SEARCH_REQUEST_MSG, e);
        } catch (Exception e) {
            if(e instanceof java.net.SocketTimeoutException){
                throw new AppException(HttpServletResponse.SC_REQUEST_TIMEOUT, SEARCH_ERROR_MSG, String.format("Request timed out after waiting for %sm", requestTimeout.getMinutes()), e);
            }
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SEARCH_ERROR_MSG, ERROR_PROCESSING_SEARCH_REQUEST_MSG, e);
        } finally {
            Long latency = System.currentTimeMillis() - startTime;
            String request = elasticSearchRequest != null ? elasticSearchRequest.source().toString() : searchRequest.toString();
            this.log.info(String.format("elastic latency: %s | elastic request-payload: %s", latency, request));
            this.auditLog(searchRequest, searchResponse);
        }
    }

    private boolean useGeoShapeQuery(RestHighLevelClient client, Query searchRequest, String index) throws IOException {
        Set<String> indexedTypes = this.fieldMappingTypeService.getFieldTypes(client, searchRequest.getSpatialFilter().getField(), index);
        // fallback to geo_point search if mixed type found for spatialFilter.field
        if (indexedTypes.isEmpty() || indexedTypes.size() > 1) return false;
        return indexedTypes.contains(GEO_SHAPE_INDEXED_TYPE);
    }

    abstract SearchRequest createElasticRequest(Query request) throws IOException;

    abstract void querySuccessAuditLogger(Query request);

    abstract void queryFailedAuditLogger(Query request);

    private void auditLog(Query searchRequest, SearchResponse searchResponse) {
        if (searchResponse != null && searchResponse.status() == RestStatus.OK) {
            this.querySuccessAuditLogger(searchRequest);
            return;
        }
        this.queryFailedAuditLogger(searchRequest);
    }
}