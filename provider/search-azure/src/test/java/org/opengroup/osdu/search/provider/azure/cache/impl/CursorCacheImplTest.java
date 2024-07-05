package org.opengroup.osdu.search.provider.azure.cache.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.search.CursorSettings;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
public class CursorCacheImplTest {

    private static final String key = "key";
    private static final String cursor = "cursor";
    private static final String userId = "userId";

    @Mock
    @Resource(name = "cursorCache")
    private ICache<String, CursorSettings> cache;

    @InjectMocks
    public CursorCacheImpl sut;

    @Test
    public void should_invoke_put_cursorCacheMethod_when_put_cache_isCalled() {
        CursorSettings cursorSettings = CursorSettings.builder().build();
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            Object[] args = invocation.getArguments();
            String arg1 = (String) (args[0]);
            CursorSettings cs = (CursorSettings) (args[1]);
            methodCalled.set(true);
            return null;
        }).when(cache).put(key, cursorSettings);

        sut.put(key, cursorSettings);

        assertTrue(methodCalled.get());
    }

    @Test
    public void should_invokeAndReturnSameCursorSettingValue_get_cursorCacheMethod_when_get_cache_isCalled() {
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<CursorSettings>) invocation -> {
            Object[] args = invocation.getArguments();
            String arg1 = (String) (args[0]);
            methodCalled.set(true);
            CursorSettings cursorSettings = new CursorSettings(cursor, userId);
            return cursorSettings;
        }).when(cache).get(key);

        CursorSettings cursorSettings = sut.get(key);

        assertEquals(cursor,cursorSettings.getCursor());
        assertTrue(methodCalled.get());
    }

    @Test
    public void should_invoke_delete_cursorCacheMethod_when_delete_cache_isCalled() {
        CursorSettings cursorSettings = CursorSettings.builder().build();
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
    public void should_invoke_clearAll_cursorCacheMethod_when_clearAll_cache_isCalled() {
        CursorSettings cursorSettings = CursorSettings.builder().build();
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            methodCalled.set(true);
            return null;
        }).when(cache).clearAll();

        sut.clearAll();

        assertTrue(methodCalled.get());
    }
}
