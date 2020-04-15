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
import org.junit.Test;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;

import java.util.Map;

public class AuditEventsTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throwException_when_creatingAuditEventsWithoutUser() {
        new AuditEvents(null);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createQueryIndexEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getQueryIndexEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        org.junit.Assert.assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        org.junit.Assert.assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        org.junit.Assert.assertEquals("Query index", payload.get("message"));
        org.junit.Assert.assertEquals(AuditAction.READ, payload.get("action"));
        org.junit.Assert.assertEquals("SE001", payload.get("actionId"));
        org.junit.Assert.assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createQueryIndexWithCursorEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getQueryIndexWithCursorEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        org.junit.Assert.assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        org.junit.Assert.assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        org.junit.Assert.assertEquals("Query index with cursor", payload.get("message"));
        org.junit.Assert.assertEquals(AuditAction.READ, payload.get("action"));
        org.junit.Assert.assertEquals("SE002", payload.get("actionId"));
        org.junit.Assert.assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createIndexSchemaEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexSchemaEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        org.junit.Assert.assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        org.junit.Assert.assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        org.junit.Assert.assertEquals("Get index schema", payload.get("message"));
        org.junit.Assert.assertEquals(AuditAction.READ, payload.get("action"));
        org.junit.Assert.assertEquals("SE003", payload.get("actionId"));
        org.junit.Assert.assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createDeleteIndexEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getDeleteIndexEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        org.junit.Assert.assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        org.junit.Assert.assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        org.junit.Assert.assertEquals("Delete index", payload.get("message"));
        org.junit.Assert.assertEquals(AuditAction.DELETE, payload.get("action"));
        org.junit.Assert.assertEquals("SE004", payload.get("actionId"));
        org.junit.Assert.assertEquals("testUser", payload.get("user"));
    }
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_createUpdateSmartSearchCacheEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getSmartSearchCacheUpdateEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        org.junit.Assert.assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        org.junit.Assert.assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        org.junit.Assert.assertEquals("Update smart search cache", payload.get("message"));
        org.junit.Assert.assertEquals(AuditAction.UPDATE, payload.get("action"));
        org.junit.Assert.assertEquals("SE005", payload.get("actionId"));
        org.junit.Assert.assertEquals("testUser", payload.get("user"));
    }
}
