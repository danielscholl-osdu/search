/* Copyright © Amazon Web Services

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */

package org.opengroup.osdu.search.provider.aws.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.opengroup.osdu.core.aws.cache.DummyCache;
import org.opengroup.osdu.core.aws.v2.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.v2.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FieldTypeMappingCacheImpl implements IFieldTypeMappingCache {
    @Value("${aws.elasticache.cluster.cursor.endpoint}")
    String redisSearchHost;
    @Value("${aws.elasticache.cluster.cursor.port}")
    String redisSearchPort;
    @Value("${aws.elasticache.cluster.cursor.key}")
    String redisSearchKey;
    @Value("${aws.elasticache.cluster.cursor.expiration}")
    String indexCacheExpiration;
    private ICache<String, Map> cache;
    private Boolean local;
    /**
     * Initializes a Cursor Cache with Redis connection parameters specified in the
     * application
     * properties file.
     *
     */
    public FieldTypeMappingCacheImpl() throws K8sParameterNotFoundException, JsonProcessingException {
        int expTimeSeconds = 60 * 60;
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        local = provider.getLocalMode();
        if (Boolean.TRUE.equals(local)) {

            if (Boolean.parseBoolean(System.getenv("DISABLE_CACHE"))) {
                cache = new DummyCache<>();
            } else {
                this.cache = new VmCache<>(expTimeSeconds, 10);
            }

        } else {
            String host = provider.getParameterAsStringOrDefault("CACHE_CLUSTER_ENDPOINT", redisSearchHost);
            int port = Integer
                    .parseInt(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_PORT", redisSearchPort));
            Map<String, String> credential = provider.getCredentialsAsMap("CACHE_CLUSTER_KEY");
            String password = credential == null ? redisSearchKey : credential.get("token");
            cache = new RedisCache<>(host, port, password, expTimeSeconds, String.class, Map.class);
        }
        local = cache instanceof AutoCloseable;
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
