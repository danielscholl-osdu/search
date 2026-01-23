// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.info.ConnectedOuterService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudConnectedOuterServicesBuilderTest {

    @Mock private ElasticClientHandler elasticClientHandler;
    @Mock private IElasticSettingService elasticSettingService;

    @Test
    void buildConnectedOuterServices_includesRedisInfo() {
        ElasticClientHandler elasticClientHandler = mock(ElasticClientHandler.class);
        IElasticSettingService elasticSettingService = mock(IElasticSettingService.class);

        RedisCache cache = mock(RedisCache.class);
        when(cache.info()).thenReturn("# Server\r\nredis_version:7.2.4\r\n");

        CloudConnectedOuterServicesBuilder builder =
                new CloudConnectedOuterServicesBuilder(List.of(cache), elasticClientHandler, elasticSettingService);

        List<ConnectedOuterService> services = builder.buildConnectedOuterServices();

        assertEquals(1, services.size());
        ConnectedOuterService s = services.get(0);

        assertTrue(s.getName().startsWith("Redis-"));
        assertEquals("7.2.4", s.getVersion());
    }

    @Test
    void buildConnectedOuterServices_includesElasticInfo_perCluster() throws IOException, ElasticsearchException {
        ClusterSettings cs = mock(ClusterSettings.class);
        when(elasticSettingService.getAllClustersSettings())
                .thenReturn(Map.of("tenant-a", cs));

        ElasticsearchClient esClient = mock(ElasticsearchClient.class);
        InfoResponse info = mock(InfoResponse.class);
        ElasticsearchVersionInfo versionInfo = mock(ElasticsearchVersionInfo.class);
        when(versionInfo.number()).thenReturn("8.15.2");
        when(info.version()).thenReturn(versionInfo);
        when(esClient.info()).thenReturn(info);
        when(elasticClientHandler.getOrCreateRestClient("tenant-a")).thenReturn(esClient);

        CloudConnectedOuterServicesBuilder builder =
                new CloudConnectedOuterServicesBuilder(Collections.emptyList(), elasticClientHandler, elasticSettingService);

        List<ConnectedOuterService> out = builder.buildConnectedOuterServices();

        assertEquals(1, out.size());
        ConnectedOuterService s = out.get(0);
        assertEquals("ElasticSearch-tenant-a", s.getName());
        assertEquals("8.15.2", s.getVersion());
    }

    @Test
    void buildConnectedOuterServices_returnsNA_whenClusterSettingsFetchFails() {
        when(elasticSettingService.getAllClustersSettings())
                .thenThrow(new AppException(500, "Error", "Cannot fetch"));

        CloudConnectedOuterServicesBuilder builder =
                new CloudConnectedOuterServicesBuilder(Collections.emptyList(), elasticClientHandler, elasticSettingService);

        List<ConnectedOuterService> out = builder.buildConnectedOuterServices();

        assertEquals(1, out.size());
        ConnectedOuterService s = out.get(0);
        assertEquals("ElasticSearch-N/A", s.getName());
        assertEquals("N/A", s.getVersion());
    }

    @Test
    void buildConnectedOuterServices_returnsNA_whenClientCreationFails() {
        ClusterSettings cs = mock(ClusterSettings.class);
        when(elasticSettingService.getAllClustersSettings())
                .thenReturn(Map.of("tenant-b", cs));

        when(elasticClientHandler.getOrCreateRestClient("tenant-b"))
                .thenThrow(new AppException(500, "Error", "create client failed"));

        CloudConnectedOuterServicesBuilder builder =
                new CloudConnectedOuterServicesBuilder(Collections.emptyList(), elasticClientHandler, elasticSettingService);

        List<ConnectedOuterService> out = builder.buildConnectedOuterServices();

        assertEquals(1, out.size());
        ConnectedOuterService s = out.get(0);
        assertEquals("ElasticSearch-tenant-b", s.getName());
        assertEquals("N/A", s.getVersion());
    }

    @Test
    void buildConnectedOuterServices_returnsEmpty_whenNoRedisAndNoElastic() {
        when(elasticSettingService.getAllClustersSettings()).thenReturn(Collections.emptyMap());

        CloudConnectedOuterServicesBuilder builder =
                new CloudConnectedOuterServicesBuilder(Collections.emptyList(), elasticClientHandler, elasticSettingService);

        List<ConnectedOuterService> out = builder.buildConnectedOuterServices();

        assertTrue(out.isEmpty());
    }
}
