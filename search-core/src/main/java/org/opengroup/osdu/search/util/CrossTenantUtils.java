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

package org.opengroup.osdu.search.util;


import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@Component
public class CrossTenantUtils {

    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private javax.inject.Provider<ITenantInfoService> tenantInfoServiceProvider;

    /* TODO: get rid of this when elastic start supporting cross-cluster search natively - index should not be copied to tenant cluster
    * kind                                 slb account id/data partition              response
    * tenantId:datasource:type:version	    tenantId					                records from kind and tenantId tenant only
    * *:datasource:type:version			    tenantId					                records from tenantId tenant only
    * tenantId:datasource:type:version	    tenantId,common				                records from kind starting with tenantId only
    * *:datasource:type:version			    tenantId,common				                records from tenantId & common tenant
    */
    public String getIndexName(Query searchRequest, String accountHeader) {

        // add tenant prefix if it's cross tenant search request and only one account id specified
        if (searchRequest.getKind().startsWith("*:")) {
            List<String> dataPartitions = Arrays.asList(accountHeader.trim().split("\\s*,\\s*"));

            if (dataPartitions.size() == 1) {
                TenantInfo tenantInfo = this.tenantInfoServiceProvider.get().getTenantInfo();
                return String.format("%s%s,-.*", tenantInfo.getName(), this.elasticIndexNameResolver.getIndexNameFromKind(searchRequest.getKind()));
            }
        }

        return String.format("%s,-.*", this.elasticIndexNameResolver.getIndexNameFromKind(searchRequest.getKind()));
    }
}
