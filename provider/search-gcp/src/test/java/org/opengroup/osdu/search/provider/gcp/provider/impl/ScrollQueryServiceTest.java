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
package org.opengroup.osdu.search.provider.gcp.provider.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.QueryResponseUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchRequest.class, SearchHits.class, RestHighLevelClient.class,
    SearchConfigurationProperties.class})
public class ScrollQueryServiceTest {

  @Mock
  private SearchConfigurationProperties searchConfigurationProperties;
  @Mock
  private ElasticClientHandler elasticClientHandler;
  @Mock
  private CursorQueryRequest cursorQueryRequest;
  @Mock
  private CursorCache redisCache;
  @Mock
  private DpsHeaders dpsHeaders;
  @Mock
  private CrossTenantUtils crossTenantUtils;
  @Mock
  private AuditLogger auditLogger;
  @Mock
  private IProviderHeaderService providerHeaderService;
  @Mock
  private SearchConfigurationProperties searchConfig;
  @Mock
  private QueryResponseUtil queryResponseUtil;

  private RestHighLevelClient restHighLevelClient;

  private SearchResponse elasticSearchResponse;

  @InjectMocks
  @Spy
  private ScrollQueryServiceImpl sut = new ScrollQueryServiceImpl();

  private final String DATA_GROUPS = "X-Data-Groups";
  private static final String DATA_GROUP_1 = "data.welldb.viewers@common.evd.cloud.slb-ds.com";
  private static final String DATA_GROUP_2 = "data.npd.viewers@common.evd.cloud.slb-ds.com";
  private static final String USER_EMAIL = "blah@slb.com";

