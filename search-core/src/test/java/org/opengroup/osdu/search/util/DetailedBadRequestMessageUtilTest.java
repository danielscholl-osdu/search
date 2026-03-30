package org.opengroup.osdu.search.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DetailedBadRequestMessageUtilTest {

    private static final String NESTED_FAIL_REASON = "failed to create query: [nested] failed to find nested object under path [data.NameAliases]";
    private static final String GEO_FIELD_FAIL_REASON = "failed to find geo field [officeAddress]";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DetailedBadRequestMessageUtil badRequestMessageUtil;

    private Throwable[] throwable;

    @Mock
    private SearchRequest searchRequest;

    @BeforeEach
    public void setUp() {
        badRequestMessageUtil = new DetailedBadRequestMessageUtil(objectMapper);
    }
    
    @Test
    public void testSingleResponse() throws IOException {
        ResponseException responseExceptionMock = Mockito.mock(ResponseException.class);
        Response responseMock = Mockito.mock(Response.class);
        HttpEntity httpEntityMock = Mockito.mock(HttpEntity.class);
        ElasticsearchException elasticsearchStatusExceptionMock = Mockito.mock(ElasticsearchException.class);

        throwable = new Throwable[]{responseExceptionMock};

        when(responseExceptionMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(getResponseContent("nestedfail.json"));
        when(elasticsearchStatusExceptionMock.getSuppressed()).thenReturn(throwable);

        String detailedBadRequestMessage = badRequestMessageUtil.getDetailedBadRequestMessage(searchRequest, elasticsearchStatusExceptionMock);
        assertEquals(NESTED_FAIL_REASON, detailedBadRequestMessage);
    }
    
    @Test
    public void testMultipleResponse() throws IOException {
        ResponseException responseExceptionMock = Mockito.mock(ResponseException.class);
        Response responseMock = Mockito.mock(Response.class);
        HttpEntity httpEntityMock = Mockito.mock(HttpEntity.class);

        when(responseExceptionMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(getResponseContent("nestedfail.json"));

        ResponseException secondResponseExceptionMock = Mockito.mock(ResponseException.class);
        Response secondResponseMock = Mockito.mock(Response.class);
        HttpEntity secondHttpEntityMock = Mockito.mock(HttpEntity.class);

        when(secondResponseExceptionMock.getResponse()).thenReturn(secondResponseMock);
        when(secondResponseMock.getEntity()).thenReturn(secondHttpEntityMock);
        when(secondHttpEntityMock.getContent()).thenReturn(getResponseContent("geofieldfail.json"));

        ElasticsearchException elasticsearchStatusExceptionMock = Mockito.mock(ElasticsearchException.class);

        throwable = new Throwable[]{responseExceptionMock, secondResponseExceptionMock};

        when(elasticsearchStatusExceptionMock.getSuppressed()).thenReturn(throwable);

        String detailedBadRequestMessage = badRequestMessageUtil.getDetailedBadRequestMessage(searchRequest, elasticsearchStatusExceptionMock);
        assertEquals(NESTED_FAIL_REASON + "." + GEO_FIELD_FAIL_REASON, detailedBadRequestMessage);
    }

    @Test
    public void testElasticsearchException_withSortFieldError_returnsSortMessage() {
        String sortErrorReason = "Text fields are not optimised for operations that require per-document field data like aggregations and sorting";
        ElasticsearchException elasticsearchException = buildMockEsException(sortErrorReason);

        SearchRequest sortRequest = mock(SearchRequest.class);
        SortOptions sortOption = mock(SortOptions.class);
        when(sortRequest.sort()).thenReturn(List.of(sortOption));

        String result = badRequestMessageUtil.getDetailedBadRequestMessage(sortRequest, elasticsearchException);
        assertEquals("Sort is not supported for one or more of the requested fields", result);
    }

    @Test
    public void testElasticsearchException_withAggregationFieldError_returnsAggregationMessage() {
        String aggErrorReason = "Text fields are not optimised for operations that require per-document field data like aggregations and sorting";
        ElasticsearchException elasticsearchException = buildMockEsException(aggErrorReason);

        SearchRequest aggRequest = mock(SearchRequest.class);
        when(aggRequest.sort()).thenReturn(Collections.emptyList());
        Aggregation aggregation = mock(Aggregation.class);
        when(aggRequest.aggregations()).thenReturn(Map.of("agg", aggregation));

        String result = badRequestMessageUtil.getDetailedBadRequestMessage(aggRequest, elasticsearchException);
        assertEquals("Aggregations are not supported for one or more of the specified fields", result);
    }

    @Test
    public void testElasticsearchException_withGeoSortError_returnsSortMessage() {
        String geoSortReason = "can't sort on geo_shape field without using specific sorting feature, like geo_distance";
        ElasticsearchException elasticsearchException = buildMockEsException(geoSortReason);

        SearchRequest sortRequest = mock(SearchRequest.class);
        SortOptions sortOption = mock(SortOptions.class);
        when(sortRequest.sort()).thenReturn(List.of(sortOption));

        String result = badRequestMessageUtil.getDetailedBadRequestMessage(sortRequest, elasticsearchException);
        assertEquals("Sort is not supported for one or more of the requested fields", result);
    }

    @Test
    public void testElasticsearchException_withNullCausedBy_returnsDefault() {
        ElasticsearchException elasticsearchException = mock(ElasticsearchException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        ErrorCause rootError = mock(ErrorCause.class);

        when(elasticsearchException.getSuppressed()).thenReturn(new Throwable[0]);
        when(elasticsearchException.response()).thenReturn(errorResponse);
        when(errorResponse.error()).thenReturn(rootError);
        when(rootError.causedBy()).thenReturn(null);

        String result = badRequestMessageUtil.getDetailedBadRequestMessage(searchRequest, elasticsearchException);
        assertEquals("Invalid parameters were given on search request", result);
    }

    @Test
    public void testNonElasticsearchException_withCause_returnsDefault() {
        RuntimeException cause = new RuntimeException("some unrelated error");
        RuntimeException exception = new RuntimeException("outer", cause);

        String result = badRequestMessageUtil.getDetailedBadRequestMessage(searchRequest, exception);
        assertEquals("Invalid parameters were given on search request", result);
    }

    @Test
    public void testElasticsearchException_withNullMsg_inKeywordFieldCheck_returnsDefault() {
        ElasticsearchException elasticsearchException = buildMockEsException(null);

        String result = badRequestMessageUtil.getDetailedBadRequestMessage(searchRequest, elasticsearchException);
        assertEquals("Invalid parameters were given on search request", result);
    }

    private ElasticsearchException buildMockEsException(String causeReason) {
        ElasticsearchException ex = mock(ElasticsearchException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        ErrorCause errorCause = mock(ErrorCause.class);
        ErrorCause rootError = mock(ErrorCause.class);
        when(ex.getSuppressed()).thenReturn(new Throwable[0]);
        when(ex.response()).thenReturn(errorResponse);
        when(errorResponse.error()).thenReturn(rootError);
        when(rootError.causedBy()).thenReturn(errorCause);
        when(errorCause.reason()).thenReturn(causeReason);
        return ex;
    }

    private InputStream getResponseContent(String fileName) {
        return this.getClass().getResourceAsStream("/errorresponses/" + fileName);
    }
}
