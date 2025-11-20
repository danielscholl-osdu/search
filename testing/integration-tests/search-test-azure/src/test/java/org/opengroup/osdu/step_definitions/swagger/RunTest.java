package org.opengroup.osdu.step_definitions.swagger;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/swagger/Swagger.feature",
    glue = {"classpath:org.opengroup.osdu.step_definitions/swagger"},
    plugin = {"pretty", "junit:target/cucumber-reports/TEST-swagger.xml"})
public class RunTest {}
