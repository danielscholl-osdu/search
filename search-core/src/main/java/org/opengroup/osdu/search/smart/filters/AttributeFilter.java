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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.models.Attribute;
import org.opengroup.osdu.search.smart.models.AttributeCollection;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;

public class AttributeFilter implements IFilter {

	@Inject
    public AttributeFilter(Attribute attribute, AttributeCollection attributes,
			Provider<ICrossTenantInfoService> tenantInfoServiceProvider) {
		this.attribute =attribute;
		this.attributes= attributes;
		this.tenantInfoServiceProvider = tenantInfoServiceProvider;
	}

	private Attribute attribute;
	private AttributeCollection attributes;
    private Provider<ICrossTenantInfoService> tenantInfoServiceProvider;
    
    @Override
    public String name() {
        return this.attribute.getName();
    }

    @Override
    public String description() {
        return this.attribute.getDescription();
    }

    @Override
    public Map<String, String> values(String query, int limit) throws IOException {
    	Map<String, String> values = new HashMap<>();
    	String attributeName = attribute.getName();
    	List<TenantInfo> tenantInfoList = tenantInfoServiceProvider.get().getAllTenantsFromPartitionId();
    	for (TenantInfo tenantInfo : tenantInfoList) {
    		Set<String> resultSet = attributes.getAllAttributes(tenantInfo.getDataPartitionId(),attributeName);
    		if (!resultSet.isEmpty()) {
				values.putAll(resultSet.stream()
						.filter(x -> x.toLowerCase().startsWith(query.toLowerCase()))
						.limit(limit)
						.distinct()
						.collect(Collectors.toMap(e -> e, e -> attributeName)));
        	}
    	}
    	return values;
    }
}
