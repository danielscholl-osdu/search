// // Copyright © Amazon Web Services
// //
// // Licensed under the Apache License, Version 2.0 (the "License");
// // you may not use this file except in compliance with the License.
// // You may obtain a copy of the License at
// //
// //      http://www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing, software
// // distributed under the License is distributed on an "AS IS" BASIS,
// // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// // See the License for the specific language governing permissions and
// // limitations under the License.

// package org.opengroup.osdu.search.provider.aws.provider.impl;

// import org.elasticsearch.action.search.SearchRequest;
// import org.elasticsearch.action.search.SearchResponse;
// import org.elasticsearch.client.RequestOptions;
// import org.elasticsearch.client.RestHighLevelClient;
// import org.elasticsearch.index.query.QueryBuilder;
// import org.elasticsearch.rest.RestStatus;
// import org.elasticsearch.search.SearchHit;
// import org.elasticsearch.search.SearchHits;
// import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
// import org.opengroup.osdu.core.common.model.http.DpsHeaders;
// import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
// import org.opengroup.osdu.core.common.model.search.Point;
// import org.opengroup.osdu.core.common.model.search.Polygon;
// import org.opengroup.osdu.core.common.model.search.SpatialFilter;
// import org.opengroup.osdu.search.cache.CursorCache;
// import org.opengroup.osdu.search.logging.AuditLogger;
// import org.opengroup.osdu.search.policy.service.IPolicyService;
// import org.opengroup.osdu.search.policy.service.PartitionPolicyStatusService;
// import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
// import org.opengroup.osdu.search.service.IFieldMappingTypeService;
// import org.opengroup.osdu.search.util.CrossTenantUtils;
// import org.opengroup.osdu.search.util.ElasticClientHandler;
// import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
// import org.opengroup.osdu.search.util.IQueryParserUtil;
// import org.opengroup.osdu.search.util.ISortParserUtil;
// import org.opengroup.osdu.search.util.ResponseExceptionParser;

// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;

// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.*;
// import org.mockito.junit.MockitoJUnitRunner;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;

// @RunWith(MockitoJUnitRunner.class)
// public class ScrollQueryServiceAwsImplIPolicyServiceTest {

//     private final String DATA_GROUPS = "X-Data-Groups";
// 	private final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";
// 	private final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";
// 	private final String PARTITION_ID = "opendes";

//     @InjectMocks
// 	ScrollQueryServiceAwsImpl scrollQueryServiceAws;

//     @Mock
// 	private ElasticClientHandler elasticClientHandler;

// 	@Mock
// 	private JaxRsDpsLog log;

// 	@Mock
// 	private IProviderHeaderService providerHeaderService;

// 	@Mock
// 	private CrossTenantUtils crossTenantUtils;

// 	@Mock
// 	private IFieldMappingTypeService fieldMappingTypeService;

// 	@Mock
// 	private PartitionPolicyStatusService statusService;

// 	@Mock
// 	private IQueryParserUtil queryParserUtil;

// 	@Mock
// 	private ISortParserUtil sortParserUtil;

// 	@Mock
// 	private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil;

// 	@Mock
// 	private DpsHeaders dpsHeaders;

// 	@Mock
// 	private AuditLogger auditLogger;

//     @Mock
//     private CursorCache cursorCache;

// 	@Mock 
// 	private ResponseExceptionParser exceptionParser;

// 	@Mock 
// 	private IPolicyService iPolicyService;

// 	@Before
// 	public void setup() {
// 		MockitoAnnotations.openMocks(this);

// 		Map<String, String> HEADERS = new HashMap<>();
// 		HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
// 		HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
// 		HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));
// 	}


// 	@Test
// 	public void should_return_CorrectQueryResponse_NullResult_noCursorSet_QueryBuilder() throws Exception {
// 		// arrange
// 		// create query request according to this example query:
// 		//	{
// 		//		"kind": "osdu:wks:reference-data--CoordinateTransformation:1.0.0",
// 		//			"query": "data.ID:\"EPSG::1078\"",
// 		//			"spatialFilter": {
// 		//			"field": "data.Wgs84Coordinates",
// 		//			"byIntersection": {
// 		//				"polygons": [
// 		//				{
// 		//					"points": [
// 		//					{
// 		//						"latitude": 10.75,
// 		//							"longitude": -8.61
// 		//					}
// 		//						]
// 		//				}
// 		//				]
// 		//			}
// 		//		}
// 		//	}
// 		CursorQueryRequest queryRequest = new CursorQueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByIntersection byIntersection = new SpatialFilter.ByIntersection();
// 		Polygon polygon = new Polygon();
// 		Point point = new Point(1.02, -8.61);
// 		Point point1 = new Point(1.02, -2.48);
// 		Point point2 = new Point(10.74, -2.48);
// 		Point point3 = new Point(10.74, -8.61);
// 		Point point4 = new Point(1.02, -8.61);
// 		List<Point> points = new ArrayList<>();
// 		points.add(point);
// 		points.add(point1);
// 		points.add(point2);
// 		points.add(point3);
// 		points.add(point4);
// 		polygon.setPoints(points);
// 		List<Polygon> polygons = new ArrayList<>();
// 		polygons.add(polygon);
// 		byIntersection.setPolygons(polygons);
// 		spatialFilter.setByIntersection(byIntersection);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

// 		QueryBuilder textQueryBuilder = Mockito.mock(QueryBuilder.class);

// 		when(iPolicyService.getCompiledPolicy(any())).thenReturn("PolicyString");

// 		when(queryParserUtil.buildQueryBuilderFromQueryString(anyString())).thenReturn(textQueryBuilder);

// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[]{});
// 		when(searchResponse.getHits())
// 				.thenReturn(searchHits);

// 		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT)))
// 				.thenReturn(searchResponse);


// 		when(elasticClientHandler.createRestClient())
// 				.thenReturn(client);

// 		String index = "some-index";
// 		when(crossTenantUtils.getIndexName(Mockito.any()))
// 				.thenReturn(index);

// 		Set<String> indexedTypes = new HashSet<>();
// 		indexedTypes.add("geo_shape");
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);


// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");

// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-8.61,1.02],[-2.48,1.02],[-2.48,10.74],[-8.61,10.74],[-8.61,1.02]]]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"wrapper\":{\"query\":\"UG9saWN5U3RyaW5n\"}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		scrollQueryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}
// }