/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.io.Closeable;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ElasticClientHandler implements Closeable {

  // Elastic cluster Rest client settings
  private static final int CLOUD_REST_CLIENT_PORT = 9243;
  private static final int REST_CLIENT_CONNECT_TIMEOUT = 60000;
  private static final int REST_CLIENT_SOCKET_TIMEOUT = 60000;
  private static final int REST_CLIENT_RETRY_TIMEOUT = 60000;

  @Value("#{new Boolean('${security.https.certificate.trust:false}')}")
  private Boolean isSecurityHttpsCertificateTrust;

  @Autowired private IElasticSettingService elasticSettingService;
  @Autowired private ICache<String, ElasticsearchClient> clientCache;
  @Autowired private TenantInfo tenantInfo;

  public ElasticsearchClient getOrCreateRestClient() {
    String partitionId = tenantInfo.getDataPartitionId();
    ElasticsearchClient client = clientCache.get(partitionId);
    if (Objects.isNull(client)) {
      client = getCloudRestClient(elasticSettingService.getElasticClusterInformation());
      clientCache.put(partitionId, client);
    }
    return client;
  }

  public ElasticsearchClient createRestClient(final ClusterSettings clusterSettings) {
    return getCloudRestClient(clusterSettings);
  }

  private ElasticsearchClient getCloudRestClient(final ClusterSettings clusterSettings) {

    String cluster = null;
    String host = null;
    int port = CLOUD_REST_CLIENT_PORT;
    String protocolScheme = "https";
    String tls = "true";

    try {
      cluster = clusterSettings.getHost();
      host = clusterSettings.getHost();
      port = clusterSettings.getPort();
      if (!clusterSettings.isHttps()) {
        protocolScheme = "http";
      }

      if (!clusterSettings.isTls()) {
        tls = "false";
      }
      String basicEncoded =
          Base64.getEncoder().encodeToString(clusterSettings.getUserNameAndPassword().getBytes());
      String basicAuthenticationHeaderVal = String.format("Basic %s", basicEncoded);

      RestClientBuilder builder =
          createClientBuilder(host, basicAuthenticationHeaderVal, port, protocolScheme, tls);

      RestClientTransport transport =
          new RestClientTransport(builder.build(), new JacksonJsonpMapper());

      return new ElasticsearchClient(transport);
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          "search client error",
          "error creating search client",
          String.format(
              "Elastic client connection params, cluster: %s, host: %s, port: %s",
              cluster, host, port),
          e);
    }
  }

  protected RestClientBuilder createClientBuilder(
      String host,
      String basicAuthenticationHeaderVal,
      int port,
      String protocolScheme,
      String tls) {

    RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, protocolScheme));
    builder.setRequestConfigCallback(
        requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout(REST_CLIENT_CONNECT_TIMEOUT)
                .setSocketTimeout(REST_CLIENT_SOCKET_TIMEOUT));

    Header[] defaultHeaders =
        new Header[] {
          new BasicHeader("client.transport.nodes_sampler_interval", "30s"),
          new BasicHeader("client.transport.ping_timeout", "30s"),
          new BasicHeader("client.transport.sniff", "false"),
          new BasicHeader("request.headers.X-Found-Cluster", host),
          new BasicHeader("cluster.name", host),
          new BasicHeader("xpack.security.transport.ssl.enabled", tls),
          new BasicHeader("Authorization", basicAuthenticationHeaderVal),
        };
    log.debug(
        String.format(
            "Elastic client connection uses protocolScheme = %s with a flag "
                + "'security.https.certificate.trust' = %s",
            protocolScheme, isSecurityHttpsCertificateTrust));
    if ("https".equals(protocolScheme) && Boolean.TRUE.equals(isSecurityHttpsCertificateTrust)) {
      log.debug("Elastic client connection uses TrustSelfSignedStrategy()");
      SSLContext sslContext = createSSLContext();
      builder.setHttpClientConfigCallback(
          httpClientBuilder ->
              httpClientBuilder
                  .setSSLContext(sslContext)
                  .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE));
    }

    builder.setDefaultHeaders(defaultHeaders);
    return builder;
  }

  private SSLContext createSSLContext() {
    SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
    try {
      sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      return sslContextBuilder.build();
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
      log.error(e.getMessage());
    }
    return null;
  }

  public Boolean isSecurityHttpsCertificateTrust() {
    return isSecurityHttpsCertificateTrust;
  }

  public void setSecurityHttpsCertificateTrust(Boolean isSecurityHttpsCertificateTrust) {
    this.isSecurityHttpsCertificateTrust = isSecurityHttpsCertificateTrust;
  }

  @Override
  public void close() throws IOException {}
}
