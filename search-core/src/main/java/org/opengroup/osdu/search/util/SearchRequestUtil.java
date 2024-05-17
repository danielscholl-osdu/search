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

package org.opengroup.osdu.search.util;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;

public class SearchRequestUtil {

    public static SearchRequest createSearchRequest(String... indices) {
        // IndicesOptions ignoreUnavailable is false by default.
        // It is possible that the indices of some kinds in the kind list may not exist in ElasticSearch
        // Setting indicesOption ignore_unavailable to true let ElasticSearch ignore the unavailable indices in the SearchRequest
        SearchRequest searchRequest = new SearchRequest(indices);
        IndicesOptions option = addIgnoreUnavailable(searchRequest.indicesOptions());
        return searchRequest.indicesOptions(option);
    }

    public static IndicesOptions addIgnoreUnavailable(IndicesOptions options) {
        return IndicesOptions.fromOptions(true, options.allowNoIndices(), options.expandWildcardsOpen(), options.expandWildcardsClosed(), options);
    }
}
