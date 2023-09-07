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


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ElasticClientHandlerAwsTest {

    private static final String host = "localhost";
    private static final String basicAuthenticationHeaderVal = "basicAuthenticationHeaderVal";
    private static final int port = 1234;
    private static final String protocolScheme = "protocolScheme";
    private static final String tls = "tls";

    @Test
    public void test_CreateClientBuilder() throws NoSuchAlgorithmException, KeyManagementException{


        RestClientBuilder expected = RestClient.builder(new HttpHost(host, port, protocolScheme));
        expected.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(60000)
                .setSocketTimeout(60000)); 

        SSLContext sslContext;            

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{ UnsafeX509ExtendedTrustManager.INSTANCE }, null);
        expected.setHttpClientConfigCallback(httpClientBuilder -> 
        httpClientBuilder.setSSLContext(sslContext)
                    .setSSLHostnameVerifier((s, session) -> true));
        
        Header[] defaultHeaders = new Header[]{
                new BasicHeader("client.transport.nodes_sampler_interval", "30s"),
                new BasicHeader("client.transport.ping_timeout", "30s"),
                new BasicHeader("client.transport.sniff", "false"),
                new BasicHeader("request.headers.X-Found-Cluster", host),
                new BasicHeader("cluster.name", host),
                new BasicHeader("xpack.security.transport.ssl.enabled", tls),
                new BasicHeader("Authorization", basicAuthenticationHeaderVal),
        };
        expected.setDefaultHeaders(defaultHeaders);


       ElasticClientHandlerAws handler = new ElasticClientHandlerAws();
       RestClientBuilder builder = handler.createClientBuilder(host, basicAuthenticationHeaderVal, port, protocolScheme, tls);

       assertTrue(new ReflectionEquals(expected, "defaultHeaders", "nodes", "httpClientConfigCallback", "requestConfigCallback").matches(builder));
    }

}
