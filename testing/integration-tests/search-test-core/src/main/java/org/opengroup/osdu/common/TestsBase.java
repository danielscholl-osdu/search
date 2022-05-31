package org.opengroup.osdu.common;

import com.google.gson.Gson;

import com.google.gson.reflect.TypeToken;
import cucumber.api.DataTable;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.models.schema.PersistentSchemaTestIndex;
import org.opengroup.osdu.request.SpatialFilter;
import org.opengroup.osdu.response.ResponseBase;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;

import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.Scenario;
import lombok.extern.java.Log;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opengroup.osdu.util.Config.*;

@Log
public abstract class TestsBase {
    protected HTTPClient httpClient;
    protected Scenario scenario;
    protected Map<String, String> tenantMap = new HashMap<>();
    protected Map<String, String> headers = new HashMap<>();

    private List<String> indexes = new ArrayList<>();
    private List<Map<String, Object>> records;
    private Map<String, List<String>> schemaRecords = new HashMap<>();

    protected SpatialFilter spatialFilter = new SpatialFilter();
    protected SpatialFilter.ByBoundingBox byBoundingBox;
    protected SpatialFilter.ByIntersection byIntersection;
    protected SpatialFilter.ByWithinPolygon byWithinPolygon;

    protected static final String timeStamp = String.valueOf(System.currentTimeMillis()) ;
    private boolean dunit = false;

    public TestsBase(HTTPClient httpClient) {
        this.httpClient = httpClient;
        headers = httpClient.getCommonHeader();
        tenantMap.put("tenant1", getDataPartitionIdTenant1());
        tenantMap.put("tenant2", getDataPartitionIdTenant2());
        tenantMap.put("common", "common");
    }


    public void i_send_request_to_tenant(String tenant) {
        headers = HTTPClient.overrideHeader(headers, getTenantMapping(tenant));
    }

    public void i_send_request_to_tenant(String tenant1, String tenant2) {
        headers = HTTPClient.overrideHeader(headers, getTenantMapping(tenant1), getTenantMapping(tenant2));
    }

    protected abstract String getApi();

    protected abstract String getHttpMethod();

    public void offset_of_starting_point_as_None() {
        //Do nothing on None
    }

    public void define_bounding_box_with_points_and(Double topLatitude, Double topLongitude, Double bottomLatitude, Double
            bottomLongitude) {
        SpatialFilter.Points bottomRight = SpatialFilter.Points.builder().latitude(bottomLatitude).longitude(bottomLongitude).build();
        SpatialFilter.Points topLeft = SpatialFilter.Points.builder().latitude(topLatitude).longitude(topLongitude).build();
        byBoundingBox = SpatialFilter.ByBoundingBox.builder().topLeft(topLeft).bottomRight(bottomRight).build();
        spatialFilter.setByBoundingBox(byBoundingBox);
    }

    public void define_intersection_polygon_with_points(Double latitude1, Double longitude1, Double latitude2, Double longitude2,
                                                        Double latitude3, Double longitude3, Double latitude4, Double longitude4,
                                                        Double latitude5, Double longitude5) {
        Point point1 = new Point(latitude1, longitude1);
        Point point2 = new Point(latitude2, longitude2);
        Point point3 = new Point(latitude3, longitude3);
        Point point4 = new Point(latitude4, longitude4);
        Point point5 = new Point(latitude5, longitude5);
        List<Point> points = Arrays.asList(point1, point2, point3, point4, point5);
        Polygon polygon = new Polygon(points);
        List<Polygon> polygons = Arrays.asList(polygon);
        byIntersection = SpatialFilter.ByIntersection.builder().polygons(polygons).build();
        spatialFilter.setByIntersection(byIntersection);
    }

    public void define_within_polygon_with_points(Double latitude1, Double longitude1) {
        Point point1 = new Point(latitude1, longitude1);
        List<Point> points = Arrays.asList(point1);
        byWithinPolygon = SpatialFilter.ByWithinPolygon.builder().points(points).build();
        spatialFilter.setByWithinPolygon(byWithinPolygon);
    }

    protected String executeQuery(String api, String payLoad, Map<String, String> headers, String token) {
        ClientResponse clientResponse = httpClient.send(this.getHttpMethod(), api, payLoad, headers, token);
        logCorrelationIdWithFunctionName(clientResponse.getHeaders());
        log.info(String.format("Response status: %s, type: %s", clientResponse.getStatus(), clientResponse.getType().toString()));
        assertEquals(MediaType.APPLICATION_JSON, clientResponse.getType().toString());
        return clientResponse.getEntity(String.class);
    }

    protected <T extends ResponseBase> T executeQuery(String api, String payLoad, Map<String, String> headers, String token, Class<T> typeParameterClass) {
        ClientResponse clientResponse = httpClient.send(this.getHttpMethod(), api, payLoad, headers, token);
        logCorrelationIdWithFunctionName(clientResponse.getHeaders());
        return getResponse(clientResponse, typeParameterClass);
    }

    protected <T extends ResponseBase> T executeQuery(String payLoad, Map<String, String> headers, String token, Class<T> typeParameterClass) {
        ClientResponse clientResponse = httpClient.send(this.getHttpMethod(), this.getApi(), payLoad, headers, token);
        logCorrelationIdWithFunctionName(clientResponse.getHeaders());
        return getResponse(clientResponse, typeParameterClass);
    }

