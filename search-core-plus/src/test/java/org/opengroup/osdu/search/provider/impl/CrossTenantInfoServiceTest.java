/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.provider.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;

@ExtendWith(MockitoExtension.class)
public class CrossTenantInfoServiceTest {

  @Mock
  private ITenantFactory tenantFactory;
  @Mock
  private DpsHeaders dpsHeaders;
  @InjectMocks
  private CrossTenantInfoServiceImpl sut;

  @Test
  public void should_return_validTenantInfoList_given_validAccountIds() {
    when(this.dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1,common");
    TenantInfo tenant1 = new TenantInfo();
    tenant1.setName("tenant1");
    when(tenantFactory.getTenantInfo("tenant1")).thenReturn(tenant1);
    TenantInfo common = new TenantInfo();
    common.setName("common");
    when(tenantFactory.getTenantInfo("common")).thenReturn(common);
    List<TenantInfo> tenantInfoList = sut.getAllTenantsFromPartitionId();
    assertEquals(2, tenantInfoList.size());
  }

  @Test
  public void should_return_tenantInfo_when_validPartitionIdProvided() {
    String partitionId = "tenant-123";
    when(this.dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionId);

    TenantInfo expectedTenantInfo = new TenantInfo();
    expectedTenantInfo.setName(partitionId);
    expectedTenantInfo.setDataPartitionId(partitionId);

    when(tenantFactory.getTenantInfo(partitionId)).thenReturn(expectedTenantInfo);

    TenantInfo result = sut.getTenantInfo();

    assertNotNull(result);
    assertEquals(partitionId, result.getName());
    assertEquals(partitionId, result.getDataPartitionId());
  }

  @Test
  public void should_throw_unauthorizedException_when_tenantInfoIsNull() {
    String partitionId = "invalid-partition";
    when(this.dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionId);
    when(tenantFactory.getTenantInfo(partitionId)).thenReturn(null);

    AppException exception = assertThrows(AppException.class, () -> sut.getTenantInfo());

    assertNotNull(exception);
    assertNotNull(exception.getMessage());
    assertEquals(401, exception.getError().getCode());
  }

  @Test
  public void should_return_allTenantInfos_when_listTenantInfoCalled() {
    TenantInfo tenant1 = new TenantInfo();
    tenant1.setName("tenant1");
    tenant1.setDataPartitionId("dp1");

    TenantInfo tenant2 = new TenantInfo();
    tenant2.setName("tenant2");
    tenant2.setDataPartitionId("dp2");

    List<TenantInfo> mockTenants = Arrays.asList(tenant1, tenant2);
    when(tenantFactory.listTenantInfo()).thenReturn(mockTenants);

    List<TenantInfo> result = sut.getAllTenantInfos();

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("tenant1", result.get(0).getName());
    assertEquals("tenant2", result.get(1).getName());
  }

  @Test
  public void should_return_singleTenant_when_singlePartitionIdProvided() {
    when(this.dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("single-tenant");

    TenantInfo tenant = new TenantInfo();
    tenant.setName("single-tenant");
    when(tenantFactory.getTenantInfo("single-tenant")).thenReturn(tenant);

    List<TenantInfo> result = sut.getAllTenantsFromPartitionId();

    assertEquals(1, result.size());
    assertEquals("single-tenant", result.get(0).getName());
  }

  @Test
  public void should_handle_multipleTenants_with_whitespace() {
    when(this.dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1, tenant2 ,tenant3");

    TenantInfo tenant1 = new TenantInfo();
    tenant1.setName("tenant1");
    when(tenantFactory.getTenantInfo("tenant1")).thenReturn(tenant1);

    TenantInfo tenant2 = new TenantInfo();
    tenant2.setName("tenant2");
    when(tenantFactory.getTenantInfo("tenant2")).thenReturn(tenant2);

    TenantInfo tenant3 = new TenantInfo();
    tenant3.setName("tenant3");
    when(tenantFactory.getTenantInfo("tenant3")).thenReturn(tenant3);

    List<TenantInfo> result = sut.getAllTenantsFromPartitionId();

    assertEquals(3, result.size());
  }

  @Test
  public void should_throw_unauthorizedException_when_tenantLookupReturnsNull_inMultipleTenants() {
    when(this.dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1,invalid-tenant,tenant3");

    TenantInfo tenant1 = new TenantInfo();
    tenant1.setName("tenant1");
    when(tenantFactory.getTenantInfo("tenant1")).thenReturn(tenant1);
    when(tenantFactory.getTenantInfo("invalid-tenant")).thenReturn(null);

    AppException exception = assertThrows(AppException.class, () -> sut.getAllTenantsFromPartitionId());

    assertNotNull(exception);
    assertEquals(401, exception.getError().getCode());
  }
}
