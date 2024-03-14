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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.provider.impl.CrossTenantInfoServiceImpl;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.models.Attribute;
import org.opengroup.osdu.search.smart.models.AttributeCollection;

@RunWith(MockitoJUnitRunner.class)
public class AttributeFilterTest {

  @InjectMocks
  private AttributeFilter sut;
  @Mock
  private Provider<ICrossTenantInfoService> tenantInfoServiceProvider;
  @Mock
  private AttributeCollection attributes;
  @Mock
  private Attribute attribute;

  @Before
  public void setup() throws IOException {
    Set<String> input = new HashSet<>();
    input.add("MCCOY");
    input.add("MILAGRO");
    input.add("MAI");
    input.add("APACHE");
    List<TenantInfo> tenantInfoList = new LinkedList<>();
    TenantInfo tenantInfo = new TenantInfo();
    tenantInfo.setName("tenant1");
    tenantInfo.setDataPartitionId("tenant1");
    tenantInfoList.add(tenantInfo);
    CrossTenantInfoServiceImpl tenantInfoService = mock(CrossTenantInfoServiceImpl.class);
    when(tenantInfoServiceProvider.get()).thenReturn(tenantInfoService);
    when(tenantInfoService.getAllTenantsFromPartitionId()).thenReturn(tenantInfoList);
    when(attributes.getAllAttributes("tenant1", "Field")).thenReturn(input);
    when(attribute.getName()).thenReturn("Field");
    when(attribute.getDescription()).thenReturn("Field Desc");
  }

  @Test
  public void should_returnTypesStartingWtihWe_when_queryisGiven() throws IOException {

    Map<String, String> result = sut.values("M", 5);
    assertEquals(3, result.size());
    assertEquals("Field", result.get("MCCOY"));
    assertEquals("Field", result.get("MILAGRO"));
    assertEquals("Field", result.get("MAI"));
  }

  @Test
  public void should_returnAttrName_as_name() {

    assertEquals("Field", sut.name());
  }

  @Test
  public void should_returnAttrDes_as_description() {

    assertEquals("Field Desc", sut.description());
  }
}
