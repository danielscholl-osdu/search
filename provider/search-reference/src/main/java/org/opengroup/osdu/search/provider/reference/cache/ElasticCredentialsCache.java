package org.opengroup.osdu.search.provider.reference.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticCredentialsCache extends RedisCache<String, ClusterSettings> {

  public ElasticCredentialsCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST,
                                 @Value("${REDIS_SEARCH_PORT}") final int REDIS_SEARCH_PORT,
                                 @Value("${ELASTIC_CACHE_EXPIRATION}") final int ELASTIC_CACHE_EXPIRATION) {
    super(REDIS_SEARCH_HOST, REDIS_SEARCH_PORT, ELASTIC_CACHE_EXPIRATION * 60, String.class, ClusterSettings.class);
  }
}
