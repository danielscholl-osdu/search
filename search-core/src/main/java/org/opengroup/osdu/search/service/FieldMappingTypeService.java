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

package org.opengroup.osdu.search.service;

import com.google.common.base.Strings;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.util.*;

@Component
@RequestScope
public class FieldMappingTypeService implements IFieldMappingTypeService {

    @Autowired
    private IFieldTypeMappingCache typeMappingCache;
    @Autowired
    private DpsHeaders headers;

    public Map<String, String> getSortableTextFields(RestHighLevelClient restClient, String fieldName, String indexPattern) throws IOException {
        String cacheKey = this.getSortableTextFieldCacheKey(fieldName, indexPattern);
        Map<String, String> cachedTypes = this.typeMappingCache.get(cacheKey);
        if (cachedTypes != null && !cachedTypes.isEmpty()) {
            return cachedTypes;
        }

        Map<String, String> fieldTypeMap = new HashMap<>();
        GetFieldMappingsResponse response = this.getFieldMappings(restClient, fieldName, indexPattern);
        Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappings = response.mappings();

        for (Map.Entry<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> indexMapping : mappings.entrySet()) {
            if (indexMapping.getValue().isEmpty()) continue;
            Map<String, GetFieldMappingsResponse.FieldMappingMetadata> typeMapping = indexMapping.getValue();
            for (Map.Entry<String, GetFieldMappingsResponse.FieldMappingMetadata> fieldMappingMetadataEntry : typeMapping.entrySet()) {
                if (fieldMappingMetadataEntry.getValue() == null) continue;
                String field = fieldMappingMetadataEntry.getValue().fullName();
                fieldTypeMap.put(field.substring(0, field.lastIndexOf(".keyword")), field);
            }
        }

        this.typeMappingCache.put(cacheKey, fieldTypeMap);

        return fieldTypeMap;
    }

    private GetFieldMappingsResponse getFieldMappings(RestHighLevelClient restClient, String fieldName, String indexPattern) throws IOException {
        Preconditions.checkNotNull(restClient, "restClient cannot be null");
        Preconditions.checkNotNullOrEmpty(fieldName, "fieldName cannot be null or empty");
        Preconditions.checkNotNullOrEmpty(indexPattern, "indexPattern cannot be null or empty");

        GetFieldMappingsRequest request = new GetFieldMappingsRequest();
        request.fields(fieldName);
        if (!Strings.isNullOrEmpty(indexPattern)) request.indices(indexPattern);
        return restClient.indices().getFieldMapping(request, RequestOptions.DEFAULT);
    }

    private String getSortableTextFieldCacheKey(String fieldName, String indexPattern) {
        return String.format("%s-sortable-text-%s-%s", this.headers.getPartitionIdWithFallbackToAccountId(), indexPattern, fieldName);
    }
}
