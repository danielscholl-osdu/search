/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ExpandWildcard;
import co.elastic.clients.elasticsearch._types.mapping.FieldMapping;
import co.elastic.clients.elasticsearch.indices.GetFieldMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetFieldMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_field_mapping.TypeFieldMappings;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.*;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class FieldMappingTypeService implements IFieldMappingTypeService {

  @Autowired private IFieldTypeMappingCache typeMappingCache;
  @Autowired private DpsHeaders headers;

  public Map<String, String> getSortableTextFields(
      ElasticsearchClient restClient, String fieldName, String indexPattern) throws IOException {
    String cacheKey = this.getSortableTextFieldCacheKey(fieldName, indexPattern);
    Map<String, String> cachedTypes = this.typeMappingCache.get(cacheKey);
    if (cachedTypes != null && !cachedTypes.isEmpty()) {
      return cachedTypes;
    }

    Map<String, String> fieldTypeMap = new HashMap<>();
    GetFieldMappingResponse response = this.getFieldMappings(restClient, fieldName, indexPattern);

    for (Map.Entry<String, TypeFieldMappings> typeFieldMapping : response.result().entrySet()) {
      if (typeFieldMapping.getValue().mappings().isEmpty()) {
        continue;
      }

      for (Map.Entry<String, FieldMapping> fieldMapping :
          typeFieldMapping.getValue().mappings().entrySet()) {
        if (fieldMapping.getValue().mapping().isEmpty()) {
          continue;
        }

        String fullName = fieldMapping.getValue().fullName();
        fieldTypeMap.put(fullName.substring(0, fullName.lastIndexOf(".keyword")), fullName);

        this.typeMappingCache.put(cacheKey, fieldTypeMap);
      }
    }
    return fieldTypeMap;
  }

  private GetFieldMappingResponse getFieldMappings(
      ElasticsearchClient restClient, String fieldName, String indexPattern) throws IOException {
    Preconditions.checkNotNull(restClient, "restClient cannot be null");
    Preconditions.checkNotNullOrEmpty(fieldName, "fieldName cannot be null or empty");
    Preconditions.checkNotNullOrEmpty(indexPattern, "indexPattern cannot be null or empty");

    GetFieldMappingRequest.Builder request = new GetFieldMappingRequest.Builder();
    request.fields(fieldName);

    if (!Strings.isNullOrEmpty(indexPattern)) {
      request.index(indexPattern);
    }

    request
        .allowNoIndices(true)
        .expandWildcards(ExpandWildcard.Open, ExpandWildcard.Closed)
        .ignoreUnavailable(true);

    return restClient.indices().getFieldMapping(request.build());
  }

  private String getSortableTextFieldCacheKey(String fieldName, String indexPattern) {
    return String.format(
        "%s-sortable-text-%s-%s",
        this.headers.getPartitionIdWithFallbackToAccountId(), indexPattern, fieldName);
  }
}
