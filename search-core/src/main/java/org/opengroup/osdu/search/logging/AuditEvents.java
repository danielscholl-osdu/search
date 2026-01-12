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

package org.opengroup.osdu.search.logging;

import com.google.common.base.Strings;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;

import java.util.List;

import static org.opengroup.osdu.core.common.logging.audit.AuditPayload.builder;

public class AuditEvents {

    private static final String QUERY_INDEX_ACTION_ID = "SE001";
    private static final String QUERY_INDEX_OPERATION = "Query index";

    private static final String QUERY_INDEX_WITH_CURSOR_ACTION_ID = "SE002";
    private static final String QUERY_INDEX_WITH_CURSOR_OPERATION = "Query index with cursor";

    private static final String GET_INDEX_SCHEMA_ACTION_ID = "SE003";
    private static final String GET_INDEX_SCHEMA_OPERATION = "Get index schema";

    private static final String DELETE_INDEX_ACTION_ID = "SE004";
    private static final String DELETE_INDEX_OPERATION = "Delete index";

    private static final String UPDATE_SMART_SEARCH_CACHE_ACTION_ID = "SE005";
    private static final String UPDATE_SMART_SEARCH_CACHE_OPERATION = "Update smart search cache";

    private final String user;

    public AuditEvents(String user) {
        if (Strings.isNullOrEmpty(user)) {
            throw new IllegalArgumentException("User not provided for audit events.");
        }
        this.user = user;
    }

    public AuditPayload getSuccessfulQueryIndexEvent(List<String> resources) {
        return builder()
                .action(AuditAction.READ)
                .status(AuditStatus.SUCCESS)
                .actionId(QUERY_INDEX_ACTION_ID)
                .message(QUERY_INDEX_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getFailedQueryIndexEvent(List<String> resources) {
        return builder()
                .action(AuditAction.READ)
                .status(AuditStatus.FAILURE)
                .actionId(QUERY_INDEX_ACTION_ID)
                .message(QUERY_INDEX_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getSuccessfulQueryIndexWithCursorEvent(List<String> resources) {
        return builder()
                .action(AuditAction.READ)
                .status(AuditStatus.SUCCESS)
                .actionId(QUERY_INDEX_WITH_CURSOR_ACTION_ID)
                .message(QUERY_INDEX_WITH_CURSOR_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getFailedQueryIndexWithCursorEvent(List<String> resources) {
        return builder()
                .action(AuditAction.READ)
                .status(AuditStatus.FAILURE)
                .actionId(QUERY_INDEX_WITH_CURSOR_ACTION_ID)
                .message(QUERY_INDEX_WITH_CURSOR_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexSchemaEvent(List<String> resources) {
        return builder()
                .action(AuditAction.READ)
                .status(AuditStatus.SUCCESS)
                .actionId(GET_INDEX_SCHEMA_ACTION_ID)
                .message(GET_INDEX_SCHEMA_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getDeleteIndexEvent(List<String> resources) {
        return builder()
                .action(AuditAction.DELETE)
                .status(AuditStatus.SUCCESS)
                .actionId(DELETE_INDEX_ACTION_ID)
                .message(DELETE_INDEX_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getSmartSearchCacheUpdateEvent(List<String> resources) {
        return builder()
                .action(AuditAction.UPDATE)
                .status(AuditStatus.SUCCESS)
                .actionId(UPDATE_SMART_SEARCH_CACHE_ACTION_ID)
                .message(UPDATE_SMART_SEARCH_CACHE_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }
}