// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.search.provider.gcp.cache;

import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.util.Crc32c;
import org.opengroup.osdu.search.cache.GroupCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("groupsCache")
public class GroupCacheImpl extends RedisCache<String, Groups> implements GroupCache {

    public GroupCacheImpl(@Value("${REDIS_GROUP_HOST}") final String REDIS_GROUP_HOST,@Value("${REDIS_GROUP_PORT}") final String REDIS_GROUP_PORT)
    {
        super(REDIS_GROUP_HOST, Integer.parseInt(REDIS_GROUP_PORT), 30, String.class,
                Groups.class);

    }

    public String getCacheKey(DpsHeaders headers) {
        String key = String.format("entitlement-groups:%s:%s", headers.getPartitionIdWithFallbackToAccountId(),
                headers.getAuthorization());
        return Crc32c.hashToBase64EncodedString(key);
    }
}
