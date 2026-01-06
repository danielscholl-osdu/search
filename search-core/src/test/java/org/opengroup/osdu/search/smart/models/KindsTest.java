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

package org.opengroup.osdu.search.smart.models;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.provider.interfaces.IKindsCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.util.ElasticClientHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KindsTest {
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private IKindsCache cache;
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private SearchConfigurationProperties configurationProperties;

    @Test
    public void should_return_no_Results_when_notInCache()throws IOException {
        KindsSut sut = setup(false);
        Set result = sut.all("no-tenant");
        verify(cache,times(1)).get(any());
        assertEquals(0, result.size());
    }

    @Test
    public void should_retrieveKindsFromCache_when_cacheIsPopulated_and_useLocalKindOnSecondCall()throws IOException{
        KindsSut sut = setup(true);
        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");
        Set result = sut.all("tenant1");
        verify(cache,times(1)).get(any());
        assertEquals(0, sut.getTermCallCount);
        assertEquals(0, result.size());

        result = sut.all("tenant1");
        //these shouldnt increment
        verify(cache,times(1)).get(any());
        assertEquals(0, sut.getTermCallCount);
        assertEquals(0, result.size());
    }

    @Test
    public void should_cache_all_Results_when_CacheSync_is_called()throws IOException {
        KindsSut sut = setup(false);
        sut.cacheSync();
        assertEquals(1, sut.getTermCallCount);
    }

    public KindsSut setup(boolean cacheHit){
        Mockito.lenient().when(dpsHeaders.getPartitionId()).thenReturn("tenant1");
        Mockito.lenient().when(cache.get(any())).thenReturn(cacheHit ? new HashSet() : null);
        return new KindsSut(configurationProperties, elasticClientHandler, cache, dpsHeaders,log);
    }

    class KindsSut extends Kinds{
        public KindsSut(SearchConfigurationProperties configurationProperties, ElasticClientHandler elasticClientHandler, IKindsCache cache, DpsHeaders headersInfo, JaxRsDpsLog log){
            super(configurationProperties, elasticClientHandler, cache, headersInfo,log);
        }

        private String ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS = "domain";

        public int getTermCallCount = 0;

        //rather than adding a tonne of elastic mocks we just override all elastic functionality and test our logic
        //this can be covered by integration test
        @Override
        protected Set<String> getTermAggregation(String termAggId, String fieldName) {
            getTermCallCount++;
            return new HashSet<>();
        }
    }
}
