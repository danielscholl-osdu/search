package org.opengroup.osdu.search.provider.azure.config.cache;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    public void allCaches_shouldCreateSuccessfully(String principalId) {
        if (principalId != null) {
            ReflectionTestUtils.setField(sut, "redisPrincipalId", principalId);
        }

        assertNotNull(sut.groupCache());
        assertNotNull(sut.cursorCache());
        assertNotNull(sut.searchAfterSettingsCache());
        assertNotNull(sut.clusterCache());
        assertNotNull(sut.aliasCache());
    }
}
