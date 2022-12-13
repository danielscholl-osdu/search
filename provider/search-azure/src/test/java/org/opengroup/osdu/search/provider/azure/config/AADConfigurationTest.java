package org.opengroup.osdu.search.provider.azure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AADConfigurationTest {

    private static final String authority="authority";
    private static final String clientId="clientId";
    private static final String secretKey="secretKey";
    private static final String oboApiField="oboApi";
    private static final String oboApi="OboApi";

    @InjectMocks
    AADConfiguration sut;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(AADConfigurationTest.this);
        ReflectionTestUtils.setField(sut, authority, authority);
        ReflectionTestUtils.setField(sut, clientId, clientId);
        ReflectionTestUtils.setField(sut, secretKey, secretKey);
        ReflectionTestUtils.setField(sut, oboApiField, oboApi);
    }

    @Test
    void should_return_setValue_when_getAuthority_isCalled() {
        assertEquals(authority+"/", sut.getAuthority());
    }

    @Test
    void should_return_setValue_when_getClientId_isCalled() {
        assertEquals(clientId, sut.getClientId());
    }

    @Test
    void should_return_setValue_when_getSecretKey_isCalled() {
        assertEquals(secretKey, sut.getSecretKey());
    }

    @Test
    void should_return_setValue_when_getOboApi_isCalled() {
        assertEquals(oboApi,sut.getOboApi());
    }
}
