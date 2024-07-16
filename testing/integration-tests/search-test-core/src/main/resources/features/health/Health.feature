Feature: To verify health api endpoints content

  @default
  Scenario: Verify liveness check endpoint content
    Given I send get request to liveness check endpoint
    Then service should respond back with 200 in response

  @default
  Scenario: Verify readiness check endpoint content
    Given I send get request to readiness check endpoint
    Then service should respond back with 200 in response
