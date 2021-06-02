// Copyright © Schlumberger
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

package org.opengroup.osdu.search.query.builder;

import junit.framework.TestCase;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.search.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SortQueryBuilderTest extends TestCase {

    @Mock
    private IFieldMappingTypeService fieldMappingTypeService;
    @InjectMocks
    private SortQueryBuilder sut;

    @Test
    public void should_return_validSortQuery_given_sortFields() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);

        SortQuery sort = new SortQuery();
        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        sortFields.add("namespace");
        sort.setField(sortFields);
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(SortOrder.ASC);
        sortOrders.add(SortOrder.DESC);
        sort.setOrder(sortOrders);

        List<FieldSortBuilder> sortQuery = this.sut.getSortQuery(restClient, sort, "osdu:wks:work-product-component--wellboremarkerset:1.0.0");
        assertNotNull(sortQuery);
        assertEquals(2, sortQuery.size());
    }

    @Test
    public void should_return_validSortQuery_given_dataSortFields() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);

        SortQuery sort = new SortQuery();
        List<String> sortFields = new ArrayList<>();
        sortFields.add("data.Country");
        sortFields.add("data.ProductionRate");
        sort.setField(sortFields);
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(SortOrder.ASC);
        sortOrders.add(SortOrder.DESC);
        sort.setOrder(sortOrders);

        Map<String, String> keywordMap = new HashMap<>();
        keywordMap.put("data.Country", "data.Country.keyword");
        when(this.fieldMappingTypeService.getSortableTextFields(any(), any(), any())).thenReturn(keywordMap);

        List<FieldSortBuilder> sortQuery = this.sut.getSortQuery(restClient, sort, "osdu:wks:work-product-component--wellboremarkerset:1.0.0");
        assertNotNull(sortQuery);
        assertEquals(2, sortQuery.size());
        assertEquals("data.Country.keyword", sortQuery.get(0).getFieldName());
        assertEquals("data.ProductionRate", sortQuery.get(1).getFieldName());
    }
}