/*
 *    Copyright (c) 2024. EPAM Systems, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.opengroup.osdu.step_definitions.query.singlecluster.collaboration;


import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.common.query.singlecluster.collaboration.QueryCollaborationSteps;
import org.opengroup.osdu.util.AWSHTTPClient;
import org.opengroup.osdu.util.Config;

import java.util.List;

public class Steps extends QueryCollaborationSteps {

    public Steps() {
        super(new AWSHTTPClient());
    }

    @When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\" with \"(.*?)\" header$")
    public void i_ingest_records_with_the_for_a_given_with_header(String record, String dataGroup, String kind, String xCollaborationHeader) {
        super.i_ingest_records_with_the_for_a_given_with_header(record, dataGroup, kind, xCollaborationHeader);
    }

    @When("^I send request with \"(.*?)\" header$")
    public void i_send_request_with_xcolab_header(String xCollaborationHeader) {
        super.i_send_request_with_xcollab_header(xCollaborationHeader);
    }

    @Override
    protected String getHttpMethod() {
        return "POST";
    }

    @Override
    protected String getApi() {
        return Config.getSearchBaseURL() + "query";
    }
}
