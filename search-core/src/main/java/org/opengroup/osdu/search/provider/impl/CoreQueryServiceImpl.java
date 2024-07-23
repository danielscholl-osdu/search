// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.provider.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import java.io.IOException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IAggregationParserUtil;
import org.opengroup.osdu.search.util.SearchRequestUtil;
import org.opengroup.osdu.search.util.SuggestionsQueryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CoreQueryServiceImpl extends CoreQueryBase implements IQueryService {

  @Inject private ElasticClientHandler elasticClientHandler;
  @Inject private AuditLogger auditLogger;
  @Inject private IAggregationParserUtil aggregationParserUtil;
  @Autowired private SuggestionsQueryUtil suggestionsQueryUtil;

  @Override
  public QueryResponse queryIndex(QueryRequest searchRequest) throws IOException {
    ElasticsearchClient client = this.elasticClientHandler.getOrCreateRestClient();
    QueryResponse queryResponse = this.executeQuery(searchRequest, client);
    return queryResponse;
  }

  private QueryResponse executeQuery(QueryRequest searchRequest, ElasticsearchClient client)
      throws AppException {
    SearchResponse<Void> searchResponse = this.makeSearchRequest(searchRequest, client);
    // List<Map<String, Object>> results = this.getHitsFromSearchResponse(searchResponse);
    // List<AggregationResponse> aggregations = getAggregationFromSearchResponse(searchResponse);
    // List<String> phraseSuggestions =
    // suggestionsQueryUtil.getPhraseSuggestionsFromSearchResponse(searchResponse);

    //        QueryResponse queryResponse = QueryResponse.getEmptyResponse();
    //        if (searchResponse.getHits().getTotalHits() == null) {
    //            queryResponse.setTotalCount(0);
    //        } else {
    //            queryResponse.setTotalCount(searchResponse.getHits().getTotalHits().value);
    //        }
    //        if (results != null) {
    //            queryResponse.setAggregations(aggregations);
    //            queryResponse.setResults(results);
    //        }
    //        if (phraseSuggestions != null) {
    //            queryResponse.setPhraseSuggestions(phraseSuggestions);
    //        }
    QueryResponse queryResponse = QueryResponse.getEmptyResponse();
    return queryResponse;
  }

  @Override
  SearchRequest createElasticRequest(Query request, String index) throws AppException, IOException {
    QueryRequest searchRequest = (QueryRequest) request;

    // set the indexes to org.opengroup.osdu.search.search against
    var elasticSearchRequest = SearchRequestUtil.createSearchRequest(index);

    // build query
    var sourceBuilder = this.createSearchSourceBuilder(request);
    sourceBuilder.from(searchRequest.getFrom());

    // aggregation
    if (!Strings.isNullOrEmpty(searchRequest.getAggregateBy())) {
      sourceBuilder
          .build(); // (aggregationParserUtil.parseAggregation(searchRequest.getAggregateBy()));
    }

    elasticSearchRequest.query(sourceBuilder.build().query());

    return elasticSearchRequest.build();
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
