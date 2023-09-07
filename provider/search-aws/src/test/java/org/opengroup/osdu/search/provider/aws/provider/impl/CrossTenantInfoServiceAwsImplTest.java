// Copyright © Amazon Web Services
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

package org.opengroup.osdu.search.provider.aws.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;

@RunWith(MockitoJUnitRunner.class)
public class CrossTenantInfoServiceAwsImplTest {

    @InjectMocks
	CrossTenantInfoServiceAwsImpl crossTenantInfoServiceAws;

    @Mock
    private ITenantFactory tenantFactory;

    @Mock
    private DpsHeaders headers;

    @Before
	public void setup() {
		MockitoAnnotations.openMocks(this);

		when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn("id");
	}
    
    
    @Test(expected = AppException.class)
    public void should_Throw_AppException_when_Null_tenantInfo(){
        this.crossTenantInfoServiceAws.getTenantInfo();
    }

    @Test
    public void should_Return_TenantInfo(){

        TenantInfo info = new TenantInfo();
        when(tenantFactory.getTenantInfo(anyString())).thenReturn(info);

        assertEquals(this.crossTenantInfoServiceAws.getTenantInfo(), info);
    }

    @Test
    public void should_Return_correct_TenantInfos(){

        when(tenantFactory.listTenantInfo()).thenReturn(new ArrayList<TenantInfo>());

        assertEquals((this.crossTenantInfoServiceAws.getAllTenantInfos()).size(), 0);
    }

    @Test
    public void should_Return_All_TenantInfos_FromPartition(){

        TenantInfo info = new TenantInfo();
        when(tenantFactory.getTenantInfo(anyString())).thenReturn(info);

        assertEquals((this.crossTenantInfoServiceAws.getAllTenantsFromPartitionId()).get(0), info);
    }
}
