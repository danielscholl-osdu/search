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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.provider.impl.CrossTenantInfoServiceImpl;
import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.models.Kinds;

@RunWith(MockitoJUnitRunner.class)
public class SourceFilterTest {

  @Mock
  javax.inject.Provider<ICrossTenantInfoService> tenantInfoServiceProvide;
  @Mock
  private Kinds kindRetriever;
  @InjectMocks
  private SourceFilter sut;

  @Test
  public void should_returnType_as_name() {
    assertEquals("source", sut.name());
  }

  @Test
  public void should_returnTypesStartingWtihWe_when_queryisGiven() throws IOException {
    Set<String> input = new HashSet<>();
    input.add("a:npd:b:1");
    input.add("c:ihs:d:1");
    input.add("d:ihs2:b:2");
    input.add("d:slb:a:7");

    setupMock(input);
    Map<String, String> result = sut.values("i", 5);

    assertEquals(2, result.size());
    assertEquals("source", result.get("ihs"));
    assertEquals("source", result.get("ihs2"));
  }

  @Test
  public void should_return1MatchOnly_when_limitIs1() throws IOException {
    Set<String> input = new HashSet<>();
    input.add("a:npd:b:1");
    input.add("c:ihs:d:1");
    input.add("d:ihs2:b:2");
    input.add("d:slb:a:7");

    setupMock(input);
    Map<String, String> result = sut.values("i", 1);

    assertEquals(1, result.size());
  }

  @Test
  public void should_returnNoMatches_when_formatOfKindIsUnexpected() throws IOException {
    Set<String> input = new HashSet<>();
    input.add("a:ib:s");
    input.add("c:idell:1");
    input.add("d:is:2");
    input.add("d:well2:7");
    setupMock(input);
    Map<String, String> result = sut.values("i", 1);

    assertEquals(0, result.size());
  }

  private void setupMock(Set<String> input) throws IOException {
    List<TenantInfo> tenantInfoList = new LinkedList<>();
    TenantInfo tenantInfo = new TenantInfo();
    tenantInfo.setName("tenant1");
    tenantInfoList.add(tenantInfo);
    CrossTenantInfoServiceImpl tenantInfoService = mock(CrossTenantInfoServiceImpl.class);
    when(tenantInfoServiceProvide.get()).thenReturn(tenantInfoService);
    when(tenantInfoService.getAllTenantsFromPartitionId()).thenReturn(tenantInfoList);
    when(kindRetriever.all("tenant1")).thenReturn(input);
  }
}
