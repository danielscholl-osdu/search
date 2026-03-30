/*
 *  Copyright 2026 Microsoft Corporation
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.search.model.SearchAfterSettings;

@ExtendWith(MockitoExtension.class)
class SearchAfterSettingsCacheImplTest {

    @Mock
    private RedisCache<String, SearchAfterSettings> redisCache;

    private SearchAfterSettingsCacheImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SearchAfterSettingsCacheImpl(redisCache);
    }

    @Test
    void put_delegatesToRedisCache() {
        SearchAfterSettings settings = mock(SearchAfterSettings.class);
        sut.put("key1", settings);
        verify(redisCache).put("key1", settings);
    }

    @Test
    void get_delegatesToRedisCache() {
        SearchAfterSettings expected = mock(SearchAfterSettings.class);
        when(redisCache.get("key1")).thenReturn(expected);

        SearchAfterSettings result = sut.get("key1");

        assertSame(expected, result);
        verify(redisCache).get("key1");
    }

    @Test
    void get_returnsNull_whenKeyNotFound() {
        assertNull(sut.get("nonexistent"));
        verify(redisCache).get("nonexistent");
    }

    @Test
    void delete_delegatesToRedisCache() {
        sut.delete("key1");
        verify(redisCache).delete("key1");
    }

    @Test
    void clearAll_delegatesToRedisCache() {
        sut.clearAll();
        verify(redisCache).clearAll();
    }
}
