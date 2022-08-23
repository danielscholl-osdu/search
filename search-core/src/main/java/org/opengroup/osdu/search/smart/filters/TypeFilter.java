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

package org.opengroup.osdu.search.smart.filters;

import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.models.Kinds;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;


import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeFilter implements IFilter {

    @Inject
    public TypeFilter(Kinds kinds, Provider<ICrossTenantInfoService> tenantInfoServiceProvider){
        this.kinds = kinds;
        this.tenantInfoServiceProvider=tenantInfoServiceProvider;
    }

    final private Kinds kinds;
    final private Provider<ICrossTenantInfoService> tenantInfoServiceProvider;
    public static final String filterName = "type";

    @Override
    public String name(){
        return filterName;
    }
    @Override
    public String description(){
        return "The type of data you wish to filter on e.g. 'well'.";
    }
    @Override
    public Map<String, String> values(String query, int limit) throws IOException {
        Map<String, String> values=new HashMap<>();
        List<TenantInfo> tenantInfos = this.tenantInfoServiceProvider.get().getAllTenantsFromPartitionId();
        for(TenantInfo tenantInfo:tenantInfos) {
            Set<String> resultSet=this.kinds.all(tenantInfo.getName());
            if(!resultSet.isEmpty()) {
                values.putAll(resultSet.stream()
                        .map(x -> {
                            String[] parts = x.split(":");
                            return parts.length == 4 ? parts[2].toLowerCase() : "";
                        })
                        .filter(x -> x.startsWith(query.toLowerCase()))
                        .distinct()
                        .limit(limit)
                        .collect(Collectors.toMap(e -> e, e -> filterName)));
            }
        }
        return values;
    }
}
