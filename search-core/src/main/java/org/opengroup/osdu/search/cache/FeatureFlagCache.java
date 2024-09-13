/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.cache;

import jakarta.inject.Inject;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagCache implements ICache<String, Boolean> {
    private VmCache<String, Boolean> cache;

    @Inject
    private DpsHeaders dpsHeaders;

    public FeatureFlagCache() {
        cache = new VmCache<>(300, 1000);
    }

    @Override
    public void put(String s, Boolean o) {
        this.cache.put(cacheKey(s), o);
    }

    @Override
    public Boolean get(String s) {
        return this.cache.get(cacheKey(s));
    }

    @Override
    public void delete(String s) {
        this.cache.delete(cacheKey(s));
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }

    private String cacheKey(String s) {
        return this.dpsHeaders.getPartitionId() + "-" + this.getClass().getSimpleName().toLowerCase() + "-" + s;
    }
}
