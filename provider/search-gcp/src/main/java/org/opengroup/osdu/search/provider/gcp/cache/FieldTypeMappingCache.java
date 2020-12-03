package org.opengroup.osdu.search.provider.gcp.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class FieldTypeMappingCache extends RedisCache<String, HashSet> {

    @Autowired
    public FieldTypeMappingCache(final SearchConfigurationProperties configurationProperties) {
        super(configurationProperties.getRedisSearchHost(),
                Integer.parseInt(configurationProperties.getRedisSearchPort()),
                1440 * 60,
                String.class, HashSet.class);
    }
}