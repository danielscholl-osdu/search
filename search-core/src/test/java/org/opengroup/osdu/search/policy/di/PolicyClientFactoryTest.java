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

package org.opengroup.osdu.search.policy.di;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.policy.IPolicyFactory;
import org.opengroup.osdu.core.common.policy.PolicyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class PolicyClientFactoryTest {
    @Mock
    private PolicyServiceConfiguration serviceConfiguration;

    @Mock
    private HttpResponseBodyMapper httpResponseBodyMapper;

    @InjectMocks
    private PolicyClientFactory policyClientFactory;

    @Test
    void getObject_returnsPolicyFactory_andUsesEndpoint() throws Exception {
        when(serviceConfiguration.getEndpoint()).thenReturn("https://policy.example");

        policyClientFactory.afterPropertiesSet();
        IPolicyFactory obj = policyClientFactory.getObject();

        assertNotNull(obj);
        assertTrue(obj instanceof PolicyFactory);

        verify(serviceConfiguration, times(1)).getEndpoint();
        verifyNoMoreInteractions(serviceConfiguration);
    }

    @Test
    void getObjectType_isIPolicyFactory() {
        assertEquals(IPolicyFactory.class, policyClientFactory.getObjectType());
    }
}
