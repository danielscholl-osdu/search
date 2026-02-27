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

package org.opengroup.osdu.search.provider.azure.config.cache;

import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.search.model.SearchAfterSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(value = "cache.provider", havingValue = "redis")
public class RedisConfig {
    @Value("${redis.port}")
    private int port;

    @Value("${redis.expiration}")
    private int expiration;

    @Value("${redis.cursor.ttl:60}")
    private int cursorRedisTtl;

    @Value("${redis.group.ttl:30}")
    public int groupRedisTtl;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.connection.timeout:15}")
    private int timeout;

    @Value("${redis.index.alias.expiration:86400}")
    private int aliasCacheExpiration;

    @Value("${redis.command.timeout:5}")
    private int commandTimeout;

    @Value("${redis.principal.id:#{null}}")
    private String redisPrincipalId;

    @Bean
    public RedisAzureCache<String, Groups> groupCache() {
        return createCache(Groups.class, groupRedisTtl);
    }

    @Bean
    public RedisAzureCache<String, CursorSettings> cursorCache() {
        return createCache(CursorSettings.class, cursorRedisTtl);
    }

    @Bean
    public RedisAzureCache<String, SearchAfterSettings> searchAfterSettingsCache() {
        return createCache(SearchAfterSettings.class, cursorRedisTtl);
    }

    @Bean
    public RedisAzureCache<String, ClusterSettings> clusterCache() {
        return createCache(ClusterSettings.class, expiration);
    }

    @Bean
    public RedisAzureCache<String, String> aliasCache() {
        return createCache(String.class, aliasCacheExpiration);
    }

    private <T> RedisAzureCache<String, T> createCache(Class<T> valueClass, int ttl) {
        RedisAzureConfiguration config = new RedisAzureConfiguration(database, ttl, port, timeout, commandTimeout, redisPrincipalId);
        return new RedisAzureCache<>(String.class, valueClass, config);
    }
}
