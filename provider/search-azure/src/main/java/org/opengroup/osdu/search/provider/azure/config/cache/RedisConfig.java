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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedisConfig {
  @Value("${redis.port}")
  private int port;

  @Value("${redis.expiration}")
  private int expiration;

  @Value("${redis.database}")
  private int database;

  @Value("${redis.connection.timeout:15}")
  private int timeout;

  @Bean
  public RedisAzureCache<String, Groups> groupCache() {
    return new RedisAzureCache<>(String.class, Groups.class, new RedisAzureConfiguration(database, expiration, port, timeout));
  }

  @Bean
  public RedisAzureCache<String, CursorSettings> cursorCache() {
    return new RedisAzureCache<>(String.class, CursorSettings.class, new RedisAzureConfiguration(database, expiration, port, timeout));
  }

  @Bean
  public RedisAzureCache<String, ClusterSettings> clusterCache() {
    return new RedisAzureCache<>(String.class, ClusterSettings.class, new RedisAzureConfiguration(database, expiration, port, timeout));
  }
}
