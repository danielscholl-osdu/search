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
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.service.FieldMappingTypeService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.ElasticClientHandler;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class QueryServiceImplTest {

    private static final String dataPartitionId = "data-partition-id";
    private static final String fieldName = "field";
    private static final String indexName = "index";
    private static final String name = "name";
    private static final String text = "text";
    private static final String GEO_SHAPE = "geo_shape";
    private static final String GEO_DISTANCE = "geo_distance";
    private static final String GEO_POLYGON = "geo_polygon";
    private static final double DELTA = 1e-6;

    private final Point dummyPoint = getPoint(0.0, 0.0);
    private final List<Point> polygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0));
    private final List<Point> closedPolygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0), getPoint(0.0, 0.0));
    private final Point topLeft = getPoint(3.0, 4.0);
    private final Point bottomRight = getPoint(2.0, 1.0);

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

    @InjectMocks
    private QueryServiceImpl sut;

    @Before
    public void init() throws IOException {
        Map<String, Object> hitFields = new HashMap<>();

        doReturn(indexName).when(crossTenantUtils).getIndexName(any());
        doReturn(client).when(elasticClientHandler).createRestClient();
        doReturn(spatialFilter).when(searchRequest).getSpatialFilter();
        doReturn(fieldName).when(spatialFilter).getField();
//        doReturn(searchResponse).when(client).search(any(), any(RequestOptions.class));
//        doReturn(searchHits).when(searchResponse).getHits();
//        doReturn(hitFields).when(searchHit).getSourceAsMap();
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
            throw(e);
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
            throw(e);
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
            throw(e);
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
            throw(e);
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
            throw(e);
        }
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

    private List<Point> getPolygonPoints(Point ...points) {
        List<Point> polygon = new ArrayList<>();
        for (Point point: points) {
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
}