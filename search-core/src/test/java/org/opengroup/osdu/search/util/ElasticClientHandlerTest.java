// Copyright 2017-2019, Schlumberger
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

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestClientBuilder.class, RestClient.class})
public class ElasticClientHandlerTest {

    private static final boolean SECURITY_HTTPS_CERTIFICATE_TRUST = false;

    @Mock
    private SearchConfigurationProperties searchConfigurationProperties;

    @Mock
    private IElasticSettingService elasticSettingService;

    @Mock
    private RestClientBuilder builder;

    @Mock
    private RestClient restClient;

    @Mock
    private JaxRsDpsLog log;

    @InjectMocks
    private ElasticClientHandler elasticClientHandler;

    @Before
    public void setup() {
        initMocks(this);

        mockStatic(RestClient.class);

        elasticClientHandler.setSecurityHttpsCertificateTrust(SECURITY_HTTPS_CERTIFICATE_TRUST);
    }

    @Test
    public void createRestClient_when_deployment_env_is_saas() {
        ClusterSettings clusterSettings = new ClusterSettings("H", 1, "U:P");
        when(searchConfigurationProperties.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.CLOUD);
        when(elasticSettingService.getElasticClusterInformation()).thenReturn(clusterSettings);
        when(RestClient.builder(new HttpHost("H", 1, "https"))).thenAnswer((Answer<RestClientBuilder>) invocation -> builder);
        when(builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        RestHighLevelClient returned = this.elasticClientHandler.createRestClient();

        assertEquals(restClient, returned.getLowLevelClient());
    }

    @Test(expected = AppException.class)
    public void failed_createRestClientForSaaS_when_restclient_is_null() {
        ClusterSettings clusterSettings = new ClusterSettings("H", 1, "U:P");
        when(searchConfigurationProperties.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.CLOUD);
        when(elasticSettingService.getElasticClusterInformation()).thenReturn(clusterSettings);
        when(RestClient.builder(new HttpHost("H", 1, "https"))).thenAnswer((Answer<RestClientBuilder>) invocation -> builder);
        when(builder.build()).thenReturn(null);

        this.elasticClientHandler.createRestClient();
    }

    @Test(expected = AppException.class)
    public void failed_createRestClientForSaaS_when_getcluster_info_throws_exception() {
        when(searchConfigurationProperties.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.CLOUD);
        when(elasticSettingService.getElasticClusterInformation()).thenThrow(new AppException(1, "", ""));
        when(RestClient.builder(new HttpHost("H", 1, "https"))).thenAnswer((Answer<RestClientBuilder>) invocation -> builder );

        this.elasticClientHandler.createRestClient();
    }
}


