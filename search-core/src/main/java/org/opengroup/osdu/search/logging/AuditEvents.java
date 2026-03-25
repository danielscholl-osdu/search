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
import org.opengroup.osdu.core.common.logging.audit.AuditPayload.AuditPayloadBuilder;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;

import java.util.List;

public class AuditEvents {

    private static final String UNKNOWN = "unknown";
    private static final String UNKNOWN_IP = "0.0.0.0";

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
    private final String userIpAddress;
    private final String userAgent;
    private final String userAuthorizedGroupName;

    public AuditEvents(String user, String userIpAddress, String userAgent, String userAuthorizedGroupName) {
        this.user = Strings.isNullOrEmpty(user) ? UNKNOWN : user;
        this.userIpAddress = Strings.isNullOrEmpty(userIpAddress) ? UNKNOWN_IP : userIpAddress;
        this.userAgent = Strings.isNullOrEmpty(userAgent) ? UNKNOWN : userAgent;
        this.userAuthorizedGroupName = Strings.isNullOrEmpty(userAuthorizedGroupName) ? UNKNOWN : userAuthorizedGroupName;
    }

    private AuditPayloadBuilder createAuditPayloadBuilder(List<String> requiredGroupsForAction, AuditStatus status, String actionId) {
        return AuditPayload.builder()
                .status(status)
                .user(this.user)
                .actionId(actionId)
                .requiredGroupsForAction(requiredGroupsForAction)
                .userIpAddress(this.userIpAddress)
                .userAgent(this.userAgent)
                .userAuthorizedGroupName(this.userAuthorizedGroupName);
    }

    public AuditPayload getSuccessfulQueryIndexEvent(List<String> resources) {
        return createAuditPayloadBuilder(AuditOperation.QUERY_INDEX.getRequiredGroups(), AuditStatus.SUCCESS, QUERY_INDEX_ACTION_ID)
                .action(AuditAction.READ)
                .message(QUERY_INDEX_OPERATION)
                .resources(resources)
                .build();
    }

    public AuditPayload getFailedQueryIndexEvent(List<String> resources) {
        return createAuditPayloadBuilder(AuditOperation.QUERY_INDEX.getRequiredGroups(), AuditStatus.FAILURE, QUERY_INDEX_ACTION_ID)
                .action(AuditAction.READ)
                .message(QUERY_INDEX_OPERATION)
                .resources(resources)
                .build();
    }

    public AuditPayload getSuccessfulQueryIndexWithCursorEvent(List<String> resources) {
        return createAuditPayloadBuilder(AuditOperation.QUERY_INDEX_WITH_CURSOR.getRequiredGroups(), AuditStatus.SUCCESS, QUERY_INDEX_WITH_CURSOR_ACTION_ID)
                .action(AuditAction.READ)
                .message(QUERY_INDEX_WITH_CURSOR_OPERATION)
                .resources(resources)
                .build();
    }

    public AuditPayload getFailedQueryIndexWithCursorEvent(List<String> resources) {
        return createAuditPayloadBuilder(AuditOperation.QUERY_INDEX_WITH_CURSOR.getRequiredGroups(), AuditStatus.FAILURE, QUERY_INDEX_WITH_CURSOR_ACTION_ID)
                .action(AuditAction.READ)
                .message(QUERY_INDEX_WITH_CURSOR_OPERATION)
                .resources(resources)
                .build();
    }

    public AuditPayload getIndexSchemaEvent(List<String> resources) {
        return createAuditPayloadBuilder(AuditOperation.GET_INDEX_SCHEMA.getRequiredGroups(), AuditStatus.SUCCESS, GET_INDEX_SCHEMA_ACTION_ID)
                .action(AuditAction.READ)
                .message(GET_INDEX_SCHEMA_OPERATION)
                .resources(resources)
                .build();
    }

    public AuditPayload getDeleteIndexEvent(List<String> resources) {
        return createAuditPayloadBuilder(AuditOperation.DELETE_INDEX.getRequiredGroups(), AuditStatus.SUCCESS, DELETE_INDEX_ACTION_ID)
                .action(AuditAction.DELETE)
                .message(DELETE_INDEX_OPERATION)
                .resources(resources)
                .build();
    }

    public AuditPayload getSmartSearchCacheUpdateEvent(List<String> resources) {
        return createAuditPayloadBuilder(AuditOperation.UPDATE_SMART_SEARCH_CACHE.getRequiredGroups(), AuditStatus.SUCCESS, UPDATE_SMART_SEARCH_CACHE_ACTION_ID)
                .action(AuditAction.UPDATE)
                .message(UPDATE_SMART_SEARCH_CACHE_OPERATION)
                .resources(resources)
                .build();
    }
}
