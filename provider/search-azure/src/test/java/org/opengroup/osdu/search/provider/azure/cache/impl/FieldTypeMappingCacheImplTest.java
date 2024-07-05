package org.opengroup.osdu.search.provider.azure.cache.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.cache.VmCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith(MockitoExtension.class)
public class FieldTypeMappingCacheImplTest {

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(FieldTypeMappingCacheImplTest.this);
    }

    @Test
    public void when_FieldTypeMappingCache_isCreated_then_VMCacheConstructorIsCalled_and_objectReturnedIsNotNull() {
        try (MockedConstruction<VmCache> vmCacheClass = mockConstruction(
                VmCache.class)) {
            FieldTypeMappingCacheImpl fieldTypeMappingCache = new FieldTypeMappingCacheImpl();
            assertNotNull(fieldTypeMappingCache);
            assertEquals(1, vmCacheClass.constructed().size());
        }
    }
}
