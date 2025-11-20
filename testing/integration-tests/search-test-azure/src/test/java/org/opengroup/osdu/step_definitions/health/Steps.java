package org.opengroup.osdu.step_definitions.health;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.common.health.HealthSteps;
import org.opengroup.osdu.util.AzureHTTPClient;

public class Steps extends HealthSteps {

  public Steps() {
    super(new AzureHTTPClient());
  }

  @Before
  public void before(Scenario scenario) {
    this.scenario = scenario;
    this.httpClient = new AzureHTTPClient();
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
