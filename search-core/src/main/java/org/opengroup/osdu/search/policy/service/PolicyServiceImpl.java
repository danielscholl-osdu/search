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

package org.opengroup.osdu.search.policy.service;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.policy.IPolicyFactory;
import org.opengroup.osdu.core.common.policy.IPolicyProvider;
import org.opengroup.osdu.core.common.policy.PolicyException;
import org.opengroup.osdu.search.policy.di.PolicyServiceConfiguration;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolicyServiceImpl implements IPolicyService {

  @Autowired
  private PolicyServiceConfiguration policyServiceConfiguration;

  @Autowired
  private IPolicyFactory policyFactory;

  @Autowired
  private DpsHeaders headers;

  @Override
  public String getCompiledPolicy(IProviderHeaderService providerHeaderService) {
    try {
      String groups = this.headers.getHeaders().get(providerHeaderService.getDataGroupsHeader());
      List<String> groupsList =
          groups != null
              ? new ArrayList<>(Arrays.asList(groups.trim().split("\\s*,\\s*")))
              : new ArrayList<>();
      Map<String, Object> input = new HashMap<>();
      input.put("groups", groupsList);
      input.put("operation", "view");
      ArrayList<String> unknownsList = new ArrayList<>(Collections.singletonList("input.record"));

      // Handle instance/partition policy
      String searchPolicyId =
          String.format(this.policyServiceConfiguration.getId(), this.headers.getPartitionId());
      String searchPolicyRule = "data." + searchPolicyId + ".allow == true";

      IPolicyProvider serviceClient = this.policyFactory.create(this.headers);
      String esQuery = serviceClient.getCompiledPolicy(searchPolicyRule, unknownsList, input);
      return esQuery.substring(9, esQuery.length() - 1);
    } catch (PolicyException e) {
      HttpResponse httpResponse = e.getHttpResponse();
      String errorMessage =
          StringUtils.isBlank(e.getMessage())
              ? "Error making request to Policy service"
              : e.getMessage();
      throw new AppException(
          httpResponse.getResponseCode(),
          errorMessage,
          httpResponse.getBody(),
          "Error calling translate endpoint",
          e);
    } catch (Exception e) {
      String errorMessage =
          StringUtils.isBlank(e.getMessage())
              ? "Error making request to Policy service"
              : e.getMessage();
      throw new AppException(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          "Policy service unavailable",
          errorMessage,
          "Error calling translate endpoint",
          e);
    }
  }
}
