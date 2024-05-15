package org.opengroup.osdu.search.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;

@RunWith(MockitoJUnitRunner.class)
public class FieldMappingTypeServiceTest {

    @Mock
    private RestHighLevelClient restClient;
    @Mock
    private IndicesClient indicesClient;
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private IFieldTypeMappingCache typeMappingCache;
    @Mock
    private GetFieldMappingsResponse mappingsResponse;
    @InjectMocks
    private FieldMappingTypeService fieldMappingTypeService;

    @Test
    public void should_addIgnoreUnavailable_when_gettingFieldMappings() throws Exception {
        when(dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("");
        when(typeMappingCache.get(any())).thenReturn(Collections.emptyMap());
        when(restClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getFieldMapping(any(GetFieldMappingsRequest.class), any())).thenReturn(mappingsResponse);
        when(mappingsResponse.mappings()).thenReturn(Collections.emptyMap());

        fieldMappingTypeService.getSortableTextFields(restClient, "testFieldName", "testIndexPattern");

        ArgumentCaptor<GetFieldMappingsRequest> requestCaptor = ArgumentCaptor.forClass(GetFieldMappingsRequest.class);
        verify(indicesClient).getFieldMapping(requestCaptor.capture(), any());
        assertTrue(requestCaptor.getValue().indicesOptions().ignoreUnavailable());
    }

}
