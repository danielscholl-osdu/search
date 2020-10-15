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
import org.opengroup.osdu.search.provider.azure.service.FieldMappingTypeService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.ElasticClientHandler;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;

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

    private Point dummyPoint = getPoint(0.0, 0.0);
    private List<Point> polygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0));
    private List<Point> closedPolygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0), getPoint(0.0, 0.0));
    private Point topLeft = getPoint(3.0, 4.0);
    private Point bottomRight = getPoint(2.0, 1.0);

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

        lenient().doReturn(dataPartitionId).when(dpsHeaders).getPartitionId();
        doReturn(indexName).when(crossTenantUtils).getIndexName(any(), eq(dataPartitionId));
        doReturn(client).when(elasticClientHandler).createRestClient();
        doReturn(spatialFilter).when(searchRequest).getSpatialFilter();
        doReturn(fieldName).when(spatialFilter).getField();
        doReturn(searchResponse).when(client).search(any(), any(RequestOptions.class));
        doReturn(searchHits).when(searchResponse).getHits();
        doReturn(hitFields).when(searchHit).getSourceAsMap();
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
        assertThat(error.getMessage(), containsString(errorMessage));
    }

    public void validateGeoPointsPolygonAndPolygonCorrespondence(List<GeoPoint> points, List<Point> polygon) {
        int length = polygon.size();
        for (int i = 0; i < length; ++i) {
            assertTrue(checkPointAndGeoPointCorrespondence(polygon.get(i), points.get(i)));
        }
        assertEquals(points.get(0), points.get(length));
    }
}