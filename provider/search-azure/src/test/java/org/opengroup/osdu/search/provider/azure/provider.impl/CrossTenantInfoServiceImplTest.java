package org.opengroup.osdu.search.provider.azure.provider.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;

import java.util.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CrossTenantInfoServiceImplTest {

    private static final String dataPartitionId = "data-partition-id";
    private static final String partitionIdWithFallbackToAccountId = "partition-id-with-fallback";
    // Note [aaljain]: Inconsistent behaviour in splitting and obtaining account IDs
    // .split(',') vs .split("\\s*,\\s*")
    private static final String partitionIdWithFallbackToAccountIdMultipleAccounts = "partition-id1,partition-id2";

    @Mock
    private ITenantFactory tenantFactory;

    @Mock
    private DpsHeaders dpsHeaders;

    @InjectMocks
    CrossTenantInfoServiceImpl sut;

    @Before
    public void init() {
        lenient().doReturn(dataPartitionId).when(dpsHeaders).getPartitionId();
        lenient().doReturn(partitionIdWithFallbackToAccountId).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
    }
}
