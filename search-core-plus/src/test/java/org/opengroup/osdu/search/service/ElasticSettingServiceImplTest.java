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

package org.opengroup.osdu.search.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Map;
import jakarta.inject.Provider;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;

@ExtendWith(MockitoExtension.class)
public class ElasticSettingServiceImplTest {

    private static final int DEFAULT_PORT = 9200;
    private static final String DUMMY_CREDENTIALS = "test-user:test-pass";

    private SearchConfigurationProperties props;
    private Provider<ITenantInfoService> tenantProvider;
    private ITenantInfoService tenantService;
    private IElasticRepository repo;
    private IElasticCredentialsCache<String, ClusterSettings> cache;
    private JaxRsDpsLog log;

    @BeforeEach
    void setUp() {
        props = mock(SearchConfigurationProperties.class);
        tenantProvider = mock(Provider.class);
        tenantService = mock(ITenantInfoService.class);
        when(tenantProvider.get()).thenReturn(tenantService);

        repo = mock(IElasticRepository.class);
        cache = mock(IElasticCredentialsCache.class);
        log = mock(JaxRsDpsLog.class);
    }

    private ClusterSettings makeClusterSettings(String host) {
        return new ClusterSettings(host, DEFAULT_PORT, DUMMY_CREDENTIALS);
    }

    @Test
    void whenCacheHit_getElasticClusterInformation_returnsCachedValue_andDoesNotCallRepo() {
        when(props.getDeployedServiceId()).thenReturn("svc");

        TenantInfo tenant = mock(TenantInfo.class);
        when(tenantService.getTenantInfo()).thenReturn(tenant);
        when(tenant.getName()).thenReturn("tenantName");

        String cacheKey = "svc-" + "tenantName";
        ClusterSettings cached = makeClusterSettings("cachedUrl");
        when(cache.get(cacheKey)).thenReturn(cached);

        ElasticSettingServiceImpl service = new ElasticSettingServiceImpl(
                props, tenantProvider, repo, cache, log);
        ClusterSettings result = service.getElasticClusterInformation();

        assertSame(cached, result);
        verify(cache, times(1)).get(cacheKey);
        verify(repo, never()).getElasticClusterSettings(any());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void whenCacheMiss_repoReturnsCluster_putsIntoCache_andReturnsIt() {

        when(props.getDeployedServiceId()).thenReturn("svc");

        TenantInfo tenant = mock(TenantInfo.class);
        when(tenantService.getTenantInfo()).thenReturn(tenant);
        when(tenant.getName()).thenReturn("tenantA");

        String cacheKey = "svc-" + "tenantA";
        when(cache.get(cacheKey)).thenReturn(null);

        ClusterSettings repoCluster = makeClusterSettings("repoUrl");
        when(repo.getElasticClusterSettings(tenant)).thenReturn(repoCluster);

        ElasticSettingServiceImpl service = new ElasticSettingServiceImpl(
                props, tenantProvider, repo, cache, log);
        ClusterSettings result = service.getElasticClusterInformation();

        assertSame(repoCluster, result);
        verify(cache).get(cacheKey);
        verify(repo).getElasticClusterSettings(tenant);
        verify(cache).put(cacheKey, repoCluster);
    }

    @Test
    void whenCacheMiss_repoReturnsNull_throwsAppExceptionNotFound() {

        when(props.getDeployedServiceId()).thenReturn("svc");

        TenantInfo tenant = mock(TenantInfo.class);
        when(tenantService.getTenantInfo()).thenReturn(tenant);
        when(tenant.getName()).thenReturn("tenantX");

        String cacheKey = "svc-" + "tenantX";
        when(cache.get(cacheKey)).thenReturn(null);
        when(repo.getElasticClusterSettings(tenant)).thenReturn(null);

        ElasticSettingServiceImpl service = new ElasticSettingServiceImpl(
                props, tenantProvider, repo, cache, log);

        AppException ex = assertThrows(AppException.class, service::getElasticClusterInformation);
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getError().getCode());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void getAllClustersSettings_buildsMapFromAllTenantInfos() {

        when(props.getDeployedServiceId()).thenReturn("svc");

        TenantInfo t1 = mock(TenantInfo.class);
        when(t1.getName()).thenReturn("name1");
        when(t1.getDataPartitionId()).thenReturn("dp1");

        TenantInfo t2 = mock(TenantInfo.class);
        when(t2.getName()).thenReturn("name2");
        when(t2.getDataPartitionId()).thenReturn("dp2");

        when(tenantService.getAllTenantInfos()).thenReturn(Arrays.asList(t1, t2));

        when(cache.get("svc-name1")).thenReturn(null);
        when(cache.get("svc-name2")).thenReturn(null);

        ClusterSettings cs1 = makeClusterSettings("url1");
        ClusterSettings cs2 = makeClusterSettings("url2");
        when(repo.getElasticClusterSettings(t1)).thenReturn(cs1);
        when(repo.getElasticClusterSettings(t2)).thenReturn(cs2);

        ElasticSettingServiceImpl service = new ElasticSettingServiceImpl(
                props, tenantProvider, repo, cache, log);

        Map<String, ClusterSettings> result = service.getAllClustersSettings();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(cs1, result.get("dp1"));
        assertSame(cs2, result.get("dp2"));

        verify(cache).put("svc-name1", cs1);
        verify(cache).put("svc-name2", cs2);
    }
}
