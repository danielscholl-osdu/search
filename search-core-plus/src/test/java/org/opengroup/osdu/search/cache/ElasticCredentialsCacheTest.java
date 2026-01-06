/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.cache;

import io.lettuce.core.RedisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ElasticCredentialsCacheTest {

    private static final String KEY1 = "key1";
    private static final String KEYX = "keyX";
    private static final String BAD_KEY = "bad-key";
    private static final String DEL_KEY = "del-key";
    private static final String LOCALHOST = "localhost";
    private static final int PORT = 9200;
    private static final String DUMMY_CREDENTIALS = "test-user:test-pass";

    private ICache<String, ClusterSettings> cacheMock;
    private ElasticCredentialsCache cache;
    private ClusterSettings testClusterSettings;

    @BeforeEach
    void setUp() {
        cacheMock = mock(ICache.class);
        cache = new ElasticCredentialsCache(cacheMock);
        testClusterSettings = new ClusterSettings(LOCALHOST, PORT, DUMMY_CREDENTIALS);
    }

    @Test
    void put_delegatesToRedisCache() {
        cache.put(KEY1, testClusterSettings);
        verify(cacheMock, times(1)).put(KEY1, testClusterSettings);
    }

    @Test
    void get_returnsValue_whenRedisCacheReturnsNormally() {
        when(cacheMock.get(KEYX)).thenReturn(testClusterSettings);

        ClusterSettings result = cache.get(KEYX);

        assertSame(testClusterSettings, result);
        verify(cacheMock, times(1)).get(KEYX);
        verify(cacheMock, never()).delete(anyString());
    }

    @Test
    void get_returnsNullAndDeletesKey_whenRedisExceptionThrown() {
        when(cacheMock.get(BAD_KEY)).thenThrow(new RedisException("Redis down"));

        ClusterSettings result = cache.get(BAD_KEY);

        assertNull(result);
        verify(cacheMock).get(BAD_KEY);
        verify(cacheMock).delete(BAD_KEY);
    }

    @Test
    void delete_delegatesToRedisCache() {
        cache.delete(DEL_KEY);
        verify(cacheMock, times(1)).delete(DEL_KEY);
    }

    @Test
    void clearAll_delegatesToRedisCache() {
        cache.clearAll();
        verify(cacheMock, times(1)).clearAll();
    }

    @Test
    void close_delegatesToUnderlyingCacheIfAutoCloseable() throws Exception {
        AutoCloseable realCache = mock(AutoCloseable.class, withSettings().extraInterfaces(ICache.class));
        ElasticCredentialsCache credentialsCache = new ElasticCredentialsCache((ICache<String, ClusterSettings>) realCache);
        credentialsCache.close();
        verify(realCache, times(1)).close();
    }
}
