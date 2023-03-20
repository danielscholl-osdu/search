//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.provider.azure.provider.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.azure.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.provider.azure.utils.DependencyLogger;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.FieldMappingTypeService;
import org.opengroup.osdu.search.util.AggregationParserUtil;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.DetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IAggregationParserUtil;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.QueryParserUtil;
import org.opengroup.osdu.search.util.SortParserUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QueryServiceImplTest {
    private final String DATA_GROUPS = "X-Data-Groups";
    private final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";
    private final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";

    private static final String dataPartitionId = "data-partition-id";
    private static final String fieldName = "field";
    private static final String indexName = "index";
    private static final String name = "name";
    private static final String text = "text";
    private static final String GEO_SHAPE = "geo_shape";
    private static final String GEO_DISTANCE = "geo_distance";
    private static final String GEO_POLYGON = "geo_polygon";
    private static final double DELTA = 1e-6;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Point dummyPoint = getPoint(0.0, 0.0);
    private final List<Point> polygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0));
    private final List<Point> closedPolygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0), getPoint(0.0, 0.0));
    private final Point topLeft = getPoint(3.0, 4.0);
    private final Point bottomRight = getPoint(2.0, 1.0);

    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private QueryRequest searchRequest;

    @Mock
    private RestHighLevelClient client;

    @Mock
    private SpatialFilter spatialFilter;

    @Mock
    private SearchResponse searchResponse;

    @Mock
    private SearchHits searchHits;

    @Mock
    private SearchHit searchHit;

    @Mock
    private ElasticClientHandler elasticClientHandler;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    DpsHeaders dpsHeaders;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private CrossTenantUtils crossTenantUtils;

    @Mock
    private IProviderHeaderService providerHeaderService;

    @Mock
    private FieldMappingTypeService fieldMappingTypeService;

    @Spy
    private SearchConfigurationProperties properties = new SearchConfigurationProperties();

    @Spy
    private IQueryParserUtil parserService = new QueryParserUtil();

    @Spy
    private ISortParserUtil sortParserUtil = new SortParserUtil();

    @Spy
    private IAggregationParserUtil aggregationParserUtil = new AggregationParserUtil(properties);

    @Spy
    private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil = new DetailedBadRequestMessageUtil(objectMapper);

    @Mock
    private ElasticLoggingConfig elasticLoggingConfig;

    @Mock
    private DependencyLogger dependencyLogger;

    @InjectMocks
    private QueryServiceImpl sut;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.openMocks(this);
        Map<String, Object> hitFields = new HashMap<>();

        doReturn(indexName).when(crossTenantUtils).getIndexName(any());
        doReturn(client).when(elasticClientHandler).createRestClient();
        doReturn(spatialFilter).when(searchRequest).getSpatialFilter();
        doReturn(fieldName).when(spatialFilter).getField();
        when(elasticLoggingConfig.getEnabled()).thenReturn(false);
        when(elasticLoggingConfig.getThreshold()).thenReturn(200L);
