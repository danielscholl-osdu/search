package org.opengroup.osdu.search.provider.azure.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceAccountJwtClientImplTest {

    private static final String authorizationToken ="bearerToken";
    private static final String partitionId ="opendes";
    private static final String expectedToken = "Bearer "+authorizationToken;

    @Mock
    public AzureServicePrincipleTokenService tokenService;

    @InjectMocks
    public ServiceAccountJwtClientImpl sut;

    @Test
    public void should_prefix_Bearer_toSetToken_when_getIdToken_isCalled() {
        when(tokenService.getAuthorizationToken()).thenReturn(authorizationToken);

        String idToken = sut.getIdToken(partitionId);

        assertEquals(expectedToken, idToken);
    }
}

