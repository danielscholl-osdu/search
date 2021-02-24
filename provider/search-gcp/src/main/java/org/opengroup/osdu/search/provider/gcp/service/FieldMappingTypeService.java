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

package org.opengroup.osdu.search.provider.gcp.service;

import com.google.common.base.Strings;
import java.util.Map.Entry;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.elasticsearch.client.indices.GetFieldMappingsResponse.FieldMappingMetadata;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.search.provider.gcp.cache.FieldTypeMappingCache;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequestScope
public class FieldMappingTypeService {

    @Inject
    private FieldTypeMappingCache typeMappingCache;
    @Inject
    private DpsHeaders headers;

    public Set<String> getFieldTypes(RestHighLevelClient restClient, String fieldName, String indexPattern) throws IOException {
        Preconditions.checkNotNull(restClient, "restClient cannot be null");
        Preconditions.checkNotNullOrEmpty(fieldName, "fieldName cannot be null or empty");
        Preconditions.checkNotNullOrEmpty(indexPattern, "indexPattern cannot be null or empty");

        Set<String> cachedTypes = this.typeMappingCache.get(getCacheKey(fieldName, indexPattern));
        if (cachedTypes != null && !cachedTypes.isEmpty()) {
            return cachedTypes;
        }

        HashSet<String> fieldTypes = new HashSet<>();
        String fieldLeafNodeLabel = fieldName.substring(fieldName.lastIndexOf('.') + 1);
        GetFieldMappingsRequest request = new GetFieldMappingsRequest();
        request.fields(fieldName);
        if (!Strings.isNullOrEmpty(indexPattern)) request.indices(indexPattern);
        GetFieldMappingsResponse response = restClient.indices().getFieldMapping(request, RequestOptions.DEFAULT);
        Map<String, Map<String, FieldMappingMetadata>> mappings = response.mappings();

        for (Entry<String, Map<String, FieldMappingMetadata>> indexMapping: mappings.entrySet()) {
            if (indexMapping.getValue().isEmpty()) continue;
            Map<String, FieldMappingMetadata> typeMapping = indexMapping.getValue();
            FieldMappingMetadata fieldMappingMetaData = typeMapping.values().iterator().next();

            if (fieldMappingMetaData == null) continue;
            Map<String, Object> mapping = fieldMappingMetaData.sourceAsMap();
            LinkedHashMap<String, Object> typeMap = (LinkedHashMap<String, Object>) mapping.get(fieldLeafNodeLabel);
            Object type = typeMap.get("type");
            if (type == null) continue;
            fieldTypes.add(type.toString());
        }

        this.typeMappingCache.put(this.getCacheKey(fieldName, indexPattern), fieldTypes);

        return fieldTypes;
    }

    private String getCacheKey(String fieldName, String indexPattern) {
        return String.format("%s-%s-%s", this.headers.getPartitionIdWithFallbackToAccountId(), indexPattern, fieldName);
    }
}