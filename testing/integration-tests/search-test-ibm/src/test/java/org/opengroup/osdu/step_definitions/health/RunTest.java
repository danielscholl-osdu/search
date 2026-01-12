package org.opengroup.osdu.step_definitions.health;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/health/Health.feature",
    glue = {"classpath:org.opengroup.osdu.step_definitions.health"},
    plugin = {"pretty", "junit:target/cucumber-reports/TEST-health.xml"})
public class RunTest {}
