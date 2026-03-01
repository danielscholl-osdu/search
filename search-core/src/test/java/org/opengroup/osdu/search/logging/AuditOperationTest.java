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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;

public class AuditOperationTest {

    @Test
    void should_haveCorrectRolesForQueryIndex() {
        List<String> roles = AuditOperation.QUERY_INDEX.getRequiredGroups();
        assertEquals(2, roles.size());
        assertTrue(roles.containsAll(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)));
    }

    @Test
    void should_haveCorrectRolesForQueryIndexWithCursor() {
        List<String> roles = AuditOperation.QUERY_INDEX_WITH_CURSOR.getRequiredGroups();
        assertEquals(2, roles.size());
        assertTrue(roles.containsAll(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)));
    }

    @Test
    void should_haveCorrectRolesForGetIndexSchema() {
        List<String> roles = AuditOperation.GET_INDEX_SCHEMA.getRequiredGroups();
        assertEquals(2, roles.size());
        assertTrue(roles.containsAll(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)));
    }

    @Test
    void should_haveCorrectRolesForDeleteIndex() {
        List<String> roles = AuditOperation.DELETE_INDEX.getRequiredGroups();
        assertEquals(2, roles.size());
        assertTrue(roles.containsAll(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)));
    }

    @Test
    void should_haveCorrectRolesForUpdateSmartSearchCache() {
        List<String> roles = AuditOperation.UPDATE_SMART_SEARCH_CACHE.getRequiredGroups();
        assertEquals(2, roles.size());
        assertTrue(roles.containsAll(Arrays.asList(SearchServiceRole.ADMIN, SearchServiceRole.USER)));
    }

    @Test
    void should_returnUnmodifiableList() {
        List<String> roles = AuditOperation.QUERY_INDEX.getRequiredGroups();
        assertNotNull(roles);
        assertThrows(UnsupportedOperationException.class, () -> roles.add("should-fail"));
    }

    @Test
    void should_haveAllOperationsDefined() {
        for (AuditOperation op : AuditOperation.values()) {
            assertNotNull(op.getRequiredGroups());
            assertTrue(op.getRequiredGroups().size() > 0, op.name() + " should have at least one required group");
        }
    }
}
