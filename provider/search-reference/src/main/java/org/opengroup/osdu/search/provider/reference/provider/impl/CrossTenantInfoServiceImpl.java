package org.opengroup.osdu.search.provider.reference.provider.impl;

import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.springframework.stereotype.Service;

@Service
public class CrossTenantInfoServiceImpl implements ITenantInfoService, ICrossTenantInfoService {

    @Inject
    private ITenantFactory tenantFactory;

    @Inject
    private DpsHeaders headers;

    @Override
    public TenantInfo getTenantInfo() {
        String primaryAccountId = this.headers.getPartitionIdWithFallbackToAccountId();
        TenantInfo tenantInfo = this.tenantFactory.getTenantInfo(primaryAccountId);
        if (tenantInfo == null) {
            throw AppException.createUnauthorized(String.format("could not retrieve tenant info for data partition id: %s", primaryAccountId));
        }
        return tenantInfo;
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
