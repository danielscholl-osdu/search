/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.search.provider.gcp.provider.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opengroup.osdu.core.common.model.search.AggregationResponse;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IAggregationParserUtil;
import org.opengroup.osdu.search.util.ISearchRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryServiceImpl extends QueryBase implements IQueryService {

  @Inject
  private SearchConfigurationProperties searchConfigurationProperties;
  @Inject
  private ElasticClientHandler elasticClientHandler;
  @Inject
  private AuditLogger auditLogger;
  @Autowired
  private IAggregationParserUtil aggregationParserUtil;
  @Inject
  private ISearchRequestUtil searchRequestUtil;

  @Override
  public QueryResponse queryIndex(QueryRequest searchRequest) throws IOException {
    try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
      return this.executeQuery(searchRequest, client);
    }
  }

  private QueryResponse executeQuery(QueryRequest searchRequest, RestHighLevelClient client) {
    SearchResponse searchResponse = this.makeSearchRequest(searchRequest, client);
    List<Map<String, Object>> results = this.getHitsFromSearchResponse(searchResponse);
    List<AggregationResponse> aggregations = getAggregationFromSearchResponse(searchResponse);

    QueryResponse queryResponse = QueryResponse.getEmptyResponse();
    if (searchResponse.getHits().getTotalHits() == null) {
      queryResponse.setTotalCount(0);
    } else {
      queryResponse.setTotalCount(searchResponse.getHits().getTotalHits().value);
    }
    if (results != null) {
      queryResponse.setAggregations(aggregations);
      queryResponse.setResults(results);
    }
    return queryResponse;
  }

  @Override
  SearchRequest createElasticRequest(Query request) throws IOException {
    QueryRequest searchRequest = (QueryRequest) request;

    // set the indexes to search against
    SearchRequest elasticSearchRequest = new SearchRequest(this.getIndex(request));
    searchRequestUtil.setIgnoreUnavailable(elasticSearchRequest, true);

    // build query
    SearchSourceBuilder sourceBuilder = this.createSearchSourceBuilder(request);
    sourceBuilder.from(searchRequest.getFrom());

    // aggregation: only make it available in pre demo for now
    if (!Strings.isNullOrEmpty(searchRequest.getAggregateBy())) {
      sourceBuilder.aggregation(
          aggregationParserUtil.parseAggregation(searchRequest.getAggregateBy()));
    }

    elasticSearchRequest.source(sourceBuilder);

    return elasticSearchRequest;
  }

  @Override
  void querySuccessAuditLogger(Query request) {
    this.auditLogger.queryIndexSuccess(Lists.newArrayList(request.toString()));
  }

  @Override
  void queryFailedAuditLogger(Query request) {
    this.auditLogger.queryIndexFailed(Lists.newArrayList(request.toString()));
  }
}
