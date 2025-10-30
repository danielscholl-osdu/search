# os-search-azure

os-search-azure is a [Spring Boot](https://spring.io/projects/spring-boot) service that hosts CRUD APIs that enable the execution of OSDU R2 domain searches against Elasticsearch.

## Running Locally

### Requirements

In order to run this service locally, you will need the following:

- [Maven 3.8.0+](https://maven.apache.org/download.cgi)
- [Java 17](https://adoptopenjdk.net/)
- Infrastructure dependencies, deployable through the relevant [infrastructure template](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-azure-provisioning/-/blob/master/docs/service-automation.md)
- While not a strict dependency, example commands in this document use [bash](https://www.gnu.org/software/bash/)

### General Tips

**Environment Variable Management**
The following tools make environment variable configuration simpler
 - [direnv](https://direnv.net/) - for a shell/terminal environment
 - [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) - for [Intellij IDEA](https://www.jetbrains.com/idea/)

**Lombok**
This project uses [Lombok](https://projectlombok.org/) for code generation. You may need to configure your IDE to take advantage of this tool.
 - [Intellij configuration](https://projectlombok.org/setup/intellij)
 - [VSCode configuration](https://projectlombok.org/setup/vscode)


### Environment Variables

In order to run the service locally, you will need to have the following environment variables defined.

**Note** The following command can be useful to pull secrets from keyvault:
```bash
az keyvault secret show --vault-name $KEY_VAULT_NAME --name $KEY_VAULT_SECRET_NAME --query value -otsv
```

**Required to run service**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `search` | Logging prefix for service | no | - |
| `server.servlet.contextPath` | `/api/search/v2/` | Servlet context path | no | - |
| `search.service.port` | `8085` | Service port | no | - |
| `AUTHORIZE_API` | ex `https://foo-entitlements.azurewebsites.net` | Entitlements API endpoint | no | output of infrastructure deployment |
| `AUTHORIZE_API_KEY` | `********` | The API key clients will need to use when calling the entitlements | yes | -- |
| `partition_service_endpoint` |  ex `https://foo-partition.azurewebsites.net` | Partition Service API endpoint | no | output of infrastructure deployment |
| `azure.activedirectory.app-resource-id` | `********` | AAD client application ID  | yes | output of infrastructure deployment |
| `cosmosdb_database` | ex `dev-osdu-r2-db` | Cosmos database for documents | no | output of infrastructure deployment |
| `azure.activedirectory.client-id` | `********` | AAD client application ID | yes | output of infrastructure deployment |
| `azure.activedirectory.AppIdUri` | `api://${azure.activedirectory.client-id}` | URI for AAD Application | no | -- |
| `azure.activedirectory.session-stateless` | `true` | Flag run in stateless mode (needed by AAD dependency) | no | -- |
| `cache.provider` | `redis` | cache provider [vm, redis], by default 'redis' | no | - |
| `ELASTIC_CACHE_EXPIRATION` | `1` | Credentials cache expiration (in minutes) | no | - |
| `MAX_CACHE_VALUE_SIZE` | `60` | Cache size | no | - |
| `KEYVAULT_URI` | ex `https://foo-keyvault.vault.azure.net/` | URI of KeyVault that holds application secrets | no | output of infrastructure deployment |
| `AZURE_CLIENT_ID` | `********` | Identity to run the service locally. This enables access to Azure resources. You only need this if running locally | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-username` |
| `AZURE_TENANT_ID` | `********` | AD tenant to authenticate users from | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-tenant-id` |
| `AZURE_CLIENT_SECRET` | `********` | Secret for `$AZURE_CLIENT_ID` | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-password` |
| `azure.application-insights.instrumentation-key` | `********` | API Key for App Insights | yes | output of infrastructure deployment |
| `appinsights_key` | `********` | API Key for App Insights | yes | output of infrastructure deployment |
| `azure_istioauth_enabled` | `true` | Flag to Disable AAD auth | no | -- |

**Required to run integration tests**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | ex `opendes` | Primary data partition for queries (positive test cases) | no | Data in search index |
| `DEFAULT_DATA_PARTITION_ID_TENANT2` | ex `microsoftwipro2` | Secondary data partition for queries (negative test cases) | no | Data (not) in search index |
| `ELASTIC_USER_NAME` | ex `elastic` | Elasticsearch cluster username | yes | output of infrastructure deployment |
| `ELASTIC_PASSWORD` | `********` | Elasticsearch cluster password | yes | output of infrastructure deployment |
| `ELASTIC_HOST` | ex `foobar.ece.msft-osdu-test.org` | Elasticsearch cluster endpoint | no | output of infrastructure deployment |
| `ELASTIC_PORT` | ex `9243` | Elasticsearch cluster port | no | output of infrastructure deployment |
| `INDEXER_HOST` | ex `http://indexer.azurewebsites.net/` | Indexer service endpoint | no | - |
| `ENTITLEMENTS_DOMAIN` | ex `contoso.com` | OSDU R2 to run tests under | no | - |
| `SEARCH_HOST` | ex `http://localhost:8085/api/search/v2/` | Search service endpoint | no | - |
| `STORAGE_HOST` | ex `http://storage.azurewebsites.net/` | Storage endpoint | no | - |
| `AZURE_AD_TENANT_ID` | `********` | AD tenant to authenticate users from | yes | -- |
| `AZURE_AD_APP_RESOURCE_ID` | `********` | AAD client application ID | yes | output of infrastructure deployment |
| `INTEGRATION_TESTER` | `********` | System identity to assume for API calls. Note: this user must have entitlements configured already | no | -- |
| `AZURE_TESTER_SERVICEPRINCIPAL_SECRET` | `********` | Secret for `$INTEGRATION_TESTER` | yes | -- | -->



### Configure Maven

Check that maven is installed:
```bash
$ mvn --version
Apache Maven 3.8.0
Maven home: /usr/share/maven
Java version: 17.0.7
...
```


### Build and run the application

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*

```bash
# build + test + install core service code
$ mvn clean install

# build + test + package azure service code
$ (cd provider/search-azure/ && mvn clean package)

# run service
#
# Note: this assumes that the environment variables for running the service as outlined
#       above are already exported in your environment.
$ java -jar $(find provider/search-azure/target/ -name *-spring-boot.jar) --add-opens java.base/java.lang=ALL-UNNAMED --add-opens  java.base/java.lang.reflect=ALL-UNNAMED

# Alternately you can run using the Mavan Task
$ mvn spring-boot:run
```

### Test the application

After the service has started it should be accessible via a web browser by visiting [http://localhost:8085/api/search/v2/swagger](http://localhost:8085/api/search/v2/swagger). If the request does not fail, you can then run the integration tests.

```bash
# build + install integration test core
$ (cd testing/integration-tests/search-test-core/ && mvn clean install)

# build + run Azure integration tests.
#
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ (cd testing/integration-tests/search-test-azure/ && mvn clean test)
```

## Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.

## Known Limitation for Azure Http Header Size

In  Microsoft Azure, there is a limitation for the Http Header size in Azure. For more information, see: https://learn.microsoft.com/en-US/troubleshoot/developer/webapps/iis/www-administration-management/http-bad-request-response-kerberos#cause

Consider If the user belongs to more than **2000** data groups. When Search calls Policy translate API, in the request header **'X-Data-Groups'** (>2000) values are passed. Microsoft Azure Application Gateway doesn't support such large headers. As a result, it returns a **'400 Request Header Or Cookie Too Large'** error before even reaching the policy service.

This error body is translated as input query for ElasticSearch, which results in ElasticSearch exception. As a result, the search API response will be:

```
{
    "code": 400,
    "reason": "Bad Request",
    "message": "Failed to derive xcontent"
}
```

#### Workaround

Delete any stale or test groups associated with the user via the Entitlements API, such that the number of data groups is within the limit of 2000.

For more information, see: https://learn.microsoft.com/en-US/troubleshoot/developer/webapps/iis/www-administration-management/http-bad-request-response-kerberos#workaround-1-decrease-the-number-of-active-directory-groups


## Deploying service to Azure

Service deployments into Azure are standardized to make the process the same for all services if using ADO and are closely related to the infrastructure deployed. The steps to deploy into Azure can be [found here](https://github.com/azure/osdu-infrastructure)

The default ADO pipeline is /devops/azure-pipeline.yml


### Manual Deployment Steps

__Environment Settings__

The following environment variables are necessary to properly deploy a service to an Azure OSDU Environment.

```bash
# Group Level Variables
export AZURE_TENANT_ID=""
export AZURE_SUBSCRIPTION_ID=""
export AZURE_SUBSCRIPTION_NAME=""
export AZURE_PRINCIPAL_ID=""
export AZURE_PRINCIPAL_SECRET=""
export AZURE_APP_ID=""
export AZURE_BASENAME_21=""
export AZURE_BASENAME=""
export AZURE_BASE=""
export AZURE_ELASTIC_HOST=""
export AZURE_ELASTIC_PASSWORD=""

# Pipeline Level Variable
export AZURE_SERVICE="search"
export AZURE_BUILD_SUBDIR="provider/search-azure"
export AZURE_TEST_SUBDIR="testing/search-test-azure"

# Required for Azure Deployment
export AZURE_CLIENT_ID="${AZURE_PRINCIPAL_ID}"
export AZURE_CLIENT_SECRET="${AZURE_PRINCIPAL_SECRET}"
export AZURE_RESOURCE_GROUP="${AZURE_BASENAME}-osdu-r2-app-rg"
export AZURE_APPSERVICE_PLAN="${AZURE_BASENAME}-osdu-r2-sp"
export AZURE_APPSERVICE_NAME="${AZURE_BASENAME_21}-au-${AZURE_SERVICE}"

# Required for Testing
export AZURE_AD_TENANT_ID="$AZURE_TENANT_ID"
export INTEGRATION_TESTER="$AZURE_PRINCIPAL_ID"
export AZURE_TESTER_SERVICEPRINCIPAL_SECRET="$AZURE_PRINCIPAL_SECRET"
export AZURE_AD_APP_RESOURCE_ID="$AZURE_APP_ID"
export aad_client_id="$AZURE_APP_ID"
export STORAGE_HOST="https://{AZURE_BASENAME_21}-au-storage.azurewebsites.net/"
export SEARCH_HOST: "https://{AZURE_BASENAME_21}-au-search.azurewebsites.net//api/search/v2/"
export ELASTIC_HOST="$AZURE_ELASTIC_HOST"
export ELASTIC_PORT="9243"
export ELASTIC_USER_NAME="elastic"
export ELASTIC_PASSWORD="$AZURE_ELASTIC_PASSWORD"
export DEFAULT_DATA_PARTITION_ID_TENANT1="opendes"
export DEFAULT_DATA_PARTITION_ID_TENANT2="common"
export ENTITLEMENTS_DOMAIN="contoso.com"
```


__Azure Service Deployment__


1. Deploy the service using the Maven Plugin  _(azure_deploy)_

```bash
cd $AZURE_BUILD_SUBDIR
mvn azure-webapp:deploy \
  -DAZURE_TENANT_ID=$AZURE_TENANT_ID \
  -Dazure.appservice.subscription=$AZURE_SUBSCRIPTION_ID \
  -DAZURE_CLIENT_ID=$AZURE_CLIENT_ID \
  -DAZURE_CLIENT_SECRET=$AZURE_CLIENT_SECRET \
  -Dazure.appservice.resourcegroup=$AZURE_RESOURCE_GROUP \
  -Dazure.appservice.plan=$AZURE_APPSERVICE_PLAN \
  -Dazure.appservice.appname=$AZURE_APPSERVICE_NAME
```

2. Configure the Web App to start the SpringBoot Application _(azure_config)_

```bash
az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID

# Set the JAR FILE as required
TARGET=$(find ./target -name '*-spring-boot.jar')
JAR_FILE=${TARGET##*/}

JAVA_COMMAND="java -jar /home/site/wwwroot/${JAR_FILE}"
JSON_TEMPLATE='{"appCommandLine":"%s"}'
JSON_FILE="config.json"
echo $(printf "$JSON_TEMPLATE" "$JAVA_COMMAND") > $JSON_FILE

az webapp config set --resource-group $AZURE_RESOURCE_GROUP --name $AZURE_APPSERVICE_NAME --generic-configurations @$JSON_FILE
```

3. Execute the Integration Tests against the Service Deployment _(azure_test)_

```bash
mvn clean test -f $AZURE_TEST_SUBDIR/pom.xml
```



## License
Copyright © Microsoft Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
