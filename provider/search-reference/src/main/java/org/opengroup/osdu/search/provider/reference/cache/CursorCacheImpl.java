package org.opengroup.osdu.search.provider.reference.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.search.cache.CursorCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CursorCacheImpl extends RedisCache<String, CursorSettings> implements CursorCache {
    
    public CursorCacheImpl(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST, @Value("${REDIS_SEARCH_PORT}") final int REDIS_SEARCH_PORT, @Value("${CURSOR_CACHE_EXPIRATION}") final int CURSOR_CACHE_EXPIRATION) {
        super(REDIS_SEARCH_HOST, REDIS_SEARCH_PORT, CURSOR_CACHE_EXPIRATION, String.class, CursorSettings.class);
    }
}
