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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.common.cache.RemovalListener;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ElasticsearchClientCache implements ICache<String, ElasticsearchClient> {

  private final VmCache<String, ElasticsearchClient> vmCache;

  public ElasticsearchClientCache(
          SearchConfigurationProperties searchConfigurationProperties,
          RemovalListener<String, ElasticsearchClient> removalListener
  ) {
    this.vmCache = new VmCache<>(
        searchConfigurationProperties.getElasticCacheExpiration(),
        searchConfigurationProperties.getMaximumCacheSize(),
        removalListener
    );
  }

  @Override
  public void put(String partitionId, ElasticsearchClient client) {
    this.vmCache.put(partitionId, client);
  }

  @Override
  public ElasticsearchClient get(String partitionId) {
    return this.vmCache.get(partitionId);
  }

  @Override
  public void delete(String partitionId) {
    this.vmCache.delete(partitionId);
  }

  @Override
  public void clearAll() {
    this.vmCache.clearAll();
  }
  
  /**
   * Thread-safe atomic compute-if-absent operation to prevent race conditions.
   * Delegates to VmCache which uses Guava Cache's atomic get(K, Callable) operation.
   * 
   * @param partitionId The partition ID to look up or compute
   * @param mappingFunction Function to create a new client if not present
   * @return The existing or newly computed client
   */
  public ElasticsearchClient computeIfAbsent(String partitionId, Function<String, ElasticsearchClient> mappingFunction) {
    return this.vmCache.computeIfAbsent(partitionId, mappingFunction);
  }
}
