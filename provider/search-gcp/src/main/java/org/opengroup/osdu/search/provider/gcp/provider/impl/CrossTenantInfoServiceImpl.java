/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.search.provider.gcp.provider.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrossTenantInfoServiceImpl implements ITenantInfoService, ICrossTenantInfoService {

  private final ITenantFactory tenantFactory;
  private final DpsHeaders headers;

  @Override
  public TenantInfo getTenantInfo() {
    String primaryAccountId = this.headers.getPartitionIdWithFallbackToAccountId();
    TenantInfo tenantInfo = this.tenantFactory.getTenantInfo(primaryAccountId);
    if (tenantInfo == null) {
      throw AppException.createUnauthorized(
          String.format("could not retrieve tenant info for data partition id: %s",
              primaryAccountId));
    }
    return tenantInfo;
  }

  @Override
  public List<TenantInfo> getAllTenantInfos() {
    return new ArrayList<>(tenantFactory.listTenantInfo());
  }

  @Override
  public List<TenantInfo> getAllTenantsFromPartitionId() {
    List<TenantInfo> tenantInfos = new LinkedList<>();

    String[] accountIdList = headers.getPartitionIdWithFallbackToAccountId().split(",");
    //Get all tenant values requested by user
    for (String accountId : accountIdList) {
      TenantInfo tenantInfo = tenantFactory.getTenantInfo(accountId);
      tenantInfos.add(tenantInfo);
    }
    return tenantInfos;
  }
}
