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

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuditEventsTest {

    @Test
    void should_throwException_when_creatingAuditEventsWithoutUser() {
        assertThrows(IllegalArgumentException.class, () -> new AuditEvents(null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createSuccessfulQueryIndexEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getSuccessfulQueryIndexEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Query index", payload.get("message"));
        assertEquals(AuditAction.READ, payload.get("action"));
        assertEquals("SE001", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createFailedQueryIndexEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getFailedQueryIndexEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.FAILURE, payload.get("status"));
        assertEquals("Query index", payload.get("message"));
        assertEquals(AuditAction.READ, payload.get("action"));
        assertEquals("SE001", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createSuccessfulQueryIndexWithCursorEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getSuccessfulQueryIndexWithCursorEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Query index with cursor", payload.get("message"));
        assertEquals(AuditAction.READ, payload.get("action"));
        assertEquals("SE002", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createFailedQueryIndexWithCursorEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getFailedQueryIndexWithCursorEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.FAILURE, payload.get("status"));
        assertEquals("Query index with cursor", payload.get("message"));
        assertEquals(AuditAction.READ, payload.get("action"));
        assertEquals("SE002", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createIndexSchemaEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexSchemaEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Get index schema", payload.get("message"));
        assertEquals(AuditAction.READ, payload.get("action"));
        assertEquals("SE003", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createDeleteIndexEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getDeleteIndexEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Delete index", payload.get("message"));
        assertEquals(AuditAction.DELETE, payload.get("action"));
        assertEquals("SE004", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createUpdateSmartSearchCacheEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getSmartSearchCacheUpdateEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Update smart search cache", payload.get("message"));
        assertEquals(AuditAction.UPDATE, payload.get("action"));
        assertEquals("SE005", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }
}
