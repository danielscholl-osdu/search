// Copyright © Amazon Web Services
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

package org.opengroup.osdu.search.provider.aws.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FieldTypeMappingCacheImpl implements IFieldTypeMappingCache {

    private RedisCache<String, Map> cache;

    /**
     * Initializes a Cursor Cache with Redis connection parameters specified in the application
     * properties file.
     *
     * @param REDIS_SEARCH_HOST - the hostname of the Cursor Cache Redis cluster.
     * @param REDIS_SEARCH_PORT - the port of the Cursor Cache Redis cluster.
     */
    public FieldTypeMappingCacheImpl(@Value("${aws.elasticache.cluster.cursor.endpoint}") final String REDIS_SEARCH_HOST,
                                     @Value("${aws.elasticache.cluster.cursor.port}") final String REDIS_SEARCH_PORT,
                                     @Value("${aws.elasticache.cluster.cursor.key}") final String REDIS_SEARCH_KEY,
                                     @Value("${aws.elasticache.cluster.cursor.expiration}") final String INDEX_CACHE_EXPIRATION) {
        cache = new RedisCache<String, Map>(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT), REDIS_SEARCH_KEY,
                Integer.parseInt(INDEX_CACHE_EXPIRATION) * 60, String.class, Map.class);
    }

    /**
     * Insert a Map object into the Redis cache
     *
     * @param s the key of the object, used for retrieval
     * @param o the Map object to store
     */
    @Override
    public void put(String s, Map o) {
        this.cache.put(s, o);
    }


    /**
     * Gets a cached Map object by key
     *
     * @param s the key of the cached Map object to get
     * @return
     */
    @Override
    public Map get(String s) {
        return this.cache.get(s);
    }

    /**
     * Deletes a Map item in the cache with the given key
     *
     * @param s the key of the cached Map object to delete
     */
    @Override
    public void delete(String s) {
        this.cache.delete(s);
    }

    /**
     * Clears the entire Redis cache
     */
    @Override
    public void clearAll() {
        this.cache.clearAll();
    }

}
