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

package org.opengroup.osdu.search.auth;

import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.entitlements.AuthorizationServiceImpl;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.util.Crc32c;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
@Primary
@Lazy
@RequiredArgsConstructor
public class CorePlusAuthorizationService extends AuthorizationServiceImpl {

  private final ICache<String, Groups> cache;
  private final JaxRsDpsLog jaxRsDpsLog;

  @Override
  public AuthorizationResponse authorizeAny(DpsHeaders dpsHeaders, String... roles) {
    String cacheKey = getGroupCacheKey(dpsHeaders);
    Groups groups = getGroupsFromCache(cacheKey);
    if (groups == null) {
      AuthorizationResponse authorizationResponse = super.authorizeAny(dpsHeaders, roles);
      this.cache.put(cacheKey, authorizationResponse.getGroups());
      return authorizationResponse;
    } else {
      return AuthorizationResponse.builder().user(groups.getMemberEmail()).groups(groups).build();
    }
  }

  @Override
  public AuthorizationResponse authorizeAny(String tenantName, DpsHeaders dpsHeaders, String... roles) {
    String cacheKey = getGroupCacheKey(dpsHeaders);
    Groups groups = getGroupsFromCache(cacheKey);
    if (groups == null) {
      AuthorizationResponse authorizationResponse = super.authorizeAny(tenantName, dpsHeaders, roles);
      this.cache.put(cacheKey, authorizationResponse.getGroups());
      return authorizationResponse;
    } else {
      return AuthorizationResponse.builder().user(groups.getMemberEmail()).groups(groups).build();
    }
  }

  protected AuthorizationResponse authorizeViaParent(
          DpsHeaders dpsHeaders, String... roles) {
    return super.authorizeAny(dpsHeaders, roles);
  }

  protected AuthorizationResponse authorizeViaParent(
          String tenantName, DpsHeaders dpsHeaders, String... roles) {
    return super.authorizeAny(tenantName, dpsHeaders, roles);
  }

  @Nullable
  private Groups getGroupsFromCache(String cacheKey) {
    Groups groups = null;
    try {
      groups = this.cache.get(cacheKey);
    } catch (RedisException ex) {
      this.jaxRsDpsLog.error(String.format("Error getting key %s from redis: %s", cacheKey, ex.getMessage()), ex);
    }
    return groups;
  }

  private String getGroupCacheKey(DpsHeaders headers) {
    String key = String.format("entitlement-groups:%s:%s", headers.getPartitionIdWithFallbackToAccountId(),
        headers.getAuthorization());
    return Crc32c.hashToBase64EncodedString(key);
  }
}
