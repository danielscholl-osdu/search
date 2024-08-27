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

import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetadata;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
//import org.elasticsearch.common.xcontent.XContentBuilder;
//import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IAttributesCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.smart.attributes.AttributeLoader;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttributesTest {

	private static MockedStatic<AttributeLoader> mockedSettings;
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
	private IAttributesCache<String, Set<String>> cache;
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

	private final String index = "tenant-test-test-1.0.0";

	@Before
	public void setup() throws IOException {
		mockedSettings = mockStatic(AttributeLoader.class);
		when(searchConfigurationProperties.getDeployedServiceId()).thenReturn("search");
		when(AttributeLoader.getAttributes()).thenAnswer((Answer<List<Attribute>>) invocation -> new ArrayList<>());

	}
	@After
	public void close() {
		mockedSettings.close();
	}

//	@Test
//	public void should_not_throw_exception_cache_all_Results_when_CacheSync_is_called()
//			throws IOException, URISyntaxException {
//
//		List<Attribute> ls = new ArrayList<Attribute>();
//		List<String> schemaMapping = new ArrayList<String>();
//		schemaMapping.add("data.Field");
//		Attribute attribute = new Attribute();
//		attribute.setName("Operataor");
//		attribute.setSchemaMapping(schemaMapping);
//		ls.add(attribute);
//
//		GetFieldMappingsResponse getFieldMappingsResponse = mock(GetFieldMappingsResponse.class);
//		XContentBuilder builder = XContentFactory.jsonBuilder();
//		builder.startObject();
//		Map<String, Object> fields = new HashMap();
//		fields.put("fields", new HashMap());
//		builder.field("Field", fields);
//		builder.endObject();
//		BytesReference bytesReference = BytesReference.bytes(builder);
//		FieldMappingMetadata mappingMetaData = new FieldMappingMetadata(index, bytesReference);
//		Map<String, FieldMappingMetadata> mapBuilder = new HashMap<>();
//		mapBuilder.put("data.Field", mappingMetaData);
//		Map<String, Map<String, FieldMappingMetadata>> mappingBuilder = new HashMap<>();
//		mappingBuilder.put("any index 1", mapBuilder);
//		mappingBuilder.put("any index 2", mapBuilder);
//		Map<String, Map<String, Map<String, FieldMappingMetadata>>> mapping = new HashMap<>();
//		mapping.put("indices 1", mappingBuilder);
//
//		try {
//			sut.cacheSync();
//		} catch (Exception e) {
//			fail("Should not throw this exception" + e.getMessage());
//		}
//	}

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
