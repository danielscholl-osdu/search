/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.provider.reference.provider.persistence;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.util.JSON.serialize;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.bson.Document;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElasticRepositoryReference implements IElasticRepository {

  private static final Logger logger = LoggerFactory.getLogger(ElasticRepositoryReference.class);

  public static final String ELASTIC_SETTINGS_DATABASE = "local";
  public static final String ELASTIC_SETTINGS_COLLECTION = "SearchSettings";

  private static final String MISSING_TENANT_INFO_REASON = "TenantInfo is null";
  private static final String MISSING_TENANT_INFO_MESSAGE = "TenantInfo is missing.";

  private final MongoDdmsClient mongoClient;
  private final SearchConfigurationProperties searchConfigurationProperties;

  @Override
  public ClusterSettings getElasticClusterSettings(TenantInfo tenantInfo) {

    if (Objects.isNull(tenantInfo)) {
      throw new AppException(HttpStatus.SC_NOT_FOUND, MISSING_TENANT_INFO_REASON,
          MISSING_TENANT_INFO_MESSAGE);
    }

    String settingId = tenantInfo.getName().concat("-")
        .concat(searchConfigurationProperties.getElasticDatastoreId());

    MongoCollection<Document> collection = this.mongoClient
        .getMongoCollection(ELASTIC_SETTINGS_DATABASE, ELASTIC_SETTINGS_COLLECTION);

    Document record = collection.find(eq("_id", settingId)).first();
    if (Objects.isNull(record)) {
      logger.warn(String.format("%s credentials not found at database.", settingId));
      return new ClusterSettings(searchConfigurationProperties.getElasticHost(),
          Integer.parseInt(searchConfigurationProperties.getElasticPort()),
          searchConfigurationProperties.getElasticUserPassword(), false, false);
    }

    ElasticSettingsDoc elasticSettingsDoc = new Gson()
        .fromJson(serialize(record), ElasticSettingsDoc.class);

    String host = elasticSettingsDoc.getSettingSchema().getHost();
    String portString = elasticSettingsDoc.getSettingSchema().getPort();
    String usernameAndPassword = elasticSettingsDoc.getSettingSchema().getUsernameAndPassword();

    Preconditions.checkNotNullOrEmpty(host, "host cannot be null");
    Preconditions.checkNotNullOrEmpty(portString, "port cannot be null");
    Preconditions.checkNotNullOrEmpty(usernameAndPassword, "configuration cannot be null");

    int port = Integer.parseInt(portString);

    return new ClusterSettings(host, port, usernameAndPassword,
        elasticSettingsDoc.getSettingSchema().isHttps(),
        elasticSettingsDoc.getSettingSchema().isHttps());
  }
}
