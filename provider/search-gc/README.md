# Search Service
os-search-gc is a [Spring Boot](https://spring.io/projects/spring-boot) service that hosts CRUD APIs that enable the execution of OSDU R2 domain searches against Elasticsearch.

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
Pre-requisites

* GCloud SDK with java (latest version)
* JDK 17 or later
* Lombok 1.16 or later
* Maven

# Configuration

## Service Configuration

### Google Cloud:
[Google Cloud service configuration ](docs/gc/README.md)

### Run Locally
Check that maven is installed:

```bash
$ mvn --version
Apache Maven 3.8.7
Maven home: /usr/share/maven
Java version: 17.0.7
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
cd provider/search-gc/ && 
java --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=${PORT} \
    -Dlog4j.formatMsgNoLookups=true \
    -Dloader.main=org.opengroup.osdu.search.provider.gcp.SearchGcpApplication \
    -jar /target/search-${PROVIDER_NAME}.jar
```

## Testing
Navigate to search service's root folder and run all the tests:

```bash
# build + install integration test core
$ (cd testing/storage-test-core/ && mvn clean install)
```
 
### Running E2E Tests 
This section describes how to run cloud OSDU E2E tests.

### Google Cloud test configuration:
[Google Cloud service configuration ](docs/gc/README.md)

## Deployment
Search Service is compatible with App Engine Flexible Environment and Cloud Run.

* To deploy into Cloud run, please, use this documentation:
  https://cloud.google.com/run/docs/quickstarts/build-and-deploy

* To deploy into GKE, please, use this documentation:
  https://cloud.google.com/kubernetes-engine/docs/deploy-app-cluster

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
