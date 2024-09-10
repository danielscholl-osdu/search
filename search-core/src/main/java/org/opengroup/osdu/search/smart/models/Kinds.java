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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.provider.interfaces.IKindsCache;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.springframework.context.annotation.Scope;

@Scope("request")
public class Kinds {

  @Inject
  public Kinds(
      SearchConfigurationProperties configurationProperties,
      ElasticClientHandler elasticClientHandler,
      IKindsCache cache,
      DpsHeaders headersInfo,
      JaxRsDpsLog log) {
    this.configurationProperties = configurationProperties;
    this.elasticClientHandler = elasticClientHandler;
    this.cache = cache;
    this.headersInfo = headersInfo;
    this.log = log;
  }

  private Map<String, Set<String>> kinds = new HashMap<>();
  private final IKindsCache<String, Set<String>> cache;
  private final ElasticClientHandler elasticClientHandler;
  private final DpsHeaders headersInfo;
  private final JaxRsDpsLog log;
  private static final int AggregationSize = 10000;
  private static final Time REQUEST_TIMEOUT = Time.of(t -> t.time("1m"));
  private SearchConfigurationProperties configurationProperties;

  @SuppressWarnings("unchecked")
  public Set<String> all(String accountId) throws IOException {
    if (kinds.get(accountId) == null) {
      String cacheKey =
          String.format("%s-%s", configurationProperties.getDeployedServiceId(), accountId);
      kinds.put(accountId, this.cache.get(cacheKey));
    }
    if (kinds.get(accountId) == null) {
      log.warning(
          "No kind was found in the cache. Please verify if background sync cron is working correctly.");
      return new HashSet<>();
    }
    return kinds.get(accountId);
  }

  public void cacheSync() throws IOException {
    String cacheKey =
        String.format(
            "%s-%s",
            configurationProperties.getDeployedServiceId(), this.headersInfo.getPartitionId());
    log.debug(String.format("updating the cache with key: %s", cacheKey));
    Set<String> kindVals = this.getTermAggregation("by_kind", RecordMetaAttribute.KIND.getValue());
    this.cache.put(cacheKey, kindVals);
  }

  protected Set<String> getTermAggregation(String termAggId, String fieldName) throws IOException {
    Map<String, Long> result = new HashMap<>();

    TermsAggregation termsAggregation =
        TermsAggregation.of(ta -> ta.field(fieldName + ".keyword").size(AggregationSize));
    SearchRequest searchRequest =
        SearchRequest.of(
            sr ->
                sr.size(0)
                    .timeout(REQUEST_TIMEOUT.time())
                    .aggregations(termAggId, a -> a.terms(termsAggregation)));

    ElasticsearchClient createRestClient = this.elasticClientHandler.getOrCreateRestClient();
    SearchResponse<Void> searchResponse = createRestClient.search(searchRequest, Void.class);
    StringTermsAggregate termAggIdAggregation =
        searchResponse.aggregations().get(termAggId).sterms();

    return termAggIdAggregation.buckets().array().stream()
        .map(bucket -> bucket.key().stringValue())
        .collect(Collectors.toSet());
  }
}
