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

package org.opengroup.osdu.search.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.POLICY_FEATURE_NAME;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.ContentTooLongException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.util.*;

@RunWith(MockitoJUnitRunner.class)
public class ScrollCoreQueryServiceImplTest {

    private static final String dataPartitionId = "data-partition-id";
    private static final String reason = "Internal Server";
    private static final String message = "Search not completed";
    private static final String indexName = "index";
    private static final String userId = "userId";
    private static final String name = "name";
    private static final String text = "text";
    private static final String rawCursor = "rawCursor";

    @Mock
    private CursorSettings cursorSettings;

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
    private IProviderHeaderService providerHeaderService;

    @Mock
    private CursorCache cursorCache;

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
    private ScrollCoreQueryServiceImpl sut;

    @Mock
    private IFeatureFlag featureFlag;

    @Before
    public void init() {
        doReturn(userId).when(dpsHeaders).getUserEmail();
        doReturn(indexName).when(crossTenantUtils).getIndexName(any());
        doReturn(cursorSettings).when(cursorCache).get(anyString());
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();
        when(elasticLoggingConfig.getEnabled()).thenReturn(false);
        when(elasticLoggingConfig.getThreshold()).thenReturn(200L);
    }

    @Test
    public void testQueryIndex_whenSearchHitsIsNotEmpty() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        ScrollResponse searchScrollResponse = mock(ScrollResponse.class);
        TotalHits totalHits = mock(TotalHits.class);

        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);

        Map<String, List<String>> highlightFields = getHighlightFields();
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String scrollId = "scrollId";
        long totalHitsCount = 1L;

        // getCursor on cursorSettings returns scrollId in this case
        doReturn(scrollId).when(cursorSettings).getCursor();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(cursor).when(searchRequest).getCursor();
        doReturn(searchScrollResponse).when(client).scroll(any(ScrollRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchScrollResponse).hits();
        doReturn(scrollId).when(searchScrollResponse).scrollId();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();
        doReturn(hits).when(searchHits).hits();
        doReturn(highlightFields).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();

        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<ScrollRequest> searchScrollRequestArgumentCaptor = ArgumentCaptor.forClass(ScrollRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).scroll(searchScrollRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(cursorCache).get(cursorArgumentCaptor.capture());

        ScrollRequest scrollRequest = searchScrollRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();

        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("highlight"));
        assertEquals(((Map<String, List<String>>)obtainedQueryResponse.getResults().get(0).get("highlight")).get(name), List.of(text));
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(scrollRequest.scrollId(), scrollId);
        assertEquals(searchRequestCursor, cursor);

        verify(this.auditLogger, times(1)).queryIndexWithCursorSuccess(Lists.newArrayList(searchRequest.toString()));
        verify(this.perfLogger, times(1)).log(eq(searchRequest), anyLong(), eq(200));
    }

    @Test
    public void testQueryIndex_whenSearchHitsIsEmpty() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        ScrollResponse searchScrollResponse = mock(ScrollResponse.class);
        TotalHits totalHits = mock(TotalHits.class);

        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        String cursor = "cursor";
        String scrollId = "scrollId";
        long totalHitsCount = 0L;

        // getCursor on cursorSettings returns scrollId in this case
        doReturn(scrollId).when(cursorSettings).getCursor();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(cursor).when(searchRequest).getCursor();
        doReturn(searchScrollResponse).when(client).scroll(any(ScrollRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchScrollResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();

        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<ScrollRequest> searchScrollRequestArgumentCaptor = ArgumentCaptor.forClass(ScrollRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).scroll(searchScrollRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(cursorCache).get(cursorArgumentCaptor.capture());

        ScrollRequest scrollRequest = searchScrollRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();

        assertEquals(obtainedQueryResponse.getResults().size(), 0);
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(scrollRequest.scrollId(), scrollId);
        assertEquals(searchRequestCursor, cursor);
    }

    @Test
    public void testQueryIndex_whenNoCursorInSearchQuery() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        TotalHits totalHits = mock(TotalHits.class);

        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);

        Map<String, List<String>> highlightFields = getHighlightFields();
        Map<String, Object> hitFields = new HashMap<>();
        long totalHitsCount = 1L;

        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(highlightFields).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));

        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("highlight"));
        assertEquals(((Map<String, List<String>>)obtainedQueryResponse.getResults().get(0).get("highlight")).get(name), List.of(text));
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
    }

    @Test
    public void testQueryIndex_whenNoCursorInSearchQueryAndSearchHitsIsEmpty() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        SearchResponse searchScrollResponse = mock(SearchResponse.class);
        TotalHits totalHits = mock(TotalHits.class);

        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        long totalHitsCount = 0L;

        doReturn(searchHits).when(searchScrollResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();
        doReturn(searchScrollResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        when(featureFlag.isFeatureEnabled(POLICY_FEATURE_NAME)).thenReturn(false);

        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        assertEquals(obtainedQueryResponse.getResults().size(), 0);
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
    }

    @Test(expected = AppException.class)
    public void testQueryIndex_whenMismatchCursorIssuerAndConsumer_thenThrowException() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);

        String cursor = "cursor";
        String mismatchUserId = "mismatchUserId";

        doReturn(cursor).when(searchRequest).getCursor();
        doReturn(mismatchUserId).when(cursorSettings).getUserId();
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 403;
            AppError error = e.getError();
            assertEquals(error.getCode(), errorCode);
            assertEquals(error.getReason(), "cursor issuer doesn't match the cursor consumer");
            assertEquals(error.getMessage(), "cursor sharing is forbidden");
            throw (e);
        }
    }


    @Test(expected = AppException.class)
    public void testQueryIndex_whenCursorSettingsNotFoundInCursorCache_thenThrowException() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);

        String cursor = "cursor";

        doReturn(cursor).when(searchRequest).getCursor();
        doReturn(null).when(cursorCache).get(any());

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 400;
            AppError error = e.getError();
            assertEquals(error.getReason(), "Can't find the given cursor");
            assertEquals(error.getMessage(), "The given cursor is invalid or expired");
            assertEquals(error.getCode(), errorCode);
            throw (e);
        }
    }

    @Test
    public void testQueryIndex_whenCursorNotFound_thenThrowException() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        doReturn("cursor").when(searchRequest).getCursor();
        doReturn("cursor").when(cursorSettings).getCursor();
        doReturn(userId).when(cursorSettings).getUserId();

        ElasticsearchException exception = mock(ElasticsearchException.class);
        doReturn(HttpServletResponse.SC_NOT_FOUND).when(exception).status();
        doReturn("No search context found for id [47500324]").when(exception).getMessage();
        doThrow(exception).when(client).scroll(any(ScrollRequest.class), eq((Type)Map.class));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 400;
            AppError error = e.getError();
            assertEquals(error.getReason(), "Can't find the given cursor");
            assertEquals(error.getMessage(), "The given cursor is invalid or expired");
            assertEquals(error.getCode(), errorCode);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryIndex_whenResponseTooLong_thenThrowException() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        doReturn("cursor").when(searchRequest).getCursor();
        doReturn("cursor").when(cursorSettings).getCursor();
        doReturn(userId).when(cursorSettings).getUserId();

        IOException exception = mock(IOException.class);
        doReturn(new ContentTooLongException(null)).when(exception).getCause();
        doThrow(exception).when(client).scroll(any(ScrollRequest.class), eq((Type)Map.class));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 413;
            AppError error = e.getError();
            assertEquals(error.getReason(), "Response is too long");
            assertEquals(error.getMessage(), "Elasticsearch response is too long, max is 100Mb");
            assertEquals(error.getCode(), errorCode);
            throw (e);
        }
    }

    private Map<String, List<String>> getHighlightFields() {
        Map<String, List<String>> highlightFields = new HashMap<>();
        highlightFields.put(name, List.of(text));
        return highlightFields;
    }

    @Test
    public void refreshCursorCache() {
        String cursor = sut.refreshCursorCache(rawCursor, userId);
        assertNotNull(cursor);
    }

    @Test
    public void refreshCursorCache_rawCursorNull() {
        String cursor = sut.refreshCursorCache(null, userId);
        assertNull(cursor);
    }

    @Test(expected = AppException.class)
    public void testQueryIndex_whenSearchGives500_thenThrowException() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        doReturn(null).when(searchRequest).getCursor();
        AppException ex = new AppException(500, reason, message);
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();
        doThrow(ex).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 500;
            AppError error = e.getError();
            assertEquals(error.getCode(), errorCode);
            assertEquals(error.getReason(), reason);
            assertEquals(error.getMessage(), message);
            throw (e);
        }
    }
}
