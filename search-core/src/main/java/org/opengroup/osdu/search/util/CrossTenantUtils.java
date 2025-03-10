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

package org.opengroup.osdu.search.util;

import com.google.api.client.util.Strings;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.util.KindParser;
import org.opengroup.osdu.search.service.IndexAliasService;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CrossTenantUtils {
    // For details, please refer to implementation of class
    // org.opengroup.osdu.core.common.model.search.validation.MultiKindValidator
    private static final int MAX_INDEX_NAME_LENGTH = 3840;
    // The last '-' in "-system-meta-data-*" is converted from kind delimiter ":" between authority and source
    private static final String[] excludedIndices = {"-.*", "-system-meta-data-*"};
    private static final String KINDS_START_WITH_WILDCARD = "*";

    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private IndexAliasService indexAliasService;

    public String getIndexName(Query searchRequest) {
        List<String> kinds = KindParser.parse(searchRequest.getKind());
        String index = getIndexName(kinds, new HashMap<>());
        if(index.length() <= MAX_INDEX_NAME_LENGTH) {
            return index;
        }
        else {
            Map<String, String> aliases = this.indexAliasService.getIndicesAliases(kinds);
            return getIndexName(kinds, aliases);
        }
    }

    private String getIndexName(List<String> kinds, Map<String, String> aliases) {
        List<String> indicesStartWithWildcard = new ArrayList<>();
        List<String> normalIndices = new ArrayList<>();
        for(String kind : kinds) {
            String index;
            if (aliases.containsKey(kind) && !Strings.isNullOrEmpty(aliases.get(kind))) {
                index = aliases.get(kind);
            } else {
                index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
            }
            if(kind.startsWith(KINDS_START_WITH_WILDCARD)) {
                indicesStartWithWildcard.add(index);
            }
            else {
                normalIndices.add(index);
            }
        }

        StringBuilder builder = new StringBuilder();
        // Exclude Lucene/ElasticSearch internal indices and system/metadata indices in searches with wildcard kind
        if(!indicesStartWithWildcard.isEmpty()) {
            builder.append(String.join(",", indicesStartWithWildcard));
            builder.append(",");
            builder.append(String.join(",", excludedIndices));
        }
        if(!normalIndices.isEmpty()) {
            if(!builder.isEmpty()) {
                builder.append(",");
            }
            builder.append(String.join(",", normalIndices));
        }

        return builder.toString();
    }
}
