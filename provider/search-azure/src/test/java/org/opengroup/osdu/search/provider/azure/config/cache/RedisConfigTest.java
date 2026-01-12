package org.opengroup.osdu.search.provider.azure.config.cache;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class RedisConfigTest {

    @Value("800")
    private int port;

    @Value("900")
    private int expiration;

    @Value("900")
    private int database;

    @Value("15")
    private int timeout;

    @InjectMocks
    public RedisConfig sut = new RedisConfig();

    @Test
    public void groupCache_shouldCreateNonNullObjectOfRedisAzureCache() {
        RedisAzureCache<String, Groups> redisAzureCache = sut.groupCache();
        assertNotNull(redisAzureCache);
    }

    @Test
    public void cursorCache_shouldCreateNonNullObjectOfRedisAzureCache() {
        RedisAzureCache<String, CursorSettings> redisAzureCache = sut.cursorCache();
        assertNotNull(redisAzureCache);
    }

    @Test
    public void clusterCache_shouldCreateNonNullObjectOfRedisAzureCache() {
        RedisAzureCache<String, ClusterSettings> redisAzureCache = sut.clusterCache();
        assertNotNull(redisAzureCache);
    }

    @Test
    public void aliasCache_shouldCreateNonNullObjectOfRedisAzureCache() {
        RedisAzureCache<String, String> redisAzureCache = sut.aliasCache();
        assertNotNull(redisAzureCache);
    }
}