//        doReturn(searchResponse).when(client).search(any(), any(RequestOptions.class));
//        doReturn(searchHits).when(searchResponse).getHits();
//        doReturn(hitFields).when(searchHit).getSourceAsMap();

        Map<String, String> HEADERS = new HashMap<>();
        HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
        HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
        HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));

        when(providerHeaderService.getDataGroupsHeader()).thenReturn(DATA_GROUPS);
        when(dpsHeaders.getHeaders()).thenReturn(HEADERS);
    }

    @Test
    @Ignore
    public void testQueryBase_whenSearchHitsIsEmpty() throws IOException {
        SearchHit[] hits = {};
        Set<String> indexedTypes = new HashSet<>();

        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        doReturn(hits).when(searchHits).getHits();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    @Ignore
    public void testQueryBase_whenSearchHitsIsNotEmpty() throws IOException {
        SearchHit[] hits = {searchHit};
        Set<String> indexedTypes = new HashSet<>();

        Map<String, HighlightField> highlightFields = getHighlightFields();

        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        doReturn(hits).when(searchHits).getHits();
        doReturn(highlightFields).when(searchHit).getHighlightFields();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        assertEquals(queryResponse.getResults().size(), 1);
        assertTrue(queryResponse.getResults().get(0).keySet().contains(name));
        assertEquals(queryResponse.getResults().get(0).get(name), text);
    }

    @Test
    @Ignore
    public void testQueryBase_useGeoShapeQueryIsFalse_getByBoundingBox() throws IOException {
        SearchHit[] hits = {};
        Set<String> indexedTypes = new HashSet<>();
        SpatialFilter.ByBoundingBox boundingBox = getValidBoundingBox();

        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        doReturn(boundingBox).when(spatialFilter).getByBoundingBox();
        doReturn(hits).when(searchHits).getHits();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);

        verify(client).search(elasticSearchRequest.capture(), eq(RequestOptions.DEFAULT));

        GeoBoundingBoxQueryBuilder queryBuilder = (GeoBoundingBoxQueryBuilder) ((BoolQueryBuilder) elasticSearchRequest.getValue().source().query()).must().get(0);
        GeoPoint topLeft = queryBuilder.topLeft();
        GeoPoint bottomRight = queryBuilder.bottomRight();

        assertTrue(checkPointAndGeoPointCorrespondence(this.topLeft, topLeft));
        assertTrue(checkPointAndGeoPointCorrespondence(this.bottomRight, bottomRight));
        assertEquals(fieldName, (queryBuilder.queryName("fieldName")).fieldName());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    @Ignore
    public void testQueryBase_useGeoShapeQueryIsTrue_getByBoundingBox() throws IOException {
        SearchHit[] hits = {};
        Set<String> indexedTypes = new HashSet<>();
        indexedTypes.add(GEO_SHAPE);
        SpatialFilter.ByBoundingBox boundingBox = getValidBoundingBox();

        doReturn(boundingBox).when(spatialFilter).getByBoundingBox();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        doReturn(hits).when(searchHits).getHits();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);

        verify(client).search(elasticSearchRequest.capture(), eq(RequestOptions.DEFAULT));

        QueryBuilder queryBuilder = ((BoolQueryBuilder) elasticSearchRequest.getValue().source().query()).must().get(0);
        EnvelopeBuilder shape = (EnvelopeBuilder) ((GeoShapeQueryBuilder) queryBuilder).shape();
        Coordinate topLeft = shape.topLeft();
        Coordinate bottomRight = shape.bottomRight();

        assertTrue(checkPointAndCoordinateCorrespondence(this.topLeft, topLeft));
        assertTrue(checkPointAndCoordinateCorrespondence(this.bottomRight, bottomRight));
        assertEquals(GEO_SHAPE, queryBuilder.getName());
        assertEquals(fieldName, ((GeoShapeQueryBuilder) queryBuilder.queryName("fieldName")).fieldName());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    @Ignore
    public void testQueryBase_useGeoShapeQueryIsFalse_getByDistance() throws IOException {
        SearchHit[] hits = {};
        Set<String> indexedTypes = new HashSet<>();
        SpatialFilter.ByDistance distance = getDistance(1.0, dummyPoint);

        doReturn(distance).when(spatialFilter).getByDistance();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        doReturn(hits).when(searchHits).getHits();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);

        verify(client).search(elasticSearchRequest.capture(), eq(RequestOptions.DEFAULT));

        QueryBuilder queryBuilder = ((BoolQueryBuilder) elasticSearchRequest.getValue().source().query()).must().get(0);

        assertEquals(((GeoDistanceQueryBuilder) queryBuilder).distance(), distance.getDistance(), DELTA);
        assertEquals(dummyPoint.getLatitude(), ((GeoDistanceQueryBuilder) queryBuilder).point().getLat(), DELTA);
        assertEquals(dummyPoint.getLongitude(), ((GeoDistanceQueryBuilder) queryBuilder).point().getLon(), DELTA);
        assertEquals(GEO_DISTANCE, queryBuilder.getName());
        assertEquals(fieldName, ((GeoDistanceQueryBuilder) queryBuilder.queryName("fieldName")).fieldName());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    @Ignore
    public void testQueryBase_useGeoShapeQueryIsFalse_getByGeoPolygon() throws IOException {
        SearchHit[] hits = new SearchHit[0];
        Set<String> indexedTypes = new HashSet<>();

        SpatialFilter.ByGeoPolygon geoPolygon = new SpatialFilter.ByGeoPolygon(polygonPoints);

        doReturn(geoPolygon).when(spatialFilter).getByGeoPolygon();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        doReturn(hits).when(searchHits).getHits();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);

        verify(client).search(elasticSearchRequest.capture(), eq(RequestOptions.DEFAULT));

        QueryBuilder queryBuilder = ((BoolQueryBuilder) elasticSearchRequest.getValue().source().query()).must().get(0);
        List<GeoPoint> geoPoints = ((GeoPolygonQueryBuilder) queryBuilder).points();

        validateGeoPointsPolygonAndPolygonCorrespondence(geoPoints, polygonPoints);
        assertEquals(GEO_POLYGON, queryBuilder.getName());
        assertEquals(fieldName, ((GeoPolygonQueryBuilder) queryBuilder.queryName("fieldName")).fieldName());
    }

    @Test
    @Ignore
    public void testQueryBase_useGeoShapeQueryIsTrue_getByGeoPolygon() throws IOException {
        SearchHit[] hits = {};
        Set<String> indexedTypes = new HashSet<>();
        indexedTypes.add(GEO_SHAPE);
        SpatialFilter.ByGeoPolygon geoPolygon = getGeoPolygon(closedPolygonPoints);

        doReturn(geoPolygon).when(spatialFilter).getByGeoPolygon();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        doReturn(hits).when(searchHits).getHits();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);

        verify(client).search(elasticSearchRequest.capture(), eq(RequestOptions.DEFAULT));

        QueryBuilder queryBuilder = ((BoolQueryBuilder) elasticSearchRequest.getValue().source().query()).must().get(0);
        PolygonBuilder shape = (PolygonBuilder) ((GeoShapeQueryBuilder) queryBuilder).shape();
        Coordinate[] coordinates = shape.coordinates()[0][0];

        // coordinates obtained starts from 1st point instead of 0th point of closedPolygon
        assertTrue(checkPointAndCoordinateCorrespondence(closedPolygonPoints.get(1), coordinates[0]));
        assertTrue(checkPointAndCoordinateCorrespondence(closedPolygonPoints.get(2), coordinates[1]));
        assertTrue(checkPointAndCoordinateCorrespondence(closedPolygonPoints.get(3), coordinates[2]));
        assertTrue(checkPointAndCoordinateCorrespondence(closedPolygonPoints.get(0), coordinates[3]));
        assertTrue(checkPointAndCoordinateCorrespondence(closedPolygonPoints.get(1), coordinates[4]));
        assertEquals(GEO_SHAPE, queryBuilder.getName());
        assertEquals(fieldName, ((GeoShapeQueryBuilder) queryBuilder.queryName("fieldName")).fieldName());
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusNotFound_throwsException() throws IOException {
        ElasticsearchStatusException exception = mock(ElasticsearchStatusException.class);

        Set<String> indexedTypes = new HashSet<>();

        doThrow(exception).when(client).search(any(), any(RequestOptions.class));
        doReturn(RestStatus.NOT_FOUND).when(exception).status();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 404;
            String errorMessage = "Resource you are trying to find does not exists";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusBadRequest_throwsException() throws IOException {
        ElasticsearchStatusException exception = mock(ElasticsearchStatusException.class);

        Set<String> indexedTypes = new HashSet<>();

        doThrow(exception).when(client).search(any(), any(RequestOptions.class));
        doReturn(RestStatus.BAD_REQUEST).when(exception).status();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 400;
            String errorMessage = "Invalid parameters were given on search request";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenUnsupportedSortRequested_statusBadRequest_throwsException() throws IOException {
        String dummySortError = "Text fields are not optimised for operations that require per-document field data like aggregations and sorting, so these operations are disabled by default. Please use a keyword field instead";
        ElasticsearchStatusException exception = new ElasticsearchStatusException("blah", RestStatus.BAD_REQUEST, new ElasticsearchException(dummySortError));

        doThrow(exception).when(client).search(any(), any(RequestOptions.class));
        doReturn(new HashSet<>()).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));
        SortQuery sortQuery = new SortQuery();
        sortQuery.setField(Collections.singletonList("data.name"));
        sortQuery.setOrder(Collections.singletonList(SortOrder.DESC));
        when(searchRequest.getSort()).thenReturn(sortQuery);
        doReturn(Collections.singletonList(new FieldSortBuilder("data.name").order(org.elasticsearch.search.sort.SortOrder.DESC)))
                .when(sortParserUtil).getSortQuery(eq(client), eq(sortQuery), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 400;
            String errorMessage = "Sort is not supported for one or more of the requested fields";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusServiceUnavailable_throwsException() throws IOException {
        ElasticsearchStatusException exception = mock(ElasticsearchStatusException.class);

        Set<String> indexedTypes = new HashSet<>();

        doThrow(exception).when(client).search(any(), any(RequestOptions.class));
        doReturn(RestStatus.SERVICE_UNAVAILABLE).when(exception).status();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 503;
            String errorMessage = "Please re-try search after some time.";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusTooManyRequests_throwsException() throws IOException {
        ElasticsearchStatusException exception = mock(ElasticsearchStatusException.class);

        Set<String> indexedTypes = new HashSet<>();

        doThrow(exception).when(client).search(any(), any(RequestOptions.class));
        doReturn(RestStatus.TOO_MANY_REQUESTS).when(exception).status();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 429;
            String errorMessage = "Too many requests, please re-try after some time";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }
    @Test(expected = AppException.class)
    public void testQueryBase_IOException_ListenerTimeout_throwsException() throws IOException {
        IOException exception = mock(IOException.class);

        Set<String> indexedTypes = new HashSet<>();
        String dummyTimeoutMessage = "listener timeout after waiting for 1m";

        doThrow(exception).when(client).search(any(), any(RequestOptions.class));
        doReturn(dummyTimeoutMessage).when(exception).getMessage();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 504;
            String errorMessage = "Request timed out after waiting for 1m";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_IOException_EmptyMessage_throwsException() throws IOException {
        IOException exception = mock(IOException.class);

        Set<String> indexedTypes = new HashSet<>();
        String dummyTimeoutMessage = "";

        doThrow(exception).when(client).search(any(), any(RequestOptions.class));
        doReturn(dummyTimeoutMessage).when(exception).getMessage();
        doReturn(indexedTypes).when(fieldMappingTypeService).getFieldTypes(eq(client), eq(fieldName), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "Error processing search request";

            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test
    public void should_searchAll_when_requestHas_noQueryString() throws IOException {

        BoolQueryBuilder builder = (BoolQueryBuilder) this.sut.buildQuery(null, null, true);
        assertNotNull(builder);

        List<QueryBuilder> topLevelFilterClause = builder.filter();
        assertEquals(1, topLevelFilterClause.size());

        verifyAcls(topLevelFilterClause.get(0), true);
    }

    @Test
    public void should_return_ownerOnlyMustClause_when_searchAsOwners() throws IOException {

        BoolQueryBuilder builder = (BoolQueryBuilder) this.sut.buildQuery(null, null, false);
        assertNotNull(builder);

        List<QueryBuilder> topLevelFilterClause = builder.filter();
        assertEquals(1, topLevelFilterClause.size());

        verifyAcls(topLevelFilterClause.get(0), false);
    }

    @Test
    public void should_return_nullQuery_when_searchAsDataRootUser() throws IOException {
        Map<String, String> HEADERS = new HashMap<>();
        HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
        HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
        HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));
        HEADERS.put(providerHeaderService.getDataRootUserHeader(), "true");
        when(dpsHeaders.getHeaders()).thenReturn(HEADERS);

        QueryBuilder builder = this.sut.buildQuery(null, null, false);
        assertNotNull(builder);
    }

    @Test
    public void should_return_CorrectQueryResponseforIntersectionSpatialFilter() throws Exception {
        // arrange
        // create query request according to this example query:
        //	{
        //		"kind": "osdu:wks:reference-data--CoordinateTransformation:1.0.0",
        //			"query": "data.ID:\"EPSG::1078\"",
        //			"spatialFilter": {
        //			"field": "data.Wgs84Coordinates",
        //			"byIntersection": {
        //				"polygons": [
        //				{
        //					"points": [
        //					{
        //						"latitude": 10.75,
        //							"longitude": -8.61
        //					}
        //						]
        //				}
        //				]
        //			}
        //		}
        //	}
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQuery("data.ID:\"EPSG::1078\"");
        SpatialFilter spatialFilter = new SpatialFilter();
        spatialFilter.setField("data.Wgs84Coordinates");
        SpatialFilter.ByIntersection byIntersection = new SpatialFilter.ByIntersection();
        Polygon polygon = new Polygon();
        Point point = new Point(1.02, -8.61);
        Point point1 = new Point(1.02, -2.48);
        Point point2 = new Point(10.74, -2.48);
        Point point3 = new Point(10.74, -8.61);
        Point point4 = new Point(1.02, -8.61);
        List<Point> points = new ArrayList<>();
        points.add(point);
        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
        polygon.setPoints(points);
        List<Polygon> polygons = new ArrayList<>();
        polygons.add(polygon);
        byIntersection.setPolygons(polygons);
        spatialFilter.setByIntersection(byIntersection);
        queryRequest.setSpatialFilter(spatialFilter);

        // mock out elastic client handler
        RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
        SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
        Mockito.when(searchResponse.status())
                .thenReturn(RestStatus.OK);

        SearchHits searchHits = Mockito.mock(SearchHits.class);
        Mockito.when(searchHits.getHits())
                .thenReturn(new SearchHit[]{});
        Mockito.when(searchResponse.getHits())
                .thenReturn(searchHits);

        Mockito.when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT)))
                .thenReturn(searchResponse);


        Mockito.when(elasticClientHandler.createRestClient())
                .thenReturn(client);

        String index = "some-index";
        Mockito.when(crossTenantUtils.getIndexName(Mockito.any()))
                .thenReturn(index);

        Set<String> indexedTypes = new HashSet<>();
        indexedTypes.add("geo_shape");
        Mockito.when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
                .thenReturn(indexedTypes);

        Mockito.when(providerHeaderService.getDataGroupsHeader())
                .thenReturn("groups");

        Map<String, String> headers = new HashMap<>();
        headers.put("groups", "[]");
        Mockito.when(dpsHeaders.getHeaders())
                .thenReturn(headers);

        String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"query_string\":{\"query\":\"data.ID:\\\"EPSG::1078\\\"\",\"fields\":[],\"type\":\"best_fields\",\"default_operator\":\"or\",\"max_determinized_states\":10000,\"allow_leading_wildcard\":false,\"enable_position_increments\":true,\"fuzziness\":\"AUTO\",\"fuzzy_prefix_length\":0,\"fuzzy_max_expansions\":50,\"phrase_slop\":0,\"escape\":false,\"auto_generate_synonyms_phrase_query\":true,\"fuzzy_transpositions\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-8.61,1.02],[-2.48,1.02],[-2.48,10.74],[-8.61,10.74],[-8.61,1.02]]]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":false,\"boost\":1.0}}],\"filter\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

        // act
        QueryResponse response = this.sut.queryIndex(queryRequest);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
        Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
        SearchRequest searchRequest = searchRequestArg.getValue();
        String actualSource = searchRequest.source().toString();
        JsonNode expectedJson = mapper.readTree(expectedSource);
        JsonNode actualJson = mapper.readTree(actualSource);
        Assert.assertTrue(expectedJson.equals(actualJson));
    }

    private Map<String, HighlightField> getHighlightFields() {
        Text[] fragments = {new Text(text)};
        HighlightField highlightField = new HighlightField(name, fragments);
        Map<String, HighlightField> highlightFields = new HashMap<>();
        highlightFields.put("highlightField", highlightField);
        return highlightFields;
    }

    private Point getPoint(double latitude, double longitude) {
        return new Point(latitude, longitude);
    }

    private List<Point> getPolygonPoints(Point... points) {
        List<Point> polygon = new ArrayList<>();
        for (Point point : points) {
            polygon.add(point);
        }
        return polygon;
    }

    private SpatialFilter.ByBoundingBox getValidBoundingBox() {
        return getBoundingBox(topLeft, bottomRight);
    }

    private SpatialFilter.ByBoundingBox getBoundingBox(Point topLeft, Point bottomRight) {
        SpatialFilter.ByBoundingBox boundingBox = new SpatialFilter.ByBoundingBox();
        boundingBox.setTopLeft(topLeft);
        boundingBox.setBottomRight(bottomRight);
        return boundingBox;
    }

    private SpatialFilter.ByGeoPolygon getGeoPolygon(List<Point> points) {
        return new SpatialFilter.ByGeoPolygon(points);
    }

    private SpatialFilter.ByDistance getDistance(double distance, Point point) {
        return new SpatialFilter.ByDistance(distance, point);
    }

    private boolean checkPointAndCoordinateCorrespondence(Point point, Coordinate coordinate) {
        if (point.getLongitude() != coordinate.getOrdinate(0)) {
            return false;
        }
        if (point.getLatitude() != coordinate.getOrdinate(1)) {
            return false;
        }
        return true;
    }

    private boolean checkPointAndGeoPointCorrespondence(Point point, GeoPoint geoPoint) {
        if (point.getLongitude() != geoPoint.getLon()) {
            return false;
        }
        if (point.getLatitude() != geoPoint.getLat()) {
            return false;
        }
        return true;
    }

    private void validateAppException(AppException e, int errorCode, String errorMessage) {
        AppError error = e.getError();
        assertEquals(error.getCode(), errorCode);
        assertEquals(error.getMessage(), errorMessage);
    }

    public void validateGeoPointsPolygonAndPolygonCorrespondence(List<GeoPoint> points, List<Point> polygon) {
        int length = polygon.size();
        for (int i = 0; i < length; ++i) {
            assertTrue(checkPointAndGeoPointCorrespondence(polygon.get(i), points.get(i)));
        }
        assertEquals(points.get(0), points.get(length));
    }

    private void verifyAcls(QueryBuilder aclFilterClause, boolean asOwner) {
        TermsQueryBuilder aclQuery = (TermsQueryBuilder) aclFilterClause;
        assertNotNull(aclQuery);
        if (asOwner) {
            assertEquals("acl.owners", aclQuery.fieldName());
        } else {
            assertEquals("x-acl", aclQuery.fieldName());
        }
        assertEquals(2, aclQuery.values().size());

        List<Object> acls = aclQuery.values();
        assertEquals(2, acls.size());
        assertTrue(acls.contains(DATA_GROUP_1));
        assertTrue(acls.contains(DATA_GROUP_2));
    }
}
