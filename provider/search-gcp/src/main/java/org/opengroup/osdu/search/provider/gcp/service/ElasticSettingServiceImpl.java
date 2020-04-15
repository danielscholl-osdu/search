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

package org.opengroup.osdu.search.provider.gcp.service;

import org.apache.http.HttpStatus;

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.search.provider.gcp.cache.ElasticCredentialsCache;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class ElasticSettingServiceImpl implements IElasticSettingService {

    @Inject
    private javax.inject.Provider<ITenantInfoService> tenantInfoServiceProvider;

    @Inject
    private IElasticRepository elasticRepository;

    @Inject
    private ElasticCredentialsCache elasticCredentialCache;

    @Inject
    private JaxRsDpsLog log;

    @Override
    public ClusterSettings getElasticClusterInformation() {

        TenantInfo tenantInfo = this.tenantInfoServiceProvider.get().getTenantInfo();

        String cacheKey = String.format("%s-%s", Config.getDeployedServiceId(), tenantInfo.getName());
        ClusterSettings clusterInfo = this.elasticCredentialCache.get(cacheKey);
        if (clusterInfo != null) {
            return clusterInfo;
        }
        log.warning(String.format("elastic-credential cache missed for tenant: %s", tenantInfo.getName()));

        clusterInfo = this.elasticRepository.getElasticClusterSettings(tenantInfo);
        if (clusterInfo == null) {
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Tenant not found", "No information about the given tenant was found");
        }

        this.elasticCredentialCache.put(cacheKey, clusterInfo);
        return clusterInfo;
    }
}
