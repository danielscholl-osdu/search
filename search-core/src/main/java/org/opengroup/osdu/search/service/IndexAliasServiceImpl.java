// Copyright © Schlumberger
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

package org.opengroup.osdu.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import com.google.api.client.util.Strings;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.search.cache.MultiPartitionIndexAliasCache;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.springframework.stereotype.Component;

@Component
public class IndexAliasServiceImpl implements IndexAliasService {
  private static final String KIND_COMPLETE_VERSION_PATTERN =
      "[\\w-\\.\\*]+:[\\w-\\.\\*]+:[\\w-\\.\\*]+:(\\d+\\.\\d+\\.\\d+)$";

  @Inject private ElasticClientHandler elasticClientHandler;
  @Inject private ElasticIndexNameResolver elasticIndexNameResolver;
  @Inject private JaxRsDpsLog log;
  @Inject private MultiPartitionIndexAliasCache indexAliasCache;

  @Override
  public Map<String, String> getIndicesAliases(List<String> kinds) {
    Map<String, String> aliases = new HashMap<>();
    List<String> unresolvedKinds = new ArrayList<>();

    List<String> validKinds =
        kinds.stream()
            .filter(k -> elasticIndexNameResolver.isIndexAliasSupported(k))
            .collect(Collectors.toList());

    for (String kind : validKinds) {
      if (indexAliasCache.get(kind) != null) {
        String alias = indexAliasCache.get(kind);
        aliases.put(kind, alias);
      } else {
        unresolvedKinds.add(kind);
      }
    }
    if (!unresolvedKinds.isEmpty()) {
      try {
        ElasticsearchClient restClient = this.elasticClientHandler.getOrCreateRestClient();
        // It is much faster to get all the aliases and verify it locally than to verify it
        // remotely.
        Set<String> allExistingAliases = getAllExistingAliases(restClient);
        for (String kind : unresolvedKinds) {
          String alias = elasticIndexNameResolver.getIndexAliasFromKind(kind);
          if (!allExistingAliases.contains(alias)) {
            try {
              alias = createIndexAlias(restClient, kind);
            } catch (Exception e) {
              log.error(String.format("Fail to create index alias for kind '%s'", kind), e);
            }
          }
          if (!Strings.isNullOrEmpty(alias)) {
            aliases.put(kind, alias);
            indexAliasCache.put(kind, alias);
          }
        }
      } catch (Exception e) {
        log.error("Fail to get index aliases for kinds", e);
      }
    }

    return aliases;
  }

  private Set<String> getAllExistingAliases(ElasticsearchClient restClient) throws IOException {
    GetAliasRequest request = new GetAliasRequest.Builder().build();
    GetAliasResponse response = restClient.indices().getAlias(request);
    if (response.result().isEmpty()) {
      return new HashSet<>();
    }

    Set<String> allAliases = new HashSet<>();
    for (Map.Entry<String, IndexAliases> entry : response.result().entrySet()) {
      List<String> aliaseNames = entry.getValue().aliases().keySet().stream().toList();
      allAliases.addAll(aliaseNames);
    }
    return allAliases;
  }

  private String createIndexAlias(ElasticsearchClient restClient, String kind) throws IOException {
    String index = elasticIndexNameResolver.getIndexNameFromKind(kind);
    String alias = elasticIndexNameResolver.getIndexAliasFromKind(kind);
    if (isCompleteVersionKind(kind)) {
      // To create an alias for an index, the index name must the concrete index name, not alias
      index = resolveConcreteIndexName(restClient, index);
    }
    if (!Strings.isNullOrEmpty(index)) {
      PutAliasRequest.Builder putAliasRequest = new PutAliasRequest.Builder();
      putAliasRequest.index(index);
      putAliasRequest.name(alias);
      PutAliasResponse putAliasResponse = restClient.indices().putAlias(putAliasRequest.build());
      return putAliasResponse.acknowledged() ? alias : null;
    }

    return null;
  }

  private boolean isCompleteVersionKind(String kind) {
    return !Strings.isNullOrEmpty(kind) && kind.matches(KIND_COMPLETE_VERSION_PATTERN);
  }

  private String resolveConcreteIndexName(ElasticsearchClient restClient, String index)
      throws IOException {
    GetAliasRequest request = GetAliasRequest.of(gar -> gar.name(index));
    GetAliasResponse response = restClient.indices().getAlias(request);
    if (response.result().isEmpty()) {
      /* index resolved from kind is actual concrete index
       * Example:
       * {
       *   "opendes-wke-well-1.0.7": {
       *       "aliases": {}
       *   }
       * }
       */
      return index;
    } else {
      /* index resolved from kind is NOT actual create index. It is just an alias
       * The concrete index name in this example is "opendes-osdudemo-wellbore-1.0.0_1649167113090"
       * Example:
       * {
       *   "opendes-osdudemo-wellbore-1.0.0_1649167113090": {
       *       "aliases": {
       *           "opendes-osdudemo-wellbore-1.0.0": {}
       *       }
       *    }
       * }
       */
      for (Map.Entry<String, IndexAliases> entry : response.result().entrySet()) {
        String actualIndex = entry.getKey();
        List<String> aliasesNames = entry.getValue().aliases().keySet().stream().toList();
        if (aliasesNames.contains(index)) {
          return actualIndex;
        }
      }
    }
    return index;
  }
}
