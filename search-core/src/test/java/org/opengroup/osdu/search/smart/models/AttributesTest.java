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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAttributesCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.smart.attributes.AttributeLoader;
import org.opengroup.osdu.search.util.ElasticClientHandler;

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
