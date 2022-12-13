package org.opengroup.osdu.search.provider.azure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SearchApplicationTest {

    @Test
    public void shouldReturn_notNullInstance_when_creatingNewObject() {
        assertNotNull(new SearchApplication());
    }
}
