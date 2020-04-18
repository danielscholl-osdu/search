// Copyright 2019 IBM Corp. All Rights Reserved.
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

package org.opengroup.osdu.search.provider.ibm.persistence;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.ibm.auth.ServiceCredentials;
import org.opengroup.osdu.core.ibm.cloudant.IBMCloudantClientFactory;
import org.opengroup.osdu.search.provider.ibm.model.ElasticSettingSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cloudant.client.api.Database;


@Repository
public class ElasticSettingSchemaRepositoryImpl implements ISchemaRepository {


	@Value("${ibm.db.url}") 
	private String dbUrl;
	@Value("${ibm.db.apikey:#{null}}")
	private String apiKey;
	@Value("${ibm.db.user:#{null}}")
	private String dbUser;
	@Value("${ibm.db.password:#{null}}")
	private String dbPassword;
	
	@Value("${ibm.env.prefix:local-dev}")
	private String dbNamePrefix;
	
	private IBMCloudantClientFactory cloudantFactory;
	private Database db;
	
	@Inject
	private JaxRsDpsLog logger;

    @PostConstruct
    public void init(){
        try {
        	if (apiKey != null) {
    			cloudantFactory = new IBMCloudantClientFactory(new ServiceCredentials(dbUrl, apiKey));
    		} else {
    			cloudantFactory = new IBMCloudantClientFactory(new ServiceCredentials(dbUrl, dbUser, dbPassword));
    		}        	db = cloudantFactory.getDatabase(dbNamePrefix, "SearchSettings");
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	@Override
	public void add(ElasticSettingSchema schema, String id) {
	  ElasticSettingsDoc sd = new ElasticSettingsDoc();
	  sd.setId(id);
	  sd.setSettingSchema(schema);
	  db.save(sd);
	}

    @Override
    public ElasticSettingSchema get(String id) {
    	if (db.contains(id)) {
    		ElasticSettingsDoc sd = db.find(ElasticSettingsDoc.class, id);
    		ElasticSettingSchema newSchema = new ElasticSettingSchema();
    		newSchema.setPort(sd.getSettingSchema().getPort());
    		newSchema.setHost(sd.getSettingSchema().getHost());
    		newSchema.setUsernameAndPassword(sd.getSettingSchema().getUsernameAndPassword());
    		newSchema.setHttps(sd.getSettingSchema().isHttps());
    		return newSchema;
		} else {
			logger.error(ElasticSettingsDoc.class + " with id " + id + " was not found in the database.");
			return null;
		}
    }

}
