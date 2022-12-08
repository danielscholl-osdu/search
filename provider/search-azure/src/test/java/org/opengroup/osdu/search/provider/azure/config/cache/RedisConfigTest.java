package org.opengroup.osdu.search.provider.azure.config.cache;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.springframework.beans.factory.annotation.Value;

@ExtendWith(MockitoExtension.class)
public class RedisConfigTest {
    @Mock
    @Value("800")
    private int port;

    @Mock
    @Value("900")
    private int expiration;

    @Mock
    @Value("900")
    private int database;

    @Mock
    @Value("15")
    private int timeout;

    @InjectMocks
    public RedisConfig sut = new RedisConfig();

    @Test
    public void groupCache_shouldCreateNonNullObjectOfRedisAzureCache() {
        RedisAzureCache<String, Groups> redisAzureCache = sut.groupCache();
        Assert.assertNotNull(redisAzureCache);
    }

    @Test
    public void cursorCache_shouldCreateNonNullObjectOfRedisAzureCache() {
        RedisAzureCache<String, CursorSettings> redisAzureCache = sut.cursorCache();
        Assert.assertNotNull(redisAzureCache);
    }

    @Test
    public void clusterCache_shouldCreateNonNullObjectOfRedisAzureCache() {
        RedisAzureCache<String, ClusterSettings> redisAzureCache = sut.clusterCache();
        Assert.assertNotNull(redisAzureCache);
    }
}
