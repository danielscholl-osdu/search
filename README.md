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

## Google Cloud Implementation

All documentation for the GC implementation of `os-search` lives [here](./provider/search-gc/README.md)

## AWS Implementation

All documentation for the AWS implementation of `os-search` lives [here](./provider/search-aws/README.md)

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

#### Server Url(full path vs relative path) configuration
- `api.server.fullUrl.enabled=true` It will generate full server url in the OpenAPI swagger
- `api.server.fullUrl.enabled=false` It will generate only the contextPath only
- default value is false (Currently only in Azure it is enabled)
[Reference]:(https://springdoc.org/faq.html#_how_is_server_url_generated) 

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

### Open API 3.0 - Swagger
- Swagger UI : https://host/context-path/swagger (will redirect to https://host/context-path/swagger-ui/index.html)
- api-docs (JSON) : https://host/context-path/api-docs
- api-docs (YAML) : https://host/context-path/api-docs.yaml

All the Swagger and OpenAPI related common properties are managed here [swagger.properties](./search-core/src/main/resources/swagger.properties)

