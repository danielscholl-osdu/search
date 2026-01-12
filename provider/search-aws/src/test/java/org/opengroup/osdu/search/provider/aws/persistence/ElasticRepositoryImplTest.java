// Copyright © Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.provider.aws.persistence;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.opengroup.osdu.core.aws.v2.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.powermock.reflect.Whitebox;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ElasticRepositoryImplTest {
    
    @InjectMocks
    private ElasticRepositoryImpl elasticRepositoryImpl;

    private final String username = "username";
    private final String password = "password";
    private final String host = "host";
    private final String port = "6369";

    @Test
    public void postConstruct_InitalizesProperly() throws Exception {
        Whitebox.setInternalState(elasticRepositoryImpl, host, host);
        Whitebox.setInternalState(elasticRepositoryImpl, "port", Integer.parseInt(port));
        Whitebox.setInternalState(elasticRepositoryImpl, username, username);
        Whitebox.setInternalState(elasticRepositoryImpl, password, password);  
        
        Map<String, String> userNamePasswordMap = new HashMap<>() {
            {
                put(username, username);
                put(password, password);
            }
        };

        try (MockedConstruction<K8sLocalParameterProvider> mockedConstruction =
                        Mockito.mockConstruction(K8sLocalParameterProvider.class, (mock, context) -> {
                            when(mock.getParameterAsStringOrDefault("elasticsearch_host", host)).thenReturn(host);
                            when(mock.getParameterAsStringOrDefault("elasticsearch_port", port)).thenReturn(port);
                            when(mock.getCredentialsAsMap("elasticsearch_credentials")).thenReturn(userNamePasswordMap);
                        })) {

            elasticRepositoryImpl.postConstruct();
            Assert.assertEquals(username + ":" + password, elasticRepositoryImpl.usernameAndPassword);
        }
    }

    @Test
    public void getElasticClusterSettings_Null_User_Test() {

        try (MockedConstruction<K8sLocalParameterProvider> provider =
                        Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
                            when(mockProvider.getLocalMode()).thenReturn(false);
                            when(mockProvider.getParameterAsStringOrDefault(eq("ELASTICSEARCH_HOST"), any())).thenReturn(host);
                            when(mockProvider.getParameterAsStringOrDefault(eq("ELASTICSEARCH_PORT"), any())).thenReturn(port);
                            when(mockProvider.getCredentialsAsMap("ELASTICSEARCH_CREDENTIALS")).thenReturn(null);
                        })) {
            ElasticRepositoryImpl elasticImpl = new ElasticRepositoryImpl();

            assertNotNull(elasticImpl.getElasticClusterSettings(new TenantInfo()));

        }
    }

    @Test
    public void getElasticClusterSettings_not_Null_User_Test() {

        Map<String, String> map = new HashMap<String, String>();
        map.put("username", username);
        map.put("password", password);

        try (MockedConstruction<K8sLocalParameterProvider> provider =
                        Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
                            when(mockProvider.getLocalMode()).thenReturn(false);
                            when(mockProvider.getParameterAsStringOrDefault(eq("ELASTICSEARCH_HOST"), any())).thenReturn(host);
                            when(mockProvider.getParameterAsStringOrDefault(eq("ELASTICSEARCH_PORT"), any())).thenReturn(port);
                            when(mockProvider.getCredentialsAsMap("ELASTICSEARCH_CREDENTIALS")).thenReturn(map);
                        })) {
            ElasticRepositoryImpl elasticImpl = new ElasticRepositoryImpl();
            elasticImpl.isHttps = true;
            assertNotNull(elasticImpl.getElasticClusterSettings(new TenantInfo()));

        }
    }
}
