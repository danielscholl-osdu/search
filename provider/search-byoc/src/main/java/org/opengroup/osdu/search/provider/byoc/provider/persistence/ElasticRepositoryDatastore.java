// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.search.provider.byoc.provider.persistence;

import org.apache.http.HttpStatus;

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.springframework.stereotype.Component;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import javax.inject.Inject;

@Component
public class ElasticRepositoryDatastore implements IElasticRepository {

    @Inject
    private ByocDatastoreFactory byocDatastoreFactory;

    @Override
    public ClusterSettings getElasticClusterSettings(TenantInfo tenantInfo) {

        try{
            EmbeddedElastic byocDatastore = this.byocDatastoreFactory.getDatastoreInstance(tenantInfo);

            String host = "localhost";
            String portString = String.valueOf(byocDatastore.getHttpPort());
            String usernameAndPassword = "";
            Preconditions.checkNotNullOrEmpty(host, "host cannot be null");
            Preconditions.checkNotNullOrEmpty(portString, "port cannot be null");
            //Preconditions.checkNotNullOrEmpty(usernameAndPassword, "configuration cannot be null");
            int port = Integer.parseInt(portString);
            return new ClusterSettings(host, port, usernameAndPassword, false, false);
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "BYOC Cluster setting fetch error", "An error has occurred fetching cluster settings from the datastore.", e);
        }
    }
}