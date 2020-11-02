//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.provider.azure.provider.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CcsQueryRequest;
import org.opengroup.osdu.core.common.model.search.CcsQueryResponse;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class CcsQueryServiceImplTest {

    private static final String dataPartitionId = "data-partition-id";
    private static final String partitionIdWithFallbackToAccountId = "partition-id-with-fallback";
    private static final String partitionIdWithFallbackToAccountIdMultipleAccounts = "partition-id1, partition-id2";

    private static class DummyObject {
        private String id;

        public DummyObject(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private ITenantFactory tenantStorageFactory;

    @Mock
    private IQueryService queryService;

    @Mock
    private IElasticRepository elasticRepository;

    @InjectMocks
    CcsQueryServiceImpl sut;

    @Before
    public void init() {
        doReturn(partitionIdWithFallbackToAccountId).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
    }

    @Test
    public void testCcsQueryResponse_whenSingleAccountProvided() throws Exception {
        QueryResponse queryResponse = mock(QueryResponse.class);
        CcsQueryRequest ccsQueryRequest = mock(CcsQueryRequest.class);
        mockCcsQueryRequest(ccsQueryRequest,1, "kind", 100, "query");

        long totalCount = 100L;
        List<Map<String, Object>> results = getDummyResults("dummy-object-key", "dummy-id");
        ClusterSettings clusterSettings = getClusterSettings("host", 1111, "username-and-password");
        TenantInfo tenantInfo = getTenantInfo(1L, "tenant1", "project-id1");

        doReturn(totalCount).when(queryResponse).getTotalCount();
        doReturn(results).when(queryResponse).getResults();
        doReturn(clusterSettings).when(elasticRepository).getElasticClusterSettings(eq(tenantInfo));
        doReturn(tenantInfo).when(tenantStorageFactory).getTenantInfo(eq(partitionIdWithFallbackToAccountId));
        doReturn(queryResponse).when(queryService).queryIndex(any(), any());

        CcsQueryResponse ccsQueryResponse = sut.makeRequest(ccsQueryRequest);

        ArgumentCaptor<QueryRequest> searchRequestArgCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        ArgumentCaptor<ClusterSettings> clusterSettingsArgCaptor = ArgumentCaptor.forClass(ClusterSettings.class);

        verify(queryService).queryIndex(searchRequestArgCaptor.capture(), clusterSettingsArgCaptor.capture());

        QueryRequest obtainedQueryRequest = searchRequestArgCaptor.getValue();
        ClusterSettings obtainedClusterSettings = clusterSettingsArgCaptor.getValue();

        validateQueryRequestAndCcsQueryRequestCorrespondence(obtainedQueryRequest, ccsQueryRequest);

        assertEquals(obtainedClusterSettings, clusterSettings);
        assertEquals(ccsQueryResponse.getResults().size(), 1);
        assertEquals(ccsQueryResponse.getResults(), results);
        assertEquals(ccsQueryResponse.getTotalCount(), totalCount);
    }

    @Test
    public void testCcsQueryResponse_whenMultipleAccountsProvided() throws Exception {
        doReturn(partitionIdWithFallbackToAccountIdMultipleAccounts).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();

        QueryResponse queryResponse1 = mock(QueryResponse.class);
        QueryResponse queryResponse2 = mock(QueryResponse.class);

        CcsQueryRequest ccsQueryRequest = mock(CcsQueryRequest.class);
        mockCcsQueryRequest(ccsQueryRequest, 1, "kind", 100, "query");

        List<Map<String, Object>> results1 = getDummyResults("dummy-object-key-1", "dummy-id-1");
        List<Map<String, Object>> results2 = getDummyResults("dummy-object-key-2", "dummy-id-2");
        String[] accounts = partitionIdWithFallbackToAccountIdMultipleAccounts.split("\\s*,\\s*");

        ClusterSettings clusterSettings1 = getClusterSettings("host1", 1111, "secret1");
        ClusterSettings clusterSettings2 = getClusterSettings("host2", 2222, "secret2");

        long totalCount1 = 100L;
        long totalCount2 = 200L;
        doReturn(totalCount1).when(queryResponse1).getTotalCount();
        doReturn(results1).when(queryResponse1).getResults();
        doReturn(totalCount2).when(queryResponse2).getTotalCount();
        doReturn(results2).when(queryResponse2).getResults();

        TenantInfo tenant1 = getTenantInfo(1L, "tenant1", "project-id1");
        TenantInfo tenant2 = getTenantInfo(2L, "tenant2", "project-id2");

        doReturn(tenant1).when(tenantStorageFactory).getTenantInfo(eq(accounts[0]));
        doReturn(tenant2).when(tenantStorageFactory).getTenantInfo(eq(accounts[1]));
        doReturn(clusterSettings1).when(elasticRepository).getElasticClusterSettings(eq(tenant1));
        doReturn(clusterSettings2).when(elasticRepository).getElasticClusterSettings(eq(tenant2));
        doReturn(queryResponse1).doReturn(queryResponse2).when(queryService).queryIndex(any(), any());

        CcsQueryResponse ccsQueryResponse = sut.makeRequest(ccsQueryRequest);

        ArgumentCaptor<QueryRequest> searchRequestArgCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        ArgumentCaptor<ClusterSettings> clusterSettingsArgCaptor = ArgumentCaptor.forClass(ClusterSettings.class);

        verify(queryService, times(2)).queryIndex(searchRequestArgCaptor.capture(), clusterSettingsArgCaptor.capture());

        QueryRequest queryRequest = searchRequestArgCaptor.getValue();
        List<ClusterSettings> clusterSettings = clusterSettingsArgCaptor.getAllValues();

        validateQueryRequestAndCcsQueryRequestCorrespondence(queryRequest, ccsQueryRequest);

        assertEquals(clusterSettings.size(), 2);
        assertTrue(clusterSettings.contains(clusterSettings1));
        assertTrue(clusterSettings.contains(clusterSettings2));

        assertEquals(ccsQueryResponse.getResults().size(), 2);
        assertTrue(ccsQueryResponse.getResults().contains(results1.get(0)));
        assertTrue(ccsQueryResponse.getResults().contains(results2.get(0)));
        assertEquals(ccsQueryResponse.getTotalCount(), totalCount1 + totalCount2);
    }

    private void mockCcsQueryRequest(CcsQueryRequest ccsQueryRequest, int offset, String kind, int limit, String query) {
        doReturn(offset).when(ccsQueryRequest).getFrom();
        doReturn(kind).when(ccsQueryRequest).getKind();
        doReturn(limit).when(ccsQueryRequest).getLimit();
        doReturn(query).when(ccsQueryRequest).getQuery();
    }

    private void validateQueryRequestAndCcsQueryRequestCorrespondence(QueryRequest queryRequest, CcsQueryRequest ccsQueryRequest) {
        assertEquals(queryRequest.getFrom(), ccsQueryRequest.getFrom());
        assertEquals(queryRequest.getLimit(), ccsQueryRequest.getLimit());
        assertEquals(queryRequest.getKind(), ccsQueryRequest.getKind());
        assertEquals(queryRequest.getQuery(), ccsQueryRequest.getQuery());
    }

    private List<Map<String, Object>> getDummyResults(String dummyObjectKey, String dummyObjectId) {
        Map<String, Object> result = new HashMap<>();
        Object dummyObject = new DummyObject(dummyObjectId);
        result.put(dummyObjectKey, dummyObject);
        List<Map<String, Object>> results = Arrays.asList(result);
        return results;
    }

    private TenantInfo getTenantInfo(Long id, String name, String projectId) {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setId(id);
        tenantInfo.setDataPartitionId(dataPartitionId);
        tenantInfo.setName(name);
        tenantInfo.setProjectId(projectId);
        return tenantInfo;
    }

    private ClusterSettings getClusterSettings(String host, int port, String userNameAndPassword) {
        ClusterSettings clusterSettings = new ClusterSettings(host, port, userNameAndPassword);
        return clusterSettings;
    }
}