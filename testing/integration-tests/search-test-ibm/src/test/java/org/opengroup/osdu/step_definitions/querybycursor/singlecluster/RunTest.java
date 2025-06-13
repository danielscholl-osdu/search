package org.opengroup.osdu.step_definitions.querybycursor.singlecluster;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features/querybycursor/singlecluster/QueryByCursor.feature",
        glue = {"classpath:org.opengroup.osdu.step_definitions/querybycursor/singlecluster"},
        plugin = {"pretty", "junit:target/cucumber-reports/TEST-querybycursor-sc.xml"})
public class RunTest {
}
