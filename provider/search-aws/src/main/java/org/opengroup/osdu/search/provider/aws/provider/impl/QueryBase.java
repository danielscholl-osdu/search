/* Copyright © Amazon Web Services

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */

package org.opengroup.osdu.search.provider.aws.provider.impl;

import java.io.IOException;
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
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.builders.GeometryCollectionBuilder;
import org.elasticsearch.common.geo.builders.MultiPointBuilder;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.geometry.Circle;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.AggregationParserUtil;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.GeoQueryBuilder;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.springframework.beans.factory.annotation.Autowired;

import static org.elasticsearch.index.query.QueryBuilders.*;

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
    private GeoQueryBuilder geoQueryBuilder;

    private static final String GEO_SHAPE_INDEXED_TYPE = "geo_shape";
    private static final String ERROR_MSG = "Error processing search request";
    private static final String ERROR_REASON = "Search error";

    private static final int MINIMUM_POLYGON_POINTS_SIZE = 4;

    // if returnedField contains property matching from excludes than query result
    // will NOT include that property
    private final Set<String> excludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.X_ACL.getValue()));

    // queryableExcludes properties can be returned by query results
    private final Set<String> queryableExcludes = new HashSet<>(
            Arrays.asList(RecordMetaAttribute.INDEX_STATUS.getValue()));

    private final TimeValue requestTimeout = TimeValue.timeValueMinutes(1);

    
    private boolean useGeoShapeQuery = false;

    QueryBuilder buildQuery(String simpleQuery, SpatialFilter spatialFilter, boolean asOwner)
            throws AppException, IOException {

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        if (!Strings.isNullOrEmpty(simpleQuery)) {
            QueryBuilder textQueryBuilder = queryParserUtil.buildQueryBuilderFromQueryString(simpleQuery);
            if (textQueryBuilder != null) {
                queryBuilder.must(textQueryBuilder);
            }
        }

        spatialQueryBuilder = getSpeciaQueryBuilderHelper(spatialFilter);

        queryBuilder = prefixQuery("id", String.format("%s:", this.dpsHeaders.getPartitionId()));

        if (textQueryBuilder != null) {
            queryBuilder = queryBuilder != null ? boolQuery().must(queryBuilder).must(textQueryBuilder)
                    : boolQuery().must(textQueryBuilder);
        }
        if (spatialQueryBuilder != null) {
            queryBuilder = queryBuilder != null ? boolQuery().must(queryBuilder).must(spatialQueryBuilder)
                    : boolQuery().must(spatialQueryBuilder);
        }

        if (this.iPolicyService != null) {
            String compiledESPolicy = this.iPolicyService.getCompiledPolicy(providerHeaderService);
            WrapperQueryBuilder wrapperQueryBuilder = new WrapperQueryBuilder(compiledESPolicy);
            return queryBuilder != null ? boolQuery().must(queryBuilder).must(wrapperQueryBuilder)
                    : boolQuery().must(wrapperQueryBuilder);
        }
        return getQueryBuilderWithAuthorization(queryBuilder, asOwner);
    }

    private QueryBuilder geoShapeQueryBuilder(SpatialFilter spatialFilter) throws AppException, IOException {
        if (spatialFilter.getByBoundingBox() != null)
            return getGeoShapeBoundingBoxQuery(spatialFilter);
        if (spatialFilter.getByDistance() != null)
            return getGeoShapeDistanceQuery(spatialFilter);
        if (spatialFilter.getByGeoPolygon() != null)
            return getGeoShapePolygonQuery(spatialFilter);
        if (spatialFilter.getByIntersection() != null)
            return getGeoShapeIntersectionQuery(spatialFilter);
        if (spatialFilter.getByWithinPolygon() != null)
            return getWithinPolygonQuery(spatialFilter);
        return null;
    }

    private QueryBuilder nonGeoShapeQueryBuilder(SpatialFilter spatialFilter) throws AppException, IOException {
        if (spatialFilter.getByBoundingBox() != null)
            return getBoundingBoxQuery(spatialFilter);
        if (spatialFilter.getByDistance() != null)
            return getDistanceQuery(spatialFilter);
        if (spatialFilter.getByGeoPolygon() != null)
            return getGeoPolygonQuery(spatialFilter);
        if (spatialFilter.getByIntersection() != null)
            return getGeoShapeIntersectionQuery(spatialFilter);
        return null;
    }
    
    private QueryBuilder getSpeciaQueryBuilderHelper(SpatialFilter spatialFilter) throws AppException, IOException {
        // use only one of the spatial request
        if (spatialFilter == null)
            return null;

        if (useGeoShapeQuery) {
            return geoShapeQueryBuilder(spatialFilter);
        }
        return nonGeoShapeQueryBuilder(spatialFilter);

    }

    private QueryBuilder getWithinPolygonQuery(SpatialFilter spatialFilter) throws IOException {
        MultiPointBuilder multiPointBuilder = new MultiPointBuilder();
        for (Point point : spatialFilter.getByWithinPolygon().getPoints()) {
            multiPointBuilder.coordinate(
                new Coordinate(point.getLongitude(), point.getLatitude()));
        }
        GeometryCollectionBuilder geometryCollection = new GeometryCollectionBuilder();
        geometryCollection.multiPoint(multiPointBuilder);
        // geoWithinQuery doesn't work with a polygon as the field to search on
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection.buildGeometry()).ignoreUnmapped(true);
    }

    private QueryBuilder getQueryBuilderWithAuthorization(QueryBuilder queryBuilder, boolean asOwner) {
        if (userHasFullDataAccess()) {
            return queryBuilder;
        }

        QueryBuilder authorizationQueryBuilder = null;
        String groups = dpsHeaders.getHeaders().get(providerHeaderService.getDataGroupsHeader());
        if (groups != null) {
            String[] groupArray = groups.trim().split("\\s*,\\s*");

            authorizationQueryBuilder = asOwner ? boolQuery().minimumShouldMatch("1")
                        .should(termsQuery(AclRole.OWNERS.getPath(), groupArray)) : boolQuery().minimumShouldMatch("1")
                        .should(termsQuery(RecordMetaAttribute.X_ACL.getValue(), groupArray));
        }
        if (authorizationQueryBuilder != null) {
            queryBuilder = queryBuilder != null ? boolQuery().must(queryBuilder).must(authorizationQueryBuilder)
                    : boolQuery().must(authorizationQueryBuilder);
        }
        return queryBuilder;
    }

    private QueryBuilder getBoundingBoxQuery(SpatialFilter spatialFilter) throws AppException {

        GeoPoint topLeft = new GeoPoint(spatialFilter.getByBoundingBox().getTopLeft().getLatitude(),
                spatialFilter.getByBoundingBox().getTopLeft().getLongitude());
        GeoPoint bottomRight = new GeoPoint(spatialFilter.getByBoundingBox().getBottomRight().getLatitude(),
                spatialFilter.getByBoundingBox().getBottomRight().getLongitude());
        return geoBoundingBoxQuery(spatialFilter.getField()).setCorners(topLeft, bottomRight).ignoreUnmapped(true);
    }

    private QueryBuilder getDistanceQuery(SpatialFilter spatialFilter) throws AppException {

        return geoDistanceQuery(spatialFilter.getField())
                .point(spatialFilter.getByDistance().getPoint().getLatitude(),
                        spatialFilter.getByDistance().getPoint().getLongitude())
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
        Geometry geometry = new PolygonBuilder(cb).buildGeometry();
        return geoWithinQuery(spatialFilter.getField(), geometry).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoShapeBoundingBoxQuery(SpatialFilter spatialFilter) throws IOException {

        Coordinate topLeft = new Coordinate(spatialFilter.getByBoundingBox().getTopLeft().getLongitude(),
                spatialFilter.getByBoundingBox().getTopLeft().getLatitude());
        Coordinate bottomRight = new Coordinate(spatialFilter.getByBoundingBox().getBottomRight().getLongitude(),
                spatialFilter.getByBoundingBox().getBottomRight().getLatitude());
        Rectangle rectangle = new EnvelopeBuilder(topLeft, bottomRight).buildGeometry();
        return geoWithinQuery(spatialFilter.getField(), rectangle).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoShapeIntersectionQuery(SpatialFilter spatialFilter) throws IOException {
        MultiPolygonBuilder multiPolygonBuilder = new MultiPolygonBuilder();
        for (Polygon polygon : spatialFilter.getByIntersection().getPolygons()) {
            List<Coordinate> coordinates = new ArrayList<>();
            for (Point point : polygon.getPoints()) {
                coordinates.add(new Coordinate(point.getLongitude(), point.getLatitude()));
            }

            if (coordinates.size() < MINIMUM_POLYGON_POINTS_SIZE ||
                    (coordinates.get(0).x != coordinates.get(coordinates.size() - 1).x
                            || coordinates.get(0).y != coordinates.get(coordinates.size() - 1).y)) {
                throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                        String.format(
                                "Polygons must have at least %s points and the first point must match the last point",
                                MINIMUM_POLYGON_POINTS_SIZE));
            }

            CoordinatesBuilder cb = new CoordinatesBuilder().coordinates(coordinates);
            multiPolygonBuilder.polygon(new PolygonBuilder(cb));
        }

        GeometryCollectionBuilder geometryCollection = new GeometryCollectionBuilder();
        geometryCollection.shape(multiPolygonBuilder);
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection.buildGeometry()).ignoreUnmapped(true);
    }

    private QueryBuilder getWithinPolygonQuery(SpatialFilter spatialFilter) throws IOException {
        MultiPointBuilder multiPointBuilder = new MultiPointBuilder();
        for (Point point : spatialFilter.getByWithinPolygon().getPoints()) {
            multiPointBuilder.coordinate(new Coordinate(point.getLongitude(), point.getLatitude()));
        }

        GeometryCollectionBuilder geometryCollection = new GeometryCollectionBuilder();
        geometryCollection.multiPoint(multiPointBuilder);
        // geoWithinQuery doesn't work with a polygon as the field to search on
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection.buildGeometry()).ignoreUnmapped(true);
    }

    private QueryBuilder getGeoShapeDistanceQuery(SpatialFilter spatialFilter) throws IOException {

        Coordinate center = new Coordinate(spatialFilter.getByDistance().getPoint().getLongitude(),
                spatialFilter.getByDistance().getPoint().getLatitude());
        Circle circleBuilder = new CircleBuilder().center(center)
                .radius(spatialFilter.getByDistance().getDistance(), DistanceUnit.METERS).buildGeometry();
        return geoWithinQuery(spatialFilter.getField(), circleBuilder).ignoreUnmapped(true);
    }

    String getIndex(Query request) {
        return this.crossTenantUtils.getIndexName(request);
    }

    List<Map<String, Object>> getHitsFromSearchResponse(SearchResponse searchResponse) {
      
        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.getHits().length == 0)
            return null;
        return getSearchResults(searchHits);
    }
    
    List<Map<String, Object>> getSearchResults(SearchHits searchHits) {
        List<Map<String, Object>> results = new ArrayList<>();
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

    List<AggregationResponse> getAggregationFromSearchResponse(SearchResponse searchResponse) {
        if (searchResponse.getAggregations() == null)
            return null;

        Terms kindAgg = null;
        ParsedNested nested = searchResponse.getAggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
        kindAgg = (nested != null) ? (Terms) getTermsAggregationFromNested(nested)
                : searchResponse.getAggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME);

        if (kindAgg.getBuckets() == null)
            return null;

        List<AggregationResponse> results = new ArrayList<>();
        for (Terms.Bucket bucket : kindAgg.getBuckets()) {
            results.add(
                    AggregationResponse.builder().key(bucket.getKeyAsString()).count(bucket.getDocCount()).build());
        }
        return results;
    }

    private Aggregation getTermsAggregationFromNested(ParsedNested parsedNested) {
        ParsedNested nested = parsedNested.getAggregations().get(AggregationParserUtil.NESTED_AGGREGATION_NAME);
        if (nested != null)
            return getTermsAggregationFromNested(nested);
        return parsedNested.getAggregations().get(AggregationParserUtil.TERM_AGGREGATION_NAME);
    }

    SearchSourceBuilder createSearchSourceBuilder(Query request) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // build query: set query options and query
        QueryBuilder queryBuilder = buildQuery(request.getQuery(), request.getSpatialFilter(),
                request.isQueryAsOwner());
        sourceBuilder.size(QueryUtils.getResultSizeForQuery(request.getLimit()));
        sourceBuilder.query(queryBuilder);
        sourceBuilder.timeout(requestTimeout);
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
        try {
            String index = this.getIndex(searchRequest);
            elasticSearchRequest = createElasticRequest(searchRequest);
            if (searchRequest.getSort() != null) {
                List<FieldSortBuilder> sortBuilders = this.sortParserUtil.getSortQuery(client, searchRequest.getSort(),
                        index);
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
                    throw new AppException(HttpServletResponse.SC_NOT_FOUND, "Not Found",
                            "Resource you are trying to find does not exists", e);
                case BAD_REQUEST:
                    throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                            detailedBadRequestMessageUtil.getDetailedBadRequestMessage(elasticSearchRequest, e), e);
                case SERVICE_UNAVAILABLE:
                    throw new AppException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, ERROR_REASON,
                            "Please re-try search after some time.", e);
                default:
                    throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_REASON,
                            ERROR_MSG, e);
            }
        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            if (e.getMessage().startsWith("listener timeout after waiting for")) {
                throw new AppException(HttpServletResponse.SC_GATEWAY_TIMEOUT, ERROR_REASON,
                        String.format("Request timed out after waiting for %sm", requestTimeout.getMinutes()), e);
            }
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_REASON,
                    ERROR_MSG, e);
        } catch (Exception e) {
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_REASON,
                    ERROR_MSG, e);
        } finally {
            Long latency = System.currentTimeMillis() - startTime;
            String request = elasticSearchRequest != null ? elasticSearchRequest.source().toString()
                    : searchRequest.toString();
            this.log.info(String.format("elastic latency: %s | elastic request-payload: %s", latency, request));
            this.auditLog(searchRequest, searchResponse);
        }
    }

    private boolean useGeoShapeQuery(RestHighLevelClient client, Query searchRequest, String index) throws IOException {
        Set<String> indexedTypes = this.fieldMappingTypeService.getFieldTypes(client,
                searchRequest.getSpatialFilter().getField(), index);
        // fallback to geo_point search if mixed type found for spatialFilter.field
        if (indexedTypes.isEmpty() || indexedTypes.size() > 1)
            return false;
        return indexedTypes.contains(GEO_SHAPE_INDEXED_TYPE);
    }

    abstract SearchRequest createElasticRequest(Query request) throws AppException, IOException;

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
        String dataRootUser = dpsHeaders.getHeaders().getOrDefault(providerHeaderService.getDataRootUserHeader(),
                "false");
        return Boolean.parseBoolean(dataRootUser);
    }
}
