
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
public class ElasticLoggingConfigTest {

    private static final String enabledField = "enabled";
    private static final Boolean enabled=true;
    private static final Boolean alterEnabled=false;
    private static final String thresholdField = "threshold";
    private static final Long threshold=900L;
    private static final Long alterThreshold=100L;

    @InjectMocks
    public ElasticLoggingConfig sut;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(ElasticLoggingConfigTest.this);
        ReflectionTestUtils.setField(sut, enabledField, enabled);
        ReflectionTestUtils.setField(sut, thresholdField, threshold);
    }

    @Test
    public void should_return_true_when_getEnabled_isCalled() {
        assertEquals(sut.getEnabled(), enabled);
    }

    @Test
    public void should_return_900_when_getThreshold_isCalled() {
        assertEquals(sut.getThreshold(),threshold);
    }

    @Test
    public void should_return_false_when_setEnabled_setToFalse() {
        sut.setEnabled(alterEnabled);
        assertFalse(sut.getEnabled());
    }

    @Test
    public void should_return_100_when_setThreshold_setTo100() {
        sut.setThreshold(alterThreshold);
        assertEquals(sut.getThreshold(), alterThreshold);
    }

    @Test
    public void testToString() {
        assertNotNull(sut.toString());
    }
}
