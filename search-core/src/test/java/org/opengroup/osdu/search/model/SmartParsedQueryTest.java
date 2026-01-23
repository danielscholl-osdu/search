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

package org.opengroup.osdu.search.model;

import org.opengroup.osdu.core.common.model.search.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SmartParsedQueryTest {

    @Test
    void ctor_fromCoreQuery_mapsFields_andEscapesQuery() {
        Query core = mock(Query.class);

        when(core.getKind()).thenReturn("osdu:well:1.0.0");
        when(core.getQuery()).thenReturn("{\"path\":\"a\\b\",\"k\":\"v\"}");
        when(core.isQueryAsOwner()).thenReturn(true);

        SmartParsedQuery spq = new SmartParsedQuery(core);

        assertEquals("osdu:well:1.0.0", spq.getKind());
        assertTrue(spq.isQueryAsOwner());

        String expected = "\"{\\\"path\\\":\\\"a\\\\b\\\",\\\"k\\\":\\\"v\\\"}\"";
        assertEquals(expected, spq.getQuery());
    }
}
