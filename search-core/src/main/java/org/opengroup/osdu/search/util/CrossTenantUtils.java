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

import javax.inject.Inject;
import java.util.List;

@Component
public class CrossTenantUtils {
    // For details, please refer to implementation of class
    // org.opengroup.osdu.core.common.model.search.validation.MultiKindValidator
    private static final int MAX_INDEX_NAME_LENGTH = 3840;

    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private IndexAliasService indexAliasService;

    public String getIndexName(Query searchRequest) {
        List<String> kinds = KindParser.parse(searchRequest.getKind());
        String indexNames = buildIndexNames(kinds, false);
        if(indexNames.length() > MAX_INDEX_NAME_LENGTH) {
            indexNames = buildIndexNames(kinds, true);
        }

        return indexNames;
    }

    private String buildIndexNames(List<String> kinds, boolean useAlias) {
        StringBuilder builder = new StringBuilder();
        for(String kind : kinds) {
            String index = null;
            if(useAlias) {
                index = this.indexAliasService.getIndexAlias(kind);
            }
            if(Strings.isNullOrEmpty(index)) {
                index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
            }
            builder.append(index);
            builder.append(",");
        }
        builder.append("-.*"); // Exclude Lucene/ElasticSearch internal indices
        return builder.toString();
    }
}
