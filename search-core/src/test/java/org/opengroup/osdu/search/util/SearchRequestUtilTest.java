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
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class SearchRequestUtilTest {
    private SearchRequestUtil searchRequestUtil = new SearchRequestUtil();

    @Test
    public void setIgnoreUnavailable_to_true() {
        SearchRequest searchRequest = new SearchRequest("index1");
        searchRequestUtil.setIgnoreUnavailable(searchRequest, true);
        Assert.assertTrue(searchRequest.indicesOptions().ignoreUnavailable());
    }

    @Test
    public void setIgnoreUnavailable_to_false() {
        SearchRequest searchRequest = new SearchRequest("index1");
        searchRequestUtil.setIgnoreUnavailable(searchRequest, false);
        Assert.assertFalse(searchRequest.indicesOptions().ignoreUnavailable());
    }
}
