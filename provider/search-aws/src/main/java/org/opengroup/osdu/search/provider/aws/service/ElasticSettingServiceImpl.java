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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.util.Map;

@Primary
@Component
public class ElasticSettingServiceImpl implements IElasticSettingService {

    @Value("${aws.es.host}")
    String host;

    @Value("${aws.es.port}")
    int port;

    @Value("${aws.es.isHttps}")
    boolean isHttps;

    @Value("${aws.es.username}")
    String username;

    @Value("${aws.es.password}")
    String password;

    String usernameAndPassword;


    @Value("${aws.elasticsearch.port}")
    String portParameter;

    @Value("${aws.elasticsearch.host}")
    String hostParameter;

    @Value("${aws.elasticsearch.credentials.secret}")
    String elasticCredentialsSecret;

    @Value("${aws.region}")
    private String amazonRegion;

    @Value("${aws.ssm}")
    String ssmEnabledString;
    @PostConstruct
    private void postConstruct() throws Exception {
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        host = provider.getParameterAsString(hostParameter);
        port = Integer.parseInt(provider.getParameterAsString(portParameter));
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> val = new Gson().fromJson(provider.getParameterAsString(elasticCredentialsSecret), mapType);
        username = val.get("username").toString();

        password = val.get("password").toString();

        //elastic expects username:password format
        usernameAndPassword = String.format("%s:%s", username, password);
    }

    @Override
    public ClusterSettings getElasticClusterInformation() {

        ClusterSettings settings = new ClusterSettings(host, port, usernameAndPassword);
        
        if (!isHttps) {
            settings.setHttps(false);
            settings.setTls(false);
        }

        return settings;
    }
}