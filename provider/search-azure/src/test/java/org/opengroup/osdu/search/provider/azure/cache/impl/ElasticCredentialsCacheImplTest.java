package org.opengroup.osdu.search.provider.azure.cache.impl;

import io.lettuce.core.RedisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class ElasticCredentialsCacheImplTest {

    private static final String key = "key";
    private static final String cursor = "cursor";
    private static final String host = "host";
    private static final Integer port = 9000;
    private static final String userNameandPassword = "userNameandPassword";

    @Mock
    @Resource(name = "clusterCache")
    private ICache<String, ClusterSettings> cache;

    @Mock
    @Autowired
    private JaxRsDpsLog log;

    @InjectMocks
    public ElasticCredentialsCacheImpl sut;

    @Test
    public void should_invoke_put_elasticCredentialsCacheMethod_when_put_cache_isCalled() {
        ClusterSettings clusterSettings = ClusterSettings.builder().build();
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            Object[] args = invocation.getArguments();
            String arg1 = (String) (args[0]);
            ClusterSettings arg2 = (ClusterSettings) (args[1]);
            methodCalled.set(true);
            return null;
        }).when(cache).put(key, clusterSettings);

        sut.put(key, clusterSettings);

        assertTrue(methodCalled.get());
    }

    @Test()
    public void put_withRedisException() {
        ClusterSettings clusterSettings = ClusterSettings.builder().build();
        doThrow(RedisException.class).when(cache).put(key, clusterSettings);

        try {
            sut.put(key, clusterSettings);
        } catch (RedisException e) {
            assertThatExceptionOfType(RedisException.class);
        }
    }

    @Test
    public void should_invoke_get_elasticCredentialsCacheMethod_when_get_cache_isCalled() {
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<ClusterSettings>) invocation -> {
            Object[] args = invocation.getArguments();
            String arg1 = (String) (args[0]);
            methodCalled.set(true);
            ClusterSettings clusterSettings = new ClusterSettings(host, port, userNameandPassword);
            return clusterSettings;
        }).when(cache).get(key);

        ClusterSettings clusterSettings = sut.get(key);

        assertEquals(host, clusterSettings.getHost());
        assertEquals(port, clusterSettings.getPort());
        assertEquals(userNameandPassword, clusterSettings.getUserNameAndPassword());
        assertTrue(methodCalled.get());
    }

    @Test()
    public void should_returnNull_when_get_isCalled_withRedisException() {
        doThrow(RedisException.class).when(cache).get(key);

        ClusterSettings clusterSettings = sut.get(key);

        assertNull(clusterSettings);
    }


    @Test
    public void should_invoke_delete_elasticCredentialsCacheMethod_when_delete_cache_isCalled() {
        ClusterSettings cursorSettings = ClusterSettings.builder().build();
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            Object[] args = invocation.getArguments();
            String arg1 = (String) (args[0]);
            methodCalled.set(true);
            return null;
        }).when(cache).delete(key);

        sut.delete(key);

        assertTrue(methodCalled.get());
    }

    @Test
    public void should_invoke_clearAll_elasticCredentialsCacheMethod_when_clearAll_cache_isCalled() {
        ClusterSettings cursorSettings = ClusterSettings.builder().build();
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            methodCalled.set(true);
            return null;
        }).when(cache).clearAll();

        sut.clearAll();

        assertTrue(methodCalled.get());
    }
}
