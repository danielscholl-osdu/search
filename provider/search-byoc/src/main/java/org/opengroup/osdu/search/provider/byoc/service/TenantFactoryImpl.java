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

package org.opengroup.osdu.search.provider.byoc.service;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class TenantFactoryImpl implements ITenantFactory {
    public static final String[] REGISTERED_TENANTS = new String[] {"tenant1","common", "opendes"};
    private Map<String, TenantInfo> tenants;

    public TenantFactoryImpl()
    {
        this.tenants = new HashMap<>();
        for (String tenantName : REGISTERED_TENANTS) {
            TenantInfo ti = new TenantInfo();
            ti.setName(tenantName);
            ti.setComplianceRuleSet(tenantName.equals("common") ?
                    TenantInfo.ComplianceRuleSets.SHARED : TenantInfo.ComplianceRuleSets.CUSTOMER);
            this.tenants.put(tenantName, ti);
        }
    }

    public boolean exists(String tenantName)
    {
        return this.tenants.containsKey(tenantName);
    }

    public TenantInfo getTenantInfo(String tenantName) {
        return this.tenants.get(tenantName);
    }

    public Collection<TenantInfo> listTenantInfo() {
        return this.tenants.values();
    }

    public <V> ICache<String, V> createCache(String tenantName, String host, int port, int expireTimeSeconds, Class<V> classOfV)
    {
        return null;
    }

    public void flushCache() {}
}

