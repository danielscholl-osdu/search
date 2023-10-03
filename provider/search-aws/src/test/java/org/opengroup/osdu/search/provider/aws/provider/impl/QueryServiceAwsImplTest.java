// /* Copyright © Amazon

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

//      http:#www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License. */

// package org.opengroup.osdu.search.provider.aws.provider.impl;

// import org.elasticsearch.search.aggregations.bucket.terms.Terms;
// import org.elasticsearch.action.search.SearchRequest;
// import org.elasticsearch.action.search.SearchResponse;
// import org.elasticsearch.action.search.SearchResponseSections;
// import org.elasticsearch.action.search.ShardSearchFailure;
// import org.elasticsearch.client.RequestOptions;
// import org.elasticsearch.client.RestHighLevelClient;
// import org.elasticsearch.common.bytes.BytesArray;
// import org.elasticsearch.common.bytes.BytesReference;
// import org.elasticsearch.common.text.Text;
// import org.elasticsearch.index.query.BoolQueryBuilder;
// import org.elasticsearch.index.query.QueryBuilder;
// import org.elasticsearch.index.query.TermsQueryBuilder;
// import org.elasticsearch.rest.RestStatus;
// import org.elasticsearch.search.SearchHit;
// import org.elasticsearch.search.SearchHits;
// import org.elasticsearch.search.aggregations.Aggregations;
// import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.Mockito;
// import org.mockito.MockitoAnnotations;
// import org.mockito.Spy;
// import org.mockito.junit.MockitoJUnitRunner;
// import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
// import org.opengroup.osdu.core.common.model.http.AppException;
// import org.opengroup.osdu.core.common.model.http.DpsHeaders;
// import org.opengroup.osdu.core.common.model.search.Point;
// import org.opengroup.osdu.core.common.model.search.Polygon;
// import org.opengroup.osdu.core.common.model.search.QueryRequest;
// import org.opengroup.osdu.core.common.model.search.QueryResponse;
// import org.opengroup.osdu.core.common.model.search.SpatialFilter;
// import org.opengroup.osdu.search.logging.AuditLogger;
// import org.opengroup.osdu.search.policy.service.PartitionPolicyStatusService;
// import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
// import org.opengroup.osdu.search.service.IFieldMappingTypeService;
// import org.opengroup.osdu.search.util.CrossTenantUtils;
// import org.opengroup.osdu.search.util.ElasticClientHandler;
// import org.opengroup.osdu.search.util.GeoQueryBuilder;
// import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
// import org.opengroup.osdu.search.util.IQueryParserUtil;
// import org.opengroup.osdu.search.util.ISortParserUtil;

// @RunWith(MockitoJUnitRunner.class)
// public class QueryServiceAwsImplTest {

// 	private final String DATA_GROUPS = "X-Data-Groups";
// 	private final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";
// 	private final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";
// 	private final String PARTITION_ID = "opendes";

// 	@InjectMocks
// 	QueryServiceAwsImpl queryServiceAws;

// 	@Mock
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

// 	@Spy
// 	private GeoQueryBuilder geoQueryBuilder = new GeoQueryBuilder();

// 	@Before
// 	public void setup() {
// 		MockitoAnnotations.openMocks(this);

// 		Map<String, String> HEADERS = new HashMap<>();
// 		HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
// 		HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
// 		HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));

