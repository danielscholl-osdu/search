package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;

@RunWith(MockitoJUnitRunner.class)
public class DetailedBadRequestMessageUtilTest {

    private static final String NESTED_FAIL_REASON = "failed to create query: [nested] failed to find nested object under path [data.NameAliases]";
    private static final String GEO_FIELD_FAIL_REASON = "failed to find geo field [officeAddress]";
    private static final String GENERIC_FAIL_REASON = "Invalid parameters were given on search request";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DetailedBadRequestMessageUtil badRequestMessageUtil;

    private Throwable[] throwable;

    @Mock
    private SearchRequest searchRequest;

    @Mock
    private SearchConfigurationProperties configurationProperties;

    @Before
    public void setUp() {
        when(this.configurationProperties.getMaxExceptionLogMessageLength()).thenReturn(5000);
        badRequestMessageUtil = new DetailedBadRequestMessageUtil(objectMapper, configurationProperties);
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
        when(httpEntityMock.getContentLength()).thenReturn(100L);

        String detailedBadRequestMessage = badRequestMessageUtil.getDetailedBadRequestMessage(searchRequest, elasticsearchStatusExceptionMock);
        assertEquals(NESTED_FAIL_REASON, detailedBadRequestMessage);
    }
    
    @Test
    public void testMultipleResponse() throws IOException {
        ResponseException responseExceptionMock = Mockito.mock(ResponseException.class);
        Response responseMock = Mockito.mock(Response.class);
        HttpEntity httpEntityMock = Mockito.mock(HttpEntity.class);

        when(httpEntityMock.getContentLength()).thenReturn(100L);
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
    public void testSingleResponse_maxLength() throws IOException {
        ResponseException responseExceptionMock = Mockito.mock(ResponseException.class);
        Response responseMock = Mockito.mock(Response.class);
        HttpEntity httpEntityMock = Mockito.mock(HttpEntity.class);
        ElasticsearchException elasticsearchStatusExceptionMock = Mockito.mock(ElasticsearchException.class);
        throwable = new Throwable[]{responseExceptionMock};
        when(responseExceptionMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getEntity()).thenReturn(httpEntityMock);
        when(elasticsearchStatusExceptionMock.getSuppressed()).thenReturn(throwable);
        when(httpEntityMock.getContentLength()).thenReturn(10000L);
        String detailedBadRequestMessage = badRequestMessageUtil.getDetailedBadRequestMessage(searchRequest, elasticsearchStatusExceptionMock);
        assertEquals(GENERIC_FAIL_REASON, detailedBadRequestMessage);
    }

    private InputStream getResponseContent(String fileName) {
        return this.getClass().getResourceAsStream("/errorresponses/" + fileName);
    }
}
