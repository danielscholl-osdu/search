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

package org.opengroup.osdu.search.smart.models;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetaData;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.google.api.client.http.HttpMethods;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.elasticsearch.search.fetch.ShardFetchSearchRequest;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.provider.interfaces.IAttributesCache;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.model.IndexMappingRequest;
import org.opengroup.osdu.search.smart.attributes.AttributeLoader;

import static org.opengroup.osdu.core.common.Constants.*;

public class AttributeCollection {

	private final Gson gson = new Gson();
	@Inject
	private ElasticClientHandler elasticClientHandler;
	@Inject
	private IUrlFetchService urlFetchService;
	@Inject
	private JaxRsDpsLog log;
	@Inject
	private DpsHeaders headersInfo;
	@Inject
	private IAttributesCache<String, Set<String>> cache;
	@Inject
    private IServiceAccountJwtClient serviceAccountJwtClient;

	private Map<String, Set<String>> attributes = new HashMap<>();
	
	private static final int AggregationSize = 10000;
	private static final TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);
	private static final int INDEXER_MAPPING_LIMIT = 10;

	public void cacheSync() throws IOException, URISyntaxException {
		
		List<Attribute> attributes = AttributeLoader.getAttributes();
		for (Attribute attr : attributes) {
			String cacheKey = String.format("%s-%s-%s", Config.getDeployedServiceId(),
					this.headersInfo.getPartitionId(), attr.getName());
			log.info(String.format("updating the cache with key: %s", cacheKey));
			Set<String> attrVals = new HashSet<>();
			for (String fieldName : attr.getSchemaMapping()) {
				attrVals.addAll(this.getTermAggregationForField("by_" + fieldName, fieldName));
			}
			if (!attrVals.isEmpty())
				this.cache.put(cacheKey, attrVals);
		}
	}

	private Set<String> getTermAggregationForField(String termAggId, String fieldName) throws IOException, URISyntaxException {
		Set<String> indices = getValidIndicesForKeywordMapping(fieldName);
		if (indices.size() != 0) {
			fixIndicesByEnablingMultiFieldIndexing(indices, fieldName);
		}
		return getTermAggregation(termAggId, fieldName + ".keyword");
	}
	
	private Set<String> getTermAggregation(String termAggId, String fieldName) throws IOException {
		Map<String, Long> result = new HashMap<>();
		SearchRequest searchRequest = new SearchRequest();
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(0);
		sourceBuilder.timeout(REQUEST_TIMEOUT);
		TermsAggregationBuilder aggregation = AggregationBuilders.terms(termAggId).field(fieldName)
				.size(AggregationSize);
		sourceBuilder.aggregation(aggregation);
		searchRequest.source(sourceBuilder);
		try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
			SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
			Aggregations aggregations = searchResponse.getAggregations();
			if (aggregations != null) {
				Terms keywordAggregation = aggregations.get(termAggId);
				for (Terms.Bucket bucket : keywordAggregation.getBuckets()) {
				result.put(bucket.getKeyAsString().toLowerCase(), bucket.getDocCount());
				}
			}
		}
		return result.keySet();
	}

	/* Sample Mapping
	*{
		"mapping": {
		   "Field": {
		    "type": "text",
		      "fields": {
		        "keyword": {
		          "type": "keyword"
		        }
		      }
		    }
		  }
	 } */
	@SuppressWarnings("unchecked")
	private Set<String> getValidIndicesForKeywordMapping(String fieldName) throws IOException {
		Set<String> updatedIndices = new HashSet<>();
		try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
			GetFieldMappingsRequest request = new GetFieldMappingsRequest();
			request.fields(fieldName);
			GetFieldMappingsResponse response = client.indices().getFieldMapping(request, RequestOptions.DEFAULT);
			Map<String, Map<String, Map<String, FieldMappingMetaData>>> mappings = response.mappings();
			Set<String> sets = mappings.keySet();

			for (String indices : sets) {
				if (!mappings.get(indices).isEmpty()) {
					Map<String, Map<String, FieldMappingMetaData>> fieldMappingData = mappings.get(indices);

					for (String key : fieldMappingData.keySet()) {
						Map<String, FieldMappingMetaData> fieldMapping = fieldMappingData.get(key);
						FieldMappingMetaData fieldMappingMetaData = fieldMapping.get(fieldName);
						Map<String, Object> mapping = fieldMappingMetaData.sourceAsMap();
						String fieldkey = fieldName.split("\\.")[1];
						Map<String, Map<String, Map<String, Object>>> keywordMapping = (Map<String, Map<String, Map<String, Object>>>) mapping.get(fieldkey);
						updatedIndices.add(indices);
						if (keywordMapping.get(FIELDS) != null && keywordMapping.get(FIELDS).get(KEYWORD) != null)
							if (keywordMapping.get(FIELDS).get(KEYWORD).get(TYPE).equals(KEYWORD))
								updatedIndices.remove(indices);
					}
				}
			}
		}
		return updatedIndices;
	}

	private void fixIndicesByEnablingMultiFieldIndexing(Set<String> indices, String fieldName) throws URISyntaxException {
		List<List<String>> listOfUpdatedIndices = getPartionedList(indices, INDEXER_MAPPING_LIMIT);
		for (List<String> updatedIndices : listOfUpdatedIndices) {
			IndexMappingRequest indexMappingRequest = new IndexMappingRequest();
			indexMappingRequest.setOperator("eq"); // Need to confirm
			Set<String> indicesSet = new HashSet<String>(updatedIndices);
			indexMappingRequest.setIndices(indicesSet);
			String body = this.gson.toJson(indexMappingRequest);
			String endUrl = String.format("kinds/%s", fieldName.split("\\.")[1]);
			headersInfo.put(DpsHeaders.AUTHORIZATION, this.checkOrGetAuthorizationHeader());
			FetchServiceHttpRequest request = FetchServiceHttpRequest
					.builder()
					.httpMethod(HttpMethods.PUT)
					.url(Config.getIndexerHostUrl() + endUrl)
					.headers(headersInfo)
					.body(body).build();
			HttpResponse response = this.urlFetchService.sendRequest(request);
			if (response.getResponseCode() != 200)
				log.warning(String.format("Failed to update field: %s | indices:  %s", fieldName, indicesSet));
			else
				log.info(String.format("Updated field: %s | indices:  %s", fieldName, indicesSet));
		}
	}

	private List<List<String>> getPartionedList(Set<String> kindVals, int size) {
		List<String> list = new ArrayList<String>(kindVals);
		return Lists.partition(list, size);
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> getAllAttributes(String dataPartitionId, String attributeName) throws IOException {
		String cacheKey = String.format("%s-%s-%s", Config.getDeployedServiceId(), dataPartitionId, attributeName);
		if (attributes.get(cacheKey) == null) {
			attributes.put(cacheKey, this.cache.get(cacheKey));
		}
		if (attributes.get(cacheKey) == null) {
			log.warning("No kind was found in the cache. Please verify if background sync cron is working correctly.");
			return new HashSet<>();
		}
		return attributes.get(cacheKey);
	}
	
   private String checkOrGetAuthorizationHeader() {
		if (Config.getDeploymentEnvironment() == DeploymentEnvironment.LOCAL) {
	        String authHeader = headersInfo.getAuthorization();
	            if (Strings.isNullOrEmpty(authHeader)) {
	                throw new AppException(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authorization header", "Authorization token cannot be empty");
	            }
	            return authHeader;
	        } else {
	            return "Bearer " + this.serviceAccountJwtClient.getIdToken(this.headersInfo.getPartitionId());
	        }
	    }
}
