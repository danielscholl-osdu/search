package org.opengroup.osdu.search.service.policy.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.search.policy.service.PartitionServiceImpl;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class PartitionServiceImplTest {

    @Mock
    private DpsHeaders headers;

    @Mock
    private IPartitionFactory factory;

    @Mock
    private IServiceAccountJwtClient tokenService;

    @Spy
    private IPartitionProvider partitionServiceClient;

    @InjectMocks
    private PartitionServiceImpl sut;

    @Test
    public void getPartitionInfoTest() throws PartitionException {
        String partitionId = "myPartition";
        PartitionInfo partitionInfo = PartitionInfo.builder().build();
        Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
        Mockito.when(tokenService.getIdToken(any())).thenReturn("myToken");
        Mockito.when(factory.create(any())).thenReturn(partitionServiceClient);
        Mockito.when(partitionServiceClient.get(partitionId)).thenReturn(partitionInfo);
        Assert.assertNotNull(sut.getPartition(partitionId));
    }
}
