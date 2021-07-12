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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.client.ResponseException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ResponseExceptionParser {

    @Autowired
    private JaxRsDpsLog log;

    private ObjectMapper objectMapper = new ObjectMapper();

    public List<String> parseException(AppException e) {
        Exception cause = e.getOriginalException();
        if (cause == null || cause.getSuppressed() == null) {
            return new ArrayList<>();
        }

        for (Throwable t : cause.getSuppressed()) {
            if (!(t instanceof ResponseException)) continue;

            ResponseException responseException = (ResponseException) t;
            try {
                JsonNode exceptionNode = objectMapper.readTree(
                        responseException.getResponse().getEntity().getContent());
                Optional<JsonNode> rootCause = Optional.ofNullable(exceptionNode.get("error"))
                        .map(errorNode -> errorNode.get("root_cause"));

                return getReason(rootCause);
            } catch (IOException ioe) {
                this.log.error("Unable to parse response exception content", ioe);
            }
        }
        return new ArrayList<>();
    }

    private List<String> getReason(Optional<JsonNode> rootCause) {
        List<String> out = new ArrayList<>();
        if (rootCause.isPresent() && rootCause.get().isArray()) {
            ArrayNode arrayNode = (ArrayNode) rootCause.get();
            for (JsonNode c : arrayNode) out.add(c.get("reason").textValue());
        }
        return out;
    }
}