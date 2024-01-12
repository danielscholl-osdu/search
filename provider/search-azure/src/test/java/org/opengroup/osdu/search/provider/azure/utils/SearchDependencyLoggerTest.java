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

import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.provider.azure.utils.SearchDependencyLogger.QUERY_DEPENDENCY_NAME;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.logging.DependencyPayload;

import java.time.Duration;
import org.opengroup.osdu.core.common.model.search.QueryRequest;

@RunWith(MockitoJUnitRunner.class)
public class SearchDependencyLoggerTest {

    private SearchDependencyLogger searchDependencyLogger = new SearchDependencyLogger();
    @Mock
    private QueryRequest queryRequest;

    @Test
    public void should_call_logDependencyFromICoreLogger_whenLogDependencyIsCalled() {
        DependencyPayload payload = new DependencyPayload(QUERY_DEPENDENCY_NAME, "data", Duration.ofMillis(1000), String.valueOf(200), true);
        payload.setType("Elasticsearch");
        payload.setTarget("target");
        when(queryRequest.getQuery()).thenReturn("data");
        when(queryRequest.getKind()).thenReturn("target");
        Assertions.assertDoesNotThrow(() -> searchDependencyLogger.log(queryRequest, 1000L, 200));
    }
}
