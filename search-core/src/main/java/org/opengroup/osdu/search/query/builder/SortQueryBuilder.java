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

import joptsimple.internal.Strings;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class SortQueryBuilder {

    @Autowired
    private IFieldMappingTypeService fieldMappingTypeService;

    public List<FieldSortBuilder> getSortQuery(RestHighLevelClient restClient, SortQuery sortQuery, String indexPattern) throws IOException {
        List<String> dataFields = new ArrayList<>();
        for (String field: sortQuery.getField()) {
            if(field.startsWith("data.")) dataFields.add(field + ".keyword");
        }

        if (dataFields.isEmpty()) {
            return getSortQuery(sortQuery);
        }

        Map<String, String> sortableFieldTypes = this.fieldMappingTypeService.getSortableTextFields(restClient, Strings.join(dataFields, ","), indexPattern);
        List<String> sortableFields = new LinkedList<>();
        sortQuery.getField().forEach(field -> {
            if (sortableFieldTypes.containsKey(field)) {
                sortableFields.add(sortableFieldTypes.get(field));
            } else {
                sortableFields.add(field);
            }
        });
        sortQuery.setField(sortableFields);
        return getSortQuery(sortQuery);
    }

    // sort: text is not suitable for sorting or aggregation, refer to: this: https://github.com/elastic/elasticsearch/issues/28638,
    // so keyword is recommended for unmappedType in general because it can handle both string and number.
    // It will ignore the characters longer than the threshold when sorting.
    private List<FieldSortBuilder> getSortQuery(SortQuery sortQuery) {
        List<FieldSortBuilder> out = new ArrayList<>();

        for (int idx = 0; idx < sortQuery.getField().size(); idx++) {
            out.add(new FieldSortBuilder(sortQuery.getFieldByIndex(idx))
                    .order(SortOrder.fromString(sortQuery.getOrderByIndex(idx).name()))
                    .missing("_last")
                    .unmappedType("keyword"));
        }
        return out;
    }
}