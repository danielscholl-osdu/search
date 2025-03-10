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

import org.junit.Test;
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.opengroup.osdu.core.common.search.Config;

import static org.junit.Assert.assertEquals;

public class QueryUtilsTest {

    @Test
    public void should_returnDefaultLimit_when_limitUndefined() {
        assertEquals(Config.getQueryDefaultLimit(), QueryUtils.getResultSizeForQuery(0));
    }

    @Test
    public void should_returnMaxLimit_when_limitOver100() {
        assertEquals(Config.getQueryLimitMaximum(), QueryUtils.getResultSizeForQuery(5000));
    }

    @Test
    public void should_returnLimit_when_limitWithInRange() {
        assertEquals(8, QueryUtils.getResultSizeForQuery(8));
    }
}
