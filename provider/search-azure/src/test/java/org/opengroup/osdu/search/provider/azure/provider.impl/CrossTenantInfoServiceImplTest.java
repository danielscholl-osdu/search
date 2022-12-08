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
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;

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

    @Test(expected = AppException.class)
    public void testGetTenantInfo_whenTenantInfoIsNull_throwsException() {
        try {
            sut.getTenantInfo();
        } catch (AppException e) {
            int errorCode = 401;
            String errorMessage = "not authorized to perform this action";
            AppError error = e.getError();

            assertEquals(error.getCode(), errorCode);
            assertThat(error.getMessage(), containsString(errorMessage));

            throw(e);
        }
    }

    @Test()
    public void testGetTenantInfo_whenTenantInfoIsProvided_thenReturnsTenantInfo() {
        TenantInfo tenantInfo = getTenantInfo(1L, "name", "projectId");

        doReturn(tenantInfo).when(tenantFactory).getTenantInfo(eq(partitionIdWithFallbackToAccountId));

        TenantInfo obtainedTenantInfo = sut.getTenantInfo();

        assertEquals(tenantInfo, obtainedTenantInfo);
    }

    @Test()
    public void testGetPartitionId_mustReturnDataPartitionId() {
        String obtainedPartitionId = sut.getPartitionId();

        assertEquals(dataPartitionId, obtainedPartitionId);
    }

    @Test()
    public void testGetAllTenantsFromPartitionId_whenGetTenantInfoReturnsNull() {
        doReturn(partitionIdWithFallbackToAccountIdMultipleAccounts).when(dpsHeaders).getPartitionId();
        List<TenantInfo> tenantInfos = sut.getAllTenantsFromPartitionId();
        assertNull(tenantInfos.get(0));
        assertNull(tenantInfos.get(1));
    }

    @Test()
    public void testGetAllTenantsFromPartitionId_whenGivenTenantInfos_thenReturnTenantInfos() {
        String[] accounts = partitionIdWithFallbackToAccountIdMultipleAccounts.split(",");

        TenantInfo tenant1 = getTenantInfo(1L, "tenant1", "project-id1");
        TenantInfo tenant2 = getTenantInfo(2L, "tenant2", "project-id2");

        doReturn(tenant1).when(tenantFactory).getTenantInfo(eq(accounts[0]));
        doReturn(tenant2).when(tenantFactory).getTenantInfo(eq(accounts[1]));
        doReturn(partitionIdWithFallbackToAccountIdMultipleAccounts).when(dpsHeaders).getPartitionId();

        List<TenantInfo> tenantInfos = sut.getAllTenantsFromPartitionId();

        assertEquals(tenantInfos.get(0), tenant1);
        assertEquals(tenantInfos.get(1), tenant2);
    }

    private TenantInfo getTenantInfo(Long id, String name, String projectId) {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setId(id);
        tenantInfo.setDataPartitionId(dataPartitionId);
        tenantInfo.setName(name);
        tenantInfo.setProjectId(projectId);
        return tenantInfo;
    }
}
