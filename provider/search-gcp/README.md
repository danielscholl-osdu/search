# Search and Indexer Service
os-search-gcp is a [Spring Boot](https://spring.io/projects/spring-boot) service that hosts CRUD APIs that enable the execution of OSDU R2 domain searches against Elasticsearch.

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
Pre-requisites

* GCloud SDK with java (latest version)
* JDK 8
* Lombok 1.16 or later
* Maven

# Configuration

## Service Configuration
### Anthos:
[Anthos service configuration ](docs/anthos/README.md)
### GCP:
[Gcp service configuration ](docs/gcp/README.md)

### Run Locally
Check that maven is installed:

```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. This file should live within `~/.mvn/community-maven.settings.xml`:

```bash
$ cat ~/.m2/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>community-maven-via-private-token</id>
            <!-- Treat this auth token like a password. Do not share it with anyone, including Microsoft support. -->
            <!-- The generated token expires on or before 11/14/2019 -->
             <configuration>
              <httpHeaders>
                  <property>
                      <name>Private-Token</name>
                      <value>${env.COMMUNITY_MAVEN_TOKEN}</value>
                  </property>
              </httpHeaders>
             </configuration>
        </server>
    </servers>
</settings>
```

* Update the Google cloud SDK to the latest version:

```bash
gcloud components update
```
* Set Google Project Id:

```bash
gcloud config set project <YOUR-PROJECT-ID>
```

* Perform a basic authentication in the selected project:

```bash
gcloud auth application-default login
```

* Navigate to search service's root folder and run:

```bash
mvn jetty:run
## Testing
* Navigate to search service's root folder and run:
 
```bash
mvn clean install   
```

* If you wish to see the coverage report then go to testing/target/site/jacoco-aggregate and open index.html

* If you wish to build the project without running tests

```bash
mvn clean install -DskipTests
```

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*

```bash
cd provider/search-gcp/ && mvn spring-boot:run
```

## Testing
Navigate to search service's root folder and run all the tests:

```bash
# build + install integration test core
$ (cd testing/storage-test-core/ && mvn clean install)
```
 
### Running E2E Tests 
This section describes how to run cloud OSDU E2E tests (testing/integration-tests/search-test-gcp).

You will need to have the following environment variables defined.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `ENTITLEMENTS_HOST` | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint | no | output of infrastructure deployment |
| `ELASTIC_PASSWORD` | `********` | Password for Elasticsearch | yes | output of infrastructure deployment |
| `ELASTIC_USER_NAME` | `********` | User name for Elasticsearch | yes | output of infrastructure deployment |
| `ELASTIC_HOST` | ex `elastic.domain.com` | Host Elasticsearch | yes | output of infrastructure deployment |
| `ELASTIC_PORT` | ex `9243` | Port Elasticsearch | yes | output of infrastructure deployment |
| `GCLOUD_PROJECT` | ex `opendes` | Google Cloud Project Id| no | output of infrastructure deployment |
| `INDEXER_HOST` | ex `https://os-indexer-dot-opendes.appspot.com/api/indexer/v2/` | Indexer API endpoint | no | output of infrastructure deployment |
| `DATA_GROUP` | `opendes` | The service account to this group and substitute | no | - |
| `ENTITLEMENTS_DOMAIN` | ex `opendes-gcp.projects.com` | OSDU R2 to run tests under  | no | - |
| `INTEGRATION_TEST_AUDIENCE` | `********` | client application ID | yes | https://console.cloud.google.com/apis/credentials || `OTHER_RELEVANT_DATA_COUNTRIES` | ex `US` | a other relevant data countries (for LEGAL_TAG) | no | - |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | ex `opendes` | HTTP Header 'Data-Partition-ID'  | no | - |
| `SEARCH_INTEGRATION_TESTER` | `********` | Service account for API calls. Note: this user must have entitlements configured already | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
| `SEARCH_HOST` | ex `http://localhost:8080/api/search/v2/` | Endpoint of search service | no | - |
| `LEGAL_TAG` | ex `my-legal-tag` | a valid legal tag | no | - |  
| `SECURITY_HTTPS_CERTIFICATE_TRUST` | ex `false` | Elastic client connection uses TrustSelfSignedStrategy(), if it is 'true' | false | output of infrastructure deployment |


**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER | NO_DATA_ACCESS_TESTER | 
| ---  | ---   |
| users<br/>service.entitlements.user<br/>service.search.user<br/>data.test1<br/>data.integration.test<br/>users@{tenant1}@{domain}.com |

Execute following command to build code and run all the integration tests:

```bash
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ (cd testing/search-test-gcp/ && mvn clean test)
```

## Deployment

* Data-Lake Search Google Cloud Endpoints on App Engine Flex environment
  * Edit the app.yaml
    * Open the [app.yaml](search/src/main/appengine/app.yaml) file in editor, and replace the YOUR-PROJECT-ID `PROJECT` line with Google Cloud Platform project Id. Also update `SEARCH_HOST`, `STORAGE_HOST`, `STORAGE_SCHEMA_HOST`, `IDENTITY_QUERY_ACCESS_HOST` and `IDENTITY_AUTHORIZE_HOST` based on your deployment
 
  * Deploy
    ```sh
    mvn appengine:deploy -pl org.opengroup.osdu.search:search -amd
    ```

  * If you wish to deploy the search service without running tests
    ```sh
    mvn appengine:deploy -pl org.opengroup.osdu.search:search -amd -DskipTests
    ```

#### Memory Store (Redis Instance) Setup

Create a new Standard tier Redis instance on the ***service project***

The Redis instance must be created under the same region with the App Engine application which needs to access it.

```bash
    gcloud beta redis instances create redis-cache-search --size=10 --region=<service-deployment-region> --zone=<service-deployment-zone> --tier=STANDARD
```

## Licence
Copyright © Google LLC
Copyright © EPAM Systems
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
