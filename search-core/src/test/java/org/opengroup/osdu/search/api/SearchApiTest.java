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

package org.opengroup.osdu.search.api;

import com.google.gson.Gson;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.CcsQueryRequest;
import org.opengroup.osdu.core.common.model.search.CcsQueryResponse;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.search.config.CcsQueryConfig;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.provider.interfaces.ICcsQueryService;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.provider.interfaces.IScrollQueryService;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchConfigurationProperties.class})
public class SearchApiTest {

    @Mock
    private SearchConfigurationProperties searchConfigurationProperties;
    @Mock
    private IQueryService queryService;
    @Mock
    private ICcsQueryService ccsQueryService;
    @Mock
    private IScrollQueryService scrollQueryService;
    @Mock
    private CursorQueryRequest cursorQueryRequest;
    @Mock
    private CcsQueryConfig ccsQueryConfig;
    @InjectMocks
    private SearchApi sut;

    private QueryRequest queryRequest;

    @Before
    public void setup() {
        queryRequest = new QueryRequest();
        when(searchConfigurationProperties.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.LOCAL);
    }

    @Test
    public void should_returnRecords_whenQueried() throws Exception {

        this.queryRequest.setKind("tenant1:welldb:well:1.0.2");

        Map<String, Object> hit = new HashMap<>();
        hit.put("_id", "tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f");
        hit.put("type", "well");

        List<Map<String, Object>> hits = new ArrayList<>();
        hits.add(hit);

        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setResults(hits);
        queryResponse.setAggregations(null);
        queryResponse.setTotalCount(1);

        when(this.queryService.queryIndex(queryRequest)).thenReturn(queryResponse);

        ResponseEntity<QueryResponse> response = this.sut.queryRecords(queryRequest);

        String responseBody = response.getBody().toString();
        assertFalse(responseBody.contains("aggregations"));

        QueryResponse apiResponse = new Gson().fromJson(responseBody, QueryResponse.class);
        assertEquals(1, apiResponse.getTotalCount());
        assertEquals(1, apiResponse.getResults().size());
        assertEquals("tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f", apiResponse.getResults().get(0).get("_id"));
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
    }

    @Test
    public void should_returnAggregations_whenQueried() throws Exception {

        this.queryRequest.setKind("tenant1:welldb:well:1.0.2");

        Map<String, Object> hit = new HashMap<>();
        hit.put("_id", "tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f");
        hit.put("type", "well");

        List<Map<String, Object>> hits = new ArrayList<>();
        hits.add(hit);

        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setResults(hits);
        queryResponse.setAggregations(new ArrayList<>());
        queryResponse.setTotalCount(1);

        when(this.queryService.queryIndex(queryRequest)).thenReturn(queryResponse);

        ResponseEntity<QueryResponse> response = this.sut.queryRecords(queryRequest);

        String responseBody = response.getBody().toString();
        assertTrue(responseBody.contains("aggregations"));

        QueryResponse apiResponse = new Gson().fromJson(responseBody, QueryResponse.class);
        assertEquals(1, apiResponse.getTotalCount());
        assertEquals(0, apiResponse.getAggregations().size());
        assertEquals(1, apiResponse.getResults().size());
        assertEquals("tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f", apiResponse.getResults().get(0).get("_id"));
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
    }

