Feature: Fetch OpenAPI specification.

  @default
  Scenario: Verify swagger endpoint content
    When I send get request to swagger endpoint
    Then I should get openapi spec in response
