// Copyright © Schlumberger
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

package org.opengroup.osdu.search.provider.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.ContentTooLongException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.search.cache.SearchAfterSettingsCache;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.context.UserContext;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.model.SearchAfterSettings;
import org.opengroup.osdu.search.util.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.POLICY_FEATURE_NAME;

@ExtendWith(MockitoExtension.class)
public class SearchAfterQueryServiceImplTest {
    private static final String reason = "Internal Server";
    private static final String message = "Search not completed";
    private static final String indexName = "index";
    private static final String userId = "userId";
    private static final String name = "name";
    private static final String text = "text";

    @Mock
    private SearchAfterSettings cursorSettings;

    @Mock
    private ElasticsearchClient client;

    @Mock
    private HitsMetadata<Map<String, Object>> searchHits;

    @Mock
    private IQueryPerformanceLogger perfLogger;

    @Mock
    private Hit<Map<String, Object>> searchHit;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private ElasticClientHandler elasticClientHandler;

    @Mock
    private SearchAfterSettingsCache cursorCache;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private CrossTenantUtils crossTenantUtils;
    @Mock
    private SuggestionsQueryUtil suggestionsQueryUtil;
    @Mock
    private ElasticLoggingConfig elasticLoggingConfig;
    @Mock
    private ResponseExceptionParser exceptionParser;
    @Mock
    public IFeatureFlag collaborationFeatureFlag;
    @InjectMocks
    private SearchAfterQueryServiceImpl sut;

    @Mock
    private UserContext userContext;

    @Mock
    private IFeatureFlag featureFlag;

    @BeforeEach
    public void init() {
        Mockito.lenient().doReturn(userId).when(dpsHeaders).getUserEmail();
        Mockito.lenient().doReturn(indexName).when(crossTenantUtils).getIndexName(any());
        Mockito.lenient().doReturn(cursorSettings).when(cursorCache).get(anyString());
        Mockito.lenient().doReturn(client).when(elasticClientHandler).getOrCreateRestClient();
        Mockito.lenient().when(elasticLoggingConfig.getEnabled()).thenReturn(false);
        Mockito.lenient().when(elasticLoggingConfig.getThreshold()).thenReturn(200L);
        Mockito.lenient().when(userContext.isRootUser()).thenReturn(false);
    }

