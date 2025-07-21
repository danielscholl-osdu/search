/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.step_definitions.query.system_metadata;


import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.common.query.singlecluster.QuerySteps;
import org.opengroup.osdu.util.HTTPClient;
import org.opengroup.osdu.util.Config;

public class Steps extends QuerySteps {

    public Steps() {
        super(new HTTPClient());
    }

    @Given("^the schema is created with the following kind$")
    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        super.the_schema_is_created_with_the_following_kind(dataTable);
    }

    @When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\"$")
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        super.i_ingest_records_with_the_for_a_given(record, dataGroup, kind);
    }

    @Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
        this.httpClient = new HTTPClient();
    }

    /******************Inputs being set**************/
    @When("^I send \"(.*?)\" with \"(.*?)\"$")
    public void i_send_with(String query, String kind) {
        super.i_send_with(query, kind);
    }

    @When("^I want to aggregate by \"(.*?)\"$")
    public void i_aggregate_by(String aggField) throws Throwable {
        super.i_aggregate_by(aggField);
    }

    /******************Assert final response**************/
    @Then("^I should get \"(.*?)\" and should not get \"(.*?)\" from aggregations$")
    public void i_should_get_included_kinds_and_should_not_get_excluded_kinds_from_aggregation(String includedKinds, String excludedKinds) {
        super.i_should_get_included_kinds_and_should_not_get_excluded_kinds_from_aggregation(includedKinds, excludedKinds);
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
