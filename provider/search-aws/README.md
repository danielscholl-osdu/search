# Search Service
search-aws is a [Spring Boot](https://spring.io/projects/spring-boot) service that provides a set of APIs query record data residing in Elasticsearch that got indexed by Indexer service

## Running Locally

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites
Pre-requisites

* JDK 17 (https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
* Maven 3.8.3 or later
* Lombok 1.18 or later
* OSDU Instance deployed on AWS

### Service Configuration
In order to run the service locally or remotely, you will need to have the following environment variables defined.

| name                                | example value              | required | description                                                                                   | sensitive? |
|-------------------------------------|----------------------------|----------|-----------------------------------------------------------------------------------------------|------------|
| `LOCAL_MODE`                        | `true`                     | yes      | Set to 'true' to use env vars in place of the k8s variable resolver                           | no         |
| `DISABLE_CACHE`                     | `true`                     | no       | Set to 'true' to disable caching for local development                                        | no         |
| `PARAMETER_MOUNT_PATH`              | `/mnt/params`              | no       | Path to mounted parameters directory                                                          | no         |
| `APPLICATION_PORT`                  | `8080`                     | yes      | The port the service will be hosted on.                                                       | no         |
| `AWS_REGION`                        | `us-east-1`                | yes      | The region where resources needed by the service are deployed                                 | no         |
| `OSDU_INSTANCE_NAME`                | `local`                    | yes      | The OSDU instance name                                                                        | no         |
| `AWS_ACCESS_KEY_ID`                 | `ASIAXXXXXXXXXXXXXX`       | yes      | The AWS Access Key for a user with access to Backend Resources required by the service        | yes        |
| `AWS_SECRET_ACCESS_KEY`             | `super-secret-key==`       | yes      | The AWS Secret Key for a user with access to Backend Resources required by the service        | yes        |
| `AWS_SESSION_TOKEN`                 | `session-token-xxxxxxxxxx` | no       | AWS Session token needed if using an SSO user session to authenticate                         | yes        |
| `LOG_LEVEL`                         | `DEBUG`                    | yes      | The Log Level severity to use (https://www.tutorialspoint.com/log4j/log4j_logging_levels.htm) | no         |
| `SSM_ENABLED`                       | `True`                     | yes      | Set to 'true' to use SSM to resolve config properties, otherwise use env vars                 | no         |
| `SSL_ENABLED`                       | `false`                    | no       | Set to 'false' to disable SSL for local development                                           | no         |
| `ENTITLEMENTS_BASE_URL`             | `http://localhost:8081`    | yes      | Specify the base url for an entitlements service instance. Can be run locally or remote       | no         |
| `PARTITION_BASE_URL`                | `http://localhost:8082`    | no       | Base URL for partition service                                                                | no         |
| `POLICY_BASE_URL`                   | `http://localhost:8083`    | no       | Base URL for policy service                                                                   | no         |
| `POLICY_SERVICE_ENABLED`            | `true`                     | no       | Enable policy service integration                                                             | no         |
| `POLICY_CACHE_TIMEOUT`              | `2`                        | no       | Policy cache timeout in minutes                                                               | no         |
| `ELASTIC_DISABLE_CERTIFICATE_TRUST` | `true`                     | no       | Disable SSL certificate validation for Elasticsearch                                          | no         |
| `TMP_VOLUME_PATH`                   | `/tmp`                     | no       | Path for temporary volume storage                                                             | no         |
| `DISABLE_USER_AGENT`                | `false`                    | no       | Disable user agent validation                                                                 | no         |

### Run Locally
Check that maven is installed:

example:
```bash
$ mvn --version
Apache Maven 3.8.3 (ff8e977a158738155dc465c6a97ffaf31982d739)
Maven home: /usr/local/Cellar/maven/3.8.3/libexec
Java version: 17.0.7, vendor: Amazon.com Inc.
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. Copy one of the below files' content to your .m2 folder
* For development against the OSDU GitLab environment, leverage: `<REPO_ROOT>~/.mvn/community-maven.settings.xml`
* For development in an AWS Environment, leverage: `<REPO_ROOT>/provider/search-aws/maven/settings.xml`

* Navigate to the service's root folder and run:

```bash
mvn clean package -pl .,search-core,provider/search-aws -P aws,core
```

* If you wish to build the project without running tests

```bash
mvn clean package -pl .,search-core,provider/search-aws -P aws,core -DskipTests
```

After configuring your environment as specified above, you can follow these steps to run the application. These steps should be invoked from the *repository root.*
<br/>
<br/>
NOTE: If not on osx/linux: Replace `*` with version numbers as defined in the provider/search-aws/pom.xml file

```bash
java -jar provider/search-aws/target/search-aws-*.*.*-SNAPSHOT-spring-boot.jar
```

## Running Elasticsearch locally
For search to work, it needs to have access to an Elasticsearch cluster. The easiest way to do this is to spin one up locally.
You can spin one up locally using Docker or Kubernetes Helm. What's detailed below is simply downloading executable and running directly.

To index data for search you can spin up indexer locally following indexer-aws's README

Instructions copied from here for longevity: https://www.elastic.co/guide/en/elasticsearch/reference/6.8/getting-started-install.html

1. Download a distribution from here: https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.8.20.tar.gz

2. Extract the archive
Linux and macOS: `tar -xvf elasticsearch-6.8.20.tar.gz`
Windows PowerShell: `Expand-Archive elasticsearch-6.8.20-windows-x86_64.zip`

3. Start Elasticsearch from the bin directory:
Linux and macOS:
   ```
   cd elasticsearch-6.8.20/bin
   ./elasticsearch
   ```
   
   Windows:
   
   ```
   cd %PROGRAMFILES%\Elastic\Elasticsearch\bin
   .\elasticsearch.exe
    ```
   
You should see in the logs that pop up what url and port it runs on. By default you should see http://localhost with port 9300

## Testing
 
 ### Running Integration Tests 
 This section describes how to run OSDU Integration tests (testing/search-test-aws).
 
 These tests will index data directly in elasticsearch and then use search API to retrieve it

 | name | example value | description | sensitive?
 | ---  | ---   | ---         | ---        |
 | `AWS_ACCESS_KEY_ID` | `ASIAXXXXXXXXXXXXXX` | The AWS Access Key for a user with access to Backend Resources required by the service | yes |
 | `AWS_SECRET_ACCESS_KEY` | `super-secret-key==` | The AWS Secret Key for a user with access to Backend Resources required by the service | yes |
 | `AWS_SESSION_TOKEN` | `session-token-xxxxxxxxx` | AWS Session token needed if using an SSO user session to authenticate | yes |
 | `AWS_COGNITO_USER_POOL_ID` | `us-east-1_xxxxxxxx` | User Pool Id for the reference cognito | no |
 | `AWS_COGNITO_CLIENT_ID` | `xxxxxxxxxxxx` | Client ID for the Auth Flow integrated with the Cognito User Pool | no |
 | `AWS_COGNITO_AUTH_FLOW` | `USER_PASSWORD_AUTH` | Auth flow used by reference cognito deployment | no |
 | `DEFAULT_DATA_PARTITION_ID_TENANT1` | `opendes` | Partition used to create and index record | no |
 | `DEFAULT_DATA_PARTITION_ID_TENANT2` | `common` | Another needed partition| no |
 | `AWS_COGNITO_AUTH_PARAMS_USER` | `int-test-user@testing.com` | Int Test Username | no |
 | `AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS` | `noaccess@testing.com` | No Access Username | no |
 | `AWS_COGNITO_AUTH_PARAMS_PASSWORD` | `some-secure-password` | Int Test User/NoAccessUser Password | yes |
 | `ENTITLEMENTS_DOMAIN` | `example.com` | Domain for user's groups | no |
 | `OTHER_RELEVANT_DATA_COUNTRIES` | `US` | Used to create demo legal tag | no |
 | `STORAGE_HOST` | `http://localhost:8080/api/storage/v2/` | The url where the storage API is hosted | no |
 | `SEARCH_HOST` | `http://localhost:8080/api/search/v2/` | The url where the storage API is hosted | no |
 | `LEGAL_TAG` | `opendes-public-usa-dataset-1` | Base url for deployment | no |
 | `ELASTIC_HOST` | `localhost` | Url for elasticsearch | no |
 | `ELASTIC_PORT` | `9300` | Port for elasticsearch | no |
 | `ELASTIC_PASSWORD` | `xxxxxxxxxxxxxxx` | Password for user to access elasticsearch | yes |
 | `ELASTIC_USER_NAME` | `xxxxxxxxxxxxxxxx` | Username for user to access elasticsearch | yes |


 **Creating a new user to use for integration tests**
 ```
 aws cognito-idp admin-create-user --user-pool-id ${AWS_COGNITO_USER_POOL_ID} --username ${AWS_COGNITO_AUTH_PARAMS_USER} --user-attributes Name=email,Value=${AWS_COGNITO_AUTH_PARAMS_USER} Name=email_verified,Value=True --message-action SUPPRESS

 aws cognito-idp initiate-auth --auth-flow ${AWS_COGNITO_AUTH_FLOW} --client-id ${AWS_COGNITO_CLIENT_ID} --auth-parameters USERNAME=${AWS_COGNITO_AUTH_PARAMS_USER},PASSWORD=${AWS_COGNITO_AUTH_PARAMS_PASSWORD}
 ```
 
 **Entitlements group configuration for integration accounts**
 <br/>
 In order to add user entitlements, run entitlements bootstrap scripts in the entitlements project
 
 | AWS_COGNITO_AUTH_PARAMS_USER |
 | ---  | 
 | service.search.admin |
 
 Execute following command to build code and run all the integration tests:

### Run Tests simulating Pipeline

* Prior to running tests, scripts must be executed locally to generate pipeline env vars

```bash
testing/integration-tests/search-test-aws/build-aws/prepare-dist.sh

#Set Neccessary ENV Vars here as defined in run-tests.sh

dist/testing/integration/build-aws/run-tests.sh 
```

### Run Tests using mvn
Set required env vars and execute the following:
```
mvn clean package -f testing/pom.xml -pl search-test-core,search-test-aws -DskipTests
mvn test -f testing/search-test-aws/pom.xml
```



## License
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
