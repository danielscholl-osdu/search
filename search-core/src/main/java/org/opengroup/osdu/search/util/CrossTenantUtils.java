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

import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.util.KindParser;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class CrossTenantUtils {

    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;

    public String getIndexName(Query searchRequest) {
        StringBuilder builder = new StringBuilder();
        List<String> kinds = KindParser.parse(searchRequest.getKind());
        for(int i = 0; i < kinds.size(); i++) {
            String kind = kinds.get(i);
            String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
            builder.append(index + ",");
        }
        builder.append("-.*"); // Exclude Lucene/ElasticSearch internal indices
        return builder.toString();
    }
}
