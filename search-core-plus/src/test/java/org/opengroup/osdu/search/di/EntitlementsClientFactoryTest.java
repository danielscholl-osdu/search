/*
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

package org.opengroup.osdu.search.di;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.search.config.CorePlusSearchConfigurationProperties;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EntitlementsClientFactoryTest {

    @Mock
    HttpResponseBodyMapper httpResponseBodyMapper;

    @Mock
    CorePlusSearchConfigurationProperties configurationProperties;

    EntitlementsClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EntitlementsClientFactory(httpResponseBodyMapper, configurationProperties);
    }

    @Test
    void createInstance_createsEntitlementsFactory_usingConfigurationProperties() {

        when(configurationProperties.getAuthorizeApi())
                .thenReturn("https://auth.example");
        when(configurationProperties.getAuthorizeApiKey())
                .thenReturn("api-key-xyz");

        IEntitlementsFactory result = factory.createInstance();

        assertNotNull(result);
        assertInstanceOf(IEntitlementsFactory.class, result);

        verify(configurationProperties, times(1)).getAuthorizeApi();
        verify(configurationProperties, times(1)).getAuthorizeApiKey();
        verifyNoMoreInteractions(configurationProperties);
    }

    @Test
    void getObjectType_returnsIEntitlementsFactoryClass() {
        assertEquals(IEntitlementsFactory.class, factory.getObjectType());
    }
}
