package org.opengroup.osdu.search.provider.azure.provider.impl;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;

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
        lenient().doReturn(dataPartitionId).when(dpsHeaders).getPartitionId();
        lenient().doReturn(partitionIdWithFallbackToAccountId).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
    }

    @Test
    public void testCcsQueryResponse_whenSingleClusterSettingsProvided() throws Exception {
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
        assertEquals(ccsQueryResponse.getResults(), results);
        assertEquals(ccsQueryResponse.getTotalCount(), totalCount);
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