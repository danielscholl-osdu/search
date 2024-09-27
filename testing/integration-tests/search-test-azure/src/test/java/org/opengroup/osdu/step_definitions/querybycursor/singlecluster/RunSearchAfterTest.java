// Copyright © Microsoft Corporation
// Copyright © Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.step_definitions.querybycursor.singlecluster;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.opengroup.osdu.common.TestConstants;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features/querybycursor/singlecluster/SingleClusterQueryByCursor.feature",
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
