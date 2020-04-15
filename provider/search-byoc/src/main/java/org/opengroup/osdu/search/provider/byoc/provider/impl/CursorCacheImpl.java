package org.opengroup.osdu.search.provider.byoc.provider.impl;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CursorCacheImpl implements CursorCache {

    private VmCache<String, CursorSettings> cache;

    public CursorCacheImpl(@Value("${ELASTIC_CACHE_EXPIRATION}") final String ELASTIC_CACHE_EXPIRATION,
                                   @Value("${MAX_CACHE_VALUE_SIZE}") final String MAX_CACHE_VALUE_SIZE) {
        cache = new VmCache<>(Integer.parseInt(ELASTIC_CACHE_EXPIRATION) * 60,
                Integer.parseInt(MAX_CACHE_VALUE_SIZE));
    }

    @Override
    public void put(String s, CursorSettings o) {
        this.cache.put(s,o);
    }

    @Override
    public CursorSettings get(String s) {
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
