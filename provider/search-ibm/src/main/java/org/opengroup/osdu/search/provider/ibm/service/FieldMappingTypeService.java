/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.search.provider.ibm.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.search.provider.ibm.cache.FieldTypeMappingCache;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.google.common.base.Strings;

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
        String fieldLeafNodeLabel = fieldName.substring(fieldName.lastIndexOf(".") + 1);
        GetFieldMappingsRequest request = new GetFieldMappingsRequest();
        request.fields(fieldName);
        if (!Strings.isNullOrEmpty(indexPattern)) request.indices(indexPattern);
        GetFieldMappingsResponse response = restClient.indices().getFieldMapping(request, RequestOptions.DEFAULT);
        Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> mappings = response.mappings();

        for (Map.Entry<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> indexMapping : mappings.entrySet()) {
            if (indexMapping.getValue().isEmpty()) continue;
            Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> typeMapping = indexMapping.getValue();
            Map<String, GetFieldMappingsResponse.FieldMappingMetadata> fieldMapping = typeMapping.values().iterator().next();
            GetFieldMappingsResponse.FieldMappingMetadata fieldMappingMetaData = fieldMapping.get(fieldName);
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