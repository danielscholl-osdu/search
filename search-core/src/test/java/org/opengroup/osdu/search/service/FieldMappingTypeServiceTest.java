//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.service;

import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FieldMappingTypeServiceTest {

    private static final String dummyTypeObjectStringRepresentation = "dummyTypeObject";
    private static final String FIELD = "field";
    private static final String TYPE = "type";

    private class DummyTypeObject {
        @Override
        public String toString() {
            return dummyTypeObjectStringRepresentation;
        }
    }

    @Mock
    private IFieldTypeMappingCache cache;
    @Mock
    private DpsHeaders headers;
    @InjectMocks
    private FieldMappingTypeService sut;

    /*
     * NOTE [aaljain] :
     * Scenarios where typeMap and fieldMapping are null will result into error
     * which the current implementation of FieldMappingTypeService does not handle
     */

    @Test
    public void testGetFieldTypes_whenAllMappingsProvided_returnsCorrectFieldTypes() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);
        GetFieldMappingsResponse response = mock(GetFieldMappingsResponse.class);
        GetFieldMappingsResponse.FieldMappingMetadata fieldMappingMetaData = mock(GetFieldMappingsResponse.FieldMappingMetadata.class);
        IndicesClient indicesClient = mock(IndicesClient.class);

        String fieldName = FIELD + "." + TYPE;
        String indexPattern = "index.pattern";

        Map<String, Object> sourceMap = getDummySourceMap();
        Map<String, GetFieldMappingsResponse.FieldMappingMetadata> typeMapping = new HashMap<>();
        typeMapping.put(FIELD, fieldMappingMetaData);
        Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> indexMapping = new HashMap<>();
        indexMapping.put(TYPE, typeMapping);

        doReturn(indicesClient).when(restClient).indices();
        doReturn(response).when(indicesClient).getFieldMapping(any(GetFieldMappingsRequest.class), any());
        doReturn(indexMapping).when(response).mappings();
        doReturn(sourceMap).when(fieldMappingMetaData).sourceAsMap();

        Set<String> fieldTypes = sut.getFieldTypes(restClient, fieldName, indexPattern);

        assertEquals(fieldTypes.size(), 1);
        assertEquals(fieldTypes.iterator().next(), dummyTypeObjectStringRepresentation);
    }

    @Test
    public void testGetFieldTypes_whenMissingIndexMapping_returnsEmptyFieldTypes() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);
        GetFieldMappingsResponse response = mock(GetFieldMappingsResponse.class);
        IndicesClient indicesClient = mock(IndicesClient.class);

        String fieldName = FIELD + "." + TYPE;
        String indexPattern = "index.pattern";

        Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> indexMapping = new HashMap<>();

        doReturn(indicesClient).when(restClient).indices();
        doReturn(response).when(indicesClient).getFieldMapping(any(GetFieldMappingsRequest.class), any());
        doReturn(indexMapping).when(response).mappings();

        Set<String> fieldTypes = sut.getFieldTypes(restClient, fieldName, indexPattern);

        assertEquals(fieldTypes.size(), 0);
    }

    @Test
    public void testGetFieldTypes_whenMissingFieldMappingMetaData_returnsEmptyFieldTypes() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);
        GetFieldMappingsResponse response = mock(GetFieldMappingsResponse.class);
        IndicesClient indicesClient = mock(IndicesClient.class);
        GetFieldMappingsResponse.FieldMappingMetadata fieldMappingMetaData = mock(GetFieldMappingsResponse.FieldMappingMetadata.class);

        String fieldName = FIELD + "." + TYPE;
        String indexPattern = "index.pattern";

        Map<String, GetFieldMappingsResponse.FieldMappingMetadata> typeMapping = new HashMap<>();
        typeMapping.put(FIELD, fieldMappingMetaData);
        Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> indexMapping = new HashMap<>();
        indexMapping.put(TYPE, typeMapping);

        doReturn(indicesClient).when(restClient).indices();
        doReturn(response).when(indicesClient).getFieldMapping(any(GetFieldMappingsRequest.class), any());
        doReturn(indexMapping).when(response).mappings();

        Set<String> fieldTypes = sut.getFieldTypes(restClient, fieldName, indexPattern);

        assertEquals(fieldTypes.size(), 0);
    }

    @Test
    public void testGetFieldTypes_whenMissingTypeObjectInTypeMap_returnsEmptyFieldTypes() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);
        GetFieldMappingsResponse response = mock(GetFieldMappingsResponse.class);
        GetFieldMappingsResponse.FieldMappingMetadata fieldMappingMetaData = mock(GetFieldMappingsResponse.FieldMappingMetadata.class);
        IndicesClient indicesClient = mock(IndicesClient.class);

        String fieldName = FIELD + "." + TYPE;
        String indexPattern = "index.pattern";

        Map<String, Object> sourceMap = getDummySourceMap();
        ((LinkedHashMap) sourceMap.get(TYPE)).put(TYPE, null);
        Map<String, GetFieldMappingsResponse.FieldMappingMetadata> typeMapping = new HashMap<>();
        typeMapping.put(FIELD, fieldMappingMetaData);
        Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> indexMapping = new HashMap<>();
        indexMapping.put(TYPE, typeMapping);

        doReturn(indicesClient).when(restClient).indices();
        doReturn(response).when(indicesClient).getFieldMapping(any(GetFieldMappingsRequest.class), any());
        doReturn(indexMapping).when(response).mappings();
        doReturn(sourceMap).when(fieldMappingMetaData).sourceAsMap();

        Set<String> fieldTypes = sut.getFieldTypes(restClient, fieldName, indexPattern);

        assertEquals(fieldTypes.size(), 0);
    }

    @Test
    public void should_getValid_typeMapping_fromCache() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);
        IndicesClient indicesClient = mock(IndicesClient.class);

        Map<String, String> cachedTypes = new HashMap<>();
        cachedTypes.put("dummyField", "geo_point");

        when(this.cache.get(any())).thenReturn(cachedTypes);

        Set<String> fieldTypes = this.sut.getFieldTypes(restClient, "dummyField", "dummyPattern");
        assertNotNull(fieldTypes);
        verify(indicesClient, never()).getFieldMapping(any(GetFieldMappingsRequest.class), any(RequestOptions.class));
    }

    private Map<String, Object> getDummySourceMap() {
        Map<String, Object> sourceMap = new HashMap<>();
        LinkedHashMap typeMap = new LinkedHashMap<>();
        Object type = new DummyTypeObject();
        typeMap.put(TYPE, type);
        sourceMap.put(TYPE, typeMap);
        return sourceMap;
    }
}