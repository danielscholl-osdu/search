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
import org.opengroup.osdu.search.smart.models.Attribute;
import org.opengroup.osdu.search.smart.models.AttributeCollection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AttributeFilterTest {

    @Test
    void values_mergesAcrossTenants_filtersByPrefix_caseInsensitive_andAppliesPerTenantLimit() throws IOException {
        Attribute attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn("country");

        TenantInfo t1 = mock(TenantInfo.class);
        when(t1.getDataPartitionId()).thenReturn("tenant-a");
        TenantInfo t2 = mock(TenantInfo.class);
        when(t2.getDataPartitionId()).thenReturn("tenant-b");

        ICrossTenantInfoService tenantSvc = mock(ICrossTenantInfoService.class);
        when(tenantSvc.getAllTenantsFromPartitionId()).thenReturn(List.of(t1, t2));

        @SuppressWarnings("unchecked")
        Provider<ICrossTenantInfoService> provider = (Provider<ICrossTenantInfoService>) mock(Provider.class);
        when(provider.get()).thenReturn(tenantSvc);

        AttributeCollection attributes = mock(AttributeCollection.class);
        when(attributes.getAllAttributes("tenant-a", "country"))
                .thenReturn(new LinkedHashSet<>(List.of("Alpha", "beta", "Apple", "ALBANIA")));
        when(attributes.getAllAttributes("tenant-b", "country"))
                .thenReturn(new LinkedHashSet<>(List.of("Apricot", "beta", "Algae", "australia")));

        AttributeFilter filter = new AttributeFilter(attribute, attributes, provider);

        Map<String, String> result = filter.values("a", 2);

        assertEquals(4, result.size());
        assertEquals("country", result.get("Alpha"));
        assertEquals("country", result.get("Apple"));
        assertEquals("country", result.get("Apricot"));
        assertEquals("country", result.get("Algae"));
    }

    @Test
    void values_returnsEmptyMap_whenNoTenantsOrNoAttributes() throws IOException {
        Attribute attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn("asset");

        AttributeCollection attributes = mock(AttributeCollection.class);

        ICrossTenantInfoService tenantSvc = mock(ICrossTenantInfoService.class);
        when(tenantSvc.getAllTenantsFromPartitionId()).thenReturn(Collections.emptyList());

        @SuppressWarnings("unchecked")
        Provider<ICrossTenantInfoService> provider = (Provider<ICrossTenantInfoService>) mock(Provider.class);
        when(provider.get()).thenReturn(tenantSvc);

        AttributeFilter filter = new AttributeFilter(attribute, attributes, provider);

        Map<String, String> result = filter.values("x", 5);

        assertTrue(result.isEmpty());
    }
}
