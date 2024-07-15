// Copyright © Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.provider.azure.cache.impl;

import com.lambdaworks.redis.RedisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.Resource;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class IndexAliasCacheTest {
    private static final String KEY = "key";
    private static final String VALUE = "value";
    @Mock
    @Resource(name = "aliasCache")
    private ICache<String, String> cache;
    @Mock
    @Autowired
    private JaxRsDpsLog log;
    @InjectMocks
    public IndexAliasCacheImpl sut;

    @Test
    public void should_invoke_put_when_put_cache_isCalled() {
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            methodCalled.set(true);
            return null;
        }).when(cache).put(KEY, VALUE);

        sut.put(KEY, VALUE);

        assertTrue(methodCalled.get());
    }

    @Test()
    public void put_withRedisException() {
        doThrow(RedisException.class).when(cache).put(KEY, VALUE);
        try {
            sut.put(KEY, VALUE);
        } catch (RedisException e) {
            assertThatExceptionOfType(RedisException.class);
        }
    }

    @Test
    public void should_invoke_get_when_get_cache_isCalled() {
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<String>) invocation -> {
            methodCalled.set(true);
            return VALUE;
        }).when(cache).get(KEY);

        String value = sut.get(KEY);

        assertEquals(VALUE, value);
        assertTrue(methodCalled.get());
    }

    @Test()
    public void should_returnNull_when_get_isCalled_withRedisException() {
        doThrow(RedisException.class).when(cache).get(KEY);

        String value = sut.get(KEY);

        assertNull(value);
    }

    @Test
    public void should_invoke_delete_when_delete_cache_isCalled() {
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            methodCalled.set(true);
            return null;
        }).when(cache).delete(KEY);

        sut.delete(KEY);

        assertTrue(methodCalled.get());
    }

    @Test
    public void should_invoke_clearAll_when_clearAll_cache_isCalled() {
        AtomicReference<Boolean> methodCalled = new AtomicReference<>(false);
        doAnswer((Answer<Boolean>) invocation -> {
            methodCalled.set(true);
            return null;
        }).when(cache).clearAll();

        sut.clearAll();

        assertTrue(methodCalled.get());
    }
}
