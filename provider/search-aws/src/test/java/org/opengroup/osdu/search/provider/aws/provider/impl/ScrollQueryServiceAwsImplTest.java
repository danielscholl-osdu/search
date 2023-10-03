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

package org.opengroup.osdu.search.provider.aws.provider.impl;

import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.policy.service.PartitionPolicyStatusService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.GeoQueryBuilder;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.ResponseExceptionParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import static org.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScrollQueryServiceAwsImplTest {

    private final String DATA_GROUPS = "X-Data-Groups";
	private final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";
	private final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";
	private final String PARTITION_ID = "opendes";
	private final String TOO_MANY_SCROLL = "Trying to create too many scroll contexts. Must be less than or equal to:";

    @InjectMocks
	ScrollQueryServiceAwsImpl scrollQueryServiceAws;

    @Mock
	private ElasticClientHandler elasticClientHandler;

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

    @Mock
    private CursorCache cursorCache;

	@Mock 
	private ResponseExceptionParser exceptionParser;

    @Mock
    private GeoQueryBuilder geoQueryBuilder;

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
	public void should_return_CorrectQueryResponse_NullResult_noCursorSet() throws Exception {
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
		CursorQueryRequest queryRequest = new CursorQueryRequest();
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
		//when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
		//		.thenReturn(indexedTypes);

		when(providerHeaderService.getDataGroupsHeader())
				.thenReturn("groups");

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");
		when(dpsHeaders.getHeaders())
				.thenReturn(headers);
		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

		String expectedSource = "{\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]},\"highlight\":{}}";

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);

		// assert
		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
		SearchRequest searchRequest = searchRequestArg.getValue();
		String actualSource = searchRequest.source().toString();
		assertEquals(expectedSource, actualSource);
	}

	@Test
	public void should_return_CorrectQueryResponse_CursorNotSet() throws Exception {
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

		CursorQueryRequest queryRequest = new CursorQueryRequest();
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

        SearchHit[] searchHitArray = new SearchHit[1];

        SearchHit searchHit = Mockito.mock(SearchHit.class);

        HighlightField hf = Mockito.mock(HighlightField.class);

        Map<String, HighlightField> hfMap = new HashMap<String, HighlightField>();

        Text[] fragments = new Text[1];

        fragments[0] = new Text("text");

        when(hf.getFragments()).thenReturn(fragments);
        
        when(hf.getName()).thenReturn("x-acll");

        hfMap.put("hf", hf);

        when(searchHit.getHighlightFields()).thenReturn(hfMap);

        when(searchHit.getSourceAsMap()).thenReturn(new HashMap<String, Object>());
        
        searchHitArray[0] = searchHit;

        SearchHits searchHits = new SearchHits(searchHitArray, new TotalHits(1, Relation.EQUAL_TO), (float) 0);

		when(searchResponse.getHits()).thenReturn(searchHits);
		when(searchResponse.getScrollId()).thenReturn("ScrollId");

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);

		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Set<String> indexedTypes = new HashSet<>();
		indexedTypes.add("geo_shape");
		//when(fieldMappingTypeService.getFieldTypes(Mockito.eq(client), Mockito.anyString(), Mockito.eq(index)))
		//		.thenReturn(indexedTypes);

		when(providerHeaderService.getDataGroupsHeader())
				.thenReturn("groups");

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");
		when(dpsHeaders.getHeaders())
				.thenReturn(headers);
		when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);

		String expectedSource = "{\"size\":10,\"timeout\":\"1m\",\"query\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"prefix\":{\"id\":{\"value\":\"opendes:\",\"boost\":1.0}}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"should\":[{\"terms\":{\"x-acl\":[\"[]\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"minimum_should_match\":\"1\",\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[],\"excludes\":[\"x-acl\",\"index\"]},\"highlight\":{}}";

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);

		// assert
		ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
		Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), Mockito.any());
		SearchRequest searchRequest = searchRequestArg.getValue();
		String actualSource = searchRequest.source().toString();
		assertEquals(expectedSource, actualSource);
	}





	@Test(expected = AppException.class)
	public void should_throw_AppException_tooManyScroll() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		queryRequest.setSpatialFilter(spatialFilter);

		// mock out elastic client handler
		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);



		when(queryParserUtil.buildQueryBuilderFromQueryString(any())).thenThrow(new AppException(400, TOO_MANY_SCROLL, "message"));

		List<String> list = new ArrayList<String>();
		list.add(TOO_MANY_SCROLL);

		when(exceptionParser.parseException(any(AppException.class))).thenReturn(list);

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);

		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}



	@Test(expected = AppException.class)
	public void should_throw_AppException_ElasticsearchStatusException_NOT_FOUND() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		queryRequest.setSpatialFilter(spatialFilter);

		// mock out elastic client handler
		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

		when(queryParserUtil.buildQueryBuilderFromQueryString(any())).thenThrow(new ElasticsearchStatusException("MSG", RestStatus.NOT_FOUND, null, null));

		List<String> list = new ArrayList<String>();
		list.add(TOO_MANY_SCROLL);

		when(exceptionParser.parseException(any(AppException.class))).thenReturn(list);

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);

		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}

	@Test(expected = AppException.class)
	public void should_throw_AppException_ElasticsearchStatusException_BAD_REQUEST() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		queryRequest.setSpatialFilter(spatialFilter);

		// mock out elastic client handler
		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

		when(queryParserUtil.buildQueryBuilderFromQueryString(any())).thenThrow(new ElasticsearchStatusException("MSG", RestStatus.BAD_REQUEST, null, null));

		List<String> list = new ArrayList<String>();
		list.add(TOO_MANY_SCROLL);

		when(exceptionParser.parseException(any(AppException.class))).thenReturn(list);

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);

		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}


	@Test(expected = AppException.class)
	public void should_throw_AppException_ElasticsearchStatusException_SERVICE_UNAVAILABLE() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		queryRequest.setSpatialFilter(spatialFilter);

		// mock out elastic client handler
		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

		when(queryParserUtil.buildQueryBuilderFromQueryString(any())).thenThrow(new ElasticsearchStatusException("MSG", RestStatus.SERVICE_UNAVAILABLE, null, null));

		List<String> list = new ArrayList<String>();
		list.add(TOO_MANY_SCROLL);

		when(exceptionParser.parseException(any(AppException.class))).thenReturn(list);

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);

		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}

	@Test(expected = AppException.class)
	public void should_throw_AppException_ElasticsearchStatusException_DEFAULT() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		queryRequest.setSpatialFilter(spatialFilter);

		// mock out elastic client handler
		RestHighLevelClient client = Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEEP_STUBS);
		SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

		when(queryParserUtil.buildQueryBuilderFromQueryString(any())).thenThrow(new ElasticsearchStatusException("MSG", RestStatus.FORBIDDEN, null, null));

		List<String> list = new ArrayList<String>();
		list.add(TOO_MANY_SCROLL);

		when(exceptionParser.parseException(any(AppException.class))).thenReturn(list);

		when(client.search(Mockito.any(SearchRequest.class), Mockito.eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);

		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		String index = "some-index";
		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}

	@Test(expected = AppException.class)
	public void should_throw_AppException_InternalAppException() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();
		queryRequest.setQuery("data.ID:\"EPSG::1078\"");
		SpatialFilter spatialFilter = new SpatialFilter();
		queryRequest.setSpatialFilter(spatialFilter);

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}

    @Test(expected = AppException.class)
	public void should_throw_AppException_forInvalidCursor() throws Exception {
		CursorQueryRequest queryRequest = new CursorQueryRequest();
        queryRequest.setCursor("Invalid");

		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}

    @Test(expected = AppException.class)
	public void should_throw_AppException_forNullCursorSettings() throws Exception {
		CursorQueryRequest queryRequest = new CursorQueryRequest();
        queryRequest.setCursor("Invalid");

		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);

	}

    @Test(expected = AppException.class)
	public void should_throw_AppException_forDIfferentUserEmail() throws Exception {
		CursorQueryRequest queryRequest = new CursorQueryRequest();
        queryRequest.setCursor("Invalid");

        when(cursorCache.get(anyString())).thenReturn(new CursorSettings("String1", "String2"));

        when(dpsHeaders.getUserEmail()).thenReturn("String1");

		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);

	}



    @Test
	public void should_return_CorrectQueryResponse_CursorSet() throws Exception {
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
		CursorQueryRequest queryRequest = new CursorQueryRequest();

        queryRequest.setCursor("Valid");

        when(cursorCache.get(anyString())).thenReturn(new CursorSettings("String1", "String2"));

        when(dpsHeaders.getUserEmail()).thenReturn("String2");


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

        SearchHit[] searchHitArray = new SearchHit[1];

        SearchHit searchHit = Mockito.mock(SearchHit.class);

        HighlightField hf = Mockito.mock(HighlightField.class);

        Map<String, HighlightField> hfMap = new HashMap<String, HighlightField>();

        Text[] fragments = new Text[1];

        fragments[0] = new Text("text");

        when(hf.getFragments()).thenReturn(fragments);
        
        when(hf.getName()).thenReturn("x-acll");

        hfMap.put("hf", hf);

        when(searchHit.getHighlightFields()).thenReturn(hfMap);

        when(searchHit.getSourceAsMap()).thenReturn(new HashMap<String, Object>());
        
        searchHitArray[0] = searchHit;

        SearchHits searchHits = new SearchHits(searchHitArray, new TotalHits(1, Relation.EQUAL_TO), (float) 0);

		when(searchResponse.getHits()).thenReturn(searchHits);

		when(client.scroll(Mockito.any(SearchScrollRequest.class), Mockito.eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);


		when(elasticClientHandler.createRestClient())
				.thenReturn(client);

		Map<String, String> headers = new HashMap<>();
		headers.put("groups", "[]");

		String expectedSource = "SearchScrollRequest{scrollId='String1', scroll=Scroll{keepAlive=1.5m}}";

		// act
		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);

		// assert
		ArgumentCaptor<SearchScrollRequest> searchRequestArg = ArgumentCaptor.forClass(SearchScrollRequest.class);
		Mockito.verify(client, Mockito.times(1)).scroll(searchRequestArg.capture(), Mockito.any());
		SearchScrollRequest searchRequest = searchRequestArg.getValue();
		String actualSource = searchRequest.toString();
		assertEquals(expectedSource, actualSource);
	}

	@Test(expected = AppException.class)
	public void should_throw_AppException_For_ElasticsearchStatusException_CursorSet() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();

        queryRequest.setCursor("Valid");

        when(cursorCache.get(anyString())).thenThrow(new ElasticsearchStatusException("No search context found for id", NOT_FOUND, null));

		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}

	@Test(expected = AppException.class)
	public void should_throw_AppException_For_CommonException_CursorSet() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();

        queryRequest.setCursor("Valid");

        when(cursorCache.get(anyString())).thenThrow(new NullPointerException());

		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}

	@Test(expected = AppException.class)
	public void should_throw_sameAppException_For_AppException_CursorSet() throws Exception {

		CursorQueryRequest queryRequest = new CursorQueryRequest();

        queryRequest.setCursor("Valid");

        when(cursorCache.get(anyString())).thenThrow(new AppException(400, "something", "something"));

		CursorQueryResponse response = scrollQueryServiceAws.queryIndex(queryRequest);
	}
}
