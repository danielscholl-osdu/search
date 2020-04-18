// Copyright © Amazon Web Services
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

package org.opengroup.osdu.search.provider.aws.provider.impl;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.opengroup.osdu.core.common.search.Config.isPreDemo;


@Service
public class QueryServiceAwsImpl extends QueryBase implements IQueryService {

    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private AuditLogger auditLogger;

    @Override
    public QueryResponse queryIndex(QueryRequest searchRequest) throws IOException {
        try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
            QueryResponse queryResponse = this.executeQuery(searchRequest, client);
            List<String> resources = new ArrayList<>();
            resources.add(searchRequest.toString());
            this.auditLogger.queryIndex(resources);
            return queryResponse;
        }
    }

    // TODO: Remove this temporary implementation when ECE CCS is utilized
    @Override
    public QueryResponse queryIndex(QueryRequest searchRequest, ClusterSettings clusterSettings) throws Exception {
        try (RestHighLevelClient client = elasticClientHandler.createRestClient(clusterSettings)) {
            QueryResponse queryResponse = executeQuery(searchRequest, client);
            List<String> resources = new ArrayList<>();
            resources.add(searchRequest.toString());
            auditLogger.queryIndex(resources);
            return queryResponse;
        }
    }

    private QueryResponse executeQuery(QueryRequest searchRequest, RestHighLevelClient client) throws AppException {
        SearchResponse searchResponse = this.makeSearchRequest(searchRequest, client);
        List<Map<String, Object>> results = this.getHitsFromSearchResponse(searchResponse);
        List<AggregationResponse> aggregations = getAggregationFromSearchResponse(searchResponse);

        if (results != null) {
            return QueryResponse.builder().results(results).aggregations(aggregations).totalCount(searchResponse.getHits().getTotalHits()).build();
        } else {
            return QueryResponse.getEmptyResponse();
        }
    }

    @Override
    SearchRequest createElasticRequest(Query request) throws AppException, IOException {
        QueryRequest searchRequest = (QueryRequest) request;

        // set the indexes to search against
        SearchRequest elasticSearchRequest = new SearchRequest(this.getIndex(request));

        // build query
        SearchSourceBuilder sourceBuilder = this.createSearchSourceBuilder(request);
        sourceBuilder.from(searchRequest.getFrom());

        if (StringUtils.isNotEmpty(searchRequest.getAggregateBy())) {
            TermsAggregationBuilder termsAggregationBuilder = new TermsAggregationBuilder(AGGREGATION_NAME, ValueType.STRING);
            termsAggregationBuilder.field(searchRequest.getAggregateBy());
            termsAggregationBuilder.size(Config.getAggregationSize());
            sourceBuilder.aggregation(termsAggregationBuilder);
        }

        elasticSearchRequest.source(sourceBuilder);

        return elasticSearchRequest;
    }
}