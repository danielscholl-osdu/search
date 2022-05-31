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

package org.opengroup.osdu.step_definitions.query.singlecluster;


import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import java.util.List;
import org.opengroup.osdu.common.query.singlecluster.QuerySteps;
import org.opengroup.osdu.util.AnthosHTTPClient;
import org.opengroup.osdu.util.Config;

public class Steps extends QuerySteps {

    public Steps() {
        super(new AnthosHTTPClient());
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
        this.httpClient = new AnthosHTTPClient();
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

    @When("^I want to aggregate by \"(.*?)\"$")
    public void i_aggregate_by(String aggField) throws Throwable {
        super.i_aggregate_by(aggField);
    }

    @When("^I want to search as owner (.*?)$")
    public void i_search_as(String isOwner) {
        super.i_search_as(isOwner);
    }

    @When("^I set the fields I want in response as ([\"(a-zA-Z0-9)\",?]*)$")
    public void i_set_the_fields_I_want_in_response_as(List<String> returnedFileds) {
        super.i_set_the_fields_I_want_in_response_as(returnedFileds);
    }

    @When("^I set the offset of starting point as None$$|^I limit the count of returned results to None$$")
    public void offset_of_starting_point_as_None() {
        super.offset_of_starting_point_as_None();
    }

    @When("^I set the offset of starting point as (-?\\d+)$")
    public void i_set_the_offset_of_starting_point_as(int offset) {
        super.i_set_the_offset_of_starting_point_as(offset);
    }

    @When("^I send request to tenant \"(.*?)\"$")
    public void i_send_request_to_tenant(String tenant) {
        super.i_send_request_to_tenant(tenant);
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

    @When("^define bounding box with points \\(None, None\\) and  \\((\\d+), (\\d+)\\)$")
    public void define_bounding_box_with_points_None_None_and(Double bottomLatitude, Double bottomLongitude) {
        super.define_bounding_box_with_points_None_None_and(bottomLatitude, bottomLongitude);
    }

    @When("^define focus coordinates as \\((-?\\d+), (-?\\d+)\\) and search in a (\\d+) radius$")
    public void define_focus_coordinates_as_and_search_in_a_radius(Double latitude, Double longitude, int distance) {
        super.define_focus_coordinates_as_and_search_in_a_radius(latitude, longitude, distance);
    }

    @When("^define intersection polygon with points \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\)")
    public void define_intersection_polygon_with_points(Double latitude1, Double longitude1, Double latitude2, Double longitude2,
        Double latitude3, Double longitude3, Double latitude4, Double longitude4,
        Double latitude5, Double longitude5) {
        super.define_intersection_polygon_with_points(latitude1, longitude1, latitude2, longitude2, latitude3, longitude3, latitude4, longitude4, latitude5,
            longitude5);
    }

    @When("^define within polygon with points \\((-?\\d+), (-?\\d+)\\)")
    public void define_within_polygon_with_points(Double latitude1, Double longitude1) {
        super.define_within_polygon_with_points(latitude1, longitude1);
    }

    /******************Assert final response**************/

    @Then("^I should get in response (\\d+) records$")
    public void i_should_get_in_response_records(int resultCount) {
        super.i_should_get_in_response_records(resultCount);
    }

    @Then("^I should get in response (\\d+) records with ([\"(a-zA-Z0-9)\",?]*)$")
    public void i_should_get_in_response_records_with_fields(int resultCount, List<String> returnedFields) {
        super.i_should_get_in_response_records(resultCount, returnedFields);
    }

    @Then("^I should get records in right order first record id: \"(.*?)\", last record id: \"(.*?)\"$")
    public void i_should_get_records_in_right_order(String firstRecId, String lastRecId) {
        super.i_should_get_records_in_right_order(firstRecId, lastRecId);
    }

    @Then("^I should get (\\d+) unique values")
    public void i_should_get_aggregation_with_unique_values(int uniqueValueCount) {
        super.i_should_get_aggregation_with_unique_values(uniqueValueCount);
    }

    @Then("^I should get ([^\"]*) response with reason: \"(.*?)\", message: \"(.*?)\" and errors: \"(.*?)\"$")
    public void i_should_get_response_with_reason_message_and_errors(List<Integer> codes, String type, String msg, String error) {
        super.i_should_get_response_with_reason_message_and_errors(codes, type, msg, error);
    }

    @When("^define geo polygon with following points ([^\"]*)$")
    public void define_geo_polygon_with_following_points_points_list(List<String> points) {
        super.define_geo_polygon_with_following_points_points_list(points);
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