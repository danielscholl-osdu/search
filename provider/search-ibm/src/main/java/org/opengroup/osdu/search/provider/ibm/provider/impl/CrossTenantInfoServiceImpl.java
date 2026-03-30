/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.search.provider.ibm.provider.impl;


import java.util.ArrayList;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import java.util.LinkedList;
import java.util.List;

@Service
public class CrossTenantInfoServiceImpl implements ITenantInfoService, ICrossTenantInfoService {

    @Inject
    private ITenantFactory tenantFactory;

    @Inject
    private DpsHeaders headers;
    
    @Inject
    private  JaxRsDpsLog log;

    @Override
    public TenantInfo getTenantInfo() {
        String primaryAccountId = this.headers.getPartitionIdWithFallbackToAccountId();
        return getTenantInfoForPartition(primaryAccountId);
    }

    @Override
    public TenantInfo getTenantInfoForPartition(String partitionId) {
        TenantInfo tenantInfo = this.tenantFactory.getTenantInfo(partitionId);
        if (tenantInfo == null) {
        	log.error("Tenant info has not found!");
            throw AppException.createUnauthorized(String.format("could not retrieve tenant info for data partition id: %s", partitionId));
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
