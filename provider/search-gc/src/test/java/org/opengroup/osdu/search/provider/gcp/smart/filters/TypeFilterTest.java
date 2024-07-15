// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.search.provider.gcp.smart.filters;

import org.opengroup.osdu.search.provider.interfaces.ICrossTenantInfoService;
import org.opengroup.osdu.search.smart.filters.TypeFilter;
import org.opengroup.osdu.search.provider.gcp.provider.impl.CrossTenantInfoServiceImpl;
import org.opengroup.osdu.search.smart.models.Kinds;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.mockito.junit.MockitoJUnitRunner;


import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TypeFilterTest {

    @Mock
    jakarta.inject.Provider<ICrossTenantInfoService> tenantInfoServiceProvide;

    @Mock
    private Kinds kindRetriever;
    @InjectMocks
    private TypeFilter sut;

    @Test
    public void should_returnType_as_name(){
        assertEquals("type", sut.name() );
    }

    @Test
    public void should_returnTypesStartingWtihWe_when_queryisGivenIgnoringCase() throws IOException {
        Set<String> input = new HashSet<>();
        input.add("a:b:slice:1");
        input.add("c:d:well:1");
        input.add("c:a:Well:1");
        input.add("c:a:wEll:1");
        input.add("d:3:las:2");
        input.add("d:t:well2:7");

        setupMock(input);
        Map<String, String> result = sut.values("we", 5);

        assertEquals(2, result.size());
        assertEquals("type", result.get("well"));
        assertEquals("type", result.get("well2"));
    }

    @Test
    public void should_return1MatchOnly_when_limitIs1() throws IOException {
        Set<String> input = new HashSet<>();
        input.add("a:b:slice:1");
        input.add("c:d:well:1");
        input.add("d:3:las:2");
        input.add("d:t:well2:7");
        input.add("d:t:well3:7");
        input.add("d:t:well4:7");

        setupMock(input);
        Map<String, String> result = sut.values("we", 1);

        assertEquals(1, result.size());
    }

    @Test
    public void should_returnNoMatches_when_formatOfKindIsUnexpected() throws IOException {
        Set<String> input = new HashSet<>();
        input.add("a:b:s:1");
        input.add("c:dell:1");
        input.add("d:s:2");
        input.add("d:well2:7");
        setupMock(input);
        Map<String, String> result = sut.values("we", 1);

        assertEquals(0, result.size());
    }

    private void setupMock(Set<String> input)throws IOException {
        List<TenantInfo> tenantInfoList=new LinkedList<>();
        TenantInfo tenantInfo=new TenantInfo();
        tenantInfo.setName("tenant1");
        tenantInfoList.add(tenantInfo);
        CrossTenantInfoServiceImpl tenantInfoService=mock(CrossTenantInfoServiceImpl.class);
        when(tenantInfoServiceProvide.get()).thenReturn(tenantInfoService);
        when(tenantInfoService.getAllTenantsFromPartitionId()).thenReturn(tenantInfoList);
        when(kindRetriever.all("tenant1")).thenReturn(input);
    }
}
