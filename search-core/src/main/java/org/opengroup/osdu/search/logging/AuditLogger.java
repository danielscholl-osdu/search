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

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class AuditLogger {

    @Inject
    private JaxRsDpsLog logger;

    @Inject
    private DpsHeaders headers;

    private AuditEvents events = null;

    private AuditEvents getAuditEvents() {
        if (this.events == null) {
            this.events = new AuditEvents(this.headers.getUserEmail());
        }
        return this.events;
    }

    public void queryIndexSuccess(List<String> resources) {
        this.writeLog(this.getAuditEvents().getSuccessfulQueryIndexEvent(resources));
    }

    public void queryIndexFailed(List<String> resources) {
        this.writeLog(this.getAuditEvents().getFailedQueryIndexEvent(resources));
    }

    public void queryIndexWithCursorSuccess(List<String> resources) {
        this.writeLog(this.getAuditEvents().getSuccessfulQueryIndexWithCursorEvent(resources));
    }

    public void queryIndexWithCursorFailed(List<String> resources) {
        this.writeLog(this.getAuditEvents().getFailedQueryIndexWithCursorEvent(resources));
    }

    public void getIndexSchema(List<String> resources) {
        this.writeLog(this.getAuditEvents().getIndexSchemaEvent(resources));
    }

    public void deleteIndex(List<String> resources) {
        this.writeLog(this.getAuditEvents().getDeleteIndexEvent(resources));
    }

    public void updateSmartSearchCache(List<String> resources) {
        this.writeLog(this.getAuditEvents().getSmartSearchCacheUpdateEvent(resources));
    }

    private void writeLog(AuditPayload log) {
        this.logger.audit(log);
    }
}