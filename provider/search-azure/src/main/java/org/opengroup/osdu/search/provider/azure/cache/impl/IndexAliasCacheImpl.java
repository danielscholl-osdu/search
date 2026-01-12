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
package org.opengroup.osdu.search.provider.azure.cache.impl;

import io.lettuce.core.RedisException;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.search.cache.IIndexAliasCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
@Primary
@ConditionalOnProperty(value = "cache.provider", havingValue = "redis")
public class IndexAliasCacheImpl implements IIndexAliasCache {
    @Resource(name = "aliasCache")
    private ICache<String, String> cache;

    @Autowired
    private JaxRsDpsLog log;

    @Override
    public void put(String s, String o) {
        try {
            this.cache.put(s, o);
        } catch (RedisException ex) {
            this.log.error(String.format("Error putting key %s into redis: %s", s, ex.getMessage()), ex);
        }
    }

    @Override
    public String get(String s) {
        String clusterSettings = null;
        try {
            clusterSettings = this.cache.get(s);
        } catch (RedisException ex) {
            this.log.error(String.format("Error getting key %s from redis: %s", s, ex.getMessage()), ex);
        }
        return clusterSettings;
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
