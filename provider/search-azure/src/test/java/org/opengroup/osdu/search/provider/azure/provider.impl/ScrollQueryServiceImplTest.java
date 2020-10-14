package org.opengroup.osdu.search.provider.azure.provider.impl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Before;
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
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.ElasticClientHandler;

import java.util.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScrollQueryServiceImplTest {

    private static final String dataPartitionId = "data-partition-id";
    private static final String indexName = "index";
    private static final String userId = "userId";
    private static final String name = "name";
    private static final String text = "text";

    private CursorSettings cursorSettings;
    private RestHighLevelClient client;
    private SearchHits searchHits;
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

    @InjectMocks
    private ScrollQueryServiceImpl sut;

    @Before
    public void init() {
        cursorSettings = mock(CursorSettings.class);
        client = mock(RestHighLevelClient.class);
        searchHits = mock(SearchHits.class);
        searchHit = mock(SearchHit.class);
        lenient().doReturn(userId).when(dpsHeaders).getUserEmail();
        lenient().doReturn(dataPartitionId).when(dpsHeaders).getPartitionId();
        lenient().doReturn(indexName).when(crossTenantUtils).getIndexName(any(), eq(dataPartitionId));
        doReturn(cursorSettings).when(cursorCache).get(any());
        doReturn(client).when(elasticClientHandler).createRestClient();
    }

    @Test
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
    }

    @Test
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

    @Test(expected = AppException.class)
    public void testQueryIndex_whenMismatchCursorIssuerAndConsumer_thenThrowException() throws Exception {
        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);

        String cursor = "cursor";
        String mismatchUserId = "mismatchUserId";

        doReturn(cursor).when(searchRequest).getCursor();
        doReturn(mismatchUserId).when(cursorSettings).getUserId();
        doReturn(client).when(elasticClientHandler).createRestClient();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 403;
            AppError error = e.getError();
            assertEquals(error.getCode(), errorCode);
            assertThat(error.getReason(), containsString("cursor issuer doesn't match the cursor consumer"));
            assertThat(error.getMessage(), containsString("cursor sharing is forbidden"));
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
}
