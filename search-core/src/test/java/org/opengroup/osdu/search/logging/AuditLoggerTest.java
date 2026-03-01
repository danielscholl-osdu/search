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

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuditLoggerTest {

    private static final String TEST_USER = "testUser";
    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_USER_AGENT = "TestAgent/1.0";
    private static final String TEST_AUTHORIZED_GROUP = "users.datalake.viewers";

    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private DpsHeaders headers;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuditLogger sut;

    private List<String> resources;
    private AuditEvents auditEvents;

    @BeforeEach
    public void setup() {
        resources = Collections.singletonList("anything");

        auditEvents = new AuditEvents(TEST_USER, TEST_IP, TEST_USER_AGENT, TEST_AUTHORIZED_GROUP);

        when(headers.getUserEmail()).thenReturn(TEST_USER);
        when(headers.getUserAuthorizedGroupName()).thenReturn(TEST_AUTHORIZED_GROUP);
        when(httpServletRequest.getRemoteAddr()).thenReturn(TEST_IP);
        lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn(TEST_USER_AGENT);
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    @Test
    public void should_createAuditLogEvent_when_queryIndex() {
        sut.queryIndexSuccess(resources);

        verify(logger).audit(auditEvents.getSuccessfulQueryIndexEvent(resources));
    }

    @Test
    public void should_createAuditLogEvent_when_queryIndexFailed() {
        sut.queryIndexFailed(resources);

        verify(logger).audit(auditEvents.getFailedQueryIndexEvent(resources));
    }

    @Test
    public void should_createAuditLogEvent_when_queryIndexWithCursor() {
        sut.queryIndexWithCursorSuccess(resources);

        verify(logger).audit(auditEvents.getSuccessfulQueryIndexWithCursorEvent(resources));
    }

    @Test
    public void should_createAuditLogEvent_when_queryIndexWithCursorFailed() {
        sut.queryIndexWithCursorFailed(resources);

        verify(logger).audit(auditEvents.getFailedQueryIndexWithCursorEvent(resources));
    }

    @Test
    public void should_createAuditLogEvent_when_getIndexSchema() {
        sut.getIndexSchema(resources);

        verify(logger).audit(auditEvents.getIndexSchemaEvent(resources));
    }

    @Test
    public void should_createAuditLogEvent_when_deleteIndex() {
        sut.deleteIndex(resources);

        verify(logger).audit(auditEvents.getDeleteIndexEvent(resources));
    }

    @Test
    public void should_createAuditLogEvent_when_updateSmartSearchCache() {
        sut.updateSmartSearchCache(resources);

        verify(logger).audit(auditEvents.getSmartSearchCacheUpdateEvent(resources));
    }

    @Test
    public void should_useUnknownFallback_whenUserIsEmpty() {
        when(headers.getUserEmail()).thenReturn("");
        when(headers.getUserAuthorizedGroupName()).thenReturn("");

        sut.queryIndexSuccess(resources);

        AuditEvents fallbackEvents = new AuditEvents("", TEST_IP, TEST_USER_AGENT, "");
        verify(logger).audit(fallbackEvents.getSuccessfulQueryIndexEvent(resources));
    }
}
