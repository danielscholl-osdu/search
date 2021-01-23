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

package org.opengroup.osdu.search.provider.reference.utils;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.provider.reference.config.MongoDBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MongoClientHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MongoClientHandler.class);
  private static final String MONGO_PREFIX = "mongodb://";
  private static final String MONGO_OPTIONS = "retryWrites=true&w=majority&maxIdleTimeMS=10000";
  private final MongoDBConfig mongoDBConfig;

  private com.mongodb.client.MongoClient mongoClient = null;

  public MongoClientHandler(MongoDBConfig mongoDBConfig) {
    this.mongoDBConfig = mongoDBConfig;
  }

  private MongoClient getOrInitMongoClient() throws RuntimeException {
    if (mongoClient != null) {
      return mongoClient;
    }

    final String connectionString = String.format("%s%s:%s@%s/?%s",
        MONGO_PREFIX,
        mongoDBConfig.getMongoDbUser(),
        mongoDBConfig.getMongoDbPassword(),
        mongoDBConfig.getMongoDbUrl(),
        MONGO_OPTIONS);
    ConnectionString connString = new ConnectionString(connectionString);
    MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(connString)
        .retryWrites(true)
        .build();
    try {
      mongoClient = MongoClients.create(settings);
    } catch (Exception ex) {
      LOG.error("Error connecting MongoDB", ex);
      throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error connecting MongoDB",
          ex.getMessage(), ex);
    }
    return mongoClient;
  }

  public MongoClient getMongoClient() {
    if (mongoClient == null) {
      getOrInitMongoClient();
    }
    return mongoClient;
  }

}
