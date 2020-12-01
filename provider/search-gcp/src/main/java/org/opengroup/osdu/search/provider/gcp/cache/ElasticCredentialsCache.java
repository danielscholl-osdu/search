package org.opengroup.osdu.search.provider.gcp.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticCredentialsCache extends RedisCache<String, ClusterSettings> {

    @Autowired
    public ElasticCredentialsCache(final SearchConfigurationProperties configurationProperties) {
        super(configurationProperties.getRedisSearchHost(),
                Integer.parseInt(configurationProperties.getRedisSearchPort()),
                configurationProperties.getElasticCacheExpiration() * 60,
                String.class,
                ClusterSettings.class);
    }
}
