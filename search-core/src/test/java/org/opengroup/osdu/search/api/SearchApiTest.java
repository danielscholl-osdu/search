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
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.provider.interfaces.ICcsQueryService;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.provider.interfaces.IScrollQueryService;
import org.opengroup.osdu.search.provider.interfaces.ISearchAfterQueryService;
import org.opengroup.osdu.search.util.SearchAfterFeatureManager;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
    private ISearchAfterQueryService searchAfterQueryService;
    @Mock
    private SearchAfterFeatureManager searchAfterFeatureManager;

    @InjectMocks
    private SearchApi sut;

    private QueryRequest queryRequest;

    private QueryResponse queryResponse;

    private CursorQueryResponse cursorQueryResponse;

    @Before
    public void setup() {
        queryRequest = new QueryRequest();

        Map<String, Object> hit = new HashMap<>();
        hit.put("_id", "tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f");
        hit.put("type", "well");
        List<Map<String, Object>> hits = new ArrayList<>();
        hits.add(hit);
        queryResponse = new QueryResponse();
        queryResponse.setResults(hits);
        queryResponse.setAggregations(null);
        queryResponse.setTotalCount(1);

        cursorQueryResponse = new CursorQueryResponse();
        cursorQueryResponse.setResults(hits);
        cursorQueryResponse.setTotalCount(1);
    }

    @Test
    public void should_returnRecords_whenQueried() throws Exception {

        this.queryRequest.setKind("tenant1:welldb:well:1.0.2");

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
        this.queryResponse.setAggregations(new ArrayList<>());

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
    public void should_returnRecords_whenCursorQueried_with_searchAfterFeature_off() throws Exception {
        when(this.searchAfterFeatureManager.isEnabled()).thenReturn(false);
        when(this.scrollQueryService.queryIndex(cursorQueryRequest)).thenReturn(this.cursorQueryResponse);

        ResponseEntity<CursorQueryResponse> response = this.sut.queryWithCursor(this.cursorQueryRequest, false);
        verify(this.scrollQueryService, times(1)).queryIndex(any());
        verify(this.searchAfterQueryService, times(0)).queryIndex(any());

        CursorQueryResponse apiResponse = response.getBody();
        assertEquals(1, apiResponse.getTotalCount());
        assertEquals(1, apiResponse.getResults().size());
        assertEquals("tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f", apiResponse.getResults().get(0).get("_id"));
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
    }

    @Test
    public void should_handle_appException_whenCursorQueried_with_searchAfterFeature_off() throws Exception {
        AppException exception = new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error in test", "some message here");

        when(this.searchAfterFeatureManager.isEnabled()).thenReturn(false);
        when(this.scrollQueryService.queryIndex(any())).thenThrow(exception);

        try {
            this.sut.queryWithCursor(new CursorQueryRequest(), false);
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
    public void should_returnRecords_whenCursorQueried_with_searchAfterFeature_on() throws Exception {
        when(this.searchAfterFeatureManager.isEnabled()).thenReturn(true);
        when(this.searchAfterQueryService.queryIndex(cursorQueryRequest)).thenReturn(this.cursorQueryResponse);

        ResponseEntity<CursorQueryResponse> response = this.sut.queryWithCursor(this.cursorQueryRequest, false);
        verify(this.scrollQueryService, times(0)).queryIndex(any());
        verify(this.searchAfterQueryService, times(1)).queryIndex(any());

        CursorQueryResponse apiResponse = response.getBody();
        assertEquals(1, apiResponse.getTotalCount());
        assertEquals(1, apiResponse.getResults().size());
        assertEquals("tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f", apiResponse.getResults().get(0).get("_id"));
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
    }

    @Test
    public void should_handle_appException_whenCursorQueried_with_searchAfterFeature_on() throws Exception {
        AppException exception = new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error in test", "some message here");

        when(this.searchAfterFeatureManager.isEnabled()).thenReturn(true);
        when(this.searchAfterQueryService.queryIndex(any())).thenThrow(exception);

        try {
            this.sut.queryWithCursor(new CursorQueryRequest(), false);
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
    public void should_returnRecords_whenSearchAfterQueried() throws Exception {
        when(this.searchAfterQueryService.queryIndex(cursorQueryRequest)).thenReturn(this.cursorQueryResponse);

        ResponseEntity<CursorQueryResponse> response = this.sut.queryWithCursor(this.cursorQueryRequest, true);

        CursorQueryResponse apiResponse = response.getBody();
        assertEquals(1, apiResponse.getTotalCount());
        assertEquals(1, apiResponse.getResults().size());
        assertEquals("tenant1:welldb:well-33fe05e1-df20-49d9-bd63-74cf750a206f", apiResponse.getResults().get(0).get("_id"));
        assertEquals(HttpServletResponse.SC_OK, response.getStatusCodeValue());
    }

    @Test
    public void should_handle_appException_whenSearchAfterQueried() throws Exception {
        AppException exception = new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error in test", "some message here");

        when(this.searchAfterQueryService.queryIndex(any())).thenThrow(exception);

        try {
            this.sut.queryWithCursor(new CursorQueryRequest(), true);
            fail("Should not succeed!");
        } catch (AppException e) {
            assertEquals(exception.getError().getCode(), e.getError().getCode());
            assertEquals(exception.getError().getMessage(), e.getError().getMessage());
            assertEquals(exception.getError().getReason(), e.getError().getReason());
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }

}
