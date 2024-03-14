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

package org.opengroup.osdu.search.smart.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.Ordering;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Provider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.attributes.AttributeLoader;
import org.opengroup.osdu.search.smart.attributes.AttributesRepository;
import org.opengroup.osdu.search.smart.models.Attribute;
import org.opengroup.osdu.search.smart.models.AttributeCollection;

@RunWith(MockitoJUnitRunner.class)
public class AllFiltersTest {

  private static MockedStatic<AttributeLoader> mockedSettings;

  @Mock
  private AttributesRepository attributesRepository;
  @Mock
  private Provider<ICrossTenantInfoService> tenantInfoServiceProvider;
  @Mock
  private AttributeCollection attributes;

  private AllFilters sut;

  @Before
  public void setup() {
    List<Attribute> listAttributes = new ArrayList<>();
    mockedSettings = mockStatic(AttributeLoader.class);
    when(AttributeLoader.getAttributes()).thenReturn(listAttributes);
  }

  @After
  public void close() {
    mockedSettings.close();
  }

  @Test
  public void should_returnFilter_when_givenName() throws IOException {
    sut = createSut();
    assertEquals("a", sut.getFilter("a").name());
  }

  @Test
  public void should_returnNull_when_givenNonExistantName() throws IOException {
    sut = createSut();
    assertNull(sut.getFilter("meh"));
  }

  @Test
  public void should_returnItemsPerGivenFilter_when_namesProvided() throws IOException {
    sut = createSut();

    Map<String, String> result = sut.values("a", Collections.singleton("a"));

    assertEquals(2, result.size());
    assertEquals("typeA", result.get("a1"));
    assertEquals("typeA", result.get("a2"));
  }

  @Test
  public void should_returnItemsForEveryFilter_when_noNamesProvided() throws IOException {
    sut = createSut();

    Map<String, String> result = sut.values("a", Collections.emptySet());

    assertEquals(5, result.size());
    assertEquals("typeA", result.get("a1"));
    assertEquals("typeA", result.get("a2"));
    assertEquals("typeB", result.get("b1"));
    assertEquals("typeB", result.get("b2"));
  }

  @Test
  public void should_returnItemsAlphabetically() throws IOException {
    sut = createSut();

    Map<String, String> result = sut.values("a", Collections.emptySet());

    assertTrue("Keys are not ordered alphabeitcally", Ordering.natural().isOrdered(result.keySet()));
  }

  private AllFilters createSut() throws IOException {
    Map<String, String> a = new HashMap<>();
    a.put("a2", "typeA");
    a.put("a1", "typeA");
    IFilter mockA = mock(IFilter.class);
    when(mockA.name()).thenReturn("a");
    when(mockA.values(any(), anyInt())).thenReturn(a);

    Map<String, String> b = new HashMap<>();
    b.put("b2", "typeB");
    b.put("b1", "typeB");
    b.put("a6", "typeB");
    IFilter mockB = mock(IFilter.class);
    when(mockB.values(any(), anyInt())).thenReturn(b);
    when(mockB.name()).thenReturn("b");

    Set<IFilter> filters = new HashSet<>();
    filters.add(mockA);
    filters.add(mockB);
    return new AllFilters(filters, tenantInfoServiceProvider, attributes);
  }

}
