// Copyright © Amazon Web Services
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

package org.opengroup.osdu.step_definitions.querybycursor.singlecluster;

import org.opengroup.osdu.common.TestConstants;
import org.opengroup.osdu.common.querybycursor.singlecluster.QueryByCursorSteps;
import org.opengroup.osdu.util.AWSHTTPClient;
import org.opengroup.osdu.util.Config;
import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import java.util.List;

public class Steps extends QueryByCursorSteps {

    public Steps() {
        super(new AWSHTTPClient());
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
        this.httpClient = new AWSHTTPClient();
    }

    /******************Inputs being set**************/

    @When("^I limit the count of returned results to (-?\\d+)$")
    public void i_limit_the_count_of_returned_results_to(int limit) {
        super.i_limit_the_count_of_returned_results_to(limit);
    }

    @When("^I send None with \"(.*?)\"$")
    public void i_send_None_with(String kind) {
        super.i_send_None_with(kind);
    }

    @When("^I send \"(.*?)\" with \"(.*?)\"$")
    public void i_send_with(String query, String kind) {
        super.i_send_with(query, kind);
    }

    @When("^I want the results sorted by (.*?)$")
    public void i_sort_with(String sortJson) {
        super.i_sort_with(sortJson);
    }

    @When("^I want to search as owner (.*?)$")
    public void i_search_as(String isOwner) {
        super.i_search_as(isOwner);
    }

    @When("^I set the fields I want in response as ([\"(a-zA-Z0-9)\",?]*)$")
    public void i_set_the_fields_I_want_in_response_as(List<String> returnedFileds) {
        super.i_set_the_fields_I_want_in_response_as(returnedFileds);
    }

    @When("^I limit the count of returned results to None$$")
    public void offset_of_starting_point_as_None() {
        super.offset_of_starting_point_as_None();
    }

    @When("^I set an invalid cursor$")
    public void i_set_an_invalid_cursor() {
        super.i_set_an_invalid_cursor();
    }

    @When("^I send request to tenant \"(.*?)\"$")
    public void i_send_request_to_tenant(String tenant) {
        super.i_send_request_to_tenant(tenant);
    }

    @When("^I set autocomplete phrase to (.*?)$")
    public void i_set_autocomplete_phrase(String autocompletePhrase) {
        super.i_set_autocomplete_phrase(autocompletePhrase);
    }

    @When("^I apply geographical query on field \"(.*?)\"$")
    public void i_apply_geographical_query_on_field(String field) {
        super.i_apply_geographical_query_on_field(field);
    }

    @When("^define bounding box with points \\((-?\\d+), (-?\\d+)\\) and  \\((-?\\d+), (-?\\d+)\\)$")
    public void define_bounding_box_with_points_and(Double topLatitude, Double topLongitude, Double bottomLatitude, Double
            bottomLongitude) {
        super.define_bounding_box_with_points_and(topLatitude, topLongitude, bottomLatitude, bottomLongitude);
    }

    /******************Assert final response**************/

    @Then("^I should get in response (\\d+) records along with a cursor$")
    public void i_should_get_in_response_records_along_with_a_cursor(int resultCount) {
        super.i_should_get_in_response_records_along_with_a_cursor(resultCount);
    }

    @Then("^I should get in response (\\d+) records$")
    public void i_should_get_in_response_records(int resultCount) {
        super.i_should_get_in_response_records(resultCount);
    }

    @Then("^I should get records in right order first record id: \"(.*?)\", last record id: \"(.*?)\"$")
    public void i_should_get_records_in_right_order(String firstRecId, String lastRecId) {
        super.i_should_get_records_in_right_order(firstRecId, lastRecId);
    }

    @Then("^I should get ([^\"]*) response with reason: \"(.*?)\", message: \"(.*?)\" and errors: \"(.*?)\"$")
    public void i_should_get_response_with_reason_message_and_errors(List<Integer> codes, String type, String msg,
                                                                     String error) {
        super.i_should_get_response_with_reason_message_and_errors(codes, type, msg, error);
    }

    @Then("^I should get following autocomplete suggesstions (.*)")
    public void i_should_get_following_autocomplete_suggestions(String autocompleteOptions) {
        super.i_should_get_following_autocomplete_suggestions(autocompleteOptions);
    }

    @Override
    protected String getHttpMethod() {
        return "POST";
    }

    @Override
    protected String getApi() {
        String apiPath = System.getProperty(TestConstants.QUERY_WITH_CURSOR_PATH_PROP, TestConstants.SCROLL_CURSOR_PATH_VALUE);
        return Config.getSearchBaseURL() + apiPath;
    }
}
