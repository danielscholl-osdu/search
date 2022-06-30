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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditLoggerTest {

    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private DpsHeaders headers;

    @InjectMocks
    private AuditLogger sut;

    @Before
    public void setup() {
        when(this.headers.getUserEmail()).thenReturn("testUser");
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void should_createAuditLogEvent_when_queryIndex() {
        this.sut.queryIndexSuccess(Lists.newArrayList("anything"));

        ArgumentCaptor<AuditPayload> payloadCaptor = ArgumentCaptor.forClass(AuditPayload.class);

        verify(this.logger).audit(payloadCaptor.capture());

        AuditPayload payload = payloadCaptor.getValue();
        assertEquals("SE001", ((Map) payload.get("auditLog")).get("actionId"));
        assertEquals("testUser", ((Map) payload.get("auditLog")).get("user"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void should_createAuditLogEvent_when_queryIndexWithCursor() {
        this.sut.queryIndexWithCursorSuccess(Lists.newArrayList("anything"));

        ArgumentCaptor<AuditPayload> payloadCaptor = ArgumentCaptor.forClass(AuditPayload.class);

        verify(this.logger).audit(payloadCaptor.capture());

        AuditPayload payload = payloadCaptor.getValue();
        assertEquals("SE002", ((Map) payload.get("auditLog")).get("actionId"));
        assertEquals("testUser", ((Map) payload.get("auditLog")).get("user"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void should_createAuditLogEvent_when_getIndexSchema() {
        this.sut.getIndexSchema(Lists.newArrayList("anything"));

        ArgumentCaptor<AuditPayload> payloadCaptor = ArgumentCaptor.forClass(AuditPayload.class);

        verify(this.logger).audit(payloadCaptor.capture());

        AuditPayload payload = payloadCaptor.getValue();
        assertEquals("SE003", ((Map) payload.get("auditLog")).get("actionId"));
        assertEquals("testUser", ((Map) payload.get("auditLog")).get("user"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void should_createAuditLogEvent_when_deleteIndex() {
        this.sut.deleteIndex(Lists.newArrayList("anything"));

        ArgumentCaptor<AuditPayload> payloadCaptor = ArgumentCaptor.forClass(AuditPayload.class);

        verify(this.logger).audit(payloadCaptor.capture());

        AuditPayload payload = payloadCaptor.getValue();
        assertEquals("SE004", ((Map) payload.get("auditLog")).get("actionId"));
        assertEquals("testUser", ((Map) payload.get("auditLog")).get("user"));
    }
}
