// Copyright 2020 IBM Corp. All Rights Reserved.
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
package org.opengroup.osdu.search.ibm.service;


import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.search.provider.ibm.service.ElasticSettingServiceImpl;
import org.springframework.test.context.junit4.SpringRunner;
@Ignore
@RunWith(SpringRunner.class)
public class ElasticSettingServiceTest {

    @InjectMocks
    ElasticSettingServiceImpl elasticSettingService;

    @Test
    public void should_returnClusterSettings_when_Initialized() {

        when(elasticSettingService.getElasticClusterInformation()).thenReturn(new ClusterSettings("https://search.cloud.com", -1, "notused"));

        ClusterSettings clusterSettings = elasticSettingService.getElasticClusterInformation();

        assertNotNull(clusterSettings);
    }
}

