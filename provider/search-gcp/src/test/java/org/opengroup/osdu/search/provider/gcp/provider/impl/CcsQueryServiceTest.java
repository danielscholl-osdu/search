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

package org.opengroup.osdu.search.provider.gcp.provider.impl;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.core.common.model.search.CcsQueryRequest;
import org.opengroup.osdu.core.common.model.search.CcsQueryResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Config.class})
public class CcsQueryServiceTest {

    @InjectMocks
    private CcsQueryServiceImpl ccsQueryService;

    @Mock
    private IElasticRepository elasticRepository;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private ITenantFactory tenantStorageFactory;

    @Mock
    private IQueryService queryService;

    @Before
    public void setup() {
        mockStatic(Config.class);

        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(DpsHeaders.ACCOUNT_ID, "tenant1,common");
        headersMap.put(DpsHeaders.AUTHORIZATION, "Bearer smth");
        when(dpsHeaders.getHeaders()).thenReturn(headersMap);
        when(dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1,common");
        when(tenantStorageFactory.getTenantInfo(any())).thenReturn(new TenantInfo());
    }

    @Test
    public void should_ReturnResultForTwoTenants_When_DataFromTwoTenantsRequested() throws Exception {
        QueryResponse tenantQueryResponse = createTenantQueryResponse();
        QueryResponse commonTenantQueryResponse = createCommonTenantQueryResponse();
        when(elasticRepository.getElasticClusterSettings(any(TenantInfo.class))).thenReturn(new ClusterSettings("", 42, ""));
        when(queryService.queryIndex(any(QueryRequest.class), any(ClusterSettings.class))).thenReturn(createTenantQueryResponse())
                .thenReturn(createCommonTenantQueryResponse());
        when(Config.isSmartSearchCcsDisabled()).thenReturn(false);
        CcsQueryResponse ccsQueryResponse = this.ccsQueryService.makeRequest(new CcsQueryRequest());
        verify(queryService, times(2)).queryIndex(any(QueryRequest.class), any(ClusterSettings.class));
        assertNotNull(ccsQueryResponse);
        assertEquals(tenantQueryResponse.getTotalCount() + commonTenantQueryResponse.getTotalCount(), ccsQueryResponse.getTotalCount());
        assertEquals(tenantQueryResponse.getResults().size() + commonTenantQueryResponse.getResults().size(), ccsQueryResponse.getResults().size());
        assertEquals("tenant1:foobar:baz:0.1", ccsQueryResponse.getResults().get(0).get("kind"));
        assertEquals("tenant1:foobar:baz:0.1", ccsQueryResponse.getResults().get(0).get("kind"));
        assertEquals("tenant1:foobar:baz", ccsQueryResponse.getResults().get(0).get("id"));
        assertEquals("tenant1:foobar:baz:42", ccsQueryResponse.getResults().get(4).get("kind"));
    }

    private QueryResponse createTenantQueryResponse() {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setTotalCount(42);
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result1 = new HashMap<>();
        result1.put("kind", "tenant1:foobar:baz:0.1");
        result1.put("id", "tenant1:foobar:baz");
        results.add(result1);
        Map<String, Object> result2 = new HashMap<>();
        result2.put("kind", "tenant1:foobar:baz:0.2");
        results.add(result2);
        Map<String, Object> result3 = new HashMap<>();
        result3.put("kind", "tenant1:foobar:baz:42");
        results.add(result3);
        queryResponse.setResults(results);
        return queryResponse;
    }

    private QueryResponse createCommonTenantQueryResponse() {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setTotalCount(2);
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result1 = new HashMap<>();
        result1.put("kind", "common:foobar:baz:0.1");
        results.add(result1);
        Map<String, Object> result2 = new HashMap<>();
        result2.put("kind", "common:foobar:baz:0.2");
        results.add(result2);
        queryResponse.setResults(results);
        return queryResponse;
    }
}
