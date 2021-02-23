/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.search.smart.models;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetadata;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IAttributesCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.opengroup.osdu.search.smart.attributes.AttributeLoader;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RestHighLevelClient.class, IndicesClient.class, SearchConfigurationProperties.class, AttributeLoader.class })
public class AttributesTest {

	@Mock
	private SearchConfigurationProperties searchConfigurationProperties;
	@Mock
	private ElasticClientHandler elasticClientHandler;
	@Mock
	private IUrlFetchService urlFetchService;
	@Mock
	private JaxRsDpsLog log;
	@Mock
	private DpsHeaders dpsHeaders;
	@Mock
	private IAttributesCache cache;
	@Mock
	private HttpResponse response;
	@Mock
	private SearchResponse searchResponse;
	@Mock
	private Aggregations aggregations;
	@Mock
	private Terms keywordAggregation;
	@InjectMocks
	private AttributeCollection sut;

	private RestHighLevelClient restHighLevelClient;
	private IndicesClient indicesClient;
	private final String index = "tenant-test-test-1.0.0";

	@Before
	public void setup() throws IOException {
		this.restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);
		this.indicesClient = PowerMockito.mock(IndicesClient.class);
		mockStatic(AttributeLoader.class);
		when(searchConfigurationProperties.getDeployedServiceId()).thenReturn("search");
		when(AttributeLoader.getAttributes()).thenReturn(new ArrayList<>());
	}

	@Test
	public void should_not_throw_exception_cache_all_Results_when_CacheSync_is_called()
			throws IOException, URISyntaxException {

		List<Attribute> ls = new ArrayList<Attribute>();
		List<String> schemaMapping = new ArrayList<String>();
		schemaMapping.add("data.Field");
		Attribute attribute = new Attribute();
		attribute.setName("Operataor");
		attribute.setSchemaMapping(schemaMapping);
		ls.add(attribute);

		when(dpsHeaders.getPartitionId()).thenReturn("tenant1");
		when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
		doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
		GetFieldMappingsResponse getFieldMappingsResponse = mock(GetFieldMappingsResponse.class);
		when(this.indicesClient.getFieldMapping((GetFieldMappingsRequest) any(), any())).thenReturn(getFieldMappingsResponse);
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		Map<String, Object> fields = new HashMap();
		fields.put("fields", new HashMap());
		builder.field("Field", fields);
		builder.endObject();
		BytesReference bytesReference = BytesReference.bytes(builder);
		FieldMappingMetadata mappingMetaData = new FieldMappingMetadata(index, bytesReference);
		Map<String, FieldMappingMetadata> mapBuilder = new HashMap<>();
		mapBuilder.put("data.Field", mappingMetaData);
		Map<String, Map<String, FieldMappingMetadata>> mappingBuilder = new HashMap<>();
		mappingBuilder.put("any index 1", mapBuilder);
		mappingBuilder.put("any index 2", mapBuilder);
		Map<String, Map<String, Map<String, FieldMappingMetadata>>> mapping = new HashMap<>();
		mapping.put("indices 1", mappingBuilder);
		when(searchResponse.getAggregations()).thenReturn(null);
		when(getFieldMappingsResponse.mappings()).thenReturn(mapping);
		when(urlFetchService.sendRequest(any())).thenReturn(response);
		when(response.getResponseCode()).thenReturn(200);
		PowerMockito.when(restHighLevelClient.search(any(), any(RequestOptions.class))).thenReturn(searchResponse);

		try {
			sut.cacheSync();
		} catch (Exception e) {
			fail("Should not throw this exception" + e.getMessage());
		}
	}

	@Test
	public void should_get_all_attributes_when_acoountId_and_attribute_name_is_provided() {
		try {
			sut.getAllAttributes("tenant1", "operator");
			verify(cache, times(1)).get(any());
		} catch (Exception e) {
			fail("Should not throw this exception" + e.getMessage());
		}
	}
}
