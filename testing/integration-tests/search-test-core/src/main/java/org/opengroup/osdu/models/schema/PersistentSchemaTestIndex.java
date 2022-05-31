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

package org.opengroup.osdu.models.schema;

import org.opengroup.osdu.common.TestsBase;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;
import org.opengroup.osdu.util.SchemaServiceClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PersistentSchemaTestIndex extends TestIndex {

    private static final Logger LOGGER = Logger.getLogger(PersistentSchemaTestIndex.class.getName());
    private final SchemaServiceClient schemaServiceClient;
    private final TestsBase testsBase;
    private SchemaModel schemaModel;

    public PersistentSchemaTestIndex(HTTPClient client, TestsBase testsBase) {
        super();
        this.schemaServiceClient = new SchemaServiceClient(client);
        this.testsBase = testsBase;
    }

    @Override
    public void setupSchema() {
        this.schemaModel = readSchemaFromJson();
        SchemaIdentity schemaIdentity = schemaModel.getSchemaInfo().getSchemaIdentity();
        LOGGER.log(Level.INFO, "Read the schema={0}", schemaIdentity);
        schemaIdentity.setAuthority(testsBase.generateActualNameWithoutTs(schemaIdentity.getAuthority()));
        schemaIdentity.setSource(testsBase.generateActualName(schemaIdentity.getSource(),""));
        LOGGER.log(Level.INFO, "Updated the schema={0}", schemaIdentity);
        schemaServiceClient.createIfNotExist(schemaModel);
        LOGGER.log(Level.INFO, "Finished setting up the schema={0}", schemaIdentity);
    }

    @Override
    public void deleteSchema(String kind) {
        // The DELETE API is not supported in the Schema service.
        // In order not to overwhelm a DB with a lots of test schemas
        // the integration tests create/update a schema per schema file if the schema does not exists
        // If a developer updates the schema manually, the developer is supposed to update its version as well
    }

    private SchemaModel readSchemaFromJson(){
        try {
            return FileHandler.readFile(getSchemaFile(), SchemaModel.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public SchemaModel getSchemaModel() {
        return schemaModel;
    }

    @Override
    protected String getSchemaFile() {
        return super.getSchemaFile() + ".json";
    }
}