  public ScrollQueryServiceTest() throws NoSuchAlgorithmException {
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    mockStatic(RestHighLevelClient.class);
    mockStatic(SearchRequest.class);

    restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);
    Map<String, String> HEADERS = new HashMap<>();
    HEADERS.put(DpsHeaders.ACCOUNT_ID, "tenant1");
    HEADERS.put(DpsHeaders.AUTHORIZATION, "Bearer blah");
    HEADERS.put(DpsHeaders.USER_EMAIL, USER_EMAIL);
    HEADERS.put(DATA_GROUPS, String.format("%s,%s", DATA_GROUP_1, DATA_GROUP_2));
    when(dpsHeaders.getHeaders()).thenReturn(HEADERS);
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);

    when(providerHeaderService.getDataGroupsHeader()).thenReturn(DATA_GROUPS);

    when(searchConfigurationProperties.getDeploymentEnvironment())
        .thenReturn(DeploymentEnvironment.LOCAL);
  }

  @Test
  public void should_not_returnCursor_when_request_resultIsSmall() throws Exception {

    Map<String, Object> hit = new HashMap<>();
    hit.put("_id", "tenant1:welldb:wellbore-33fe05e1-df20-49d9-bd63-74cf750a206f");
    hit.put("type", "wellbore");

    List<Map<String, Object>> hits = new ArrayList<>();
    hits.add(hit);

    elasticSearchResponse = createSearchResponse(1, null);
    doReturn(elasticSearchResponse).when(this.sut).makeSearchRequest(any(), any());
    doReturn(hits).when(this.sut).getHitsFromSearchResponse(any());
    doReturn(hits).when(queryResponseUtil).getQueryResponseResults(any());
    doReturn(null).when(this.sut).refreshCursorCache(any(), any());

    CursorQueryResponse queryResponse = this.sut.queryIndex(cursorQueryRequest);
    assertNotNull(queryResponse);
    assertEquals(1, queryResponse.getTotalCount());
    assertNull(queryResponse.getCursor());
  }

  @Test
  public void should_returnCursor_when_request_resultIsOverDefaultLimit() throws Exception {

    Map<String, Object> hit = new HashMap<>();
    hit.put("_id", "tenant1:welldb:wellbore-33fe05e1-df20-49d9-bd63-74cf750a206f");
    hit.put("type", "wellbore");

    List<Map<String, Object>> hits = new ArrayList<>();
    hits.add(hit);
    elasticSearchResponse = createSearchResponse(15, null);
    doReturn(elasticSearchResponse).when(this.sut).makeSearchRequest(any(), any());
    doReturn(hits).when(this.sut).getHitsFromSearchResponse(any());
    doReturn("fso09flgl").when(this.sut).refreshCursorCache(any(), any());

    CursorQueryResponse queryResponse = this.sut.queryIndex(cursorQueryRequest);
    assertNotNull(queryResponse);
    assertEquals(15, queryResponse.getTotalCount());
    assertNotNull(queryResponse.getCursor());
  }

  @Test
  public void should_returnCursorResponse_when_request_hadValidCursor() throws Exception {

    Map<String, Object> hit = new HashMap<>();
    hit.put("_id", "tenant1:welldb:wellbore-33fe05e1-df20-49d9-bd63-74cf750a206f");
    hit.put("type", "wellbore");

    List<Map<String, Object>> hits = new ArrayList<>();
    hits.add(hit);

    String cursor = "fso09flgl";

    CursorSettings cursorSettings = CursorSettings.builder().cursor(cursor).userId(USER_EMAIL)
        .build();

    when(this.cursorQueryRequest.getCursor()).thenReturn(cursor);

    elasticSearchResponse = createSearchResponse(1, cursor);

    doReturn(restHighLevelClient).when(this.elasticClientHandler).createRestClient();

    when(restHighLevelClient.scroll(any(), any(RequestOptions.class)))
        .thenReturn(elasticSearchResponse);

    doReturn(hits).when(this.sut).getHitsFromSearchResponse(any());
    doReturn(cursor).when(this.sut).refreshCursorCache(any(), any());
    doReturn(cursorSettings).when(this.redisCache).get(cursor);

    CursorQueryResponse queryResponse = this.sut.queryIndex(cursorQueryRequest);
    assertNotNull(queryResponse);
    verify(this.auditLogger)
        .queryIndexWithCursorSuccess(Lists.newArrayList(cursorQueryRequest.toString()));
  }

  @Test
  public void should_TotalCountIsZero_when_cursorIsNull() throws Exception {
    List<Map<String, Object>> results = null;

    doReturn(elasticSearchResponse).when(this.sut).makeSearchRequest(any(), any());
    doReturn(results).when(this.sut).getHitsFromSearchResponse(any());

    CursorQueryResponse queryResponse = this.sut.queryIndex(cursorQueryRequest);
    assertNotNull(queryResponse);
    assertEquals(0, queryResponse.getTotalCount());
    assertNull(queryResponse.getCursor());
  }

  @Test
  public void should_returnRightTotalCount_when_resultsIsNull() throws Exception {
    List<Map<String, Object>> results = null;

    String cursor = "fso09flgl";

    CursorSettings cursorSettings = CursorSettings.builder().cursor(cursor).userId(USER_EMAIL)
        .build();

    when(this.cursorQueryRequest.getCursor()).thenReturn(cursor);

    elasticSearchResponse = createSearchResponse(100, cursor);

    doReturn(restHighLevelClient).when(this.elasticClientHandler).createRestClient();

    when(restHighLevelClient.scroll(any(), any(RequestOptions.class)))
        .thenReturn(elasticSearchResponse);

    doReturn(results).when(this.sut).getHitsFromSearchResponse(any());
    doReturn(cursorSettings).when(this.redisCache).get(cursor);

    CursorQueryResponse queryResponse = this.sut.queryIndex(cursorQueryRequest);
    assertNotNull(queryResponse);
    assertEquals(100, queryResponse.getTotalCount());
  }

  @Test
  public void should_returnRightTotalCount_when_cusorBeyondLastRecord() throws Exception {
    String cursor = "fso09flgl";
    Map<String, Object> hit1 = new HashMap<>();
    hit1.put("_id", "tenant1:welldb:wellbore-33fe05e1-df20-49d9-bd63-74cf750a206f");
    hit1.put("type", "wellbore");

    List<Map<String, Object>> hits1 = new ArrayList<>();
    hits1.add(hit1);

    List<Map<String, Object>> hits2 = null;

    CursorSettings cursorSettings = CursorSettings.builder().cursor(cursor).userId(USER_EMAIL)
        .build();

    when(this.cursorQueryRequest.getCursor()).thenReturn(cursor);
    elasticSearchResponse = createSearchResponse(100, cursor);

    doReturn(restHighLevelClient).when(this.elasticClientHandler).createRestClient();

    when(restHighLevelClient.scroll(any(), any(RequestOptions.class)))
        .thenReturn(elasticSearchResponse);

    doReturn(hits1).when(this.sut).getHitsFromSearchResponse(any());
    doReturn(cursorSettings).when(this.redisCache).get(cursor);

    // first call queryIndex()
    CursorQueryResponse queryResponse1 = this.sut.queryIndex(cursorQueryRequest);
    assertNotNull(queryResponse1);
    assertEquals(1, queryResponse1.getResults().size());
    assertEquals(100, queryResponse1.getTotalCount());

    //second call queryIndex()
    doReturn(hits2).when(this.sut).getHitsFromSearchResponse(any());
    CursorQueryResponse queryResponse2 = this.sut.queryIndex(cursorQueryRequest);
    assertNotNull(queryResponse2);
    assertEquals(0, queryResponse2.getResults().size());
    assertEquals(100, queryResponse2.getTotalCount());
  }

  @Test
  public void should_return_correctElasticRequest_given_firstCursorRequestQuery()
      throws IOException {

    int limit = 5;
    String kind = "tenant1:welldb:well:1.0.0";

    List<String> returnedFields = new ArrayList<>();
    returnedFields.add("id");
    when(cursorQueryRequest.getKind()).thenReturn(kind);
    when(cursorQueryRequest.getLimit()).thenReturn(limit);
    when(cursorQueryRequest.getReturnedFields()).thenReturn(returnedFields);
    when(searchConfig.getQueryLimitMaximum()).thenReturn(1000);

    Mockito.when(crossTenantUtils.getIndexName(any(), any()))
        .thenReturn("tenant1-welldb-well-1.0.0,-.*");

    SearchRequest elasticRequest = this.sut.createElasticRequest(cursorQueryRequest);
    assertNotNull(elasticRequest);

    String[] indices = elasticRequest.indices();
    assertEquals(1, indices.length);
    assertEquals("tenant1-welldb-well-1.0.0,-.*", indices[0]);

    SearchSourceBuilder elasticSearchSourceBuilder = elasticRequest.source();
    assertNotNull(elasticSearchSourceBuilder);
    assertEquals(limit, elasticSearchSourceBuilder.size());
    assertEquals(1, elasticSearchSourceBuilder.timeout().getMinutes());
    assertEquals(2, elasticSearchSourceBuilder.sorts().size());
    assertTrue(elasticSearchSourceBuilder.sorts().contains(SortBuilders.scoreSort()));
    assertTrue(elasticSearchSourceBuilder.sorts().contains(SortBuilders.fieldSort("_doc")));

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

  private SearchResponse createSearchResponse(long totalHitsCount, String scrollId) {
    TotalHits totalHits = new TotalHits(totalHitsCount, Relation.EQUAL_TO);
    SearchHits searchHits = new SearchHits(new SearchHit[0], totalHits, 2);
    SearchResponse mockSearchResponse = new SearchResponse(
        new SearchResponseSections(searchHits, null,
            null, false, false, null, 1), scrollId,
        5, 5, 0, 100, ShardSearchFailure.EMPTY_ARRAY,
        SearchResponse.Clusters.EMPTY);

    return mockSearchResponse;
  }

}
