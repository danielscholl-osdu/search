/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

// limitations under the License.

package org.opengroup.osdu.search.provider.ibm.persistence;

import org.opengroup.osdu.search.provider.ibm.model.ElasticSettingSchema;

public interface ISchemaRepository {
    String SCHEMA_KIND = "IndexerSchema";

    String SCHEMA = "schema";
    String KIND = "KIND";

    void add(ElasticSettingSchema schema, String id);

    ElasticSettingSchema get(String id);
}

