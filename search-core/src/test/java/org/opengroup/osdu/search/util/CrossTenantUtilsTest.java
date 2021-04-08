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


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CrossTenantUtilsTest {

    @Mock
    private QueryRequest queryRequest;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @InjectMocks
    private CrossTenantUtils sut;

    @Test
    public void should_returnIndexAsIs_when_searchedCrossKind_given_multipleAccountId() {
        String kind = "*:ihs:well:1.0.0";
        when(queryRequest.getKind()).thenReturn(kind);
        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn("*-ihs-well-1.0.0");

        assertEquals("*-ihs-well-1.0.0,-.*", this.sut.getIndexName(queryRequest));
    }
}
