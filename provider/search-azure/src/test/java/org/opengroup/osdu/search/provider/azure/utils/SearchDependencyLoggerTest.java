// Copyright 2017-2022, Schlumberger
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

package org.opengroup.osdu.search.provider.azure.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.DependencyPayload;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.provider.azure.utils.SearchDependencyLogger.CURSOR_QUERY_DEPENDENCY_NAME;
import static org.opengroup.osdu.search.provider.azure.utils.SearchDependencyLogger.QUERY_DEPENDENCY_NAME;

@ExtendWith(MockitoExtension.class)
public class SearchDependencyLoggerTest {

    private SearchDependencyLogger searchDependencyLogger = new SearchDependencyLogger();

    @Test
    public void should_call_logDependencyFromICoreLogger_whenLogDependencyIsCalled_forQueryRequest() {
        QueryRequest queryRequest = mock(QueryRequest.class);
        DependencyPayload payload = new DependencyPayload(QUERY_DEPENDENCY_NAME, "data", Duration.ofMillis(1000), String.valueOf(200), true);
        payload.setType("Elasticsearch");
        payload.setTarget("target");

        when(queryRequest.getQuery()).thenReturn("data");
        when(queryRequest.getKind()).thenReturn("target");

        assertDoesNotThrow(() -> searchDependencyLogger.log(queryRequest, 1000L, 200));
    }

    @Test
    public void should_call_logDependencyFromICoreLogger_whenLogDependencyIsCalled_forCursorQueryRequest_withSuccessCode() {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        DependencyPayload payload = new DependencyPayload(CURSOR_QUERY_DEPENDENCY_NAME, "data", Duration.ofMillis(1000), String.valueOf(200), true);
        payload.setType("Elasticsearch");
        payload.setTarget("target");

        when(cursorQueryRequest.getCursor()).thenReturn("data");

        assertDoesNotThrow(() -> searchDependencyLogger.log(cursorQueryRequest, 1000L, 200));
    }

    @Test
    public void should_call_logDependencyFromICoreLogger_whenLogDependencyIsCalled_forCursorQueryRequest_withNonSuccessCode() {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        DependencyPayload payload = new DependencyPayload(CURSOR_QUERY_DEPENDENCY_NAME, "data", Duration.ofMillis(1000), String.valueOf(200), true);
        payload.setType("Elasticsearch");
        payload.setTarget("target");

        when(cursorQueryRequest.getCursor()).thenReturn("data");

        assertDoesNotThrow(() -> searchDependencyLogger.log(cursorQueryRequest, 1000L, 500));
    }

    @Test
    public void should_doNothing_when_queryInstance_notMatch() {
        Query query = mock(Query.class);
        assertDoesNotThrow(() -> searchDependencyLogger.log(query, 1000L, 200));
    }
}
