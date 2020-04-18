package org.opengroup.osdu.step_definitions.query.singlecluster;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features/query/singlecluster/Query.feature",
        glue = {"classpath:org.opengroup.osdu.step_definitions/query/singlecluster"},
        format = {"pretty", "junit:target/cucumber-reports/TEST-query-sc.xml"})
public class RunTest {
}