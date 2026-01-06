/*
 *  Copyright © Amazon Web Services
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.provider.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.POLICY_FEATURE_NAME;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.JsonData;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.model.QueryNode;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.GeoQueryBuilder;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.IQueryPerformanceLogger;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.ResponseExceptionParser;
import org.opengroup.osdu.search.util.SuggestionsQueryUtil;

@ExtendWith(MockitoExtension.class)
public class ScrollCoreQueryServiceImplIPolicyServiceTest {

  @InjectMocks
  private ScrollCoreQueryServiceImpl scrollQueryService;

  @Mock
  private ElasticClientHandler elasticClientHandler;

  @Mock
  private JaxRsDpsLog log;

  @Mock
  private CrossTenantUtils crossTenantUtils;

  @Mock
  private IFieldMappingTypeService fieldMappingTypeService;

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
  private IPolicyService iPolicyService;

  @Mock
  private GeoQueryBuilder geoQueryBuilder;

  @Mock
  private ElasticLoggingConfig elasticLoggingConfig;

  @Mock
  private IQueryPerformanceLogger searchDependencyLogger;

  @Mock
  private SuggestionsQueryUtil suggestionsQueryUtil;

  @Mock
  public IFeatureFlag iFeatureFlag;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

	lenient().when(iFeatureFlag.isFeatureEnabled(anyString())).thenReturn(false);

	when(elasticLoggingConfig.getEnabled()).thenReturn(false);
    when(elasticLoggingConfig.getThreshold()).thenReturn(200L);
  }

	@Test
	public void should_return_CorrectQueryResponse_NullResult_noCursorSet_QueryBuilder() throws Exception {
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
        ElasticsearchClient client = Mockito.mock(ElasticsearchClient.class);
		String index = "some-index";

		SearchResponse<Object> searchResponse = SearchResponse.of(r -> r
				.took(0)
				.timedOut(false)
				.hits(h -> h
						.total(t -> t.value(1).relation(TotalHitsRelation.Eq))
						.hits(hit -> hit
								.id("1234")
								.index(index)
								.fields(Map.of("hostname", JsonData.of("H")
				))))
				.shards(s -> s
						.total(1)
						.failed(0)
						.successful(1)
				)
		);

		List<QueryNode> queryNodes = Collections.singletonList(new QueryNode(queryRequest.getQuery(), null));
        BoolQuery.Builder textQueryBuilder = new BoolQuery.Builder();
		textQueryBuilder.must(queryNodes.get(0).toQueryBuilder().build());

		when(iPolicyService.getCompiledPolicy()).thenReturn("PolicyString");
		when(iFeatureFlag.isFeatureEnabled(POLICY_FEATURE_NAME)).thenReturn(true);
		when(queryParserUtil.buildQueryBuilderFromQueryString(anyString())).thenReturn(textQueryBuilder);
		when(elasticClientHandler.getOrCreateRestClient())
				.thenReturn(client);
		when(client.search(any(SearchRequest.class), eq((Type)Map.class))).thenReturn(searchResponse);

		when(crossTenantUtils.getIndexName(Mockito.any()))
				.thenReturn(index);

    String jsonString =
        "{\"_source\":{\"excludes\":[\"x-acl\",\"index\"],\"includes\":[]},\"highlight\":{\"fields\":{}},\"query\":{\"bool\":{\"boost\":1.0, \"must\":[{\"bool\":{\"must\":[{\"query_string\":{\"boost\":1.0,\"allow_leading_wildcard\":false,\"auto_generate_synonyms_phrase_query\":true,\"default_operator\":\"or\",\"enable_position_increments\":true,\"escape\":false,\"fields\":[],\"fuzziness\":\"AUTO\",\"fuzzy_max_expansions\":50,\"fuzzy_prefix_length\":0,\"fuzzy_transpositions\":true,\"max_determinized_states\":10000,\"phrase_slop\":0.0,\"query\":\"data.ID:\\\"EPSG::1078\\\"\",\"type\":\"best_fields\"}}]}},{\"wrapper\":{\"query\":\"UG9saWN5U3RyaW5n\"}}]}},\"size\":10,\"sort\":[{\"_score\":{\"order\":\"desc\"}},{\"_doc\":{\"order\":\"asc\"}}],\"timeout\":\"1m\"}";
		SearchRequest searchRequest = SearchRequest.of(s -> s
				.withJson(new StringReader(jsonString))
		);

		// act
		scrollQueryService.queryIndex(queryRequest);

		// assert
		ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

		verify(client, Mockito.times(1)).search(searchRequestCaptor.capture(), eq((Type)Map.class));
		SearchRequest capturedRequest = searchRequestCaptor.getValue();

        assert capturedRequest.query() != null;
        String actualSource = capturedRequest.query().toString();
        assert searchRequest.query() != null;
        assertEquals(searchRequest.query().toString(), actualSource);
	}
}
