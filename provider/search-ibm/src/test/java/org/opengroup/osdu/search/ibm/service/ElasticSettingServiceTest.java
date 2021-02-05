/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

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

