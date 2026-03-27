/*
 *  Copyright 2017-2019 © Schlumberger
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

package org.opengroup.osdu.search.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.cache.ElasticsearchClientCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;

import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
public class ElasticClientHandlerTest {

  private static final boolean SECURITY_HTTPS_CERTIFICATE_TRUST = false;
  private static final String DATA_PARTITION_ID_1 = "dp1";
  private static final String DATA_PARTITION_ID_2 = "dp2";
  private static final String SCHEME = "https";
  private static final String HOSTNAME = "H";
  private static MockedStatic<RestClient> mockedRestClients;

  @Mock
  private IElasticSettingService elasticSettingService;
  @Mock
  private RestClientBuilder builder;
  @Mock
  private RestClient restClient;
  @Mock
  private JaxRsDpsLog log;
  @Mock
  private ElasticsearchClientCache clientCache;
  @Mock
  private TenantInfo tenantInfo;
  @Mock
  private SearchConfigurationProperties properties;

  @InjectMocks
  private ElasticClientHandler elasticClientHandler;

  @BeforeEach
  public void setup() {

    mockedRestClients = mockStatic(RestClient.class);

    elasticClientHandler.setSecurityHttpsCertificateTrust(SECURITY_HTTPS_CERTIFICATE_TRUST);

    // Mock the cache to call the lambda function when computeIfAbsent is invoked
    when(clientCache.computeIfAbsent(eq(DATA_PARTITION_ID_1), any())).thenAnswer(invocation -> {
      String partitionId = invocation.getArgument(0);
      Function<String, ElasticsearchClient> supplier = invocation.getArgument(1);
      return supplier.apply(partitionId);
    });
  }

  @AfterEach
  public void close() {
    mockedRestClients.close();
  }

  @Test
  public void createRestClient_when_deployment_env_is_saas() {
    ClusterSettings clusterSettings = new ClusterSettings(HOSTNAME, 1, "U:P");
    when(elasticSettingService.getElasticClusterInformationForPartition(anyString())).thenReturn(clusterSettings);
    when(RestClient.builder(new HttpHost(HOSTNAME, 1, SCHEME))).thenAnswer(invocation -> builder);
    when(builder.build()).thenReturn(restClient);
    when(tenantInfo.getDataPartitionId()).thenReturn(DATA_PARTITION_ID_1);

    ElasticsearchClient returned = this.elasticClientHandler.getOrCreateRestClient();
    RestClientTransport clientTransport = (RestClientTransport) returned._transport();
    assertEquals(restClient, clientTransport.restClient());
  }

  @Test
  public void failed_createRestClientForSaaS_when_getcluster_info_throws_exception() {
    when(elasticSettingService.getElasticClusterInformationForPartition(anyString()))
      .thenThrow(new AppException(1, "", ""));
    when(RestClient.builder(new HttpHost(HOSTNAME, 1, SCHEME))).thenAnswer(invocation -> builder);
    when(tenantInfo.getDataPartitionId()).thenReturn(DATA_PARTITION_ID_1);
    assertThrows(AppException.class, () -> {
      this.elasticClientHandler.getOrCreateRestClient();
    });
  }

  @Test
  public void getOrCreateRestClient_shouldUsePartitionCredentials_notRequestScopeCredentials() {
    ClusterSettings clusterSettings = new ClusterSettings(HOSTNAME, 1, "U:P");

    when(elasticSettingService.getElasticClusterInformationForPartition(DATA_PARTITION_ID_1)).thenReturn(clusterSettings);
    when(RestClient.builder(new HttpHost(HOSTNAME, 1, SCHEME))).thenAnswer(invocation -> builder);
    when(builder.build()).thenReturn(restClient);

    elasticClientHandler.getOrCreateRestClient(DATA_PARTITION_ID_1);

    verify(elasticSettingService).getElasticClusterInformationForPartition(DATA_PARTITION_ID_1);
    verify(elasticSettingService, never()).getElasticClusterInformationForPartition(DATA_PARTITION_ID_2);
    verify(elasticSettingService, never()).getElasticClusterInformation();
  }
}
