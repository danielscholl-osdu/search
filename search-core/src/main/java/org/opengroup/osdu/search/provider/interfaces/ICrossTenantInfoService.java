package org.opengroup.osdu.search.provider.interfaces;

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;

import java.util.List;

public interface ICrossTenantInfoService {
    List<TenantInfo> getAllTenantsFromPartitionId();
}
