package org.opengroup.osdu.search.provider.gcp.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CursorCacheImpl extends RedisCache<String, CursorSettings> implements CursorCache {

    @Autowired
    public CursorCacheImpl(final SearchConfigurationProperties configurationProperties) {
        super(configurationProperties.getRedisSearchHost(),
                Integer.parseInt(configurationProperties.getRedisSearchPort()),
                configurationProperties.getCursorCacheExpiration(),
                String.class,
                CursorSettings.class);
    }
}
