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

package org.opengroup.osdu.search.provider.azure.cache.impl;

import javax.annotation.Resource;

import org.opengroup.osdu.azure.cache.ElasticCredentialsCache;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("clusterSettingsCache")
@ConditionalOnProperty(value = "cache.provider", havingValue = "redis")
public class ElasticCredentialsCacheImpl extends ElasticCredentialsCache {

  @Resource(name = "clusterCache")
  private ICache<String, ClusterSettings> cache;

  @Override
  public void put(String s, ClusterSettings o) {
    this.cache.put(s, o);
  }

  @Override
  public ClusterSettings get(String s) {
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
