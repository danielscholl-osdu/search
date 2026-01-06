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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.cache.RedisCacheBuilder;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.opengroup.osdu.search.config.CorePlusSearchConfigurationProperties;
import org.opengroup.osdu.search.model.SearchAfterSettings;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CacheConfigTest {

    @Mock
    RedisCacheBuilder<String, ClusterSettings> clusterSettingsCacheBuilder;

    @Mock
    RedisCacheBuilder<String, CursorSettings> cursorSettingsCacheBuilder;

    @Mock
    RedisCacheBuilder<String, SearchAfterSettings> searchAfterSettingsCacheBuilderBuilder;

    @Mock
    RedisCacheBuilder<String, Groups> groupsRedisCacheBuilder;

    @Mock
    RedisCacheBuilder<String, Map> fieldTypeMappingCacheBuilder;

    CorePlusSearchConfigurationProperties props;

    CacheConfig cacheConfig;

    @BeforeEach
    void setup() {

        props = mock(CorePlusSearchConfigurationProperties.class);
        Mockito.lenient().when(props.getRedisGroupHost()).thenReturn("g-host");
        Mockito.lenient().when(props.getRedisGroupPort()).thenReturn(1111);
        Mockito.lenient().when(props.getRedisGroupPassword()).thenReturn("g-pass");
        Mockito.lenient().when(props.getRedisGroupExpiration()).thenReturn(3600);
        Mockito.lenient().when(props.getRedisGroupWithSsl()).thenReturn(false);

        Mockito.lenient().when(props.getRedisSearchHost()).thenReturn("s-host");
        Mockito.lenient().when(props.getRedisSearchPort()).thenReturn("2222");
        Mockito.lenient().when(props.getRedisSearchPassword()).thenReturn("s-pass");
        Mockito.lenient().when(props.getRedisSearchExpiration()).thenReturn(7200);
        Mockito.lenient().when(props.getRedisSearchWithSsl()).thenReturn(false);

        cacheConfig = new CacheConfig(
                clusterSettingsCacheBuilder,
                cursorSettingsCacheBuilder,
                searchAfterSettingsCacheBuilderBuilder,
                groupsRedisCacheBuilder,
                fieldTypeMappingCacheBuilder
        );
    }

    @Test
    void groupCache_builds_usingGroupsBuilder_andReturnsICache() {
        RedisCache<String, Groups> mockedCache = mock(RedisCache.class);
        when(groupsRedisCacheBuilder.buildRedisCache(
                eq("g-host"), eq(1111), eq("g-pass"), eq(3600), eq(false), eq(String.class), eq(Groups.class)))
                .thenReturn(mockedCache);

        ICache<String, Groups> bean = cacheConfig.groupCache(props);

        assertNotNull(bean);
        assertSame(mockedCache, bean);
        verify(groupsRedisCacheBuilder).buildRedisCache(eq("g-host"), eq(1111), eq("g-pass"), eq(3600), eq(false),
                eq(String.class), eq(Groups.class));
    }

    @Test
    void fieldTypeMappingCache_builds_andWrapsRedisCache() {
        RedisCache<String, Map> redisCache = mock(RedisCache.class);
        when(fieldTypeMappingCacheBuilder.buildRedisCache(
                eq("s-host"), eq(2222), eq("s-pass"), eq(7200), eq(false), eq(String.class), eq(Map.class)))
                .thenReturn(redisCache);

        IFieldTypeMappingCache bean = cacheConfig.fieldTypeMappingCache(props);
        assertNotNull(bean);
        verify(fieldTypeMappingCacheBuilder).buildRedisCache(eq("s-host"), eq(2222), eq("s-pass"), eq(7200), eq(false),
                eq(String.class), eq(Map.class));
    }

    @Test
    void elasticCache_and_elasticCredentialsCache_returnCorrectBeans() {
        RedisCache<String, ClusterSettings> redisClusterCache = mock(RedisCache.class);
        when(clusterSettingsCacheBuilder.buildRedisCache(
                anyString(), anyInt(), anyString(), anyInt(), anyBoolean(), eq(String.class), eq(ClusterSettings.class)))
                .thenReturn(redisClusterCache);

        RedisCache<String, ClusterSettings> redisBean = cacheConfig.elasticCache(props);
        assertNotNull(redisBean);
        assertSame(redisClusterCache, redisBean);

        IElasticCredentialsCache<String, ClusterSettings> credentialsCache = cacheConfig.elasticCredentialsCache(redisBean);
        assertNotNull(credentialsCache);
    }

    @Test
    void cursorCache_builds_andReturnsCursorCacheImpl() {
        RedisCache<String, CursorSettings> redisCursorCache = mock(RedisCache.class);
        when(cursorSettingsCacheBuilder.buildRedisCache(
                anyString(), anyInt(), anyString(), anyInt(), anyBoolean(), eq(String.class), eq(CursorSettings.class)))
                .thenReturn(redisCursorCache);

        CursorCache cursorCacheBean = cacheConfig.cursorCache(props);
        assertNotNull(cursorCacheBean);

        verify(cursorSettingsCacheBuilder).buildRedisCache(eq("s-host"), eq(2222), eq("s-pass"), eq(7200), eq(false),
                eq(String.class), eq(CursorSettings.class));
    }

    @Test
    void searchAfterSettingsCache_builds_andReturns() {
        RedisCache<String, SearchAfterSettings> redisSearchAfter = mock(RedisCache.class);
        when(searchAfterSettingsCacheBuilderBuilder.buildRedisCache(
                anyString(), anyInt(), anyString(), anyInt(), anyBoolean(), eq(String.class), eq(SearchAfterSettings.class)))
                .thenReturn(redisSearchAfter);

        SearchAfterSettingsCache cacheBean = cacheConfig.searchAfterSettingsCache(props);
        assertNotNull(cacheBean);

        verify(searchAfterSettingsCacheBuilderBuilder).buildRedisCache(eq("s-host"), eq(2222), eq("s-pass"), eq(7200), eq(false),
                eq(String.class), eq(SearchAfterSettings.class));
    }

    @Test
    void partitionInfoCache_returnsVmCache() {
        ICache<String, PartitionInfo> partCache = cacheConfig.partitionInfoCache();
        assertNotNull(partCache);
    }
}
