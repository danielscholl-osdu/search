package org.opengroup.osdu.search.provider.azure.service;

import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private FieldMappingTypeService sut;

    @Test
    public void testGetFieldTypes_whenAllMappingsProvided_returnsCorrectFieldTypes() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);
        GetFieldMappingsResponse response = mock(GetFieldMappingsResponse.class);
        GetFieldMappingsResponse.FieldMappingMetaData fieldMappingMetaData = mock(GetFieldMappingsResponse.FieldMappingMetaData.class);
        IndicesClient indicesClient = mock(IndicesClient.class);

        String fieldName = FIELD + "." + TYPE;
        String indexPattern = "index.pattern";

        Map<String, Object> sourceMap = getDummySourceMap();
        Map<String, GetFieldMappingsResponse.FieldMappingMetaData> fieldMapping = new HashMap<>();
        fieldMapping.put(fieldName, fieldMappingMetaData);
        Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetaData>> typeMapping = new HashMap<>();
        typeMapping.put(FIELD, fieldMapping);
        Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetaData>>> indexMapping = new HashMap<>();
        indexMapping.put(TYPE, typeMapping);

        doReturn(indicesClient).when(restClient).indices();
        doReturn(response).when(indicesClient).getFieldMapping(any(), any());
        doReturn(indexMapping).when(response).mappings();
        doReturn(sourceMap).when(fieldMappingMetaData).sourceAsMap();

        Set<String> fieldTypes = sut.getFieldTypes(restClient, fieldName, indexPattern);

        assertEquals(fieldTypes.size(), 1);
        assertEquals(fieldTypes.iterator().next(), dummyTypeObjectStringRepresentation);
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