/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.provider.gcp.cache;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.opengroup.osdu.core.gcp.cache.RedisCacheBuilder;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;
import org.opengroup.osdu.search.provider.gcp.config.GcpSearchConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    @Bean
    public ICache<String, Groups> groupCache(GcpSearchConfigurationProperties appProperties) {
        return new GcGroupCache();
    }

    @Bean
    public IFieldTypeMappingCache fieldTypeMappingCache(GcpSearchConfigurationProperties appProperties) {
        RedisCacheBuilder<String, Map> cacheBuilder = new RedisCacheBuilder<>();
        RedisCache<String, Map> fieldTypeMappingCache = cacheBuilder.buildRedisCache(
            appProperties.getRedisSearchHost(),
            Integer.parseInt(appProperties.getRedisSearchPort()),
            appProperties.getRedisSearchPassword(),
            appProperties.getRedisSearchExpiration(),
            appProperties.getRedisSearchWithSsl(),
            String.class,
            Map.class
        );
        return new FieldTypeMappingCache(fieldTypeMappingCache);
    }

    @Bean
    public IElasticCredentialsCache<String, ClusterSettings> elasticCredentialsCache(GcpSearchConfigurationProperties gcpAppServiceConfig) {
        RedisCacheBuilder<String, ClusterSettings> cacheBuilder = new RedisCacheBuilder<>();
        RedisCache<String, ClusterSettings> clusterSettingCache = cacheBuilder.buildRedisCache(
            gcpAppServiceConfig.getRedisSearchHost(),
            Integer.parseInt(gcpAppServiceConfig.getRedisSearchPort()),
            gcpAppServiceConfig.getRedisSearchPassword(),
            gcpAppServiceConfig.getRedisSearchExpiration(),
            gcpAppServiceConfig.getRedisSearchWithSsl(),
            String.class,
            ClusterSettings.class
        );
        return new ElasticCredentialsCache(clusterSettingCache);
    }

    @Bean
    public CursorCache cursorCache(GcpSearchConfigurationProperties gcpAppServiceConfig) {
        RedisCacheBuilder<String, CursorSettings> cacheBuilder = new RedisCacheBuilder<>();
        RedisCache<String, CursorSettings> cursorSettingsRedisCache = cacheBuilder.buildRedisCache(
            gcpAppServiceConfig.getRedisSearchHost(),
            Integer.parseInt(gcpAppServiceConfig.getRedisSearchPort()),
            gcpAppServiceConfig.getRedisSearchPassword(),
            gcpAppServiceConfig.getRedisSearchExpiration(),
            gcpAppServiceConfig.getRedisSearchWithSsl(),
            String.class,
            CursorSettings.class
        );
        return new CursorCacheImpl(cursorSettingsRedisCache);
    }
}
