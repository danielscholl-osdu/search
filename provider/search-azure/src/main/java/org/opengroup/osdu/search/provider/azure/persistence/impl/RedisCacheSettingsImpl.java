// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.search.provider.azure.persistence.impl;


import org.opengroup.osdu.search.provider.azure.persistence.CacheSettingDoc;
import org.opengroup.osdu.search.provider.azure.persistence.CacheSettingSchema;
import org.opengroup.osdu.search.provider.azure.persistence.CosmosDBCacheSettings;
import org.opengroup.osdu.search.provider.azure.persistence.ICacheSettings;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class RedisCacheSettingsImpl implements ICacheSettings {

    @Inject
    private CosmosDBCacheSettings db;

    @Override
    public void add(CacheSettingSchema doc, String id) {
        CacheSettingDoc newDoc = new CacheSettingDoc();
        newDoc.setId(id);
        newDoc.setSettingSchema(doc);
        db.save(newDoc);
    }

    @Override
    public CacheSettingSchema get(String id) {
        Optional<CacheSettingDoc> sd = db.findById(id);
        if (!sd.isPresent())
            return null;

        CacheSettingSchema newSchema = new CacheSettingSchema();
        newSchema.setPort(sd.get().getSettingSchema().getPort());
        newSchema.setHost(sd.get().getSettingSchema().getHost());
        newSchema.setUsernameAndPassword(sd.get().getSettingSchema().getUsernameAndPassword());
        newSchema.setHttps(sd.get().getSettingSchema().isHttps());
        return newSchema;
    }
}