    private <T extends ResponseBase> T getResponse(ClientResponse clientResponse, Class<T> typeParameterClass) {
        if (clientResponse.getType() == null || log == null){
            int i = 0;
        }
        log.info(String.format("Response status: %s", clientResponse.getStatus()));
        if(clientResponse.getType() != null){
            log.info(String.format("Response type: %s", clientResponse.getType().toString()));
        }else {
            log.info("Got response type: null");
        }
        assertTrue(clientResponse.getType().toString().contains(MediaType.APPLICATION_JSON));
        String responseEntity = clientResponse.getEntity(String.class);

        T response = new Gson().fromJson(responseEntity, typeParameterClass);
        response.setHeaders(clientResponse.getHeaders());
        response.setResponseCode(clientResponse.getStatus());
        return response;
    }

    protected ClientResponse executeGetRequest(String api, Map<String, String> headers, String token) {
        return executeRequest(this.getHttpMethod(), api, headers, token);
    }

    protected ClientResponse executeRequest(String method, String api, Map<String, String> headers, String token) {
        ClientResponse clientResponse = httpClient.send(method, api, null, headers, token);
        if (clientResponse.getType() != null) {
            log.info(String.format("Response status: %s, type: %s", clientResponse.getStatus(), clientResponse.getType().toString()));
        }
        logCorrelationIdWithFunctionName(clientResponse.getHeaders());
        return clientResponse;
    }

    private void logCorrelationIdWithFunctionName(MultivaluedMap<String, String> headers) {
        log.info(String.format("Scenario Name: %s, Correlation-Id: %s", scenario.getId(), headers.get("correlation-id")));
    }

    protected String getTenantMapping(String tenant) {
        if (tenantMap.containsKey(tenant)) {
            return tenantMap.get(tenant);
        }
        return null;
    }

    public String generateActualName(String rawName, String timeStamp) {
        for (String tenant : tenantMap.keySet()) {
            rawName = rawName.replaceAll(tenant, getTenantMapping(tenant));
        }
        return rawName.replaceAll("<timestamp>", this.timeStamp);
    }

    protected Legal generateLegalTag() {
        Legal legal = new Legal();
        Set<String> legalTags = new HashSet<>();
        legalTags.add(getLegalTag());
        legal.setLegaltags(legalTags);
        Set<String> otherRelevantCountries = new HashSet<>();
        otherRelevantCountries.add(getOtherRelevantDataCountries());
        legal.setOtherRelevantDataCountries(otherRelevantCountries);
        return legal;
    }

    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        List<Setup> inputList = dataTable.asList(Setup.class);
        inputList.forEach(this::createSchema);


    }

    protected  void tearDownSchema() throws IOException {
        String payload="{}";
        for(String kind : schemaRecords.keySet()){
            List<String> recordIds = schemaRecords.get(kind);
            for (String id : recordIds) {
                ClientResponse clientResponse = httpClient.send(HttpMethod.DELETE, getStorageBaseURL() + "records/" +id,payload,headers, httpClient.getAccessToken());
            }
            ClientResponse clientResponse = httpClient.send(HttpMethod.DELETE, getIndexerBaseURL() + "index?kind=" +kind,payload,headers, httpClient.getAccessToken());
        }

    }

    private void createSchema(Setup input) {
        PersistentSchemaTestIndex testIndex = new PersistentSchemaTestIndex(httpClient, this);
        testIndex.setSchemaFile(input.getSchemaFile());
        testIndex.setHttpClient(httpClient);
        testIndex.setupSchema();
        testIndex.setKind(testIndex.getSchemaModel().getSchemaInfo().getSchemaIdentity().getId());
    }

    public String generateActualNameWithoutTs(String rawName) {
        for (Map.Entry<String, String> tenant : tenantMap.entrySet()) {
            rawName = rawName.replaceAll(tenant.getKey(), tenant.getValue());
        }
        return rawName.replaceAll("<timestamp>", "");
    }

    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {

        String actualKind = generateActualName(kind, timeStamp);
        List<String> recordsForKind = new ArrayList<>();
        try {
            String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
            records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {}.getType());
            String createTime = java.time.Instant.now().toString();

            for (Map<String, Object> testRecord : records) {
                testRecord.put("kind", actualKind);
                testRecord.put("id", generateRecordId(testRecord));
                recordsForKind.add(testRecord.get("id").toString());
                testRecord.put("legal", generateLegalTag());
                String[] x_acl = {generateActualName(dataGroup,timeStamp)+"."+getEntitlementsDomain()};
                Acl acl = Acl.builder().viewers(x_acl).owners(x_acl).build();
                testRecord.put("acl", acl);
                String[] kindParts = kind.split(":");
                String authority = tenantMap.get(kindParts[0]);
                String source = kindParts[1];
                testRecord.put("authority", authority);
                testRecord.put("source", generateActualName(source,timeStamp));
                testRecord.put("createUser", "TestUser");
                testRecord.put("createTime", createTime);
            }
            schemaRecords.put(actualKind,recordsForKind);
            String payLoad = new Gson().toJson(records);
            log.log(Level.INFO, "Start ingesting records={0}", payLoad);
            ClientResponse clientResponse = httpClient.send(HttpMethod.PUT, getStorageBaseURL() + "records", payLoad, headers, httpClient.getAccessToken());
            assertEquals(201, clientResponse.getStatus());
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        tearDownSchema();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            TimeUnit.SECONDS.sleep(40);
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    protected String generateRecordId(Map<String, Object> testRecord) {
        return generateActualId(testRecord.get("id").toString(), timeStamp, testRecord.get("kind").toString());
    }

    protected String generateActualId(String rawName, String timeStamp, String kind) {

        rawName = generateActualName(rawName, timeStamp);

        String kindSubType = kind.split(":")[2];

        return rawName.replaceAll("<kindSubType>", kindSubType);

    }

}
