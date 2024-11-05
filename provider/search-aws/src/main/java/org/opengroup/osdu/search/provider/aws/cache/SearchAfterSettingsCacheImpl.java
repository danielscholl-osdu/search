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

package org.opengroup.osdu.search.provider.aws.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.opengroup.osdu.core.aws.cache.DummyCache;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.search.cache.SearchAfterSettingsCache;
import org.opengroup.osdu.search.model.SearchAfterSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchAfterSettingsCacheImpl implements SearchAfterSettingsCache {
    @Value("${aws.elasticache.cluster.cursor.endpoint}")
    String redisSearchHost;
    @Value("${aws.elasticache.cluster.cursor.port}")
    String redisSearchPort;
    @Value("${aws.elasticache.cluster.cursor.key}")
    String redisSearchKey;
    @Value("${aws.elasticache.cluster.cursor.expiration}")
    String indexCacheExpiration;

    private ICache<String, SearchAfterSettings> cache;
    private Boolean local;

    public void close() throws Exception {
        if (Boolean.FALSE.equals(this.local)) {
            ((AutoCloseable) this.cache).close();
        }
    }

    public SearchAfterSettingsCacheImpl() throws K8sParameterNotFoundException, JsonProcessingException {
        int expTimeSeconds = 60 * 60;
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        if (Boolean.TRUE.equals(provider.getLocalMode())) {
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
            String password = (credential == null) ? redisSearchKey : credential.get("token");
            cache = new RedisCache<>(host, port, password, expTimeSeconds, String.class, SearchAfterSettings.class);
        }
        local = cache instanceof AutoCloseable;
    }

    @Override
    public void put(String s, SearchAfterSettings o) {
        this.cache.put(s, o);
    }

    @Override
    public SearchAfterSettings get(String s) {
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
