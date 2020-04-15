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

package org.opengroup.osdu.search.util;


import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CrossTenantUtilsTest {

    @Mock
    private QueryRequest queryRequest;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private ITenantInfoService tenantInfoService;
    @Mock
    private javax.inject.Provider<ITenantInfoService> tenantInfoServiceProvider;
    @InjectMocks
    private CrossTenantUtils sut;

    @Before
    public void setup() {
        when(this.tenantInfoServiceProvider.get()).thenReturn(this.tenantInfoService);
    }

    @Test
    public void should_returnTenantPrefixedIndex_when_searchedCrossKind_given_tenantAccountId() {
        String crmAccountId = "slb-tenant1";

        when(queryRequest.getKind()).thenReturn("*:ihs:well:1.0.0");
        when(this.elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn("*-ihs-well-1.0.0");
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setName("tenant1");
        when(this.tenantInfoService.getTenantInfo()).thenReturn(tenantInfo);

        assertEquals("tenant1*-ihs-well-1.0.0,-.*", this.sut.getIndexName(queryRequest, crmAccountId));
    }

    @Test
    public void should_returnIndexAsIs_when_searchedCrossKind_given_multipleAccountId() {
        when(queryRequest.getKind()).thenReturn("*:ihs:well:1.0.0");
        when(this.elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn("*-ihs-well-1.0.0");

        assertEquals("*-ihs-well-1.0.0,-.*", this.sut.getIndexName(queryRequest, "tenant1,common"));
    }

    @Test
    public void should_returnIndexAsIs2_when_searchedCrossKind_given_multipleAccountId() {
        when(queryRequest.getKind()).thenReturn("tenant1:ihs:well:1.0.0");
        when(this.elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn("tenant1-ihs-well-1.0.0");

        assertEquals("tenant1-ihs-well-1.0.0,-.*", this.sut.getIndexName(queryRequest, "tenant1,common"));
    }

    @Test
    public void should_returnIndexAsIs_when_searchedCrossKind_given_oneAccountId() {
        when(queryRequest.getKind()).thenReturn("tenant1:ihs:well:1.0.0");
        when(this.elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn("tenant1-ihs-well-1.0.0");

        assertEquals("tenant1-ihs-well-1.0.0,-.*", this.sut.getIndexName(queryRequest, "tenant1"));
    }
}
