package org.opengroup.osdu.step_definitions.querybycursor.singlecluster;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.opengroup.osdu.common.TestConstants;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features/querybycursor/singlecluster/QueryByCursor.feature",
        glue = {"classpath:org.opengroup.osdu.step_definitions/querybycursor/singlecluster"},
        plugin = {"pretty", "junit:target/cucumber-reports/TEST-querybysearchafter-sc.xml"})
public class RunSearchAfterTest {
    @BeforeClass
    public static void setup(){
        System.setProperty(TestConstants.QUERY_WITH_CURSOR_PATH_PROP, TestConstants.SEARCH_AFTER_PATH_VALUE);
    }

    @AfterClass
    public static void teardown(){
        System.clearProperty(TestConstants.QUERY_WITH_CURSOR_PATH_PROP);
    }
}
