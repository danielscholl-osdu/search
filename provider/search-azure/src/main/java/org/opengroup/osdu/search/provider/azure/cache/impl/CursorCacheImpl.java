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

package org.opengroup.osdu.search.provider.azure.cache.impl;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.search.cache.CursorCache;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CursorCacheImpl implements CursorCache {

    @Resource(name = "cursorCache")
    private ICache<String, CursorSettings> cache;

    @Override
    public void put(String s, CursorSettings o) {
        this.cache.put(s, o);
    }

    @Override
    public CursorSettings get(String s) {
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
