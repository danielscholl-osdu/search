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

package org.opengroup.osdu.search.provider.gcp.provider.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class CrossTenantInfoServiceTest {

    @Mock
    private ITenantFactory tenantFactory;
    @Mock
    private DpsHeaders dpsHeaders;
    @InjectMocks
    private CrossTenantInfoServiceImpl sut;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void should_return_validTenantInfoList_given_validAccountIds() {
        when(this.dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1,common");
        TenantInfo tenant1 = new TenantInfo();
        tenant1.setName("tenant1");
        when(tenantFactory.getTenantInfo("tenant1")).thenReturn(tenant1);
        TenantInfo common = new TenantInfo();
        common.setName("common");
        when(tenantFactory.getTenantInfo("common")).thenReturn(common);
        List<TenantInfo> tenantInfoList=sut.getAllTenantsFromPartitionId();
        assertEquals(2,tenantInfoList.size());
    }
}