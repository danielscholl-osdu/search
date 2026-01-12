// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.search.smart.filters;

import java.io.IOException;
import java.util.*;

import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.models.Kinds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class SourceFilterTest {

    @Test
    void values_mergesAcrossTenants_filtersByPrefix_caseInsensitive_andAppliesLimit() throws IOException {
        TenantInfo t1 = mock(TenantInfo.class);
        when(t1.getName()).thenReturn("tenant-a");
        TenantInfo t2 = mock(TenantInfo.class);
        when(t2.getName()).thenReturn("tenant-b");

        ICrossTenantInfoService tenantService = mock(ICrossTenantInfoService.class);
        when(tenantService.getAllTenantsFromPartitionId()).thenReturn(List.of(t1, t2));

        @SuppressWarnings("unchecked")
        Provider<ICrossTenantInfoService> provider = (Provider<ICrossTenantInfoService>) mock(Provider.class);
        when(provider.get()).thenReturn(tenantService);

        Kinds kinds = mock(Kinds.class);
        when(kinds.all("tenant-a"))
                .thenReturn(Set.of("osdu:ABC:dataset:1", "osdu:Abacus:dataset:2", "osdu:Beta:dataset:1"));
        when(kinds.all("tenant-b"))
                .thenReturn(Set.of("osdu:alpha:dataset:3", "osdu:Gamma:dataset:4", "invalidstring"));

        SourceFilter filter = new SourceFilter(kinds, provider);

        Map<String, String> result = filter.values("a", 2);

        assertTrue(result.keySet().stream().allMatch(k -> k.startsWith("a")));
        assertTrue(result.values().stream().allMatch(v -> v.equals("source")));
        assertTrue(result.containsKey("abc"));
        assertTrue(result.containsKey("abacus"));
        assertTrue(result.containsKey("alpha"));
    }

    @Test
    void values_returnsEmptyMap_whenNoTenantsOrNoKinds() throws IOException {
        Kinds kinds = mock(Kinds.class);
        ICrossTenantInfoService tenantService = mock(ICrossTenantInfoService.class);
        when(tenantService.getAllTenantsFromPartitionId()).thenReturn(Collections.emptyList());

        @SuppressWarnings("unchecked")
        Provider<ICrossTenantInfoService> provider = (Provider<ICrossTenantInfoService>) mock(Provider.class);
        when(provider.get()).thenReturn(tenantService);

        SourceFilter filter = new SourceFilter(kinds, provider);

        Map<String, String> result = filter.values("anything", 5);

        assertTrue(result.isEmpty());
        verify(kinds, never()).all(anyString());
    }

    @Test
    void values_skipsInvalidKindStrings_gracefully() throws IOException {
        TenantInfo tenant = mock(TenantInfo.class);
        when(tenant.getName()).thenReturn("tenant-x");

        ICrossTenantInfoService tenantService = mock(ICrossTenantInfoService.class);
        when(tenantService.getAllTenantsFromPartitionId()).thenReturn(List.of(tenant));

        @SuppressWarnings("unchecked")
        Provider<ICrossTenantInfoService> provider = (Provider<ICrossTenantInfoService>) mock(Provider.class);
        when(provider.get()).thenReturn(tenantService);

        Kinds kinds = mock(Kinds.class);
        when(kinds.all("tenant-x")).thenReturn(Set.of("badformat", "osdu:Proper:dataset:1"));

        SourceFilter filter = new SourceFilter(kinds, provider);

        Map<String, String> result = filter.values("p", 5);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("proper"));
        assertEquals("source", result.get("proper"));
    }
}
