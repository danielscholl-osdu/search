/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.provider.gcp.cache;

import com.google.gson.Gson;
import java.util.Objects;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class ElasticCredentialsCache implements IElasticCredentialsCache<String, ClusterSettings>,
    AutoCloseable {

  private RedisCache<String, String> cache;

  public ElasticCredentialsCache(final SearchConfigurationProperties configurationProperties) {
    this.cache = new RedisCache<>(configurationProperties.getRedisSearchHost(),
        Integer.parseInt(configurationProperties.getRedisSearchPort()),
        configurationProperties.getElasticCacheExpiration() * 60,
        String.class,
        String.class);
  }

  @Override
  public void put(String key, ClusterSettings value) {
    String jsonSettings = new Gson().toJson(value);
    this.cache.put(key, jsonSettings);
  }

  @Override
  public ClusterSettings get(String key) {
    String jsonSettings = this.cache.get(key);
    if (Objects.isNull(jsonSettings) || jsonSettings.isEmpty()) {
      return null;
    }
    return new Gson().fromJson(jsonSettings, ClusterSettings.class);
  }

  @Override
  public void delete(String s) {
    this.cache.delete(s);
  }

  @Override
  public void clearAll() {
    this.cache.clearAll();
  }

  @Override
  public void close() {
    this.cache.close();
  }
}
