# Running Search Service

The Search Service is a Maven multi-module project with each cloud implementation placed in its submodule.

## AWS

Instructions for running the AWS implementation can be found [here](https://community.opengroup.org/osdu/platform/system/search-service/-/blob/master/provider/search-aws/README.md).

## Azure

Instructions for running the Azure implementation can be found [here](https://community.opengroup.org/osdu/platform/system/search-service/-/blob/master/provider/search-azure/README.md).


## Google

Instructions for running the Google implementation can be found [here](https://community.opengroup.org/osdu/platform/system/search-service/-/tree/master/provider/search-gc).

## BYOC Environment Setup
Checkout the code and perform the following actions: 
```shell script
  $ cd os-search  
  $ mvn clean install
  $ java -jar provider/search-byoc/target/search-byoc-1.0-SNAPSHOT-spring-boot.jar  
```
Once the service is up visit browser and hit the url http://localhost:8080/api/search/v2/swagger-ui.html
Enter ```opendes@byoc.local / 123 ``` as username and ***password***

Click on "Search API"

Click on "/query"

Fill in the "queryRequest" textfield with the following contents:

 if you search for 
 ```shell script
 {"kind": "*:*:*:*"}
```
you would get a 200 with no results (that is, if you have no data in memory)

If you search for
```shell script
{
  "kind": "common:ihs:well:1.0.0"
}
```
you will get a 404 which means there is no data found for that kind. 