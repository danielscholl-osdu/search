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

import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.attributes.AttributeLoader;
import org.opengroup.osdu.search.smart.models.Attribute;
import org.opengroup.osdu.search.smart.models.AttributeCollection;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class AllFiltersTest {

    @Test
    void constructor_registersInjectedAndAttributeFilters() {
        IFilter f1 = mock(IFilter.class);
        IFilter f2 = mock(IFilter.class);
        when(f1.name()).thenReturn("city");
        when(f2.name()).thenReturn("country");

        Attribute a1 = mock(Attribute.class);
        Attribute a2 = mock(Attribute.class);
        when(a1.getName()).thenReturn("alpha");
        when(a2.getName()).thenReturn("beta");

        Provider<ICrossTenantInfoService> provider = mock(Provider.class);
        AttributeCollection attrCollection = mock(AttributeCollection.class);

        try (MockedStatic<AttributeLoader> loader = mockStatic(AttributeLoader.class)) {
            loader.when(AttributeLoader::getAttributes).thenReturn(List.of(a1, a2));

            AllFilters all = new AllFilters(Set.of(f1, f2), provider, attrCollection);

            assertNotNull(all.getFilter("city"));
            assertNotNull(all.getFilter("country"));
            assertNotNull(all.getFilter("alpha"));
            assertNotNull(all.getFilter("beta"));
            assertEquals(4, all.list().size());
        }
    }

    @Test
    void values_callsOnlyNamedFilters_andPassesLimit8() throws IOException {
        IFilter f1 = mock(IFilter.class);
        IFilter f2 = mock(IFilter.class);
        when(f1.name()).thenReturn("f1");
        when(f2.name()).thenReturn("f2");

        Map<String, String> f1Result = Map.of("k1", "v1");
        when(f1.values(eq("q"), eq(8))).thenReturn(f1Result);

        Provider<ICrossTenantInfoService> provider = mock(Provider.class);
        AttributeCollection attrCollection = mock(AttributeCollection.class);

        try (MockedStatic<AttributeLoader> loader = mockStatic(AttributeLoader.class)) {
            loader.when(AttributeLoader::getAttributes).thenReturn(Collections.emptyList());

            AllFilters all = new AllFilters(Set.of(f1, f2), provider, attrCollection);

            Map<String, String> out = all.values("q", new HashSet<>(Set.of("f1")));

            assertEquals(f1Result, out);
            verify(f1).values("q", 8);
            verify(f2, never()).values(anyString(), anyInt());
        }
    }

    @Test
    void values_callsAllWhenFilterNamesNull() throws IOException {
        IFilter f1 = mock(IFilter.class);
        IFilter f2 = mock(IFilter.class);
        when(f1.name()).thenReturn("A");
        when(f2.name()).thenReturn("B");

        when(f1.values(anyString(), eq(8))).thenReturn(Map.of("a", "1"));
        when(f2.values(anyString(), eq(8))).thenReturn(Map.of("b", "2"));

        Provider<ICrossTenantInfoService> provider = mock(Provider.class);
        AttributeCollection attrCollection = mock(AttributeCollection.class);

        try (MockedStatic<AttributeLoader> loader = mockStatic(AttributeLoader.class)) {
            loader.when(AttributeLoader::getAttributes).thenReturn(Collections.emptyList());

            AllFilters all = new AllFilters(Set.of(f1, f2), provider, attrCollection);

            Map<String, String> out = all.values("any", null);

            assertEquals(2, out.size());
            assertEquals("1", out.get("a"));
            assertEquals("2", out.get("b"));
            verify(f1).values("any", 8);
            verify(f2).values("any", 8);
        }
    }

    @Test
    void getFilter_returnsNullForUnknown() {
        IFilter f = mock(IFilter.class);
        when(f.name()).thenReturn("known");

        Provider<ICrossTenantInfoService> provider = mock(Provider.class);
        AttributeCollection attrCollection = mock(AttributeCollection.class);

        try (MockedStatic<AttributeLoader> loader = mockStatic(AttributeLoader.class)) {
            loader.when(AttributeLoader::getAttributes).thenReturn(Collections.emptyList());

            AllFilters all = new AllFilters(Set.of(f), provider, attrCollection);

            assertNull(all.getFilter("unknown"));
            assertNotNull(all.getFilter("known"));
        }
    }
}
