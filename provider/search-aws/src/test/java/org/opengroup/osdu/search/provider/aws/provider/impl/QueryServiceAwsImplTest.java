// Copyright © Amazon
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http:#www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.provider.aws.provider.impl;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.policy.service.PartitionPolicyStatusService;
import org.opengroup.osdu.search.provider.aws.provider.impl.QueryServiceAwsImpl;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.*;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueryServiceAwsImplTest {

	private final String DATA_GROUPS = "X-Data-Groups";
	private final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";
	private final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";

	@InjectMocks
	QueryServiceAwsImpl queryServiceAws;

	@Mock
	private ElasticClientHandler elasticClientHandler;

	private ElasticClientHandler realElasticClientHandler = new ElasticClientHandler();

	@Mock
	private JaxRsDpsLog log;

	@Mock
	private IProviderHeaderService providerHeaderService;

	@Mock
	private CrossTenantUtils crossTenantUtils;

	@Mock
	private IFieldMappingTypeService fieldMappingTypeService;

	@Mock
	private PartitionPolicyStatusService statusService;

	@Mock
	private IQueryParserUtil queryParserUtil;

	@Mock
	private ISortParserUtil sortParserUtil;

	@Mock
	private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil;

	@Mock
	private DpsHeaders dpsHeaders;

	@Mock
	private AuditLogger auditLogger;

	@Before
	public void setup() {
		MockitoAnnotations.openMocks(this);

		Map<String, String> HEADERS = new HashMap<>();
		HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
		HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
		HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));

		when(providerHeaderService.getDataGroupsHeader()).thenReturn(DATA_GROUPS);
		when(dpsHeaders.getHeaders()).thenReturn(HEADERS);
	}

	@Test
	public void should_return_CorrectQueryResponseforIntersectionSpatialFilter() throws Exception {
		// arrange
		// create query request according to this example query:
		//	{
		//		"kind": "osdu:wks:reference-data--CoordinateTransformation:1.0.0",
		//			"query": "data.ID:\"EPSG::1078\"",
		//			"spatialFilter": {
		//			"field": "data.Wgs84Coordinates",
		//			"byIntersection": {
		//				"polygons": [
		//				{
		//					"points": [
		//					{
		//						"latitude": 10.75,
		//							"longitude": -8.61
		//					}
		//						]
		//				}
		//				]
		//			}
		//		}
		//	}
		QueryRequest queryRequest = new QueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		spatialFilter.setField("data.Wgs84Coordinates");
		SpatialFilter.ByIntersection byIntersection = new SpatialFilter.ByIntersection();
		Polygon polygon = new Polygon();
		Point point = new Point(1.02, -8.61);
		Point point1 = new Point(1.02, -2.48);
		Point point2 = new Point(10.74, -2.48);
		Point point3 = new Point(10.74, -8.61);
		Point point4 = new Point(1.02, -8.61);
		List<Point> points = new ArrayList<>();
		points.add(point);
		points.add(point1);
		points.add(point2);
		points.add(point3);
		points.add(point4);
		polygon.setPoints(points);
		List<Polygon> polygons = new ArrayList<>();
		polygons.add(polygon);
		byIntersection.setPolygons(polygons);
		spatialFilter.setByIntersection(byIntersection);
		queryRequest.setSpatialFilter(spatialFilter);

		// mock out elastic client handler
		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
		when(searchResponse.status())
				.thenReturn(RestStatus.OK);

		SearchHits searchHits = Mockito.mock(SearchHits.class);
		when(searchHits.getHits())
				.thenReturn(new SearchHit[]{});
		when(searchResponse.getHits())
				.thenReturn(searchHits);

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT)))
				.thenReturn(searchResponse);


		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Set<String> indexedTypes = new HashSet<>();
		indexedTypes.add("geo_shape");
		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
				.thenReturn(indexedTypes);

		when(providerHeaderService.getDataGroupsHeader())
				.thenReturn("groups");

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");
		when(dpsHeaders.getHeaders())
				.thenReturn(headers);

		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-8.61,1.02],[-2.48,1.02],[-2.48,10.74],[-8.61,10.74],[-8.61,1.02]]]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":false,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

		// act
		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

		// assert
		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
		SearchRequest searchRequest = searchRequestArg.getValue();
		String actualSource = searchRequest.source().toString();
		assertEquals(expectedSource, actualSource);
	}

	@Test
	public void should_return_CorrectQueryResponseforWithinSpatialFilter() throws Exception {
		// arrange
		// create query request according to this example query:
		//		{
		//				"kind": "osdu:wks:reference-data--CoordinateTransformation:1.0.1",
		//				"query": "data.ID:\"EPSG::blahblah8\"",
		//				"spatialFilter": {
		//					"field": "data.SpatialLocation.Wgs84Coordinates",
		//							"byWithinPolygon": {
		//						"points": [
		//						{
		//							"latitude": 10.71,
		//								"longitude": -8.60
		//						}
		//							]
		//					}
		//
		//				}
		//		}
		QueryRequest queryRequest = new QueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		spatialFilter.setField("data.Wgs84Coordinates");
		SpatialFilter.ByWithinPolygon byWithinPolygon = new SpatialFilter.ByWithinPolygon();
		Point point = new Point(1.02, -8.61);
		List<Point> points = new ArrayList<>();
		points.add(point);
		byWithinPolygon.setPoints(points);
		spatialFilter.setByWithinPolygon(byWithinPolygon);
		queryRequest.setSpatialFilter(spatialFilter);

		// mock out elastic client handler
		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
		when(searchResponse.status())
				.thenReturn(RestStatus.OK);

		SearchHits searchHits = Mockito.mock(SearchHits.class);
		when(searchHits.getHits())
				.thenReturn(new SearchHit[]{});
		when(searchResponse.getHits())
				.thenReturn(searchHits);

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT)))
				.thenReturn(searchResponse);


		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Set<String> indexedTypes = new HashSet<>();
		indexedTypes.add("geo_shape");
		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
				.thenReturn(indexedTypes);

		when(providerHeaderService.getDataGroupsHeader())
				.thenReturn("groups");

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");
		when(dpsHeaders.getHeaders())
				.thenReturn(headers);

		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPoint\",\"coordinates\":[[-8.61,1.02]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":false,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

		// act
		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

		// assert
		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
		SearchRequest searchRequest = searchRequestArg.getValue();
		String actualSource = searchRequest.source().toString();
		assertEquals(expectedSource, actualSource);
	}

	@Test
	public void should_searchAll_when_requestHas_noQueryString() throws IOException {

		BoolQueryBuilder builder = (BoolQueryBuilder) this.queryServiceAws.buildQuery(null, null, true);
		assertNotNull(builder);

		List<QueryBuilder> topLevelMustClause = builder.must();
		assertEquals(1, topLevelMustClause.size());

		verifyAcls(topLevelMustClause.get(0), true);
	}

	@Test
	public void should_return_ownerOnlyMustClause_when_searchAsOwners() throws IOException {

		BoolQueryBuilder builder = (BoolQueryBuilder) this.queryServiceAws.buildQuery(null, null, false);
		assertNotNull(builder);

		List<QueryBuilder> topLevelMustClause = builder.must();
		assertEquals(1, topLevelMustClause.size());

		verifyAcls(topLevelMustClause.get(0), false);
	}

	@Test
	public void should_return_nullQuery_when_searchAsDataRootUser() throws IOException {
		Map<String, String> HEADERS = new HashMap<>();
		HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
		HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
		HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));
		HEADERS.put(providerHeaderService.getDataRootUserHeader(), "true");
		when(dpsHeaders.getHeaders()).thenReturn(HEADERS);

		QueryBuilder builder = this.queryServiceAws.buildQuery(null, null, false);
		assertNull(builder);
	}

	private void verifyAcls(QueryBuilder aclMustClause, boolean asOwner) {
		BoolQueryBuilder aclLevelBuilder = (BoolQueryBuilder) aclMustClause;
		assertNotNull(aclLevelBuilder);
		assertEquals("1", aclLevelBuilder.minimumShouldMatch());

		List<QueryBuilder> aclShouldClause = aclLevelBuilder.should();
		assertEquals(1, aclShouldClause.size());

		TermsQueryBuilder aclQuery = (TermsQueryBuilder) aclShouldClause.get(0);
		assertNotNull(aclQuery);
		if (asOwner) {
			assertEquals("acl.owners", aclQuery.fieldName());
		} else {
			assertEquals("x-acl", aclQuery.fieldName());
		}
		assertEquals(2, aclQuery.values().size());

		List<Object> acls = aclQuery.values();
		assertEquals(2, acls.size());
		assertTrue(acls.contains(DATA_GROUP_1));
		assertTrue(acls.contains(DATA_GROUP_2));
	}
}
