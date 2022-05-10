/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
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
package org.opengroup.osdu.search.provider.gcp.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.core.common.model.search.SpatialFilter.ByBoundingBox;
import org.opengroup.osdu.core.common.model.search.SpatialFilter.ByDistance;
import org.opengroup.osdu.core.common.model.search.SpatialFilter.ByGeoPolygon;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.AggregationParserUtil;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.DetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IAggregationParserUtil;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.QueryParserUtil;
import org.opengroup.osdu.search.util.QueryResponseUtil;
import org.opengroup.osdu.search.util.SortParserUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchRequest.class, SearchHits.class, RestHighLevelClient.class})
public class QueryServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  private SpatialFilter spatialFilter;
  @Mock
  private ByBoundingBox byBoundingBox;
  @Mock
  private ByDistance byDistance;
  @Mock
  private ByGeoPolygon byGeoPolygon;
  @Mock
  private ElasticClientHandler elasticClientHandler;
  @Mock
  private QueryRequest searchRequest;
  @Mock
  private DpsHeaders dpsHeaders;
  @Mock
  private CrossTenantUtils crossTenantUtils;
  @Mock
  private IFieldMappingTypeService fieldMappingTypeService;
  @Mock
  private AuditLogger auditLogger;
  @Mock
  private JaxRsDpsLog log;
  @Mock
  private IProviderHeaderService providerHeaderService;
  @Mock
  private QueryResponseUtil queryResponseUtil;
  @Spy
  private SearchConfigurationProperties properties = new SearchConfigurationProperties();
  @Spy
  private IQueryParserUtil parserService = new QueryParserUtil();
  @Spy
  private ISortParserUtil sortParserUtil = new SortParserUtil();
  @Spy
  private IAggregationParserUtil aggregationParserUtil = new AggregationParserUtil(properties);
  @Spy
  private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil = new DetailedBadRequestMessageUtil(
      objectMapper);

  @Mock
  private RestHighLevelClient restHighLevelClient;

  private SearchRequest elasticSearchRequest;

  @InjectMocks
  @Spy
  private QueryServiceImpl sut = new QueryServiceImpl();

  private final String DATA_GROUPS = "X-Data-Groups";
  private final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";

  private final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";

  @Before
  public void setup() {
    initMocks(this);

    mockStatic(RestHighLevelClient.class);
    mockStatic(SearchRequest.class);
    mockStatic(SearchHits.class);

    when(properties.getEnvironment()).thenReturn("evt");
    restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);

    Map<String, String> HEADERS = new HashMap<>();
    HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
    HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
    HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));

    when(providerHeaderService.getDataGroupsHeader()).thenReturn(DATA_GROUPS);
    when(dpsHeaders.getHeaders()).thenReturn(HEADERS);
  }

  @Test
  public void should_returnSimpleQuery_when_request_queryIsSpecified() throws Exception {

    Map<String, Object> hit = new HashMap<>();
    hit.put("_id", "tenant1:welldb:wellbore-33fe05e1-df20-49d9-bd63-74cf750a206f");
    hit.put("type", "wellbore");

    List<Map<String, Object>> results = new ArrayList<>();
    results.add(hit);

    when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);

    TotalHits totalHits = new TotalHits(1, Relation.EQUAL_TO);
    SearchHits searchHits = new SearchHits(new SearchHit[0], totalHits, 2);
    SearchResponse mockSearchResponse = new SearchResponse(
        new SearchResponseSections(searchHits, null,
            null, false, false, null, 1), "2",
        5, 5, 0, 100, ShardSearchFailure.EMPTY_ARRAY,
        SearchResponse.Clusters.EMPTY);

    doReturn(mockSearchResponse).when(this.sut).makeSearchRequest(any(), any());
    when(queryResponseUtil.getQueryResponseResults(any())).thenReturn(results);
    doReturn(results).when(this.sut).getHitsFromSearchResponse(any());

    QueryResponse queryResponse = this.sut.queryIndex(searchRequest);
    assertNotNull(queryResponse);
    assertEquals(1, queryResponse.getTotalCount());
  }

  @Test
  public void should_returnRightTotalCount_when_queryResponseResultsIsNull() throws Exception {
    List<Map<String, Object>> results = null;
    when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);

    TotalHits totalHits = new TotalHits(1, Relation.EQUAL_TO);
    SearchHits searchHits = new SearchHits(new SearchHit[0], totalHits, 2);
    SearchResponse mockSearchResponse = new SearchResponse(
        new SearchResponseSections(searchHits, null,
            null, false, false, null, 1), "2",
        5, 5, 0, 100, ShardSearchFailure.EMPTY_ARRAY,
        SearchResponse.Clusters.EMPTY);

    doReturn(mockSearchResponse).when(this.sut).makeSearchRequest(any(), any());
    doReturn(results).when(this.sut).getHitsFromSearchResponse(any());

    QueryResponse queryResponse = this.sut.queryIndex(searchRequest);
    assertNotNull(queryResponse);
    assertEquals(1, queryResponse.getTotalCount());
  }

  @Test
  public void should_throwElasticException_when_indexNotFound() throws Exception {

    ElasticsearchStatusException notFound = new ElasticsearchStatusException("blah",
        RestStatus.NOT_FOUND);

    doReturn(elasticSearchRequest).when(this.sut).createElasticRequest(any());
    PowerMockito.when(restHighLevelClient.search(any(), any(RequestOptions.class)))
        .thenThrow(notFound);

    try {
      this.sut.makeSearchRequest(searchRequest, restHighLevelClient);
      fail("Should not succeed!");
    } catch (AppException e) {
      assertEquals("Resource you are trying to find does not exists", e.getError().getMessage());
    }
  }

  @Test
  public void should_throwElasticException_given_badRequest() throws Exception {

    ElasticsearchStatusException badRequest = new ElasticsearchStatusException("blah",
        RestStatus.BAD_REQUEST);

    doReturn(elasticSearchRequest).when(this.sut).createElasticRequest(any());
    when(restHighLevelClient.search(any(), any(RequestOptions.class))).thenThrow(badRequest);

    try {
      this.sut.makeSearchRequest(searchRequest, restHighLevelClient);
      fail("Should not succeed!");
    } catch (AppException e) {
      assertEquals("Invalid parameters were given on search request", e.getError().getMessage());
    }
  }

  @Test
  public void should_throwElasticException_given_searchError() throws Exception {

    ElasticsearchStatusException generic = new ElasticsearchStatusException("blah",
        RestStatus.BAD_GATEWAY);

    doReturn(elasticSearchRequest).when(this.sut).createElasticRequest(any());
    when(restHighLevelClient.search(any(), any(RequestOptions.class))).thenThrow(generic);

    try {
      this.sut.makeSearchRequest(searchRequest, restHighLevelClient);
      fail("Should not succeed!");
    } catch (AppException e) {
      assertEquals("Error processing search request", e.getError().getMessage());
    }
  }

  @Test
  public void should_throwSystemException_given_genericError() throws Exception {

    IllegalArgumentException exception = new IllegalArgumentException("search on fire");

    doReturn(elasticSearchRequest).when(this.sut).createElasticRequest(any());
    when(restHighLevelClient.search(any(), any(RequestOptions.class))).thenThrow(exception);

    try {
      this.sut.makeSearchRequest(searchRequest, restHighLevelClient);
      fail("Should not succeed!");
    } catch (AppException e) {
      assertEquals("Error processing search request", e.getError().getMessage());
    }
  }

  @Test
  public void should_throwTimeoutException_given_timeoutError() throws Exception {

    IOException exception = new IOException("listener timeout after waiting for [60000] ms");

    doReturn(elasticSearchRequest).when(this.sut).createElasticRequest(any());
    when(restHighLevelClient.search(any(), any(RequestOptions.class))).thenThrow(exception);

    try {
      this.sut.makeSearchRequest(searchRequest, restHighLevelClient);
      fail("Should not succeed!");
    } catch (AppException e) {
      assertEquals(HttpServletResponse.SC_GATEWAY_TIMEOUT, e.getError().getCode());
      assertEquals("Request timed out after waiting for 1m", e.getError().getMessage());
    }
  }

  @Test
  public void should_throwSystemException_given_genericIOError() throws Exception {

    IOException exception = new IOException("search on fire");

    doReturn(elasticSearchRequest).when(this.sut).createElasticRequest(any());
    when(restHighLevelClient.search(any(), any(RequestOptions.class))).thenThrow(exception);

    try {
      this.sut.makeSearchRequest(searchRequest, restHighLevelClient);
      fail("Should not succeed!");
    } catch (AppException e) {
      assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getError().getCode());
      assertEquals("Error processing search request", e.getError().getMessage());
    }
  }

  @Test
  public void should_return_textQuery_when_requestHasIt() throws IOException {

    String simpleQuery = "find-me";
    BoolQueryBuilder builder = (BoolQueryBuilder) this.sut.buildQuery(simpleQuery, null, true);
    assertNotNull(builder);

    List<QueryBuilder> topLevelMustClause = builder.must();
    assertEquals(2, topLevelMustClause.size());

    BoolQueryBuilder queryLevelBuilder = (BoolQueryBuilder) topLevelMustClause.get(0);
    assertNotNull(queryLevelBuilder);

    List<QueryBuilder> queryLevelMustClause = queryLevelBuilder.must();
    assertEquals(1, queryLevelMustClause.size());

    BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryLevelMustClause.get(0);
    List<QueryBuilder> must = boolQueryBuilder.must();
    QueryStringQueryBuilder queryBuilder = (QueryStringQueryBuilder) must.get(0);

    assertNotNull(boolQueryBuilder);
    assertEquals(simpleQuery, queryBuilder.queryString());

    verifyAcls(topLevelMustClause.get(1), true);
  }

  @Test
  public void should_searchAll_when_requestHas_noQueryString() throws IOException {

    BoolQueryBuilder builder = (BoolQueryBuilder) this.sut.buildQuery(null, null, true);
    assertNotNull(builder);

    List<QueryBuilder> topLevelMustClause = builder.must();
    assertEquals(1, topLevelMustClause.size());

    verifyAcls(topLevelMustClause.get(0), true);
  }

  @Test
  public void should_return_ownerOnlyMustClause_when_searchAsOwners() throws IOException {

    BoolQueryBuilder builder = (BoolQueryBuilder) this.sut.buildQuery(null, null, false);
    assertNotNull(builder);

    List<QueryBuilder> topLevelMustClause = builder.must();
    assertEquals(1, topLevelMustClause.size());

    verifyAcls(topLevelMustClause.get(0), false);
  }

  @Test(expected = AppException.class)
  public void testQueryBase_whenUnsupportedSortRequested_statusBadRequest_throwsException()
      throws IOException {
    String fieldName = "field";
    String indexName = "index";
    String dummySortError = "Text fields are not optimised for operations that require per-document field data like aggregations and sorting, so these operations are disabled by default. Please use a keyword field instead";
    ElasticsearchStatusException exception = new ElasticsearchStatusException("blah",
        RestStatus.BAD_REQUEST, new ElasticsearchException(dummySortError));

    doThrow(exception).when(restHighLevelClient).search(any(), any(RequestOptions.class));
    doReturn(new HashSet<>()).when(fieldMappingTypeService)
        .getFieldTypes(eq(restHighLevelClient), eq(fieldName), eq(indexName));
    SortQuery sortQuery = new SortQuery();
    sortQuery.setField(Collections.singletonList("name"));
    sortQuery.setOrder(Collections.singletonList(SortOrder.DESC));
    when(searchRequest.getSort()).thenReturn(sortQuery);
    when(sortParserUtil.getSortQuery(restHighLevelClient, sortQuery, indexName))
        .thenReturn(Collections.singletonList(
            new FieldSortBuilder("name").order(org.elasticsearch.search.sort.SortOrder.DESC)));

    try {
      this.sut.makeSearchRequest(searchRequest, restHighLevelClient);
    } catch (AppException e) {
      int errorCode = 400;
      String errorMessage = "Sort is not supported for one or more of the requested fields";
      assertEquals(e.getError().getCode(), errorCode);
      assertEquals(e.getError().getMessage(), errorMessage);

      throw (e);
    }
  }

  @Test
  public void should_return_boundingBoxQuery_given_spatialCriteria() throws IOException {

    String simpleQuery = "dare-find-me";

    String field = "LonLat";
    Double bottomRightLon = 124.174762;
    Double bottomRightLat = 36.450727;
    Double topLeftLon = 37.450727;
    Double topLeftLat = 70.174762;

    when(this.spatialFilter.getField()).thenReturn(field);
    when(this.spatialFilter.getByBoundingBox()).thenReturn(byBoundingBox);
    when(this.spatialFilter.getByBoundingBox().getBottomRight()).thenReturn(mock(Point.class));
    when(this.spatialFilter.getByBoundingBox().getBottomRight().getLongitude())
        .thenReturn(bottomRightLon);
    when(this.spatialFilter.getByBoundingBox().getBottomRight().getLatitude())
        .thenReturn(bottomRightLat);
    when(this.spatialFilter.getByBoundingBox().getTopLeft()).thenReturn(mock(Point.class));
    when(this.spatialFilter.getByBoundingBox().getTopLeft().getLongitude()).thenReturn(37.450727);
    when(this.spatialFilter.getByBoundingBox().getTopLeft().getLatitude()).thenReturn(70.174762);

    BoolQueryBuilder builder = (BoolQueryBuilder) this.sut
        .buildQuery(simpleQuery, this.spatialFilter, true);
    assertNotNull(builder);

    List<QueryBuilder> topLevelMustClause = builder.must();
    assertEquals(2, topLevelMustClause.size());

    BoolQueryBuilder queryLevelBuilder = (BoolQueryBuilder) topLevelMustClause.get(0);
    assertNotNull(queryLevelBuilder);

    List<QueryBuilder> queryLevelMustClause = queryLevelBuilder.must();
    assertEquals(2, queryLevelMustClause.size());

    BoolQueryBuilder queryStringBoolQueryBuilder = (BoolQueryBuilder) queryLevelMustClause.get(0);
    assertNotNull(queryStringBoolQueryBuilder);

    GeoBoundingBoxQueryBuilder geoBoundingBoxQueryBuilder = (GeoBoundingBoxQueryBuilder) queryLevelMustClause
        .get(1);
    assertNotNull(geoBoundingBoxQueryBuilder);
    assertEquals(field, geoBoundingBoxQueryBuilder.fieldName());
    assertEquals(topLeftLon, geoBoundingBoxQueryBuilder.topLeft().getLon(), .001);
    assertEquals(topLeftLat, geoBoundingBoxQueryBuilder.topLeft().getLat(), .001);
    assertEquals(bottomRightLon, geoBoundingBoxQueryBuilder.bottomRight().getLon(), .001);
    assertEquals(bottomRightLat, geoBoundingBoxQueryBuilder.bottomRight().getLat(), .001);

    verifyAcls(topLevelMustClause.get(1), true);
  }

  @Test
  public void should_return_distanceQuery_given_spatialCriteria() throws IOException {

    String simpleQuery = "oh no you found me";

    String field = "LonLat";
    Double pointLon = 124.174762;
    Double pointLat = 36.450727;
    Double distance = 10.0;

    when(this.spatialFilter.getField()).thenReturn(field);
    when(this.spatialFilter.getByDistance()).thenReturn(byDistance);
    when(this.spatialFilter.getByDistance().getPoint()).thenReturn(mock(Point.class));
    when(this.spatialFilter.getByDistance().getPoint().getLongitude()).thenReturn(pointLon);
    when(this.spatialFilter.getByDistance().getPoint().getLatitude()).thenReturn(pointLat);
    when(this.spatialFilter.getByDistance().getDistance()).thenReturn(distance);

    BoolQueryBuilder builder = (BoolQueryBuilder) this.sut
        .buildQuery(simpleQuery, this.spatialFilter, true);
    assertNotNull(builder);

    List<QueryBuilder> topLevelMustClause = builder.must();
    assertEquals(2, topLevelMustClause.size());

    BoolQueryBuilder queryLevelBuilder = (BoolQueryBuilder) topLevelMustClause.get(0);
    assertNotNull(queryLevelBuilder);

    List<QueryBuilder> queryLevelMustClause = queryLevelBuilder.must();
    assertEquals(2, queryLevelMustClause.size());

    BoolQueryBuilder queryStringBoolQueryBuilder = (BoolQueryBuilder) queryLevelMustClause.get(0);
    assertNotNull(queryStringBoolQueryBuilder);

    GeoDistanceQueryBuilder geoDistanceQueryBuilder = (GeoDistanceQueryBuilder) queryLevelMustClause
        .get(1);
    assertNotNull(geoDistanceQueryBuilder);
    assertEquals(field, geoDistanceQueryBuilder.fieldName());
    assertEquals(distance, geoDistanceQueryBuilder.distance(), .001);
    assertEquals(pointLon, geoDistanceQueryBuilder.point().getLon(), .001);
    assertEquals(pointLat, geoDistanceQueryBuilder.point().getLat(), .001);

    verifyAcls(topLevelMustClause.get(1), true);
  }

  @Test
  public void should_return_polygonBoxQuery_given_spatialCriteria() throws IOException {

    String simpleQuery = "polygons are fun";

    String field = "LonLat";
    List<Point> points = new ArrayList<>();
    points.add(new Point(124.174762, 36.450727));
    points.add(new Point(126.174762, 36.450727));
    points.add(new Point(128.174762, 46.450727));
    points.add(new Point(124.174762, 36.450727));

    when(this.spatialFilter.getField()).thenReturn(field);
    when(this.spatialFilter.getByGeoPolygon()).thenReturn(byGeoPolygon);
    when(this.spatialFilter.getByGeoPolygon().getPoints()).thenReturn(points);

    BoolQueryBuilder builder = (BoolQueryBuilder) this.sut
        .buildQuery(simpleQuery, this.spatialFilter, true);
    assertNotNull(builder);

    List<QueryBuilder> topLevelMustClause = builder.must();
    assertEquals(2, topLevelMustClause.size());

    BoolQueryBuilder queryLevelBuilder = (BoolQueryBuilder) topLevelMustClause.get(0);
    assertNotNull(queryLevelBuilder);

    List<QueryBuilder> queryLevelMustClause = queryLevelBuilder.must();
    assertEquals(2, queryLevelMustClause.size());

    BoolQueryBuilder queryStringBoolQueryBuilder = (BoolQueryBuilder) queryLevelMustClause.get(0);
    assertNotNull(queryStringBoolQueryBuilder);

    GeoPolygonQueryBuilder geoPolygonQueryBuilder = (GeoPolygonQueryBuilder) queryLevelMustClause
        .get(1);
    assertNotNull(geoPolygonQueryBuilder);
    assertEquals(field, geoPolygonQueryBuilder.fieldName());
    assertEquals(points.size(), geoPolygonQueryBuilder.points().size());
    List<GeoPoint> points1 = geoPolygonQueryBuilder.points();
    for (int i = 0; i < points1.size(); i++) {
      GeoPoint geoPoint = points1.get(i);
      Point requestPoint = points.get(i);
      assertEquals(requestPoint.getLongitude(), geoPoint.getLon(), .001);
      assertEquals(requestPoint.getLatitude(), geoPoint.getLat(), .001);
    }

    verifyAcls(topLevelMustClause.get(1), true);
  }

  @Test
  public void should_return_correctElasticRequest_given_requestQuery() throws IOException {
    int limit = 5;
    int from = 2;
    String kind = "tenant1:welldb:well:1.0.0";

    List<String> returnedFields = new ArrayList<>();
    returnedFields.add("id");
    when(searchRequest.getKind()).thenReturn(kind);
    when(searchRequest.getLimit()).thenReturn(limit);
    when(searchRequest.getFrom()).thenReturn(from);
    when(searchRequest.getReturnedFields()).thenReturn(returnedFields);

    when(crossTenantUtils.getIndexName(any())).thenReturn("tenant1-welldb-well-1.0.0,-.*");

    SearchRequest elasticRequest = this.sut.createElasticRequest(searchRequest);
    assertNotNull(elasticRequest);

    String[] indices = elasticRequest.indices();
    assertEquals(1, indices.length);
    assertEquals("tenant1-welldb-well-1.0.0,-.*", indices[0]);

    SearchSourceBuilder elasticSearchSourceBuilder = elasticRequest.source();
    assertNotNull(elasticSearchSourceBuilder);
    assertEquals(limit, elasticSearchSourceBuilder.size());
    assertEquals(from, elasticSearchSourceBuilder.from());
    assertEquals(1, elasticSearchSourceBuilder.timeout().getMinutes());

    FetchSourceContext elasticFetchSourceContext = elasticSearchSourceBuilder.fetchSource();
    assertNotNull(elasticFetchSourceContext);

    String[] elasticExcludes = elasticFetchSourceContext.excludes();
    assertEquals(2, elasticExcludes.length);
    assertEquals("x-acl", elasticExcludes[0]);
    assertEquals("index", elasticExcludes[1]);

    String[] elasticIncludes = elasticFetchSourceContext.includes();
    assertEquals(1, elasticIncludes.length);
    assertEquals("id", elasticIncludes[0]);

    QueryBuilder elasticQueryBuilder = elasticSearchSourceBuilder.query();
    assertNotNull(elasticQueryBuilder);
  }

  @Test
  public void should_return_correctElasticRequest_given_returnedFieldContainsQueryableExcludes()
      throws IOException {

    List<String> returnedFields = new ArrayList<>(Arrays.asList("id", "index"));
    when(searchRequest.getKind()).thenReturn("tenant1:welldb:well:1.0.0");
    when(searchRequest.getReturnedFields()).thenReturn(returnedFields);

    when(crossTenantUtils.getIndexName(any())).thenReturn("tenant1-welldb-well-1.0.0,-.*");

    SearchRequest elasticRequest = this.sut.createElasticRequest(searchRequest);
    assertNotNull(elasticRequest);

    String[] indices = elasticRequest.indices();
    assertEquals(1, indices.length);
    assertEquals("tenant1-welldb-well-1.0.0,-.*", indices[0]);

    SearchSourceBuilder elasticSearchSourceBuilder = elasticRequest.source();
    assertNotNull(elasticSearchSourceBuilder);

    FetchSourceContext elasticFetchSourceContext = elasticSearchSourceBuilder.fetchSource();
    assertNotNull(elasticFetchSourceContext);

    String[] elasticExcludes = elasticFetchSourceContext.excludes();
    assertEquals(1, elasticExcludes.length);
    assertEquals("x-acl", elasticExcludes[0]);

    List<String> elasticIncludes = Arrays.asList(elasticFetchSourceContext.includes());
    assertEquals(2, elasticIncludes.size());
    assertTrue(elasticIncludes.contains("index"));
    assertTrue(elasticIncludes.contains("id"));
  }

  @Test
  public void should_return_correctElasticRequest_given_noReturnedField() throws IOException {

    when(searchRequest.getKind()).thenReturn("tenant1:welldb:well:1.0.0");
    when(crossTenantUtils.getIndexName(any())).thenReturn("tenant1-welldb-well-1.0.0,-.*");

    SearchRequest elasticRequest = this.sut.createElasticRequest(searchRequest);
    assertNotNull(elasticRequest);

    String[] indices = elasticRequest.indices();
    assertEquals(1, indices.length);
    assertEquals("tenant1-welldb-well-1.0.0,-.*", indices[0]);

    SearchSourceBuilder elasticSearchSourceBuilder = elasticRequest.source();
    assertNotNull(elasticSearchSourceBuilder);

    FetchSourceContext elasticFetchSourceContext = elasticSearchSourceBuilder.fetchSource();
    assertNotNull(elasticFetchSourceContext);

    List<String> elasticExcludes = Arrays.asList(elasticFetchSourceContext.excludes());
    assertEquals(2, elasticExcludes.size());
    assertTrue(elasticExcludes.contains("index"));
    assertTrue(elasticExcludes.contains("x-acl"));

    List<String> elasticIncludes = Arrays.asList(elasticFetchSourceContext.includes());
    assertEquals(0, elasticIncludes.size());
  }

  @Test
  public void should_return_correctElasticRequest_given_groupByField() throws IOException {
    when(searchRequest.getKind()).thenReturn("tenant1:welldb:well:1.0.0");
    when(searchRequest.getAggregateBy()).thenReturn("namespace");
    when(crossTenantUtils.getIndexName(any())).thenReturn("tenant1-welldb-well-1.0.0,-.*");

    SearchRequest elasticRequest = this.sut.createElasticRequest(searchRequest);
    assertNotNull(elasticRequest);
    assertNotNull(elasticRequest.source().aggregations());
    assertEquals(1, elasticRequest.source().aggregations().count());
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

  /* arrange
   create query request according to this example query:
  	{
  		"kind": "osdu:wks:reference-data--CoordinateTransformation:1.0.0",
  			"query": "data.ID:\"EPSG::1078\"",
  			"spatialFilter": {
  			"field": "data.Wgs84Coordinates",
  			"byIntersection": {
  				"polygons": [
  				{
  					"points": [
  					{
  						"latitude": 10.75,
  							"longitude": -8.61
  					}
  						]
  				}
  				]
  			}
  		}
  	}*/
  @Test
  public void should_return_CorrectQueryResponseForIntersectionSpatialFilter() throws Exception {
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

    SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

    when(searchResponse.status()).thenReturn(RestStatus.OK);

    SearchHits searchHits = mock(SearchHits.class);
    when(searchHits.getHits()).thenReturn(new SearchHit[]{});
    when(searchResponse.getHits()).thenReturn(searchHits);

    when(restHighLevelClient.search(any(SearchRequest.class),
        eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);
    when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);

    String index = "some-index";
    when(crossTenantUtils.getIndexName(any())).thenReturn(index);

    Set<String> indexedTypes = new HashSet<>();
    indexedTypes.add("geo_shape");
    when(fieldMappingTypeService.getFieldTypes(eq(restHighLevelClient), anyString(),
        eq(index))).thenReturn(indexedTypes);

    when(providerHeaderService.getDataGroupsHeader()).thenReturn("groups");

    Map<String, String> headers = new HashMap<>();
    headers.put("groups", "[]");
    when(dpsHeaders.getHeaders()).thenReturn(headers);

    String expectedSource = toString(getContent("1.json"));

    QueryResponse response = sut.queryIndex(queryRequest);

    ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
    verify(restHighLevelClient, times(1)).search(searchRequestArg.capture(), any());
    SearchRequest searchRequest = searchRequestArg.getValue();
    String actualSource = searchRequest.source().toString();
    assertEquals(expectedSource, actualSource);
  }

  /* arrange
     create query request according to this example query:
        {
            "kind": "osdu:wks:reference-data--CoordinateTransformation:1.0.1",
            "query": "data.ID:\"EPSG::blahblah8\"",
            "spatialFilter": {
              "field": "data.SpatialLocation.Wgs84Coordinates",
                  "byWithinPolygon": {
                "points": [
                {
                  "latitude": 10.71,
                    "longitude": -8.60
                }
                  ]
              }

            }
        }*/
  @Test
  public void should_return_CorrectQueryResponseForWithinSpatialFilter() throws Exception {
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

    SearchResponse searchResponse = mock(SearchResponse.class);
    when(searchResponse.status()).thenReturn(RestStatus.OK);

    SearchHits searchHits = mock(SearchHits.class);
    when(searchHits.getHits()).thenReturn(new SearchHit[]{});
    when(searchResponse.getHits()).thenReturn(searchHits);

    when(restHighLevelClient.search(any(SearchRequest.class),
        eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);
    when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
    when(restHighLevelClient.search(any(SearchRequest.class),
        eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);
    when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);

    String index = "some-index";
    when(crossTenantUtils.getIndexName(any())).thenReturn(index);

    Set<String> indexedTypes = new HashSet<>();
    indexedTypes.add("geo_shape");

    when(fieldMappingTypeService.getFieldTypes(eq(restHighLevelClient), anyString(),
        eq(index))).thenReturn(indexedTypes);
    when(providerHeaderService.getDataGroupsHeader()).thenReturn("groups");

    Map<String, String> headers = new HashMap<>();
    headers.put("groups", "[]");
    when(dpsHeaders.getHeaders()).thenReturn(headers);

    String expectedSource = toString(getContent("2.json"));

    QueryResponse response = sut.queryIndex(queryRequest);

    ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
    Mockito.verify(restHighLevelClient, times(1)).search(searchRequestArg.capture(), any());
    SearchRequest searchRequest = searchRequestArg.getValue();
    String actualSource = searchRequest.source().toString();
    assertEquals(expectedSource, actualSource);
  }

  private InputStream getContent(String fileName) {
    return this.getClass().getResourceAsStream("/elasticresponses/spatialfilters/" + fileName);
  }

  private String toString(InputStream inputStream) {
    return new BufferedReader(new InputStreamReader(inputStream)).lines()
        .collect(Collectors.joining("")).replace(" ", "");
  }
}
