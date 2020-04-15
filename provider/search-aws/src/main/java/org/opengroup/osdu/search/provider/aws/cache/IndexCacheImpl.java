package org.opengroup.osdu.search.provider.aws.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.provider.interfaces.IIndexCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IndexCacheImpl implements IIndexCache<String, Boolean>, AutoCloseable {

    private RedisCache<String, Boolean> cache;

    public IndexCacheImpl(@Value("${aws.elasticache.cluster.index.endpoint}") final String REDIS_SEARCH_HOST,
                          @Value("${aws.elasticache.cluster.index.port}") final String REDIS_SEARCH_PORT,
                          @Value("${aws.elasticache.cluster.index.expiration}") final String INDEX_CACHE_EXPIRATION) {
        cache = new RedisCache<>(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT),
                Integer.parseInt(INDEX_CACHE_EXPIRATION) * 60, String.class, Boolean.class);
    }

    @Override
    public void close() throws Exception {
        this.cache.close();
    }

    @Override
    public void put(String s, Boolean o) {
        this.cache.put(s, o);
    }

    @Override
    public Boolean get(String s) {
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
