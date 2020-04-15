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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.provider.gcp.cache.FieldTypeMappingCache;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestHighLevelClient.class, IndicesClient.class})
public class FieldMappingTypeServiceTest {

    @InjectMocks
    private FieldMappingTypeService sut;
    @Mock
    private FieldTypeMappingCache typeMappingCache;
    @Mock
    private DpsHeaders headers;

    private RestHighLevelClient restHighLevelClient;
    private IndicesClient indicesClient;

    @Before
    public void setup() {
        initMocks(this);
        restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);
        indicesClient = PowerMockito.mock(IndicesClient.class);
    }

    @Test
    public void should_returnTypeMapping_given_validRequest() throws IOException {
        String responseJson = "{\"opendes-test-shape-1.0.0\":{\"mappings\":{\"shape\":{\"data.Data.ExtensionProperties.locationWGS84\":{\"full_name\":\"data.Data.ExtensionProperties.locationWGS84\",\"mapping\":{\"locationWGS84\":{\"type\":\"geo_shape\"}}}}}}}";
        GetFieldMappingsResponse fieldMappingsResponse = GetFieldMappingsResponse.fromXContent(getXContentParser(responseJson));

        doReturn(indicesClient).when(restHighLevelClient).indices();
        doReturn(fieldMappingsResponse).when(indicesClient).getFieldMapping(any(), any(RequestOptions.class));

        Set<String> fieldTypes = this.sut.getFieldTypes(restHighLevelClient, "data.Data.ExtensionProperties.locationWGS84", "opendes-test-shape-1.0.0");
        assertNotNull(fieldTypes);
        assertTrue(fieldTypes.contains("geo_shape"));
    }

    @Test
    public void should_getValid_typeMapping_fromCache() throws IOException {
        HashSet<String> cachedTypes = new HashSet<>();
        cachedTypes.add("geo_point");

        when(this.typeMappingCache.get(any())).thenReturn(cachedTypes);
        doReturn(indicesClient).when(restHighLevelClient).indices();

        Set<String> fieldTypes = this.sut.getFieldTypes(restHighLevelClient, "dummyField", "dummyPattern");
        assertNotNull(fieldTypes);
        verify(this.indicesClient, never()).getFieldMapping(any(), any(RequestOptions.class));
    }

    private XContentParser getXContentParser(String json) throws IOException {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> object = new Gson().fromJson(json, type);

        XContentBuilder contentBuilder = JsonXContent.contentBuilder().value(object);

        XContentParser parser = JsonXContent.jsonXContent.createParser(
                NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                BytesReference.bytes(contentBuilder).streamInput()
        );
        return parser;
    }
}