    @Test
    public void testQueryIndex_whenSearchHitsIsNotEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, List<String>> highlightFields = getHighlightFields();
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String pitId = "pitId";
        long totalHitsCount = 1L;

        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(totalHitsCount).when(cursorSettings).getTotalCount();
        doReturn(cursor).when(cursorQueryRequest).getCursor();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(pitId).when(searchResponse).pitId();
        doReturn(hits).when(searchHits).hits();
        doReturn(highlightFields).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(cursorQueryRequest);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).search(searchRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(cursorCache).get(cursorArgumentCaptor.capture());
        SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();
        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("highlight"));
        assertEquals(((Map<String, List<String>>)obtainedQueryResponse.getResults().get(0).get("highlight")).get(name), List.of(text));
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(searchRequest.pit().id(), pitId);
        assertEquals(searchRequestCursor, cursor);
        verify(this.auditLogger, times(1)).queryIndexWithCursorSuccess(Lists.newArrayList(cursorQueryRequest.toString()));
        verify(this.perfLogger, times(1)).log(eq(cursorQueryRequest), anyLong(), eq(200));
    }

    @Test
    public void testQueryIndex_whenSearchHitsIsEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        String cursor = "cursor";
        String pitId = "pitId";
        long totalHitsCount = 0L;

        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(totalHitsCount).when(cursorSettings).getTotalCount();
        doReturn(cursor).when(cursorQueryRequest).getCursor();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(cursorQueryRequest);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).search(searchRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(cursorCache).get(cursorArgumentCaptor.capture());
        SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();
        assertEquals(obtainedQueryResponse.getResults().size(), 0);
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(searchRequest.pit().id(), pitId);
        assertEquals(searchRequestCursor, cursor);
    }


    @Test
    public void testQueryIndex_whenNoCursorInSearchQuery() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, List<String>> highlightFields = getHighlightFields();
        Map<String, Object> hitFields = new HashMap<>();
        String pitId = "pitId";
        long totalHitsCount = 1L;

        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        OpenPointInTimeResponse openPitResponse = mock(OpenPointInTimeResponse.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        TotalHits totalHits = mock(TotalHits.class);
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(highlightFields).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();
        doReturn(openPitResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        doReturn(pitId).when(openPitResponse).id();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(cursorQueryRequest);

        // assert
        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("highlight"));
        assertEquals(((Map<String, List<String>>)obtainedQueryResponse.getResults().get(0).get("highlight")).get(name), List.of(text));
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
    }

    @Test
    public void testQueryIndex_whenNoCursorInSearchQueryAndSearchHitsIsEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        String pitId = "pitId";
        long totalHitsCount = 0L;

        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        OpenPointInTimeResponse openPitResponse = mock(OpenPointInTimeResponse.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        TotalHits totalHits = mock(TotalHits.class);
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();
        doReturn(openPitResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        doReturn(pitId).when(openPitResponse).id();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        Mockito.lenient().when(featureFlag.isFeatureEnabled(POLICY_FEATURE_NAME)).thenReturn(false);

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        // assert
        assertEquals(obtainedQueryResponse.getResults().size(), 0);
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
    }

    @Test
    public void testQueryIndex_whenMismatchCursorIssuerAndConsumer_thenThrowException() throws Exception {
        String cursor = "cursor";
        String mismatchUserId = "mismatchUserId";

        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn(cursor).when(cursorQueryRequest).getCursor();
        doReturn(mismatchUserId).when(cursorSettings).getUserId();
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();

        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 403;
        AppError error = e.getError();
        assertEquals(errorCode, error.getCode());
        assertEquals("cursor issuer doesn't match the cursor consumer", error.getReason());
        assertEquals("cursor sharing is forbidden", error.getMessage());
    }

    @Test
    public void testQueryIndex_whenCursorSettingsNotFoundInCursorCache_thenThrowException() throws Exception {
        String cursor = "cursor";
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn(cursor).when(cursorQueryRequest).getCursor();
        doReturn(null).when(cursorCache).get(any());

        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 400;
        AppError error = e.getError();
        assertEquals("Can't find the given cursor", error.getReason());
        assertEquals("The given cursor is invalid or expired", error.getMessage());
        assertEquals(errorCode, error.getCode());
    }

    @Test
    public void testQueryIndex_whenCursorNotFound_thenThrowException() throws Exception {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn("cursor").when(cursorQueryRequest).getCursor();
        doReturn("cursor").when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        ElasticsearchException exception = mock(ElasticsearchException.class);
        doReturn(HttpServletResponse.SC_NOT_FOUND).when(exception).status();
        doReturn("No search context found for id [47500324]").when(exception).getMessage();

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        try {
            sut.queryIndex(cursorQueryRequest);
        } catch (AppException e) {
            int errorCode = 400;
            AppError error = e.getError();
            assertEquals(error.getReason(), "Can't find the given cursor");
            assertEquals(error.getMessage(), "The given cursor is invalid or expired");
            assertEquals(error.getCode(), errorCode);
        }
    }

    @Test
    public void testQueryIndex_whenResponseTooLong_thenThrowException() throws Exception {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn("cursor").when(cursorQueryRequest).getCursor();
        doReturn("cursor").when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        IOException exception = mock(IOException.class);
        doReturn(new ContentTooLongException(null)).when(exception).getCause();

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 413;
        AppError error = e.getError();
        assertEquals("Response is too long", error.getReason());
        assertEquals("Elasticsearch response is too long, max is 100Mb", error.getMessage());
        assertEquals(errorCode, error.getCode());
    }

    @Test
    public void testQueryIndex_whenSearchGives500_thenThrowException() throws Exception {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        OpenPointInTimeResponse openPitResponse = mock(OpenPointInTimeResponse.class);
        doReturn(null).when(cursorQueryRequest).getCursor();
        doReturn(openPitResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        doReturn("pitId").when(openPitResponse).id();
        AppException ex = new AppException(500, reason, message);
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();

        doThrow(ex).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 500;
        AppError error = e.getError();
        assertEquals(errorCode, error.getCode());
        assertEquals(reason, error.getReason());
        assertEquals(message, error.getMessage());
    }

    @Test
    public void refreshCursorCache_update_returned_cursor() {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn("pitId").when(searchResponse).pitId();
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();

        String cursor = sut.refreshCursorCache(searchResponse, true, cursorSettings);
        assertNotNull(cursor);
    }

    @Test
    public void refreshCursorCache_pitNull()  {
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(null).when(searchResponse).pitId();
        String cursor = sut.refreshCursorCache(searchResponse, true, cursorSettings);
        assertNull(cursor);
    }

    @Test
    public void close_open_cursor() throws Exception {
        String pitId = "pitId";
        String cursor = "cursor";
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(false).when(cursorSettings).isClosed();

        // act
        sut.close(cursor);

        // assert
        verify(this.cursorCache, times(1)).delete(eq(cursor));
        verify(this.client, times(1)).closePointInTime(any(ClosePointInTimeRequest.class));
    }

    @Test
    public void close_closed_cursor() throws Exception {
        String cursor = "cursor";
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(true).when(cursorSettings).isClosed();

        // act
        sut.close(cursor);

        // assert
        verify(this.cursorCache, times(1)).delete(eq(cursor));
        verify(this.client, times(0)).closePointInTime(any(ClosePointInTimeRequest.class));
    }

    @Test
    public void close_cursor_whenMismatchCursorIssuerAndConsumer_thenThrowException() throws Exception {
        String cursor = "cursor";
        String mismatchUserId = "mismatchUserId";
        doReturn(mismatchUserId).when(cursorSettings).getUserId();

        AppException e = assertThrows(AppException.class, () -> sut.close(cursor));

        int errorCode = 403;
        AppError error = e.getError();
        assertEquals(errorCode, error.getCode());
        assertEquals("cursor issuer doesn't match the cursor consumer", error.getReason());
        assertEquals("cursor sharing is forbidden", error.getMessage());
    }

    private Map<String, List<String>> getHighlightFields() {
        Map<String, List<String>> highlightFields = new HashMap<>();
        highlightFields.put(name, List.of(text));
        return highlightFields;
    }
}
