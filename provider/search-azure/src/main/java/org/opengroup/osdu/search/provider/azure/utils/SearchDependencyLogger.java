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

import java.time.Duration;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyPayload;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.search.util.ITracingLogger;
import org.springframework.stereotype.Component;

@Component
public class SearchDependencyLogger implements ITracingLogger {

    public static final String QUERY_DEPENDENCY_NAME = "QUERY_ELASTICSEARCH";
    public static final String CURSOR_QUERY_DEPENDENCY_NAME = "CURSOR_QUERY_ELASTICSEARCH";

    private static final String ECK_DEPENDENCY_TYPE = "Elasticsearch";
    private static final String ECK_LOGGER = "ElasticCluster";

    /**
     * Log dependency.
     *
     * @param searchRequest contains the command and the target of this dependency call, the name of
     *                      the command is defined by the class
     * @param latency       the request duration in milliseconds
     * @param statusCode    the result code of the call
     */
    @Override
    public void log(Query searchRequest, Long latency, int statusCode) {
        String dependencyName = "";
        String data = "";

        if(searchRequest instanceof QueryRequest qR){
            dependencyName = QUERY_DEPENDENCY_NAME;
            data = qR.getQuery();
        }else if(searchRequest instanceof CursorQueryRequest cQR){
            dependencyName = CURSOR_QUERY_DEPENDENCY_NAME;
            data = String.format("cursor:%s", cQR.getCursor());
        }else {
            return;
        }

        String target = String.valueOf(searchRequest.getKind());
        boolean success = statusCode == HttpStatus.SC_OK;

        DependencyPayload payload = new DependencyPayload(
            dependencyName,
            data,
            Duration.ofMillis(latency),
            String.valueOf(statusCode),
            success
        );

        payload.setType(ECK_DEPENDENCY_TYPE);
        payload.setTarget(target);
        CoreLoggerFactory.getInstance().getLogger(ECK_LOGGER).logDependency(payload);
    }
}
