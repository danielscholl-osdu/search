// Copyright © Schlumberger
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

package org.opengroup.osdu.search.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class IndexAliasCacheVmImpl implements IndexAliasCache{
    private VmCache<String, String> cache;

    @Inject
    private TenantInfo tenant;

    public IndexAliasCacheVmImpl() {
        // The index alias won't be changed once it is created
        cache = new VmCache<>(24 * 3600, 2000);
    }

    @Override
    public void put(String s, String o) {
        this.cache.put(getKey(s), o);
    }

    @Override
    public String get(String s) {
        return this.cache.get(getKey(s));
    }

    @Override
    public void delete(String s) {
        this.cache.delete(getKey(s));
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }

    private String getKey(String kind) {
        return tenant.getDataPartitionId() + "-" + kind;
    }
}
