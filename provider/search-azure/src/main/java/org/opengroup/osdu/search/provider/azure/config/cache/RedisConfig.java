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

import javax.inject.Named;

import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.security.keyvault.secrets.SecretClient;

@Configuration
public class RedisConfig {

  @Bean
  @Named("REDIS_HOST")
  public String redisHost(SecretClient kv) {
    return KeyVaultFacade.getSecretWithValidation(kv, "redis-hostname");
  }

  @Bean
  @Named("REDIS_PASSWORD")
  public String redisPassword(SecretClient kv) {
    return KeyVaultFacade.getSecretWithValidation(kv, "redis-password");
  }


  @Configuration
  @ConditionalOnExpression(value = "'${cache.provider}' == 'redis' && '${redis.ssl.enabled:false}'")
  static class SslConfig {

    @Value("${redis.port}")
    private int port;

    @Value("${redis.expiration}")
    private int expiration;

    @Value("${redis.database}")
    private int database;

    @Bean
    public RedisCache<String, Groups> groupCache(@Named("REDIS_HOST") String host, @Named("REDIS_PASSWORD") String password) {
      return new RedisCache<>(host, port, password, expiration, database, String.class, Groups.class);
    }

    @Bean
    public RedisCache<String, CursorSettings> cursorCache(@Named("REDIS_HOST") String host, @Named("REDIS_PASSWORD") String password) {
      return new RedisCache<>(host, port, password, expiration, database, String.class, CursorSettings.class);
    }

    @Bean
    public RedisCache<String, ClusterSettings> clusterCache(@Named("REDIS_HOST") String host, @Named("REDIS_PASSWORD") String password) {
      return new RedisCache<>(host, port, password, expiration, database, String.class, ClusterSettings.class);
    }
  }

  @Configuration
  @ConditionalOnExpression(value = "'${cache.provider}' == 'redis' && !'${redis.ssl.enabled:true}'")
  static class NoSslConfig {

    @Value("${redis.port}")
    private int port;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.expiration}")
    private int expiration;

    @Bean
    public RedisCache<String, Groups> groupCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, expiration, database, String.class, Groups.class);
    }

    @Bean
    public RedisCache<String, CursorSettings> cursorCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, expiration, database, String.class, CursorSettings.class);
    }

    @Bean
    public RedisCache<String, ClusterSettings> clusterCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, expiration, database, String.class, ClusterSettings.class);
    }
  }

}
