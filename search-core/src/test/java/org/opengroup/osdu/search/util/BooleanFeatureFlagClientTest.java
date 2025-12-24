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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.search.cache.FeatureFlagCache;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class BooleanFeatureFlagClientTest {

    @Mock
    private FeatureFlagCache cache;
    @Mock private JaxRsDpsLog logger;
    @Mock private DpsHeaders headers;
    @Mock private IPartitionFactory factory;
    @Mock private IServiceAccountJwtClient tokenService;
    @Mock private IPartitionProvider partitionProvider;
    @Mock private PartitionInfo partitionInfo;

    @InjectMocks
    private BooleanFeatureFlagClient client;

    private final String FEATURE = "testFeature";
    private final String PARTITION = "opendes";

    @BeforeEach
    void setUp() {
        lenient().when(headers.getPartitionId()).thenReturn(PARTITION);
        lenient().when(headers.getHeaders()).thenReturn(new HashMap<>());
    }

    @Test
    void isEnabled_returnsCachedValue_whenFeatureExistsInCache() {
        when(cache.get(FEATURE)).thenReturn(true);

        boolean result = client.isEnabled(FEATURE, false);

        assertTrue(result);
        verify(cache, never()).put(anyString(), any());
        verifyNoInteractions(factory, tokenService, partitionProvider);
    }

    @Test
    void isEnabled_fetchesAndReturnsTrueFromPartitionProperties() throws Exception {
        when(cache.get(FEATURE)).thenReturn(null);
        when(factory.create(any())).thenReturn(partitionProvider);
        when(tokenService.getIdToken(PARTITION)).thenReturn("token");
        when(partitionProvider.get(PARTITION)).thenReturn(partitionInfo);

        Property prop = new Property(false, "true");
        Map<String, Property> props = Map.of(FEATURE, prop);
        when(partitionInfo.getProperties()).thenReturn(props);

        boolean result = client.isEnabled(FEATURE, false);

        assertTrue(result);
        verify(cache).put(FEATURE, true);
        verify(logger).info(contains("feature flag 'testFeature'"));
    }

    @Test
    void isEnabled_returnsDefault_whenFeatureNotInPartitionProperties() throws Exception {
        when(cache.get(FEATURE)).thenReturn(null);

        when(factory.create(any())).thenReturn(partitionProvider);
        when(tokenService.getIdToken(PARTITION)).thenReturn("token");

        Map<String, String> hdrs = new HashMap<>();
        hdrs.put(DpsHeaders.DATA_PARTITION_ID, PARTITION);
        when(headers.getHeaders()).thenReturn(hdrs);
        when(headers.getPartitionId()).thenReturn(PARTITION);

        PartitionInfo partitionInfo = new PartitionInfo();
        Map<String, Property> props = new HashMap<>();
        props.put("anotherFlag", new Property(true, "true"));
        partitionInfo.setProperties(props);
        when(partitionProvider.get(PARTITION)).thenReturn(partitionInfo);

        boolean result = client.isEnabled(FEATURE,  false);

        assertFalse(result);
        verify(cache).put(FEATURE, false);
        verify(logger).info(contains("feature flag"));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void isEnabled_returnsDefault_whenPartitionExceptionOccurs() throws Exception {
        when(cache.get(FEATURE)).thenReturn(null);
        when(factory.create(any())).thenReturn(partitionProvider);
        when(tokenService.getIdToken(PARTITION)).thenReturn("token");

        HttpResponse resp = new HttpResponse();
        resp.setResponseCode(500);
        resp.setBody("failure");

        when(partitionProvider.get(PARTITION))
                .thenThrow(new PartitionException("failure", resp));

        boolean result = client.isEnabled(FEATURE, true);

        assertTrue(result);
        verify(logger).error(contains("Error on getting the feature flag"), any(Exception.class));
        verify(cache).put(FEATURE, true);
    }

    @Test
    void isEnabled_returnsDefault_whenPartitionInfoNull() throws Exception {
        when(cache.get(FEATURE)).thenReturn(null);
        when(factory.create(any())).thenReturn(partitionProvider);
        when(tokenService.getIdToken(PARTITION)).thenReturn("token");
        when(partitionProvider.get(PARTITION)).thenReturn(null);

        boolean result = client.isEnabled(FEATURE, false);

        assertFalse(result);
        verify(cache).put(FEATURE, false);
    }

    @Test
    void isEnabled_returnsDefault_whenPropertiesNull() throws Exception {
        when(cache.get(FEATURE)).thenReturn(null);
        when(factory.create(any())).thenReturn(partitionProvider);
        when(tokenService.getIdToken(PARTITION)).thenReturn("token");
        when(partitionProvider.get(PARTITION)).thenReturn(partitionInfo);
        when(partitionInfo.getProperties()).thenReturn(null);

        boolean result = client.isEnabled(FEATURE, true);

        assertTrue(result);
        verify(cache).put(FEATURE, true);
    }
}
