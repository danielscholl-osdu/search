package org.opengroup.osdu.search.service.policy.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.policy.PolicyStatus;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.search.policy.cache.PolicyCache;
import org.opengroup.osdu.search.policy.service.IPartitionService;
import org.opengroup.osdu.search.policy.service.PartitionPolicyStatusService;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class PartitionPolicyStatusServiceTest {

    @Mock
    private IPartitionService partitionService;

    @Mock
    private PolicyCache cache;

    @InjectMocks
    private PartitionPolicyStatusService sut;

    private String dataPartition = "myPartition";

    @Test
    public void returnFalseIfPartitionServiceIsNull() {
        PartitionPolicyStatusService partitionPolicyStatusService = new PartitionPolicyStatusService();
        Assert.assertFalse(partitionPolicyStatusService.policyEnabled(dataPartition));
    }

    @Test
    public void returnEnabledFlagFromCache() {
        PolicyStatus policyStatus = PolicyStatus.builder().build();
        policyStatus.setEnabled(true);
        Mockito.when(cache.containsKey(dataPartition+"-policy")).thenReturn(true);
        Mockito.when(cache.get(dataPartition+"-policy")).thenReturn(policyStatus);
        Assert.assertTrue(sut.policyEnabled(dataPartition));
    }

    @Test
    public void returneEnabledFlagFromPartitionInfo() {
        PartitionInfo partitionInfo = PartitionInfo.builder().build();
        Map<String, Property> properties = new HashMap<>();
        Property policyProperty = Property.builder().build();
        policyProperty.setSensitive(false);
        policyProperty.setValue("true");
        properties.put("policy-service-enabled", policyProperty);
        partitionInfo.setProperties(properties);
        Mockito.when(cache.containsKey(dataPartition+"-policy")).thenReturn(false);
        Mockito.when(partitionService.getPartition("myPartition")).thenReturn(partitionInfo);
        Assert.assertTrue(sut.policyEnabled(dataPartition));
    }
}
