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

package org.opengroup.osdu.search.provider.azure.provider.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IAggregationParserUtil;
import org.opengroup.osdu.search.util.QueryResponseUtil;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class QueryServiceImpl extends QueryBase implements IQueryService {

    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private AuditLogger auditLogger;
    @Inject
    private SearchConfigurationProperties configurationProperties;
    @Inject
    private QueryResponseUtil queryResponseUtil;
    @Inject
    private IAggregationParserUtil aggregationParserUtil;

    @Override
    public QueryResponse queryIndex(QueryRequest searchRequest) throws IOException {
        try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
            QueryResponse queryResponse = this.executeQuery(searchRequest, client);
            return queryResponse;
        }
    }

    private QueryResponse executeQuery(QueryRequest searchRequest, RestHighLevelClient client) throws AppException {
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
            queryResponse.setResults(queryResponseUtil.getQueryResponseResults(results));
        }
        return queryResponse;
    }

    @Override
    SearchRequest createElasticRequest(Query request) throws AppException, IOException {
        QueryRequest searchRequest = (QueryRequest) request;

        // set the indexes to org.opengroup.osdu.search.search against
        SearchRequest elasticSearchRequest = new SearchRequest(this.getIndex(request));

        // build query
        SearchSourceBuilder sourceBuilder = this.createSearchSourceBuilder(request);
        sourceBuilder.from(searchRequest.getFrom());

        // aggregation
        if (!Strings.isNullOrEmpty(searchRequest.getAggregateBy())) {
            sourceBuilder.aggregation(aggregationParserUtil.parseAggregation(searchRequest.getAggregateBy()));
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
