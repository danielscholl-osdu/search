package org.opengroup.osdu.search.provider.azure.provider.impl;
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


import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;

@Component
public class CrossTenantInfoServiceImpl implements ITenantInfoService, ICrossTenantInfoService {

    @Inject
    private ITenantFactory tenantFactory;

    /**
     * TODO: Fix this: Due to two instances of DpsHeaders (one from corelib and another from os-indexer-search-lib, spring unable to inject the right one. So, instantiate using ConfigModule.
     */
    private DpsHeaders dpsHeaders;

    @PostConstruct
    public void init(){
        dpsHeaders = new DpsHeaders();
    }

    @Override
    public TenantInfo getTenantInfo() {
        String primaryAccountId= TenantInfo.COMMON;

        if(dpsHeaders != null){
            if(dpsHeaders.getAccountId() != null)
                primaryAccountId = dpsHeaders.getAccountId();
            else if(dpsHeaders.getPartitionId() != null)
                primaryAccountId = dpsHeaders.getPartitionId();
        }

        TenantInfo tenantInfo = this.tenantFactory.getTenantInfo(primaryAccountId);
        if (tenantInfo == null) {
            throw AppException.createUnauthorized(String.format("could not retrieve tenant info for data partition id: %s", primaryAccountId));
        }
        return tenantInfo;
    }

    public String getPartitionId(){
        return dpsHeaders.getPartitionId();
    }

    public TenantInfo getTenantInfo(String accountId){
        return tenantFactory.getTenantInfo(accountId);
    }

    @Override
    public List<TenantInfo> getAllTenantsFromPartitionId() {
        List<TenantInfo> tenantInfos = new LinkedList<>();

        String[] accountIdList = getPartitionId().split(",");
        //Get all tenant values requested by user
        for (String accountId : accountIdList) {
            TenantInfo tenantInfo = getTenantInfo(accountId);
            tenantInfos.add(tenantInfo);
        }
        return tenantInfos;
    }

}
