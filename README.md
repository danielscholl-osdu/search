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
