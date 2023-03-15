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
import org.springframework.stereotype.Component;

@Component
public class IndexAliasCacheVmImpl implements IIndexAliasCache {
    private VmCache<String, String> cache;
    // The index alias won't be changed once it is created
    private final int ALIAS_CACHE_EXPIRATION = 86400;
    private final int MAX_CACHE_SIZE = 2000;

    public IndexAliasCacheVmImpl() {
        cache = new VmCache<>(ALIAS_CACHE_EXPIRATION, MAX_CACHE_SIZE);
    }

    @Override
    public void put(String s, String o) {
        this.cache.put(s, o);
    }

    @Override
    public String get(String s) {
        return this.cache.get(s);
    }

    @Override
    public void delete(String s) {
        this.cache.delete(s);
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }
}
