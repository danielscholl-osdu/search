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

import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyPayload;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DependencyLogger {

    private static final String ECK_DEPENDENCY_TYPE = "Elasticsearch";
    private static final String ECK_LOGGER = "ElasticCluster";

    /**
     * Log dependency.
     *
     * @param name          the name of the command initiated with this dependency call
     * @param data          the command initiated by this dependency call
     * @param target        the target of this dependency call
     * @param timeTakenInMs the request duration in milliseconds
     * @param resultCode    the result code of the call
     * @param success       indication of successful or unsuccessful call
     */
    public void logDependency(final String name, final String data, final String target, final long timeTakenInMs, final int resultCode, final boolean success) {
        DependencyPayload payload = new DependencyPayload(name, data, Duration.ofMillis(timeTakenInMs), String.valueOf(resultCode), success);
        payload.setType(ECK_DEPENDENCY_TYPE);
        payload.setTarget(target);
        CoreLoggerFactory.getInstance().getLogger(ECK_LOGGER).logDependency(payload);
    }
}