package org.opengroup.osdu.search.provider.azure.utils;

import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigModuleTest {

    @Mock
    ResteasyHttpHeaders resteasyHttpHeaders;

    @InjectMocks
    ConfigModule sut;

    @Test
    void should_return_nonNullObject_when_resteasyHttpHeaders_isCalled() {
        ResteasyHttpHeaders resteasyHttpHeaders = sut.resteasyHttpHeaders();
        assertNotNull(resteasyHttpHeaders);
    }
}
