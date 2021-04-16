package org.opengroup.osdu.search.provider.gcp.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class GroupCacheImpl extends RedisCache<String, Groups> {

    public GroupCacheImpl(SearchConfigurationProperties properties) {
        super(properties.getRedisGroupHost(), properties.getRedisGroupPort(), 30, String.class, Groups.class);
    }
}
