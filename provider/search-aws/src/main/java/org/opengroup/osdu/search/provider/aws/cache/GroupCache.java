/* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.

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
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GroupCache<K, V> implements ICache<K, V> {
    @Value("${aws.elasticache.cluster.endpoint:null}")
    String redisSearchHost;
    @Value("${aws.elasticache.cluster.port:null}")
    String redisSearchPort;
    @Value("${aws.elasticache.cluster.key:null}")
    String redisSearchKey;
    private ICache<K, V> cache;

    public GroupCache() throws K8sParameterNotFoundException, JsonProcessingException {
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        Boolean isLocal = provider.getLocalMode();
        if (Boolean.TRUE.equals(isLocal)) {
            if (Boolean.parseBoolean(System.getenv("DISABLE_CACHE"))) {
                this.cache = new DummyCache<>();
            }
            this.cache = new VmCache<>(60, 10);
        } else {
            String host = provider.getParameterAsStringOrDefault("CACHE_CLUSTER_ENDPOINT", redisSearchHost);
            int port = Integer
                    .parseInt(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_PORT", redisSearchPort));
            Map<String, String> credential = provider.getCredentialsAsMap("CACHE_CLUSTER_KEY");
            String password = credential == null ? redisSearchKey : credential.get("token");    
            this.cache = new RedisCache(host, port, password, 60, String.class, Groups.class);
        }
    }

    @Override
    public void put(K k, V o) {
        this.cache.put(k, o);
    }

    @Override
    public V get(K k) {
        return this.cache.get(k);
    }

    @Override
    public void delete(K k) {
        this.cache.delete(k);
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }
}