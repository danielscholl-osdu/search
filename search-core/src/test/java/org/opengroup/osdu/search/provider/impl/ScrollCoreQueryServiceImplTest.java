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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.ContentTooLongException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
@Ignore
//TODO:
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
    private RestHighLevelClient client;

    @Mock
    private SearchHits searchHits;

    @Mock
    private IQueryPerformanceLogger perfLogger;

    @Mock
    private SearchHit searchHit;

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
    @InjectMocks
    private ScrollCoreQueryServiceImpl sut;

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
    @Ignore
    public void testQueryIndex_whenSearchHitsIsNotEmpty() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        SearchResponse searchScrollResponse = mock(SearchResponse.class);

        SearchHit[] hits = {searchHit};
        Map<String, HighlightField> highlightFields = getHighlightFields();
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String scrollId = "scrollId";
        long totalHitsCount = 1L;

        // getCursor on cursorSettings returns scrollId in this case
        doReturn(scrollId).when(cursorSettings).getCursor();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(cursor).when(searchRequest).getCursor();
        doReturn(searchScrollResponse).when(client).scroll(any(), any());
        doReturn(searchHits).when(searchScrollResponse).getHits();
        doReturn(scrollId).when(searchScrollResponse).getScrollId();
        doReturn(hits).when(searchHits).getHits();
        doReturn(totalHitsCount).when(searchHits).getTotalHits();
        doReturn(highlightFields).when(searchHit).getHighlightFields();
        doReturn(hitFields).when(searchHit).getSourceAsMap();

        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<SearchScrollRequest> searchScrollRequestArgumentCaptor = ArgumentCaptor.forClass(SearchScrollRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).scroll(searchScrollRequestArgumentCaptor.capture(), eq(RequestOptions.DEFAULT));
        verify(cursorCache).get(cursorArgumentCaptor.capture());

        SearchScrollRequest scrollRequest = searchScrollRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();

        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("name"));
        assertEquals(obtainedQueryResponse.getResults().get(0).get("name"), "text");
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(scrollRequest.scrollId(), scrollId);
        assertEquals(searchRequestCursor, cursor);

        verify(this.auditLogger, times(1)).queryIndexWithCursorSuccess(Lists.newArrayList(searchRequest.toString()));
        verify(this.perfLogger, times(1)).log(searchRequest, 0L, 200);
    }

    @Test
    @Ignore
    public void testQueryIndex_whenSearchHitsIsEmpty() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        SearchResponse searchScrollResponse = mock(SearchResponse.class);

        SearchHit[] hits = {};
        String cursor = "cursor";
        String scrollId = "scrollId";
        long totalHitsCount = 0L;

        // getCursor on cursorSettings returns scrollId in this case
        doReturn(scrollId).when(cursorSettings).getCursor();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(cursor).when(searchRequest).getCursor();
        doReturn(searchScrollResponse).when(client).scroll(any(), any());
        doReturn(searchHits).when(searchScrollResponse).getHits();
        doReturn(hits).when(searchHits).getHits();
        doReturn(totalHitsCount).when(searchHits).getTotalHits();

        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        ArgumentCaptor<SearchScrollRequest> searchScrollRequestArgumentCaptor = ArgumentCaptor.forClass(SearchScrollRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).scroll(searchScrollRequestArgumentCaptor.capture(), eq(RequestOptions.DEFAULT));
        verify(cursorCache).get(cursorArgumentCaptor.capture());

        SearchScrollRequest scrollRequest = searchScrollRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();

        assertEquals(obtainedQueryResponse.getResults().size(), 0);
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(scrollRequest.scrollId(), scrollId);
        assertEquals(searchRequestCursor, cursor);
    }

    @Test
    @Ignore
    public void testQueryIndex_whenNoCursorInSearchQuery() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        SearchResponse searchScrollResponse = mock(SearchResponse.class);

        SearchHit[] hits = {searchHit};
        Map<String, HighlightField> highlightFields = getHighlightFields();
        long totalHitsCount = 1L;

        doReturn(searchHits).when(searchScrollResponse).getHits();
        doReturn(hits).when(searchHits).getHits();
        doReturn(highlightFields).when(searchHit).getHighlightFields();
        doReturn(totalHitsCount).when(searchHits).getTotalHits();
        doReturn(searchScrollResponse).when(client).search(any(), any(RequestOptions.class));

        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("name"));
        assertEquals(obtainedQueryResponse.getResults().get(0).get("name"), "text");
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
    }

    @Test
    public void testQueryIndex_whenNoCursorInSearchQueryAndSearchHitsIsEmpty() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        SearchResponse searchScrollResponse = mock(SearchResponse.class);


        SearchHit[] hits = {};
        long totalHitsCount = 0L;

        doReturn(searchHits).when(searchScrollResponse).getHits();
        when(searchScrollResponse.status()).thenReturn(RestStatus.OK);
        doReturn(hits).when(searchHits).getHits();
        doReturn(searchScrollResponse).when(client).search(any(), any(RequestOptions.class));

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
        doReturn(userId).when(cursorSettings).getUserId();

        ElasticsearchStatusException exception = mock(ElasticsearchStatusException.class);
        doReturn(RestStatus.NOT_FOUND).when(exception).status();
        doReturn("No search context found for id [47500324]").when(exception).getMessage();
        doThrow(exception).when(client).scroll(any(), any(RequestOptions.class));

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
        doReturn(userId).when(cursorSettings).getUserId();

        IOException exception = mock(IOException.class);
        doReturn(new ContentTooLongException(null)).when(exception).getCause();
        doThrow(exception).when(client).scroll(any(), any(RequestOptions.class));

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

    private Map<String, HighlightField> getHighlightFields() {
        Text[] fragments = {new Text(text)};
        HighlightField highlightField = new HighlightField(name, fragments);
        Map<String, HighlightField> highlightFields = new HashMap<>();
        highlightFields.put("highlightField", highlightField);
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
        doThrow(ex).when(client).search(any(), any(RequestOptions.class));
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
