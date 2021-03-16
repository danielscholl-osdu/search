package org.opengroup.osdu.search.service.policy.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.policy.BatchPolicyResponse;
import org.opengroup.osdu.core.common.model.policy.BatchResult;
import org.opengroup.osdu.core.common.model.policy.PolicyRequest;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.core.common.policy.IPolicyFactory;
import org.opengroup.osdu.core.common.policy.IPolicyProvider;
import org.opengroup.osdu.core.common.policy.PolicyException;
import org.opengroup.osdu.search.policy.di.PolicyServiceConfiguration;
import org.opengroup.osdu.search.policy.model.SearchPolicy;
import org.opengroup.osdu.search.policy.service.PolicyServiceImpl;
import org.opengroup.osdu.search.service.IEntitlementsExtensionService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceImplTest {

    @Mock
    private DpsHeaders headers;

    @Mock
    private IPolicyFactory policyFactory;

    @Mock
    private IEntitlementsExtensionService entitlementsService;

    @Mock
    private PolicyServiceConfiguration policyServiceConfiguration;

    @Spy
    private IPolicyProvider serviceClient;

    @InjectMocks
    private PolicyServiceImpl sut;

    @Test
    public void evaluateBatchPolicyTest() throws PolicyException {
        PolicyRequest policyRequest = new PolicyRequest();
        BatchPolicyResponse batchPolicyResponse = new BatchPolicyResponse();
        policyRequest.setPolicyId("search");
        policyRequest.setInput(setPolicyInput());
        Mockito.when(policyFactory.create(any())).thenReturn(serviceClient);
        Mockito.when(serviceClient.evaluateBatchPolicy(policyRequest)).thenReturn(batchPolicyResponse);
        Assert.assertNotNull(sut.evaluateBatchPolicy(policyRequest));
    }

    @Test
    public void evaluateSearchDataAuthorizationPolicyTest() throws PolicyException{
        List<RecordMetadata> recordMetadataList = new ArrayList<>();
        RecordMetadata recordMetadata = new RecordMetadata();
        recordMetadata.setId("id:123");
        recordMetadata.setKind("kind:123");
        recordMetadata.setModifyUser("user123");
        recordMetadataList.add(recordMetadata);
        Mockito.when(policyFactory.create(any())).thenReturn(serviceClient);
        BatchPolicyResponse batchPolicyResponse = new BatchPolicyResponse();
        BatchResult batchResult = new BatchResult();
        List<String> allowedRecords = new ArrayList<>();
        allowedRecords.add("id:123");
        batchResult.setAllowed_records(allowedRecords);
        batchPolicyResponse.setResult(batchResult);
        Mockito.when(serviceClient.evaluateBatchPolicy(any())).thenReturn(batchPolicyResponse);
        Groups groups = new Groups();
        Mockito.when(entitlementsService.getGroups(any())).thenReturn(groups);
        Mockito.when(policyServiceConfiguration.getPolicyId()).thenReturn("search");
        List<String> result = sut.evaluateSearchDataAuthorizationPolicy(recordMetadataList, OperationType.view);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("id:123", result.get(0));
    }

    private JsonObject setPolicyInput() {
        SearchPolicy searchPolicy = new SearchPolicy();
        List<String> groups = new ArrayList<>();
        groups.add("group1@abc");
        searchPolicy.setGroups(groups);
        searchPolicy.setOperation(OperationType.view);
        List<Record> records = new ArrayList<>();
        Record record = new Record();
        record.setId("id:123");
        record.setKind("kind:123");
        records.add(record);
        searchPolicy.setRecords(records);
        return new JsonParser().parse(new Gson().toJson(searchPolicy)).getAsJsonObject();
    }
}
