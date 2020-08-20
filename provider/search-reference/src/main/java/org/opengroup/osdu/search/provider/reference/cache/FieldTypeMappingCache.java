package org.opengroup.osdu.search.provider.reference.cache;

import java.util.HashSet;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FieldTypeMappingCache extends RedisCache<String, HashSet> {

  public FieldTypeMappingCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST,
                               @Value("${REDIS_SEARCH_PORT}") final int REDIS_SEARCH_PORT) {
    super(REDIS_SEARCH_HOST, REDIS_SEARCH_PORT, 1440 * 60, String.class, HashSet.class);
  }
}
