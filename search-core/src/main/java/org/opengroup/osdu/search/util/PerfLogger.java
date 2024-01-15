/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.util;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.search.Query;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "default.elastic.performance.logger", havingValue = "true", matchIfMissing = true)
public class PerfLogger implements IPerfLogger {


  @Override
  public void log(Query searchRequest, Long latency, int statusCode) {
    log.debug("Query: {}, Kind: {}, Latency: {}, Status code: {}", searchRequest.getQuery(),
        searchRequest.getKind(), latency, statusCode);
  }
}
