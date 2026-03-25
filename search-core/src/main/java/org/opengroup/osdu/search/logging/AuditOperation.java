// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.search.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;

public enum AuditOperation {
    QUERY_INDEX(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)),
    QUERY_INDEX_WITH_CURSOR(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)),
    GET_INDEX_SCHEMA(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)),
    DELETE_INDEX(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)),
    UPDATE_SMART_SEARCH_CACHE(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER));

    private final List<String> requiredGroups;

    AuditOperation(List<String> requiredGroups) {
        this.requiredGroups = Collections.unmodifiableList(requiredGroups);
    }

    public List<String> getRequiredGroups() {
        return requiredGroups;
    }
}
