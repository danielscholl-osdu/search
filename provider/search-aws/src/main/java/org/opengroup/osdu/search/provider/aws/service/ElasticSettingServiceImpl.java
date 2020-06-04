// Copyright © Amazon Web Services
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

package org.opengroup.osdu.search.provider.aws.service;

import org.opengroup.osdu.core.aws.ssm.ParameterStorePropertySource;
import org.opengroup.osdu.core.aws.ssm.SSMConfig;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Primary
@Component
public class ElasticSettingServiceImpl implements IElasticSettingService {

    @Value("${aws.es.host}")
    String host;

    @Value("${aws.es.port}")
    int port;

    @Value("${aws.es.userNameAndPassword}")
    String userNameAndPassword;

    @Value("${aws.ssm}")
    String isEnabledString;

    @Value("${aws.elasticsearch.host}")
    String hostParameter;

    @Value("${aws.elasticsearch.port}")
    String portParameter;

    private ParameterStorePropertySource ssm;

    @PostConstruct
    private void postConstruct() {
        Boolean isEnabled = Boolean.parseBoolean(isEnabledString);
        if(isEnabled) {
            SSMConfig ssmConfig = new SSMConfig();
            ssm = ssmConfig.amazonSSM();
            host = ssm.getProperty(hostParameter).toString();
            port = Integer.parseInt(ssm.getProperty(portParameter).toString());
        }
    }

    @Override
    public ClusterSettings getElasticClusterInformation() {

        return new ClusterSettings(host, port, userNameAndPassword);
    }
}