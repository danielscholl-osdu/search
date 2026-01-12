/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.step_definitions.health;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.common.health.HealthSteps;
import org.opengroup.osdu.util.AnthosHTTPClient;

public class Steps extends HealthSteps {

  public Steps() {
    super(new AnthosHTTPClient());
  }

  @Before
  public void before(Scenario scenario) {
    this.scenario = scenario;
    this.httpClient = new AnthosHTTPClient();
  }

  @When("^I send get request to liveness check endpoint")
  public void i_send_get_request_to_liveness_endpoint() {
    super.i_send_get_request_to_liveness_endpoint();
  }

  @Then("^service should respond back with 200 in response$")
  public void i_should_get_OK_in_response() {
    super.i_should_get_OK_in_response();
  }

  @When("^I send get request to readiness check endpoint")
  public void i_send_get_request_to_readiness_endpoint() {
    super.i_send_get_request_to_readiness_endpoint();
  }

  @Override
  protected String getHttpMethod() {
    return "GET";
  }
}
