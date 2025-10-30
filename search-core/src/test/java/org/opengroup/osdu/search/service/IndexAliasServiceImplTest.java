/*
 *  Copyright © Schlumberger
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.service;


import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import java.io.IOException;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.search.cache.MultiPartitionIndexAliasCache;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.springframework.context.annotation.Lazy;

@RunWith(MockitoJUnitRunner.class)
public class IndexAliasServiceImplTest {

    private static final String KIND = "common:welldb:wellbore:1.2.0";
    private static final String INDEX = "common-welldb-wellbore-1.2.0";
    private static final String ALIAS = "a-714731401";
    private static final String KIND_WITH_MAJOR = "common:welldb:wellbore:1.*.*";
    private static final String INDEX_WITH_MAJOR = "common-welldb-wellbore-1.*.*";
    private static final String ALIAS_FOR_KIND_WITH_MAJOR = "a-714739095";
    private static final String OTHER_INDEX = "otherIndex";
    private static final String OTHER_ALIAS = "otherAlias";

    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private MultiPartitionIndexAliasCache indexAliasCache;
    @Mock
    @Lazy
    private JaxRsDpsLog log;
    @InjectMocks
    private IndexAliasServiceImpl sut;

    private ElasticsearchClient restHighLevelClient;
    private ElasticsearchIndicesClient indicesClient;
    private GetAliasResponse getAliasesResponse;

    @Before
    public void setup() {
        initMocks(this);
        indicesClient = mock(ElasticsearchIndicesClient.class);
        restHighLevelClient = mock(ElasticsearchClient.class);
        getAliasesResponse = mock(GetAliasResponse.class);
    }

    @Test
    public void getIndicesAliases_when_kind_is_not_supported_for_alias() throws IOException {
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(false);
        List<String> kinds = Collections.singletonList(KIND);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        assertTrue(kindAliasMap.isEmpty());
    }

    @Test
    public void getIndicesAliases_when_alias_exist() throws IOException {
        setup_when_alias_exist();

        List<String> kinds = Collections.singletonList(KIND);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);

        verify(this.indicesClient, times(1)).getAlias(any(GetAliasRequest.class));
        assertTrue(kindAliasMap.containsKey(KIND));
        assertEquals(ALIAS, kindAliasMap.get(KIND));
    }

    @Test
    public void getIndicesAliases_when_alias_exist_with_cache_take_effect() throws IOException {
        setup_when_alias_exist();

        List<String> kinds = Collections.singletonList(KIND);
        ArgumentCaptor<String> kindCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aliasCapture = ArgumentCaptor.forClass(String.class);
        doNothing().when(indexAliasCache).put(kindCapture.capture(), aliasCapture.capture());
        when(indexAliasCache.get(anyString())).thenAnswer(invocation -> {
            if (kindCapture.getAllValues().isEmpty())
                return null;

            String k = invocation.getArgument(0);
            if (k.equals(kindCapture.getValue()))
                return aliasCapture.getValue();
            else
                return null;
        });
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);

        verify(this.indicesClient, times(1)).getAlias(any(GetAliasRequest.class));
        assertTrue(kindAliasMap.containsKey(KIND));
        assertEquals(ALIAS, kindAliasMap.get(KIND));

        // call again. It should get from cache
        sut.getIndicesAliases(kinds);

        verify(this.indicesClient, times(1)).getAlias(any(GetAliasRequest.class));
    }

    @Test
    public void getIndicesAliases_when_alias_not_exist_and_create_alias_successfully() throws IOException {
        setup_when_alias_not_exist_and_try_create_alias(true);

        List<String> kinds = Collections.singletonList(KIND);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        assertTrue(kindAliasMap.containsKey(KIND));
        assertEquals(ALIAS, kindAliasMap.get(KIND));
        verify(this.indicesClient, times(2)).getAlias(any(GetAliasRequest.class));
    }

    @Test
    public void getIndicesAliases_for_major_version_when_alias_not_exist_and_create_alias_successfully() throws IOException {
        setup__for_major_version_when_alias_not_exist_and_try_create_alias(true);

        List<String> kinds = List.of(KIND_WITH_MAJOR);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        assertTrue(kindAliasMap.containsKey(KIND_WITH_MAJOR));
        assertEquals(ALIAS_FOR_KIND_WITH_MAJOR, kindAliasMap.get(KIND_WITH_MAJOR));
        verify(this.indicesClient, times(1)).getAlias(any(GetAliasRequest.class));
    }

    @Test
    public void getIndicesAliases_when_alias_not_exist_and_fail_create_alias() throws IOException {
        setup_when_alias_not_exist_and_try_create_alias(false);

        List<String> kinds = Collections.singletonList(KIND);
        Map<String, String> kindAliasMap = sut.getIndicesAliases(kinds);
        assertTrue(kindAliasMap.isEmpty());
    }

    private void setup_when_alias_exist() throws IOException {
        Map<String, IndexAliases> aliases = new HashMap<>();
        AliasDefinition aliasDefinition = new AliasDefinition.Builder().build();
        IndexAliases indexAliases = new IndexAliases.Builder()
                .aliases(ALIAS, aliasDefinition)
                .build();
        aliases.put(INDEX, indexAliases);

        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(ALIAS);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(elasticClientHandler.getOrCreateRestClient()).thenReturn(restHighLevelClient);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasRequest.class))).thenReturn(getAliasesResponse);
        when(getAliasesResponse.result()).thenReturn(aliases);
    }

    private void setup_when_alias_not_exist_and_try_create_alias(boolean create_ok) throws IOException {
        GetAliasResponse getAliasesResponseWithAliasConstraint = mock(GetAliasResponse.class);

        Map<String,IndexAliases> aliases = new HashMap<>();
        AliasDefinition aliasDefinition = new AliasDefinition.Builder().build();

        IndexAliases indexAliases = new IndexAliases.Builder()
                .aliases(OTHER_ALIAS, aliasDefinition)
                .build();
        aliases.put(OTHER_INDEX, indexAliases);

        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(INDEX);
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(ALIAS);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(elasticClientHandler.getOrCreateRestClient()).thenReturn(restHighLevelClient);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasRequest.class)))
                .thenAnswer(invocation ->
                {
                    GetAliasRequest request = invocation.getArgument(0);
                    if (request.index().isEmpty())
                        return getAliasesResponse;
                    else
                        return getAliasesResponseWithAliasConstraint;
                });
        when(getAliasesResponse.result()).thenReturn(aliases);
        when(indicesClient.updateAliases(any(UpdateAliasesRequest.class)))
            .thenReturn(UpdateAliasesResponse.of(uar -> uar.acknowledged(create_ok)));
    }

    private void setup__for_major_version_when_alias_not_exist_and_try_create_alias(boolean create_ok) throws IOException {
        GetAliasResponse getAliasesResponseWithAliasConstraint = mock(GetAliasResponse.class);

        Map<String,IndexAliases> aliases = new HashMap<>();
        AliasDefinition aliasDefinition = new AliasDefinition.Builder().build();

        IndexAliases indexAliases = new IndexAliases.Builder()
                .aliases(OTHER_ALIAS, aliasDefinition)
                .build();
        aliases.put(OTHER_INDEX, indexAliases);


        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(INDEX_WITH_MAJOR);
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(ALIAS_FOR_KIND_WITH_MAJOR);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(elasticClientHandler.getOrCreateRestClient()).thenReturn(restHighLevelClient);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasRequest.class)))
                .thenAnswer(invocation ->
                {
                    GetAliasRequest request = invocation.getArgument(0);
                    if (request.index().isEmpty())
                        return getAliasesResponse;
                    else
                        return getAliasesResponseWithAliasConstraint;
                });
        when(getAliasesResponse.result()).thenReturn(aliases);
        when(indicesClient.updateAliases(any(UpdateAliasesRequest.class)))
                .thenReturn(UpdateAliasesResponse.of(uar -> uar.acknowledged(create_ok)));
    }
}
