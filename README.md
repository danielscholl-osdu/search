Copyright 2017-2019, Schlumberger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
# Search and Indexer Service

## Azure Implementation

All documentation for the Azure implementation of `os-search` lives [here](./provider/search-azure/README.md)

## GCP Implementation

### Pre-requisites

* GCloud SDK with java (latest version)
* JDK 8
* Lombok 1.16 or later
* Maven

You will also require Git to work on the project.

### Update the Google cloud SDK to the latest version:

```sh
gcloud components update
```

### Setting up the local development environment
* Update the Google cloud SDK to the latest version:

```sh
gcloud components update
```

```sh
gcloud config set project <YOUR-PROJECT-ID>
```
* Perform a basic authentication in the selected project

```sh
gcloud auth application-default login
```

### Build project and run unit tests
* Navigate to search service's root folder and run:
 
```sh
mvn clean install   
```

* If you wish to see the coverage report then go to testing/target/site/jacoco-aggregate and open index.html

* If you wish to build the project without running tests

```sh
mvn clean install -DskipTests
```

* If you wish to run integration tests

```sh
mvn clean install -P integration-test
```
    
* Running locally
* Navigate to search service's root folder and run:

```sh
mvn jetty:run
```

### Deployment
* Data-Lake Indexer Service Google Cloud Endpoints on App Engine Standard environment
  * Edit the appengine-web.xml
    * Open the [appengine-web.xml](indexer/src/main/webapp/WEB-INF/appengine-web.xml) file in editor, and replace the YOUR-PROJECT-ID `PROJECT` line with Google Cloud Platform project Id. Also update `STORAGE_HOST`, `STORAGE_SCHEMA_HOST`, `IDENTITY_QUERY_ACCESS_HOST` and `IDENTITY_AUTHORIZE_HOST` based on your deployment

  * Deploy
    ```sh
    mvn appengine:deploy -pl org.opengroup.osdu.search:indexer -amd
    ```

  * If you wish to deploy the indexer service without running tests
    ```sh
    mvn appengine:deploy -pl org.opengroup.osdu.search:indexer -amd -DskipTests
    ```
    
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
  
### Cloud Environment Setup
Refer to [Cloud Environment Setup](docs/setup.md) whenever setting up new services on new google projects

### Open API spec
go-swagger brings to the go community a complete suite of fully-featured, high-performance, API components to work with a Swagger API: server, client and data model.
* How to generate go client libraries?
    Assumptions:
    a.	Running Windows
    b.	Using Powershell
    c.	Directory for source code: C:\devel\

    1.	Install Golang
    2.	Install go-swagger.exe, add to $PATH
        ```
        go get -u github.com/go-swagger/go-swagger/cmd/swagger
        ```
    3.	Create the following directories:
        ```
        C:\devel\datalake-test\src\
        ```
    4.	Copy “search_openapi.json” to “C:\devel\datalake-test\src”
    5.	Set environment variable GOPATH (run the following in Powershell):
        ```
        $env:GOPATH="C:\devel\datalake-test\"
        ```
    6.	Change current directory to “C:\devel\datalake-test\src”
        ```
        cd C:\devel\datalake-test\src
        ```
    7.	Run the following command:
        ```
        swagger generate client -f 'search_openapi.json' -A search_openapi
        ```
        
       

### Maintenance
* Indexer:
  * Cleanup indexes - Indexer has a cron job running which hits following url:
  ```
  /_ah/cron/indexcleanup
  ```
  Note: The job will run for all the tenants in a deployment. It will delete all the indices following the pattern as:
  ```
    <accountid>indexpattern
  ```
  where indexpattern is the index pattern regular expression which you want to delete
  indexpattern is defined in web.xml (in indexer) file with an environment variable as CRON_INDEX_CLEANUP_PATTERN
  The scheduling of cron is done in the following repository:
  https://slb-swt.visualstudio.com/data-management/_git/deployment-init-scripts?path=%2F3_post_deploy%2F1_appengine_cron%2Fcron.yaml&version=GBmaster
  