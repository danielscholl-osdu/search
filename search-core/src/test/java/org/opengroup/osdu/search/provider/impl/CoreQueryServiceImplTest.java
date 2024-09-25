//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.provider.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.ContentTooLongException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.CollaborationContextFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.CollaborationContext;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.opengroup.osdu.search.util.AggregationParserUtil;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.DetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.GeoQueryBuilder;
import org.opengroup.osdu.search.util.IAggregationParserUtil;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryPerformanceLogger;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.QueryParserUtil;
import org.opengroup.osdu.search.util.SortParserUtil;
import org.opengroup.osdu.search.util.SuggestionsQueryUtil;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CoreQueryServiceImplTest {

    private static final String COLLABORATIONS_FEATURE_NAME = "collaborations-enabled";
    private static final String COLLABORATION_ID = "c3cc62d5-0ff6-4931-b4e8-51e5e72667fc";
    private static final String COLLABORATION_APPLICATION = "pws";
    private final String DATA_GROUPS = "X-Data-Groups";
    private final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";
    private final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";

    private static final String dataPartitionId = "data-partition-id";
    private static final String fieldName = "field";
    private static final String indexName = "index";
    private static final String name = "name";
    private static final String text = "text";
    private static final String GEO_SHAPE = "geo_shape";
    private static final String GEO_DISTANCE = "geo_distance";
    private static final String GEO_POLYGON = "geo_polygon";
    private static final double DELTA = 1e-6;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Point dummyPoint = getPoint(0.0, 0.0);
    private final List<Point> polygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0));
    private final List<Point> closedPolygonPoints = getPolygonPoints(getPoint(0.0, 0.0), getPoint(0.0, 1.0), getPoint(1.0, 1.0), getPoint(1.0, 0.0), getPoint(0.0, 0.0));
    private final Point topLeft = getPoint(3.0, 4.0);
    private final Point bottomRight = getPoint(2.0, 1.0);

    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private QueryRequest searchRequest;

    @Mock
    private ElasticsearchClient client;

    @Mock
    private SpatialFilter spatialFilter;

    @Mock
    private SearchResponse<Map<String, Object>> searchResponse;

    @Mock
    private HitsMetadata<Map<String, Object>> searchHits;

    @Mock
    private Hit<Map<String, Object>>  searchHit;

    @Mock
    private ElasticClientHandler elasticClientHandler;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    DpsHeaders dpsHeaders;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private CrossTenantUtils crossTenantUtils;

    @Mock
    private IProviderHeaderService providerHeaderService;

    @Spy
    private SearchConfigurationProperties properties = new SearchConfigurationProperties();

    @Spy
    private IQueryParserUtil parserService = new QueryParserUtil();

    @Spy
    private IFeatureFlag autocompleteFeatureFlag;
    
    @Mock
    private SuggestionsQueryUtil suggestionsQueryUtil;

    @Mock
    private IFieldMappingTypeService fieldMappingTypeService;

    @Spy
    private ISortParserUtil sortParserUtil = new SortParserUtil(fieldMappingTypeService, parserService);

    @Spy
    private IAggregationParserUtil aggregationParserUtil = new AggregationParserUtil(properties);

    @Spy
    private IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil = new DetailedBadRequestMessageUtil(objectMapper);

    @Mock
    private ElasticLoggingConfig elasticLoggingConfig;

    @Mock
    private IQueryPerformanceLogger searchDependencyLogger;

    @Spy
    private GeoQueryBuilder geoQueryBuilder = new GeoQueryBuilder();
    @Mock
    private CollaborationContextFactory collaborationContextFactory;

    @InjectMocks
    private CoreQueryServiceImpl sut;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.openMocks(this);
        Map<String, Object> hitFields = new HashMap<>();

        doReturn(indexName).when(crossTenantUtils).getIndexName(any());
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();
        doReturn(spatialFilter).when(searchRequest).getSpatialFilter();
        when(elasticLoggingConfig.getEnabled()).thenReturn(false);
        when(elasticLoggingConfig.getThreshold()).thenReturn(200L);
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hitFields).when(searchHit).source();

        Map<String, String> HEADERS = new HashMap<>();
        HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
        HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
        HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));

        when(providerHeaderService.getDataGroupsHeader()).thenReturn(DATA_GROUPS);
        when(dpsHeaders.getHeaders()).thenReturn(HEADERS);

        ReflectionTestUtils.setField(suggestionsQueryUtil, "autocompleteFeatureFlag", autocompleteFeatureFlag);
    }

    @Test
    public void testQueryBase_whenSearchHitsIsEmpty() throws IOException {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();

        doReturn(hits).when(searchHits).hits();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    public void testQueryBase_whenSearchHitsIsNotEmpty() throws IOException {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);

        Map<String, List<String>> highlightFields = getHighlightFields();
        doReturn(hits).when(searchHits).hits();
        doReturn(highlightFields).when(searchHit).highlight();

        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        assertEquals(queryResponse.getResults().size(), 1);
        assertTrue(queryResponse.getResults().get(0).keySet().contains("highlight"));
        assertEquals(((Map<String, List<String>>)queryResponse.getResults().get(0).get("highlight")).get(name), List.of(text));

        verify(this.auditLogger, times(1)).queryIndexSuccess(Lists.newArrayList(searchRequest.toString()));
        verify(this.searchDependencyLogger, times(1)).log(eq(searchRequest), anyLong(), eq(200));
    }

    @Test
    public void testQueryBase_useGeoShapeQueryIsFalse_getByBoundingBox() throws IOException {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        SpatialFilter.ByBoundingBox boundingBox = getValidBoundingBox();

        doReturn(fieldName).when(spatialFilter).getField();
        doReturn(boundingBox).when(spatialFilter).getByBoundingBox();
        doReturn(hits).when(searchHits).hits();
        String jsonString = """
                {
                     "_source": {
                         "excludes": ["x-acl", "index"],
                         "includes": []
                     },
                     "from": 0,
                     "highlight": {
                         "fields": {}
                     },
                     "query": {
                         "bool": {
                             "filter": [{
                                     "geo_shape": {
                                         "field": {
                                             "shape": "{coordinates=[[4.0, 3.0], [1.0, 2.0]], type=Envelope}",
                                             "relation": "within"
                                         },
                                         "boost": 1.0,
                                         "ignore_unmapped": true
                                     }
                                 }, {
                                     "terms": {
                                         "x-acl": ["data.welldb.viewers@common.evd.cloud.slb-ds.com", "data.npd.viewers@common.evd.cloud.slb-ds.com"]
                                     ,"boost": 1.0}
                                 }
                             ]
                         }
                     },
                     "size": 10,
                     "timeout": "1m"
                }
                """;
        SearchRequest expectedSearchRequest = SearchRequest.of(s -> s.withJson(new StringReader(jsonString)));

        // act
        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        // assert
        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(elasticSearchRequest.capture(), eq((Type)Map.class));
        SearchRequest capaturedSearchRequest = elasticSearchRequest.getValue();
        assertNotNull(expectedSearchRequest.query());
        assertNotNull(capaturedSearchRequest.query());
        assertEquals(expectedSearchRequest.query().toString(), capaturedSearchRequest.query().toString());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    public void testQueryBase_useGeoShapeQueryIsTrue_getByBoundingBox() throws IOException {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        SpatialFilter.ByBoundingBox boundingBox = getValidBoundingBox();

        doReturn(fieldName).when(spatialFilter).getField();
        doReturn(boundingBox).when(spatialFilter).getByBoundingBox();
        doReturn(hits).when(searchHits).hits();
        String jsonString = """
                {
                      "_source": {
                          "excludes": ["x-acl", "index"],
                          "includes": []
                      },
                      "from": 0,
                      "highlight": {
                          "fields": {}
                      },
                      "query": {
                          "bool": {
                              "filter": [{
                                      "geo_shape": {
                                          "field": {
                                              "shape": "{coordinates=[[4.0, 3.0], [1.0, 2.0]], type=Envelope}",
                                              "relation": "within"
                                          },
                                          "boost": 1.0,
                                          "ignore_unmapped": true
                                      }
                                  }, {
                                      "terms": {
                                          "x-acl": ["data.welldb.viewers@common.evd.cloud.slb-ds.com", "data.npd.viewers@common.evd.cloud.slb-ds.com"]
                                      ,"boost": 1.0}
                                  }
                              ]
                          }
                      },
                      "size": 10,
                      "timeout": "1m"
                }  
                """;
        SearchRequest expectedSearchRequest = SearchRequest.of(s -> s.withJson(new StringReader(jsonString)));

        // act
        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        // assert
        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(elasticSearchRequest.capture(), eq((Type)Map.class));
        SearchRequest capaturedSearchRequest = elasticSearchRequest.getValue();
        assertNotNull(expectedSearchRequest.query());
        assertNotNull(capaturedSearchRequest.query());
        assertEquals(expectedSearchRequest.query().toString(), capaturedSearchRequest.query().toString());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    public void testQueryBase_useGeoShapeQueryIsFalse_getByDistance() throws IOException {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        SpatialFilter.ByDistance distance = getDistance(1.0, dummyPoint);

        doReturn(fieldName).when(spatialFilter).getField();
        doReturn(distance).when(spatialFilter).getByDistance();
        doReturn(hits).when(searchHits).hits();
        String jsonString = """
                {
                   "_source": {
                       "excludes": ["x-acl", "index"],
                       "includes": []
                   },
                   "from": 0,
                   "highlight": {
                       "fields": {}
                   },
                   "query": {
                       "bool": {
                           "filter": [{
                                   "geo_shape": {
                                       "field": {
                                           "shape": "{coordinates=[0.0, 0.0], type=Circle, radius=1.0m}",
                                           "relation": "within"
                                       },
                                       "boost": 1.0,
                                       "ignore_unmapped": true
                                   }
                               }, {
                                   "terms": {
                                       "x-acl": ["data.welldb.viewers@common.evd.cloud.slb-ds.com", "data.npd.viewers@common.evd.cloud.slb-ds.com"],
                                       "boost" : 1.0
                                   }
                               }
                           ]
                       }
                   },
                   "size": 10,
                   "timeout": "1m"
                }
                """;
        SearchRequest expectedSearchRequest = SearchRequest.of(s -> s.withJson(new StringReader(jsonString)));

        // act
        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        // assert
        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(elasticSearchRequest.capture(), eq((Type)Map.class));
        SearchRequest capaturedSearchRequest = elasticSearchRequest.getValue();
        assertNotNull(expectedSearchRequest.query());
        assertNotNull(capaturedSearchRequest.query());
        assertEquals(expectedSearchRequest.query().toString(), capaturedSearchRequest.query().toString());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    public void testQueryBase_useGeoShapeQueryIsFalse_getByGeoPolygon() throws IOException {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        SpatialFilter.ByGeoPolygon geoPolygon = new SpatialFilter.ByGeoPolygon(polygonPoints);

        doReturn(fieldName).when(spatialFilter).getField();
        doReturn(geoPolygon).when(spatialFilter).getByGeoPolygon();
        doReturn(hits).when(searchHits).hits();
        String jsonString = """
                {
                    "_source": {
                        "excludes": ["x-acl", "index"],
                        "includes": []
                    },
                    "from": 0,
                    "highlight": {
                        "fields": {}
                    },
                    "query": {
                        "bool": {
                            "filter": [{
                                    "geo_shape": {
                                        "field": {
                                            "shape": "{coordinates=[[[0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 1.0], [0.0, 0.0]]], type=Polygon}",
                                            "relation": "within"
                                        },
                                        "boost": 1.0,
                                        "ignore_unmapped": true
                                    }
                                }, {
                                    "terms": {
                                        "x-acl": ["data.welldb.viewers@common.evd.cloud.slb-ds.com", "data.npd.viewers@common.evd.cloud.slb-ds.com"],
                                        "boost": 1.0
                                    }
                                }
                            ]
                        }
                    },
                    "size": 10,
                    "timeout": "1m"
                }             
                """;
        SearchRequest expectedSearchRequest = SearchRequest.of(s -> s.withJson(new StringReader(jsonString)));

        // act
        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        // assert
        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(elasticSearchRequest.capture(), eq((Type)Map.class));
        SearchRequest capaturedSearchRequest = elasticSearchRequest.getValue();
        assertNotNull(expectedSearchRequest.query());
        assertNotNull(capaturedSearchRequest.query());
        assertEquals(expectedSearchRequest.query().toString(), capaturedSearchRequest.query().toString());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test
    public void testQueryBase_useGeoShapeQueryIsTrue_getByGeoPolygon() throws IOException {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        SpatialFilter.ByGeoPolygon geoPolygon = getGeoPolygon(closedPolygonPoints);

        doReturn(fieldName).when(spatialFilter).getField();
        doReturn(geoPolygon).when(spatialFilter).getByGeoPolygon();
        doReturn(hits).when(searchHits).hits();
        String jsonString = """
                {
                    "_source": {
                        "excludes": ["x-acl", "index"],
                        "includes": []
                    },
                    "from": 0,
                    "highlight": {
                        "fields": {}
                    },
                    "query": {
                        "bool": {
                            "filter": [{
                                    "geo_shape": {
                                        "field": {
                                            "shape": "{coordinates=[[[0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 1.0], [0.0, 0.0]]], type=Polygon}",
                                            "relation": "within"
                                        },
                                        "boost": 1.0,
                                        "ignore_unmapped": true
                                    }
                                }, {
                                    "terms": {
                                        "x-acl": ["data.welldb.viewers@common.evd.cloud.slb-ds.com", "data.npd.viewers@common.evd.cloud.slb-ds.com"]
                                    ,"boost": 1.0}
                                }
                            ]
                        }
                    },
                    "size": 10,
                    "timeout": "1m"
                }          
                """;
        SearchRequest expectedSearchRequest = SearchRequest.of(s -> s.withJson(new StringReader(jsonString)));

        // act
        QueryResponse queryResponse = sut.queryIndex(searchRequest);

        // assert
        ArgumentCaptor<SearchRequest> elasticSearchRequest = ArgumentCaptor.forClass(SearchRequest.class);

        verify(client).search(elasticSearchRequest.capture(), eq((Type)Map.class));
        SearchRequest capaturedSearchRequest = elasticSearchRequest.getValue();
        assertNotNull(expectedSearchRequest.query());
        assertNotNull(capaturedSearchRequest.query());
        assertEquals(expectedSearchRequest.query().toString(), capaturedSearchRequest.query().toString());
        assertEquals(queryResponse.getResults().size(), 0);
        assertEquals(queryResponse.getAggregations().size(), 0);
        assertEquals(queryResponse.getTotalCount(), 0);
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusNotFound_throwsException() throws IOException {
        ElasticsearchException exception = mock(ElasticsearchException.class);

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(HttpServletResponse.SC_NOT_FOUND).when(exception).status();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 404;
            String errorMessage = "Resource you are trying to find does not exists";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusBadRequest_throwsException() throws IOException {
        ElasticsearchException exception = mock(ElasticsearchException.class);

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(HttpServletResponse.SC_BAD_REQUEST).when(exception).status();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 400;
            String errorMessage = "Invalid parameters were given on search request";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenUnsupportedSortRequested_statusBadRequest_throwsException() throws IOException {
        String dummySortError = "Text fields are not optimised for operations that require per-document field data like aggregations and sorting, so these operations are disabled by default. Please use a keyword field instead";
        ErrorResponse errorResponse = ErrorResponse.of(es -> es.status(400).error(ErrorCause.of(ec -> ec.causedBy(by -> by.type("illegal_argument_exception").reason(dummySortError)))));
        ElasticsearchException exception = new ElasticsearchException("blah", errorResponse);

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        SortQuery sortQuery = new SortQuery();
        sortQuery.setField(Collections.singletonList("data.name"));
        sortQuery.setOrder(Collections.singletonList(SortOrder.DESC));
        when(searchRequest.getSort()).thenReturn(sortQuery);
        doReturn(Collections.singletonList(
                SortOptions.of(so -> so.field(
                        FieldSort.of(fs -> fs.field("data.name").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))))
                ))
                .when(sortParserUtil).getSortQuery(eq(client), eq(sortQuery), eq(indexName));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 400;
            String errorMessage = "Sort is not supported for one or more of the requested fields";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusServiceUnavailable_throwsException() throws IOException {
        ElasticsearchException exception = mock(ElasticsearchException.class);

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(HttpServletResponse.SC_SERVICE_UNAVAILABLE).when(exception).status();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 503;
            String errorMessage = "Please re-try search after some time.";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_whenClientSearchResultsInElasticsearchStatusException_statusTooManyRequests_throwsException() throws IOException {
        ElasticsearchException exception = mock(ElasticsearchException.class);

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(429).when(exception).status();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 429;
            String errorMessage = "Too many requests, please re-try after some time";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_IOException_ListenerTimeout_throwsException() throws IOException {
        IOException exception = mock(IOException.class);

        String dummyTimeoutMessage = "listener timeout after waiting for 1m";

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(dummyTimeoutMessage).when(exception).getMessage();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 504;
            String errorMessage = "Request timed out after waiting for 1m";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_SocketTimeoutException_ListenerTimeout_throwsException() throws IOException {
        SocketTimeoutException exception = mock(SocketTimeoutException.class);

        String dummyTimeoutMessage = "60,000 milliseconds timeout on connection";

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(dummyTimeoutMessage).when(exception).getMessage();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 504;
            String errorMessage = "Request timed out after waiting for 1m";
            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_IOException_EmptyMessage_throwsException() throws IOException {
        IOException exception = mock(IOException.class);

        String dummyTimeoutMessage = "";

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(dummyTimeoutMessage).when(exception).getMessage();

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "Error processing search request";

            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test(expected = AppException.class)
    public void testQueryBase_IOException_RespopnseTooLong_throwsException() throws IOException {
        IOException exception = mock(IOException.class);
        doReturn(new ContentTooLongException(null)).when(exception).getCause();
        doReturn("dummyMessage").when(exception).getMessage();

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));

        try {
            sut.queryIndex(searchRequest);
        } catch (AppException e) {
            int errorCode = 413;
            String errorMessage = "Elasticsearch response is too long, max is 100Mb";

            validateAppException(e, errorCode, errorMessage);
            throw (e);
        }
    }

    @Test
    public void should_searchAll_when_requestHas_noQueryString() throws IOException {

        BoolQuery.Builder builder = this.sut.buildQuery(null, null, true);
        assertNotNull(builder);

        List<Query> topLevelFilterClause = builder.build().filter();
        assertEquals(1, topLevelFilterClause.size());

        verifyAcls(topLevelFilterClause.get(0), true);
    }

    @Test
    public void should_return_ownerOnlyFilterClause_when_searchAsOwners() throws IOException {

        BoolQuery.Builder builder = this.sut.buildQuery(null, null, false);
        assertNotNull(builder);

        List<Query> topLevelFilterClause = builder.build().filter();
        assertEquals(1, topLevelFilterClause.size());

        verifyAcls(topLevelFilterClause.get(0), false);
    }

    @Test
    public void should_return_nullQuery_when_searchAsDataRootUser() throws IOException {
        Map<String, String> HEADERS = new HashMap<>();
        HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
        HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
        HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));
        HEADERS.put(providerHeaderService.getDataRootUserHeader(), "true");
        when(dpsHeaders.getHeaders()).thenReturn(HEADERS);

        BoolQuery.Builder builder = this.sut.buildQuery(null, null, false);
        assertNotNull(builder);

        // Have full data access so acl filter is not set
        List<Query> topLevelFilterClause = builder.build().filter();
        assertEquals(0, topLevelFilterClause.size());
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
        co.elastic.clients.elasticsearch.ElasticsearchClient client = Mockito.mock(ElasticsearchClient.class, Mockito.RETURNS_DEEP_STUBS);
        SearchResponse searchResponse = Mockito.mock(SearchResponse.class);

        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        doReturn(hits).when(searchHits).hits();
        Mockito.when(searchResponse.hits())
                .thenReturn(searchHits);

        Mockito.when(client.search(Mockito.any(SearchRequest.class), eq((Type)Map.class)))
                .thenReturn(searchResponse);


        Mockito.when(elasticClientHandler.getOrCreateRestClient())
                .thenReturn(client);

        String index = "some-index";
        Mockito.when(crossTenantUtils.getIndexName(Mockito.any()))
                .thenReturn(index);

        Mockito.when(providerHeaderService.getDataGroupsHeader())
                .thenReturn("groups");

        Map<String, String> headers = new HashMap<>();
        headers.put("groups", "[]");
        Mockito.when(dpsHeaders.getHeaders())
                .thenReturn(headers);

        String expectedSource = getJsonOfSearchRequestWithIntersectionSpatialFilter();
        SearchRequest expectedSearchRequest = SearchRequest.of(s -> s.withJson(new StringReader(expectedSource)));

        // act
        QueryResponse response = this.sut.queryIndex(queryRequest);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArg = ArgumentCaptor.forClass(SearchRequest.class);
        Mockito.verify(client, Mockito.times(1)).search(searchRequestArg.capture(), eq((Type)Map.class));
        SearchRequest searchRequest = searchRequestArg.getValue();
        assertNotNull(expectedSearchRequest.query());
        assertNotNull(searchRequest.query());
        Assert.assertEquals(expectedSearchRequest.query().toString(), searchRequest.query().toString());
    }

    @Test
    public void should_work_with_x_collaboration_when_feature_flag_enabled() throws IOException {
        String xCollaboration = String.format("id=%s,application=%s", COLLABORATION_ID, COLLABORATION_APPLICATION);
        CollaborationContext collaborationContext = new CollaborationContext(
            UUID.fromString(COLLABORATION_ID), COLLABORATION_APPLICATION, Map.of());
        Optional<CollaborationContext> optionalCollaborationContext = Optional.of(collaborationContext);
        when(autocompleteFeatureFlag.isFeatureEnabled(COLLABORATIONS_FEATURE_NAME)).thenReturn(true);
        when(dpsHeaders.getCollaboration()).thenReturn(xCollaboration);
        when(collaborationContextFactory.create(xCollaboration)).thenReturn(optionalCollaborationContext);
        String expected = getStringFromFile("src/test/resources/testqueries/expected/simple-query-with-x-collaboration.json");
        BoolQuery expectedQuery = BoolQuery.of(s -> s.withJson(new StringReader(expected)));

        // act
        BoolQuery.Builder queryBuilder = this.sut.buildQuery(null, null, false);

        // assert
        BoolQuery actualQuery = queryBuilder.build();
        assertEquals(expectedQuery.toString(), actualQuery.toString());
    }

    @Test
    public void should_work_without_x_collaboration_when_feature_flag_enabled() throws IOException {
        when(autocompleteFeatureFlag.isFeatureEnabled(COLLABORATIONS_FEATURE_NAME)).thenReturn(true);
        String expected = getStringFromFile("src/test/resources/testqueries/expected/simple-query-without-x-collaboration.json");
        BoolQuery expectedQuery = BoolQuery.of(s -> s.withJson(new StringReader(expected)));

        // act
        BoolQuery.Builder queryBuilder = this.sut.buildQuery(null, null, false);

        // assert
        BoolQuery actualQuery = queryBuilder.build();
        assertEquals(expectedQuery.toString(), actualQuery.toString());
    }

    private Map<String, List<String>> getHighlightFields() {
        Map<String, List<String>> highlightFields = new HashMap<>();
        highlightFields.put(name, List.of(text));
        return highlightFields;
    }

    private Point getPoint(double latitude, double longitude) {
        return new Point(latitude, longitude);
    }

    private List<Point> getPolygonPoints(Point... points) {
        List<Point> polygon = new ArrayList<>();
        for (Point point : points) {
            polygon.add(point);
        }
        return polygon;
    }

    private SpatialFilter.ByBoundingBox getValidBoundingBox() {
        return getBoundingBox(topLeft, bottomRight);
    }

    private SpatialFilter.ByBoundingBox getBoundingBox(Point topLeft, Point bottomRight) {
        SpatialFilter.ByBoundingBox boundingBox = new SpatialFilter.ByBoundingBox();
        boundingBox.setTopLeft(topLeft);
        boundingBox.setBottomRight(bottomRight);
        return boundingBox;
    }

    private SpatialFilter.ByGeoPolygon getGeoPolygon(List<Point> points) {
        return new SpatialFilter.ByGeoPolygon(points);
    }

    private SpatialFilter.ByDistance getDistance(double distance, Point point) {
        return new SpatialFilter.ByDistance(distance, point);
    }

    private void validateAppException(AppException e, int errorCode, String errorMessage) {
        AppError error = e.getError();
        assertEquals(error.getCode(), errorCode);
        assertEquals(error.getMessage(), errorMessage);
    }

    private void verifyAcls(Query aclQuery, boolean asOwner) {
        assertNotNull(aclQuery);
        String jsonString;
        if (asOwner) {
            jsonString = """
                    {
                        "terms": {
                            "acl.owners": ["data.welldb.viewers@common.evd.cloud.slb-ds.com", "data.npd.viewers@common.evd.cloud.slb-ds.com"]
                        ,"boost": 1.0}
                    }
                    """;
        } else {
            jsonString = """
                    {
                        "terms": {
                            "x-acl": ["data.welldb.viewers@common.evd.cloud.slb-ds.com", "data.npd.viewers@common.evd.cloud.slb-ds.com"]
                        ,"boost": 1.0}
                    }                
                    """;
        }
        Query expectedQuery = Query.of(s -> s.withJson(new StringReader(jsonString)));
        assertEquals(expectedQuery.toString(), aclQuery.toString());
    }

    private String getStringFromFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path))).replaceAll("\\r\\n|\\n|\\r", "\n").trim();
    }

    private String getJsonOfSearchRequestWithIntersectionSpatialFilter() {
    return """
                {
                    "_source": {
                        "excludes": ["x-acl", "index"],
                        "includes": []
                    },
                    "from": 0,
                    "highlight": {
                        "fields": {}
                    },
                    "query": {
                        "bool": {
                            "filter": [{
                                    "geo_shape": {
                                        "data.Wgs84Coordinates": {
                                            "shape": "{geometries=[{coordinates=[[{lon=-8.61, lat=1.02}, {lon=-2.48, lat=1.02}, {lon=-2.48, lat=10.74}, {lon=-8.61, lat=10.74}, {lon=-8.61, lat=1.02}]], type=polygon}], type=geometrycollection}",
                                            "relation": "intersects"
                                        },
                                        "ignore_unmapped": true
                                    }
                                }, {
                                    "terms": {
                                        "x-acl": ["[]"]
                                    , "boost": 1.0}
                                }
                            ],
                            "must": [{
                                    "bool": {
                                        "boost": 1.0,
                                        "must": [{
                                                "query_string": {
                                                    "boost": 1.0,
                                                    "allow_leading_wildcard": false,
                                                    "auto_generate_synonyms_phrase_query": true,
                                                    "default_operator": "or",
                                                    "enable_position_increments": true,
                                                    "escape": false,
                                                    "fields": [],
                                                    "fuzziness": "AUTO",
                                                    "fuzzy_max_expansions": 50,
                                                    "fuzzy_prefix_length": 0,
                                                    "fuzzy_transpositions": true,
                                                    "max_determinized_states": 10000,
                                                    "phrase_slop": 0.0,
                                                    "query": "data.ID:\\"EPSG::1078\\"",
                                                    "type": "best_fields"
                                                }
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    },
                    "size": 10,
                    "timeout": "1m"
                }
                """;
    }
}
