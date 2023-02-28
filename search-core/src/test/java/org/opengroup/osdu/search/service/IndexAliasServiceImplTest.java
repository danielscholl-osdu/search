package org.opengroup.osdu.search.service;


import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.rest.RestStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.search.cache.IndexAliasCache;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class IndexAliasServiceImplTest {
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private IndexAliasCache indexAliasCache;
    @Mock
    @Lazy
    private JaxRsDpsLog log;
    @InjectMocks
    private IndexAliasServiceImpl sut;

    private RestHighLevelClient restHighLevelClient;
    private IndicesClient indicesClient;
    private GetAliasesResponse getAliasesResponse;

    private static String kind = "common:welldb:wellbore:1.2.0";
    private static String index = "common-welldb-wellbore-1.2.0";
    private static String alias = "a1234567890";

    @Before
    public void setup() {
        initMocks(this);
        indicesClient = PowerMockito.mock(IndicesClient.class);
        restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);
        getAliasesResponse = PowerMockito.mock(GetAliasesResponse.class);
    }

    @Test
    public void getInDicesAliases_when_kind_is_not_supported_for_alias() throws IOException {
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(false);
        List<String> kinds = Arrays.asList(kind);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        assertTrue(kindAliasMap.isEmpty());
    }

    @Test
    public void getInDicesAliases_when_alias_exist() throws IOException {
        setup_when_alias_exist();

        List<String> kinds = Arrays.asList(kind);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        verify(this.indicesClient, times(1)).getAlias(any(), any());
        assertTrue(kindAliasMap.containsKey(kind));
        assertEquals(alias, kindAliasMap.get(kind));
    }

    @Test
    public void getInDicesAliases_when_alias_exist_with_cache_take_effect() throws IOException {
        setup_when_alias_exist();

        List<String> kinds = Arrays.asList(kind);
        ArgumentCaptor<String> kindCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aliasCapture = ArgumentCaptor.forClass(String.class);
        doNothing().when(indexAliasCache).put(kindCapture.capture(), aliasCapture.capture());
        when(indexAliasCache.get(anyString())).thenAnswer(invocation -> {
            if(kindCapture.getAllValues().size() == 0)
                return null;

            String k = invocation.getArgument(0);
            if(k.equals(kindCapture.getValue()))
                return aliasCapture.getValue();
            else
                return null;
        });
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        verify(this.indicesClient, times(1)).getAlias(any(), any());
        assertTrue(kindAliasMap.containsKey(kind));
        assertEquals(alias, kindAliasMap.get(kind));

        // call again. It should get from cache
        sut.getIndicesAliases(kinds);
        verify(this.indicesClient, times(1)).getAlias(any(), any());
    }

    @Test
    public void getInDicesAliases_when_alias_not_exist_and_create_alias_successfully() throws IOException {
        setup_when_alias_not_exist_and_try_create_alias(true);

        List<String> kinds = Arrays.asList(kind);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        assertTrue(kindAliasMap.containsKey(kind));
        assertEquals(alias, kindAliasMap.get(kind));
    }

    @Test
    public void getInDicesAliases_when_alias_not_exist_and_fail_create_alias() throws IOException {
        setup_when_alias_not_exist_and_try_create_alias(false);

        List<String> kinds = Arrays.asList(kind);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        assertTrue(kindAliasMap.isEmpty());
    }

    private void setup_when_alias_exist() throws IOException {
        Map<String, Set<AliasMetadata>> aliases = new HashMap<>();
        Set<AliasMetadata> aliasMetadataSet = new HashSet<>();
        aliasMetadataSet.add(AliasMetadata.builder(alias).build());
        aliases.put(index, aliasMetadataSet);

        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(alias);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasesRequest.class), any(RequestOptions.class))).thenReturn(getAliasesResponse);
        when(getAliasesResponse.status()).thenReturn(RestStatus.OK);
        when(getAliasesResponse.getAliases()).thenReturn(aliases);
    }

    private void setup_when_alias_not_exist_and_try_create_alias(boolean create_ok) throws IOException {
        GetAliasesResponse getAliasesResponseWithAliasConstraint = PowerMockito.mock(GetAliasesResponse.class);
        Map<String, Set<AliasMetadata>> aliases = new HashMap<>();
        Set<AliasMetadata> aliasMetadataSet = new HashSet<>();
        aliasMetadataSet.add(AliasMetadata.builder("otherAlias").build());
        aliases.put("otherIndex", aliasMetadataSet);
        AcknowledgedResponse updateAliasesResponse = new AcknowledgedResponse(create_ok);
        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(index);
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(alias);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasesRequest.class), any(RequestOptions.class)))
                .thenAnswer(invocation ->
                {
                    GetAliasesRequest request = invocation.getArgument(0);
                    if(request.aliases().length == 0)
                        return getAliasesResponse;
                    else
                        return getAliasesResponseWithAliasConstraint;
                });
        when(getAliasesResponse.status()).thenReturn(RestStatus.OK);
        when(getAliasesResponse.getAliases()).thenReturn(aliases);
        when(getAliasesResponseWithAliasConstraint.status()).thenReturn(RestStatus.NOT_FOUND);
        when(indicesClient.updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class))).thenReturn(updateAliasesResponse);
    }
}
