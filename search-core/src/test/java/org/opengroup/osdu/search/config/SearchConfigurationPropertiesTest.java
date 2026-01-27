// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.search.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class SearchConfigurationPropertiesTest {

    private SearchConfigurationProperties props;

    @BeforeEach
    void setUp() {
        props = new SearchConfigurationProperties();
    }

    @Test
    void getDeploymentEnvironment_returnsEnum() {
        props.setDeploymentEnvironment("LOCAL");
        assertEquals(DeploymentEnvironment.LOCAL, props.getDeploymentEnvironment());
    }

    @Test
    void isLocalEnvironment_returnsTrueWhenLocal() {
        props.setEnvironment("local");
        assertTrue(props.isLocalEnvironment());
        props.setEnvironment("prod");
        assertFalse(props.isLocalEnvironment());
    }

    @Test
    void isPreP4dAndPreDemoBehaviors() {
        props.setEnvironment("evd");
        assertTrue(props.isPreP4d());
        assertTrue(props.isPreDemo());

        props.setEnvironment("p4d");
        assertTrue(props.isPreDemo());
    }

    @Test
    void getIndexCleanupPattern_splitsCommaSeparatedValues() {
        props.setCronIndexCleanupPattern("a,b,c");
        String[] result = props.getIndexCleanupPattern();
        assertArrayEquals(new String[] {"a", "b", "c"}, result);
    }

    @Test
    void getIndexCleanupPattern_returnsEmptyArrayWhenNullOrEmpty() {
        props.setCronIndexCleanupPattern(null);
        assertEquals(0, props.getIndexCleanupPattern().length);

        props.setCronIndexCleanupPattern("");
        assertEquals(0, props.getIndexCleanupPattern().length);
    }

    @Test
    void getIndexCleanupPattern_withoutCommaReturnsSingleElement() {
        props.setCronIndexCleanupPattern("[invalid-regex");
        String[] result = props.getIndexCleanupPattern();
        assertEquals(1, result.length);
        assertEquals("[invalid-regex", result[0]);
    }

    @Test
    void getIndexCleanupTenants_splitsCommaSeparatedValues() {
        props.setCronIndexCleanupTenants("t1,t2");
        assertArrayEquals(new String[] {"t1", "t2"}, props.getIndexCleanupTenants());
    }

    @Test
    void isSmartSearchCcsDisabled_parsesTrueCaseInsensitive() {
        props.setSmartSearchCcsDisabled("TRUE");
        assertTrue(props.isSmartSearchCcsDisabled());

        props.setSmartSearchCcsDisabled("false");
        assertFalse(props.isSmartSearchCcsDisabled());
    }

    @Test
    void getDeploymentIdentifiers_returnExpectedValues() {
        props.setGaeService("search-service");
        props.setGaeVersion("v1");
        props.setGoogleCloudProjectRegion("us-central1");

        assertEquals("search-service", props.getDeployedServiceId());
        assertEquals("v1", props.getDeployedVersionId());
        assertEquals("us-central1", props.getDeploymentLocation());
    }
}