// 		when(providerHeaderService.getDataGroupsHeader()).thenReturn(DATA_GROUPS);
// 		when(dpsHeaders.getHeaders()).thenReturn(HEADERS);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforIntersectionSpatialFilter() throws Exception {
// 		// arrange
// 		// create query request according to this example query:
// 		// {
// 		// "kind": "osdu:wks:reference-data--CoordinateTransformation:1.0.0",
// 		// "query": "data.ID:\"EPSG::1078\"",
// 		// "spatialFilter": {
// 		// "field": "data.Wgs84Coordinates",
// 		// "byIntersection": {
// 		// "polygons": [
// 		// {
// 		// "points": [
// 		// {
// 		// "latitude": 10.75,
// 		// "longitude": -8.61
// 		// }
// 		// ]
// 		// }
// 		// ]
// 		// }
// 		// }
// 		// }
// 		QueryRequest queryRequest = new QueryRequest();
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
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-8.61,1.02],[-2.48,1.02],[-2.48,10.74],[-8.61,10.74],[-8.61,1.02]]]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_throw_AppException_FewPolygons() throws Exception {
// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByIntersection byIntersection = new SpatialFilter.ByIntersection();
// 		Polygon polygon = new Polygon();
// 		Point point = new Point(1.02, -8.61);
// 		Point point1 = new Point(1.02, -2.48);
// 		Point point4 = new Point(1.02, -8.61);
// 		List<Point> points = new ArrayList<>();
// 		points.add(point);
// 		points.add(point1);
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

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-8.61,1.02],[-2.48,1.02],[-2.48,10.74],[-8.61,10.74],[-8.61,1.02]]]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		try {
// 			// act
// 			QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 			// assert
// 			ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 			Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 			SearchRequest searchRequest = searchRequestArg.getValue();
// 			String actualSource = searchRequest.source().toString();
// 			assertEquals(expectedSource, actualSource);
// 		} catch (AppException ex) {
// 			ex.printStackTrace();
// 		}
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforDistanceSpatialFilter() throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByDistance byDistance = new SpatialFilter.ByDistance();

// 		byDistance.setDistance(10.0);
// 		byDistance.setPoint(new Point(10.0, 10.0));
// 		spatialFilter.setByDistance(byDistance);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"Circle\",\"radius\":\"10.0m\",\"coordinates\":[10.0,10.0]},\"relation\":\"within\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforGeoShapeBoundingBoxSpatialFilter() throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByBoundingBox byBoundingBox = new SpatialFilter.ByBoundingBox();
// 		Point topLeft = new Point(0.0, 10.0);
// 		Point bottomRight = new Point(10.0, 0.0);
// 		topLeft.setLatitude(10.0);
// 		bottomRight.setLatitude(10.0);
// 		topLeft.setLongitude(10.0);
// 		bottomRight.setLongitude(10.0);
// 		byBoundingBox.setTopLeft(topLeft);
// 		byBoundingBox.setBottomRight(bottomRight);
// 		spatialFilter.setByBoundingBox(byBoundingBox);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"Envelope\",\"coordinates\":[[10.0,10.0],[10.0,10.0]]},\"relation\":\"within\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforGeoShapePolygonSpatialFilter() throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByGeoPolygon byGeoPolygon = new SpatialFilter.ByGeoPolygon();
// 		Point point1 = new Point(0.0, 10.0);
// 		Point point2 = new Point(10.0, 0.0);
// 		Point point3 = new Point(5.0, 10.0);
// 		Point point4 = new Point(10.0, 5.0);
// 		Point point5 = new Point(0.0, 1.0);
// 		Point point6 = new Point(0.0, 10.0);
// 		point1.setLatitude(1.0);
// 		point2.setLatitude(12.0);
// 		point3.setLatitude(3.0);
// 		point4.setLatitude(4.0);
// 		point5.setLatitude(5.0);
// 		point6.setLatitude(1.0);
// 		point1.setLongitude(1.0);
// 		point2.setLongitude(2.0);
// 		point3.setLongitude(3.0);
// 		point4.setLongitude(4.0);
// 		point5.setLongitude(5.0);
// 		point6.setLongitude(1.0);

// 		List<Point> pointList = new ArrayList<Point>();
// 		pointList.add(point1);
// 		pointList.add(point2);
// 		pointList.add(point3);
// 		pointList.add(point4);
// 		pointList.add(point5);
// 		pointList.add(point6);

// 		byGeoPolygon.setPoints(pointList);
// 		spatialFilter.setByGeoPolygon(byGeoPolygon);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"Polygon\",\"coordinates\":[[[1.0,1.0],[2.0,12.0],[3.0,3.0],[4.0,4.0],[5.0,5.0],[1.0,1.0]]]},\"relation\":\"within\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforWithinSpatialFilter() throws Exception {
// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByWithinPolygon byWithinPolygon = new SpatialFilter.ByWithinPolygon();
// 		Point point = new Point(1.02, -8.61);
// 		List<Point> points = new ArrayList<>();
// 		points.add(point);
// 		byWithinPolygon.setPoints(points);
// 		spatialFilter.setByWithinPolygon(byWithinPolygon);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPoint\",\"coordinates\":[[-8.61,1.02]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforGeoShapePolygonSpatialFilter_notUseGeoShapeQuery()
// 			throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByGeoPolygon byGeoPolygon = new SpatialFilter.ByGeoPolygon();
// 		Point point1 = new Point(0.0, 10.0);
// 		Point point2 = new Point(10.0, 0.0);
// 		Point point3 = new Point(5.0, 10.0);
// 		Point point4 = new Point(10.0, 5.0);
// 		Point point5 = new Point(0.0, 1.0);
// 		Point point6 = new Point(0.0, 10.0);
// 		point1.setLatitude(1.0);
// 		point2.setLatitude(12.0);
// 		point3.setLatitude(3.0);
// 		point4.setLatitude(4.0);
// 		point5.setLatitude(5.0);
// 		point6.setLatitude(1.0);
// 		point1.setLongitude(1.0);
// 		point2.setLongitude(2.0);
// 		point3.setLongitude(3.0);
// 		point4.setLongitude(4.0);
// 		point5.setLongitude(5.0);
// 		point6.setLongitude(1.0);

// 		List<Point> pointList = new ArrayList<Point>();
// 		pointList.add(point1);
// 		pointList.add(point2);
// 		pointList.add(point3);
// 		pointList.add(point4);
// 		pointList.add(point5);
// 		pointList.add(point6);

// 		byGeoPolygon.setPoints(pointList);
// 		spatialFilter.setByGeoPolygon(byGeoPolygon);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_polygon\":{\"data.Wgs84Coordinates\":{\"points\":[[1.0,1.0],[2.0,12.0],[3.0,3.0],[4.0,4.0],[5.0,5.0],[1.0,1.0]]},\"validation_method\":\"STRICT\",\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforIntersectionSpatialFilter_notUseGeoShapeQuery() throws Exception {
// 		QueryRequest queryRequest = new QueryRequest();
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
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_shape\":{\"data.Wgs84Coordinates\":{\"shape\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-8.61,1.02],[-2.48,1.02],[-2.48,10.74],[-8.61,10.74],[-8.61,1.02]]]]}]},\"relation\":\"intersects\"},\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforDistanceSpatialFilter_notUseGeoShapeQuery() throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByDistance byDistance = new SpatialFilter.ByDistance();

// 		byDistance.setDistance(10.0);
// 		byDistance.setPoint(new Point(10.0, 10.0));
// 		spatialFilter.setByDistance(byDistance);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_distance\":{\"data.Wgs84Coordinates\":[10.0,10.0],\"distance\":10.0,\"distance_type\":\"arc\",\"validation_method\":\"STRICT\",\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforGeoShapeBoundingBoxSpatialFilter_notUseGeoShapeQuery()
// 			throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByBoundingBox byBoundingBox = new SpatialFilter.ByBoundingBox();
// 		Point topLeft = new Point(0.0, 10.0);
// 		Point bottomRight = new Point(10.0, 0.0);
// 		topLeft.setLatitude(10.0);
// 		bottomRight.setLatitude(5.0);
// 		topLeft.setLongitude(10.0);
// 		bottomRight.setLongitude(5.0);
// 		byBoundingBox.setTopLeft(topLeft);
// 		byBoundingBox.setBottomRight(bottomRight);
// 		spatialFilter.setByBoundingBox(byBoundingBox);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"geo_bounding_box\":{\"data.Wgs84Coordinates\":{\"top_left\":[10.0,10.0],\"bottom_right\":[5.0,5.0]},\"validation_method\":\"STRICT\",\"type\":\"MEMORY\",\"ignore_unmapped\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQueryResponseforWithinSpatialFilter_notUseGeoShapeQuery() throws Exception {
// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByWithinPolygon byWithinPolygon = new SpatialFilter.ByWithinPolygon();
// 		Point point = new Point(1.02, -8.61);
// 		List<Point> points = new ArrayList<>();
// 		points.add(point);
// 		byWithinPolygon.setPoints(points);
// 		spatialFilter.setByWithinPolygon(byWithinPolygon);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);
// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQuery_setAggregations() throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByWithinPolygon byWithinPolygon = new SpatialFilter.ByWithinPolygon();
// 		Point point = new Point(1.02, -8.61);
// 		List<Point> points = new ArrayList<>();
// 		points.add(point);
// 		byWithinPolygon.setPoints(points);
// 		spatialFilter.setByWithinPolygon(byWithinPolygon);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

// 		Aggregations aggregations = Mockito.mock(Aggregations.class);

// 		Terms aggregation = Mockito.mock(Terms.class);

// 		when(aggregations.get(AggregationParserUtil.TERM_AGGREGATION_NAME)).thenReturn(aggregation);

// 		when(searchResponse.getAggregations()).thenReturn(aggregations);

// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_return_CorrectQuery_getTermsTwice() throws Exception {

// 		QueryRequest queryRequest = new QueryRequest();
// 		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
// 		SpatialFilter spatialFilter = new SpatialFilter();
// 		spatialFilter.setField("data.Wgs84Coordinates");
// 		SpatialFilter.ByWithinPolygon byWithinPolygon = new SpatialFilter.ByWithinPolygon();
// 		Point point = new Point(1.02, -8.61);
// 		List<Point> points = new ArrayList<>();
// 		points.add(point);
// 		byWithinPolygon.setPoints(points);
// 		spatialFilter.setByWithinPolygon(byWithinPolygon);
// 		queryRequest.setSpatialFilter(spatialFilter);

// 		// mock out elastic client handler
// 		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
// 		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

// 		ParsedNested nested1 = Mockito.mock(ParsedNested.class);
// 		ParsedNested nested2 = Mockito.mock(ParsedNested.class);

// 		Aggregations aggregations1 = Mockito.mock(Aggregations.class);
// 		Aggregations aggregations2 = Mockito.mock(Aggregations.class);
// 		Aggregations aggregations3 = Mockito.mock(Aggregations.class);

// 		Terms aggregation = Mockito.mock(Terms.class);

// 		when(aggregations3.get(AggregationParserUtil.TERM_AGGREGATION_NAME)).thenReturn(aggregation);

// 		when(nested2.getAggregations()).thenReturn(aggregations3);

// 		when(aggregations2.get(AggregationParserUtil.NESTED_AGGREGATION_NAME)).thenReturn(nested2);

// 		when(nested1.getAggregations()).thenReturn(aggregations2);

// 		when(aggregations1.get(AggregationParserUtil.NESTED_AGGREGATION_NAME)).thenReturn(nested1);

// 		when(searchResponse.getAggregations()).thenReturn(aggregations1);

// 		when(searchResponse.status())
// 				.thenReturn(RestStatus.OK);

// 		SearchHits searchHits = Mockito.mock(SearchHits.class);
// 		when(searchHits.getHits())
// 				.thenReturn(new SearchHit[] {});
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
// 		when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
// 				.thenReturn(indexedTypes);

// 		when(providerHeaderService.getDataGroupsHeader())
// 				.thenReturn("groups");

// 		Map<String, String> headers = new HashMap<>();
// 		headers.put("groups", "[]");
// 		when(dpsHeaders.getHeaders())
// 				.thenReturn(headers);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		String expectedSource = "{\"from\":0,\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]}}";

// 		// act
// 		QueryResponse response = queryServiceAws.queryIndex(queryRequest);

// 		// assert
// 		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
// 		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
// 		SearchRequest searchRequest = searchRequestArg.getValue();
// 		String actualSource = searchRequest.source().toString();
// 		assertEquals(expectedSource, actualSource);
// 	}

// 	@Test
// 	public void should_searchAll_when_requestHas_noQueryString() throws IOException {

// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
// 		BoolQueryBuilder builder = (BoolQueryBuilder) this.queryServiceAws.buildQuery(null, null, true);
// 		assertNotNull(builder);

// 		List<QueryBuilder> topLevelMustClause = builder.must();
// 		assertEquals(2, topLevelMustClause.size());

// 		verifyAcls(topLevelMustClause.get(1), true);
// 	}

// 	@Test
// 	public void should_return_ownerOnlyMustClause_when_searchAsOwners() throws IOException {

// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
// 		BoolQueryBuilder builder = (BoolQueryBuilder) this.queryServiceAws.buildQuery(null, null, false);
// 		assertNotNull(builder);

// 		List<QueryBuilder> topLevelMustClause = builder.must();
// 		assertEquals(2, topLevelMustClause.size());

// 		verifyAcls(topLevelMustClause.get(1), false);
// 	}

// 	@Test
// 	public void should_return_notNullQuery_when_searchAsDataRootUser() throws IOException {
// 		String expectedBuilder = "{\n" +
// 				"  \"prefix\" : {\n" +
// 				"    \"id\" : {\n" +
// 				"      \"value\" : \"opendes:\",\n" +
// 				"      \"boost\" : 1.0\n" +
// 				"    }\n" +
// 				"  }\n" +
// 				"}";
// 		Map<String, String> HEADERS = new HashMap<>();
// 		HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
// 		HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
// 		HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));
// 		HEADERS.put(providerHeaderService.getDataRootUserHeader(), "true");
// 		when(dpsHeaders.getHeaders()).thenReturn(HEADERS);
// 		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

// 		QueryBuilder builder = this.queryServiceAws.buildQuery(null, null, false);
// 		assertEquals(builder.toString(), expectedBuilder);
// 	}

// 	@Test
// 	public void should_parse_response_when_hightlight_is_present() throws Exception {
// 		TotalHits totalHits = new TotalHits(1, Relation.EQUAL_TO);
// 		Map<String, HighlightField> highlightFields = Stream.of(new String[][] {
// 		{"FieldName", "<em>TextValue</em>" },  
// 		}).collect(Collectors.toMap(data -> data[0], data -> new HighlightField(data[0], new Text[] { new Text(data[1])})));
// 		SearchHit searchHit = new SearchHit(42);
// 		BytesReference source = new BytesArray("{\"FieldName\""
// 			+ ":\"TextValue\"}");
// 		searchHit = searchHit.sourceRef(source);
// 		searchHit.highlightFields(highlightFields);

// 		SearchHits searchHits = new SearchHits(new SearchHit[] {searchHit}, totalHits, 2);
// 		SearchResponse mockSearchResponse = new SearchResponse(
// 			new SearchResponseSections(searchHits, null,
// 				null, false, false, null, 1), "2",
// 			5, 5, 0, 100, ShardSearchFailure.EMPTY_ARRAY,
// 			SearchResponse.Clusters.EMPTY);

// 		List<Map<String, Object>> results = this.queryServiceAws.getHitsFromSearchResponse(mockSearchResponse);
// 		assertEquals("[{highlight={FieldName=[<em>TextValue</em>]}, FieldName=TextValue}]", results.toString());
// 	}


// 	private void verifyAcls(QueryBuilder aclMustClause, boolean asOwner) {
// 		BoolQueryBuilder aclLevelBuilder = (BoolQueryBuilder) aclMustClause;
// 		assertNotNull(aclLevelBuilder);
// 		assertEquals("1", aclLevelBuilder.minimumShouldMatch());

// 		List<QueryBuilder> aclShouldClause = aclLevelBuilder.should();
// 		assertEquals(1, aclShouldClause.size());

// 		TermsQueryBuilder aclQuery = (TermsQueryBuilder) aclShouldClause.get(0);
// 		assertNotNull(aclQuery);
// 		if (asOwner) {
// 			assertEquals("acl.owners", aclQuery.fieldName());
// 		} else {
// 			assertEquals("x-acl", aclQuery.fieldName());
// 		}
// 		assertEquals(2, aclQuery.values().size());

// 		List<Object> acls = aclQuery.values();
// 		assertEquals(2, acls.size());
// 		assertTrue(acls.contains(DATA_GROUP_1));
// 		assertTrue(acls.contains(DATA_GROUP_2));
// 	}
// }
