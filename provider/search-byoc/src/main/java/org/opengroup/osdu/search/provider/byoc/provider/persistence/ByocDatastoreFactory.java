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

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.stereotype.Component;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.util.HashMap;
import java.util.Map;

@Component
public class ByocDatastoreFactory {

    public static Map<String, EmbeddedElastic> DATASTORE_CLIENTS = new HashMap<>();

    public EmbeddedElastic getDatastoreInstance(TenantInfo tenantInfo) throws Exception{
        if (DATASTORE_CLIENTS.get(tenantInfo.getName()) == null) {
            DATASTORE_CLIENTS.put(tenantInfo.getName(), DATASTORE_CLIENTS.get("byoc"));
        }
        return DATASTORE_CLIENTS.get(tenantInfo.getName());
    }


}
