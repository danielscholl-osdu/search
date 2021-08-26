package org.opengroup.osdu.common;

import com.google.gson.Gson;

import cucumber.api.DataTable;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.request.SpatialFilter;
import org.opengroup.osdu.response.ResponseBase;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.Scenario;
import lombok.extern.java.Log;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opengroup.osdu.util.Config.*;

@Log
public abstract class TestsBase {
    protected HTTPClient httpClient;
    protected ElasticUtils elasticUtils;
    protected Scenario scenario;
    protected Map<String, String> tenantMap = new HashMap<>();
    protected Map<String, TestIndex> inputRecordMap = new HashMap<>();
    protected Map<String, String> headers = new HashMap<>();

    protected SpatialFilter spatialFilter = new SpatialFilter();
    protected SpatialFilter.ByBoundingBox byBoundingBox;

    protected String timeStamp = String.valueOf(System.currentTimeMillis());
    private boolean dunit = false;

    public TestsBase(HTTPClient httpClient) {
        this.httpClient = httpClient;
        this.elasticUtils = new ElasticUtils();
        headers = httpClient.getCommonHeader();
        tenantMap.put("tenant1", getDataPartitionIdTenant1());
        tenantMap.put("tenant2", getDataPartitionIdTenant2());
        tenantMap.put("common", "common");
    }

    public TestsBase(HTTPClient httpClient, ElasticUtils elasticUtils)  {
        this.httpClient = httpClient;
        this.elasticUtils = elasticUtils;
        headers = httpClient.getCommonHeader();
        headers.put("user", getUserEmail());
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

    protected void setUp(List<Setup> inputList, String timeStamp) {
        for (Setup input : inputList) {
            TestIndex testIndex = new TestIndex();
            testIndex.setHttpClient(httpClient);
            testIndex.setElasticUtils(elasticUtils);
            testIndex.setIndex(generateActualName(input.getIndex(), timeStamp));
            testIndex.setKind(generateActualName(input.getKind(), timeStamp));
            testIndex.setMappingFile(input.getMappingFile());
            testIndex.setRecordFile(input.getRecordFile());
            List<String> dataGroup = new ArrayList<>();
            String[] viewerGroup = input.getViewerGroup().split(",");
            for (int i = 0; i < viewerGroup.length; i++) {
                viewerGroup[i] = generateActualName(viewerGroup[i], timeStamp) + "." + getEntitlementsDomain();
                dataGroup.add(viewerGroup[i]);
            }
            String[] ownerGroup = input.getOwnerGroup().split(",");
            for (int i = 0; i < ownerGroup.length; i ++) {
                ownerGroup[i] = generateActualName(ownerGroup[i], timeStamp) + "." + getEntitlementsDomain();
                if (dataGroup.indexOf(ownerGroup[i]) > 0) {
                    dataGroup.add(ownerGroup[i]);
                }
            }
            testIndex.setViewerGroup(viewerGroup);
            testIndex.setOwnerGroup(ownerGroup);
            testIndex.setDataGroup(dataGroup.toArray(new String[dataGroup.size()]));
            inputRecordMap.put(testIndex.getKind(), testIndex);
        }
        /******************One time setup for whole feature**************/
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                tearDown();
            }
        });
        for (String kind : inputRecordMap.keySet()) {
            TestIndex testIndex = inputRecordMap.get(kind);
            testIndex.setupIndex();
        }

    }

    /******************One time cleanup for whole feature**************/
    public void tearDown() {
        for (String kind : inputRecordMap.keySet()) {
            TestIndex testIndex = inputRecordMap.get(kind);
            testIndex.cleanupIndex();
        }
    }

    protected abstract String getApi();

    protected abstract String getHttpMethod();

    public void the_elastic_search_is_initialized_with_the_following_data(DataTable dataTable) {
        if (!dunit) {
            List<Setup> inputlist = dataTable.asList(Setup.class);
            setUp(inputlist, timeStamp);
            dunit = true;
        }
    }

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

    protected String generateActualName(String rawName, String timeStamp) {
        for (String tenant : tenantMap.keySet()) {
            rawName = rawName.replaceAll(tenant, getTenantMapping(tenant));
        }
        return rawName.replaceAll("<timestamp>", timeStamp);
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
}
