/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
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

package org.opengroup.osdu.search.cache;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.cache.RedisCacheBuilder;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.opengroup.osdu.search.config.CorePlusSearchConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

  private final RedisCacheBuilder<String, ClusterSettings> clusterSettingsCacheBuilder;
  private final RedisCacheBuilder<String, CursorSettings> cursorSettingsCacheBuilderBuilder;
  private final RedisCacheBuilder<String, Map> fieldTypeMappingCacheBuilder;

  @Bean
  public ICache<String, Groups> groupCache(CorePlusSearchConfigurationProperties appProperties) {
    return new GroupCache();
  }

  @Bean
  public IFieldTypeMappingCache fieldTypeMappingCache(
      CorePlusSearchConfigurationProperties appProperties) {
    RedisCache<String, Map> fieldTypeMappingCache =
        fieldTypeMappingCacheBuilder.buildRedisCache(
            appProperties.getRedisSearchHost(),
            Integer.parseInt(appProperties.getRedisSearchPort()),
            appProperties.getRedisSearchPassword(),
            appProperties.getRedisSearchExpiration(),
            appProperties.getRedisSearchWithSsl(),
            String.class,
            Map.class);
    return new FieldTypeMappingCache(fieldTypeMappingCache);
  }

  @Bean
  public IElasticCredentialsCache<String, ClusterSettings> elasticCredentialsCache(
      RedisCache<String, ClusterSettings> elasticCache) {
    return new ElasticCredentialsCache(elasticCache);
  }

  @Bean
  public RedisCache<String, ClusterSettings> elasticCache(
      CorePlusSearchConfigurationProperties gcpAppServiceConfig) {
    return clusterSettingsCacheBuilder.buildRedisCache(
        gcpAppServiceConfig.getRedisSearchHost(),
        Integer.parseInt(gcpAppServiceConfig.getRedisSearchPort()),
        gcpAppServiceConfig.getRedisSearchPassword(),
        gcpAppServiceConfig.getRedisSearchExpiration(),
        gcpAppServiceConfig.getRedisSearchWithSsl(),
        String.class,
        ClusterSettings.class);
  }

  @Bean
  public CursorCache cursorCache(CorePlusSearchConfigurationProperties gcpAppServiceConfig) {
    RedisCache<String, CursorSettings> stringCursorSettingsCache =
        cursorSettingsCacheBuilderBuilder.buildRedisCache(
            gcpAppServiceConfig.getRedisSearchHost(),
            Integer.parseInt(gcpAppServiceConfig.getRedisSearchPort()),
            gcpAppServiceConfig.getRedisSearchPassword(),
            gcpAppServiceConfig.getRedisSearchExpiration(),
            gcpAppServiceConfig.getRedisSearchWithSsl(),
            String.class,
            CursorSettings.class);
    return new CursorCacheImpl(stringCursorSettingsCache);
  }

  @Bean
  public ICache<String, PartitionInfo> partitionInfoCache() {
    return new VmCache<>(600, 2000);
  }
}
