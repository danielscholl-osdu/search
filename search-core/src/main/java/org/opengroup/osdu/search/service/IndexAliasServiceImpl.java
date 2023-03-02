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

import com.google.api.client.util.Strings;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.rest.RestStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.search.cache.MultiPartitionIndexAliasCache;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IndexAliasServiceImpl implements IndexAliasService {
    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private JaxRsDpsLog log;
    @Inject
    private MultiPartitionIndexAliasCache indexAliasCache;

    @Override
    public Map<String, String> getIndicesAliases(List<String> kinds) {
        Map<String, String> aliases = new HashMap<>();
        List<String> unresolvedKinds = new ArrayList<>();

        List<String> validKinds = kinds.stream().filter(k -> elasticIndexNameResolver.isIndexAliasSupported(k)).collect(Collectors.toList());
        for (String kind : validKinds) {
            if (indexAliasCache.get(kind) != null) {
                String alias = indexAliasCache.get(kind);
                aliases.put(kind, alias);
            } else {
                unresolvedKinds.add(kind);
            }
        }
        if (!unresolvedKinds.isEmpty()) {
            try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
                // It is much faster to get all the aliases and verify it locally than to verify it remotely.
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
                log.error(String.format("Fail to get index aliases for kinds"), e);
            }
        }

        return aliases;
    }

    private Set<String> getAllExistingAliases(RestHighLevelClient restClient) throws IOException {
        GetAliasesRequest request = new GetAliasesRequest();
        GetAliasesResponse response = restClient.indices().getAlias(request, RequestOptions.DEFAULT);
        if (response.status() != RestStatus.OK)
            return new HashSet<>();

        Set<String> allAliases = new HashSet<>();
        for (Set<AliasMetadata> aliasSet : response.getAliases().values()) {
            List<String> aliases = aliasSet.stream().map(a -> a.getAlias()).collect(Collectors.toList());
            allAliases.addAll(aliases);
        }
        return allAliases;
    }

    private String createIndexAlias(RestHighLevelClient restClient, String kind) throws IOException {
        String index = elasticIndexNameResolver.getIndexNameFromKind(kind);
        String alias = elasticIndexNameResolver.getIndexAliasFromKind(kind);
        // To create an alias for an index, the index name must the concrete index name, not alias
        index = resolveConcreteIndexName(restClient, index);
        if (!Strings.isNullOrEmpty(index)) {
            IndicesAliasesRequest addRequest = new IndicesAliasesRequest();
            IndicesAliasesRequest.AliasActions aliasActions = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                    .index(index)
                    .alias(alias);
            addRequest.addAliasAction(aliasActions);
            AcknowledgedResponse response = restClient.indices().updateAliases(addRequest, RequestOptions.DEFAULT);
            if (response.isAcknowledged()) {
                return alias;
            }
        }

        return null;
    }

    private String resolveConcreteIndexName(RestHighLevelClient restClient, String index) throws IOException {
        GetAliasesRequest request = new GetAliasesRequest(index);
        GetAliasesResponse response = restClient.indices().getAlias(request, RequestOptions.DEFAULT);
        if (response.status() == RestStatus.NOT_FOUND) {
            /* index resolved from kind is actual concrete index
             * Example:
             * {
             *   "opendes-wke-well-1.0.7": {
             *       "aliases": {}
             *   }
             * }
             */
            return index;
        }
        if (response.status() == RestStatus.OK) {
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
            Map<String, Set<AliasMetadata>> aliases = response.getAliases();
            for (Map.Entry<String, Set<AliasMetadata>> entry : aliases.entrySet()) {
                String actualIndex = entry.getKey();
                List<String> aliaseNames = entry.getValue().stream().map(a -> a.getAlias()).collect(Collectors.toList());
                if (aliaseNames.contains(index))
                    return actualIndex;
            }
        }
        return null;
    }
}
