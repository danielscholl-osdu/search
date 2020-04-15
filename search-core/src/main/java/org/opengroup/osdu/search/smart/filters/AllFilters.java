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
import java.util.*;


import javax.inject.Inject;
import javax.inject.Provider;

import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.attributes.AttributeLoader;
import org.opengroup.osdu.search.smart.models.Attribute;
import org.opengroup.osdu.search.smart.models.AttributeCollection;

public class AllFilters {
    private Map<String, IFilter> filters = new HashMap<>();
	@Inject
    public AllFilters(Set<IFilter> filters, Provider<ICrossTenantInfoService> tenantInfoServiceProvider, AttributeCollection attributes){
        for (IFilter f : filters) {
            this.filters.put(f.name(), f);
        }
        for(Attribute attribute:AttributeLoader.getAttributes()){
        	IFilter f=new AttributeFilter(attribute,attributes,tenantInfoServiceProvider);
        	this.filters.put(f.name(), f);
        	
        }
    }

    public IFilter getFilter(String name){
        return filters.get(name);
    }
    public Collection<IFilter> list(){
        return filters.values();
    }
    public Map<String, String> values(String query, Set<String> filterNames) throws IOException {
        Map<String, String> output = new TreeMap<>();
        int limitPerFilter = 8;

        if(filterNames == null || filterNames.size() == 0)
            filterNames = filters.keySet();

        for (String name : filterNames){
            IFilter f = getFilter(name);
            if(f != null){
                output.putAll(f.values(query, limitPerFilter));
            }
        }

        return output;
    }
}
