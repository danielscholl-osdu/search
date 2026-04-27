package org.opengroup.osdu.search.provider.azure.config.cache;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

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

    @BeforeEach
    public void setUp() {
        // Set default values for required fields to ensure consistent test state
        ReflectionTestUtils.setField(sut, "port", 6379);
        ReflectionTestUtils.setField(sut, "database", 0);
        ReflectionTestUtils.setField(sut, "timeout", 15);
        ReflectionTestUtils.setField(sut, "commandTimeout", 5);
        ReflectionTestUtils.setField(sut, "groupRedisTtl", 30);
        ReflectionTestUtils.setField(sut, "cursorRedisTtl", 60);
        ReflectionTestUtils.setField(sut, "expiration", 900);
        ReflectionTestUtils.setField(sut, "aliasCacheExpiration", 86400);
    }

    @ParameterizedTest(name = "[{index}] principalId={0}, hostname={1}")
    @CsvSource({
        ",",
        ", redis.example.com",
        "a1b2c3d4-e5f6-7890-abcd-ef1234567890,",
        "a1b2c3d4-e5f6-7890-abcd-ef1234567890, redis.example.com"
    })
    public void allCaches_shouldCreateSuccessfully(String principalId, String hostname) {
        if (principalId != null) {
            ReflectionTestUtils.setField(sut, "redisPrincipalId", principalId);
        }
        if (hostname != null) {
            ReflectionTestUtils.setField(sut, "redisHostname", hostname);
        }

        assertNotNull(sut.groupCache());
        assertNotNull(sut.cursorCache());
        assertNotNull(sut.searchAfterSettingsCache());
        assertNotNull(sut.clusterCache());
        assertNotNull(sut.aliasCache());
    }
}
