// Copyright 2017-2021, Schlumberger
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

import com.google.gson.Gson;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.policy.service.PartitionPolicyStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QueryResponseUtil {

    @Autowired
    private DpsHeaders dpsHeaders;

    @Autowired(required = false)
    private IPolicyService iPolicyService;

    @Inject
    private PartitionPolicyStatusService statusService;

    private final Gson gson = new Gson();

    public List<Map<String, Object>> getQueryResponseResults(List<Map<String, Object>> results) {
        if(isPolicyEnabled()) {
            if(checkIfRequiredFieldsArePresent(results)) {
                List<RecordMetadata> policyMetadataList = getRecordMetadataListForPolicyEvaluation(results);
                List<String> policyEvaluatedRecords = iPolicyService.evaluateSearchDataAuthorizationPolicy(policyMetadataList, OperationType.view);
                return processAuthorizedResults(results, policyEvaluatedRecords);
            } else {
                throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "Returned fields must have acl, kind, legal and id");
            }
        } else {
            return results;
        }
    }

    private boolean checkIfRequiredFieldsArePresent(List<Map<String, Object>> results) {
        for(Map<String, Object> result : results) {
            if(!result.containsKey(RecordMetaAttribute.ACL.getValue())
                    || !result.containsKey(RecordMetaAttribute.KIND.getValue())
                    || !result.containsKey(RecordMetaAttribute.LEGAL.getValue())
                    || !result.containsKey(RecordMetaAttribute.ID.getValue()))
                return false;
        }
        return true;
    }

    private boolean isPolicyEnabled() {
        return this.iPolicyService != null && this.statusService.policyEnabled(this.dpsHeaders.getPartitionId());
    }

    private List<Map<String, Object>> processAuthorizedResults(List<Map<String, Object>> results, List<String> records) {
        List<Map<String, Object>> authorizedResults = new ArrayList<>();
        for(Map<String, Object> result : results) {
            if(records.contains(result.get("id").toString()))
                authorizedResults.add(result);
        }
        return authorizedResults;
    }

    private List<RecordMetadata> getRecordMetadataListForPolicyEvaluation(List<Map<String, Object>> results) {
        List<RecordMetadata> recordMetadataList = new ArrayList<>();
        for(Map<String, Object> result : results) {
            RecordMetadata recordMetadata = new RecordMetadata();
            recordMetadata.setAcl(gson.fromJson(result.get("acl").toString(), Acl.class));
            recordMetadata.setKind(result.get("kind").toString());
            recordMetadata.setLegal(gson.fromJson(result.get("legal").toString(), Legal.class));
            recordMetadata.setId(result.get("id").toString());
            recordMetadataList.add(recordMetadata);
        }
        return recordMetadataList;
    }
}