    @Test
    public void should_handle_appException_whenQueried() throws Exception {
        AppException exception = new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error in test", "some message here");

        when(this.queryService.queryIndex(any())).thenThrow(exception);

        try {
            this.sut.queryRecords(new QueryRequest());
            fail("Should not succeed!");
        } catch (AppException e) {
            assertEquals(exception.getError().getCode(), e.getError().getCode());
            assertEquals(exception.getError().getMessage(), e.getError().getMessage());
            assertEquals(exception.getError().getReason(), e.getError().getReason());
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }

    @Test
    public void should_handle_notFoundException_whenQueried() throws Exception {
        AppException exception = new AppException(HttpServletResponse.SC_NOT_FOUND, "Not Found", "Resource you are trying to find does not exists");

        when(this.queryService.queryIndex(any())).thenThrow(exception);

        ResponseEntity<QueryResponse> response = this.sut.queryRecords(new QueryRequest());
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
        QueryResponse apiResponse = response.getBody();
        assertEquals(0, apiResponse.getTotalCount());
        assertEquals(0, apiResponse.getAggregations().size());
        assertEquals(0, apiResponse.getResults().size());
    }

    @Test
    public void should_handle_notFoundException_whenCursorQueried() throws Exception {
        AppException exception = new AppException(HttpServletResponse.SC_NOT_FOUND, "Not Found", "Resource you are trying to find does not exists");

        when(this.scrollQueryService.queryIndex(any())).thenThrow(exception);

        ResponseEntity<CursorQueryResponse> response = this.sut.queryWithCursor(new CursorQueryRequest());
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
        CursorQueryResponse apiResponse = response.getBody();
        assertEquals(0, apiResponse.getTotalCount());
        assertEquals(0, apiResponse.getResults().size());
    }

    @Test
    public void should_returnRecords_whenCursorQueried() throws Exception {

        when(this.cursorQueryRequest.getKind()).thenReturn("tenant1:welldb:well:1.0.2");

        Map<String, Object> hit = new HashMap<>();
        hit.put("_id", "tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f");
        hit.put("type", "well");

        List<Map<String, Object>> hits = new ArrayList<>();
        hits.add(hit);

        CursorQueryResponse cursorQueryResponse = new CursorQueryResponse();
        cursorQueryResponse.setResults(hits);
        cursorQueryResponse.setTotalCount(1);

        when(this.scrollQueryService.queryIndex(cursorQueryRequest)).thenReturn(cursorQueryResponse);

        ResponseEntity<CursorQueryResponse> response = this.sut.queryWithCursor(this.cursorQueryRequest);

        CursorQueryResponse apiResponse = response.getBody();
        assertEquals(1, apiResponse.getTotalCount());
        assertEquals(1, apiResponse.getResults().size());
        assertEquals("tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f", apiResponse.getResults().get(0).get("_id"));
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
    }

    @Test
    public void should_handle_appException_whenCursorQueried() throws Exception {
        AppException exception = new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error in test", "some message here");

        when(this.scrollQueryService.queryIndex(any())).thenThrow(exception);

        try {
            this.sut.queryWithCursor(new CursorQueryRequest());
            fail("Should not succeed!");
        } catch (AppException e) {
            assertEquals(exception.getError().getCode(), e.getError().getCode());
            assertEquals(exception.getError().getMessage(), e.getError().getMessage());
            assertEquals(exception.getError().getReason(), e.getError().getReason());
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }

    @Test
    public void should_returnCcsResult_when_Queried() throws Exception {
        CcsQueryRequest ccsQuery = new CcsQueryRequest();
        ccsQuery.setKind("tenant1:welldb:well:1.0.2");
        CcsQueryResponse queryResponse = new CcsQueryResponse();
        when(this.ccsQueryConfig.isDisabled()).thenReturn(false);
        when(this.ccsQueryService.makeRequest(ccsQuery)).thenReturn(queryResponse);

        ResponseEntity<CcsQueryResponse> response = this.sut.ccsQuery(ccsQuery);
        String responseBody = response.getBody().toString();
        CcsQueryResponse apiResponse = new Gson().fromJson(responseBody, CcsQueryResponse.class);

        assertEquals(0, apiResponse.getTotalCount());
        assertEquals(0, apiResponse.getResults().size());
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
    }

    @Test
    public void should_returnHttp404_when_ccsQueryApiIsDisabled() throws Exception {
        when(this.ccsQueryConfig.isDisabled()).thenReturn(true);

        CcsQueryRequest ccsQuery = new CcsQueryRequest();
        ccsQuery.setKind("tenant1:welldb:well:1.0.2");
        try {
            ResponseEntity<CcsQueryResponse> response = this.sut.ccsQuery(ccsQuery);
            fail("Should not succeed");
        } catch (AppException e){
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getError().getCode());
            assertEquals("This API has been deprecated", e.getError().getReason());
        }
    }
}
