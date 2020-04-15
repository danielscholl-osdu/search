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
# Data Ecosystem Service Dashboard

## Links
Unified summary report: [link](https://datastudio.google.com/u/0/reporting/1h_edNOGn2aCefxuT17YXuHS9-nIqY5vP/page/nQN)
### Sub links
All the links below could be found on the unified summary report, which is suggested to be the entry point of the dashboard

Search Detail Report: [link](https://datastudio.google.com/u/0/reporting/1VdLunUNNlci6OG3Gx8X6pv4czSN3T5Wv/page/nQN)

Search Analytics Report: [link](https://datastudio.google.com/u/0/reporting/1-xZu20zsTlDtMCgUYz1wdNbYwVbbVNXm/page/nQN)

Indexer Detail Report: [link](https://datastudio.google.com/u/0/reporting/15VPuCNjeVw26Ek2E6WxAtEPe-oNdsP-6/page/nQN)

## Dashboard structure
There are 3 levels reports: Summary, Detail and Analytics, and all reports integrate all the environments.

Summary Report: is aim to integrates all services in data ecosystem in one page report and shows latency, request and error metrics

Detail Report: is aim to report latency break-down, and error details

Analytics Report: is aim to bring BI into the report 

## Application log
There are 2 parts in the application log: audit log (necessary info for all services), customized log (customized info for individual service).

Refer to [AuditLogStructure.java](../core/src/main/java/com/slb/com/logging/AuditLogStrcture.java) about what are the necessary audit info to log in the application log

Refer to [SearchLogStrcture.java](../search/src/main/java/com/slb/com/logging/SearchLogStructure.java) as an example of customized info to log in application log

Remember to log the trace id in the application log at the same location as the nginx request. Refer to the [AuditLogStructure.java](../core/logging/AuditLogStrcture.java) about how to log the trace id in application log

## Query log sink
Once the logging implementation is finished, you need to create the sinks. Take search service as an example, the incoming request payload and equivalent elastic queries are logged for search service. Queries are logged in search.app in StackDriver. Also you could refer to the [existing sinks](https://console.cloud.google.com/logs/exports?project=evd-ddl-us-services&organizationId=980806506473&minLogLevel=0&expandAll=false&timestamp=2018-07-16T19:27:37.041000000Z&customFacets=&limitCustomFacetWidth=true&dateRangeStart=2018-07-16T18:27:37.292Z&dateRangeEnd=2018-07-16T19:27:37.292Z&interval=PT1H&resource=gae_app%2Fmodule_id%2Findexer&logName=projects%2Fevd-ddl-us-services%2Flogs%2Fappengine.googleapis.com%252Frequest_log) in the google cloud project to create an new one. 

To create BigQuery sink to export logs
  * Create filter in StackDriver
  ```sh
  resource.type="gae_app"
  resource.labels.module_id="search"
  logName="projects/slb-data-lake-dev/logs/search.app"
  jsonPayload.elastic-query="*" OR
  jsonPayload.request-query="*"
  ```
  
  * Create big query [sink](https://cloud.google.com/logging/docs/export/configure_export_v2)
  
  * Query logs will start showing up in Big Query after sink is setup properly in query_log_ table
  
  * jsonPayload_request_query & jsonPayload_elastic_query column are the logged queries.  

## Big Query Scripts
Now you have the sinks that dumps the log into bigquery, and it will create separated table in bigquery for each day, so it is time to work on the big query scripts to organize your information to report

All dashboard related big query scripts are located under "service_dashboard_datasources" dataset in [Google BigQuery](https://bigquery.cloud.google.com/project/evd-ddl-us-services). If you have new services need to be added in to the summary report. Please follow the serivce_request script and modify it to union the new service request log. If you want to work on detail or analytics report for a new service, please take "search_combined", "indexer_combined", and "indexer_issue_records" big query views as examples.

### How to modify the existing views
Go to the [Google BigQuery](https://bigquery.cloud.google.com/project/evd-ddl-us-services), and find the view that you want to modify under the "service_dashboard_datasources" dataset, then clicking the "details" on the right and you will see the big query script in the bottom. You can modify the script by clicking the "Edit query" button and save your script by using "Save view" after you finished the modification.

### Summary report datasource (service_dashboard_datasrouce view)
This view is used for the summary report, and only depends on nginx request log now, so it could be easily expend to any other services and integrate them together into one page report. Once we implemented the application for all the services, we could include application log information into this view.

Here is the example and explanation of the search service script of the view

```bigquery script
select timestamp, receiveTimeStamp, httpRequest.status as Status, httpRequest.latency as ResponseTime, httpRequest.requestUrl as requestURL, httpRequest.requestMethod as requestMethod, "P4D-EU" as env, resource.labels.module_id as service, resource.labels.version_id as versionId,
labels.appengine_googleapis_com_trace_id as traceId,
CASE WHEN REGEXP_CONTAINS(httpRequest.requestURL, '.*/index/schema.*') AND httpRequest.requestMethod='GET' THEN 'getKindSchema'
     WHEN REGEXP_CONTAINS(httpRequest.requestURL, '.*/index.*') AND httpRequest.requestMethod='DELETE' THEN 'deleteIndex'
     WHEN REGEXP_CONTAINS(httpRequest.requestURL, '.*/query(\?)+.*') AND httpRequest.requestMethod='POST' THEN 'query'
     WHEN REGEXP_CONTAINS(httpRequest.requestURL, '.*/query_with_cursor(\?).*') AND httpRequest.requestMethod='POST' THEN 'query'
     ELSE 'others' END as APIName
from `p4d-ddl-eu-services.p4d_datalake_search_all_logs.appengine_googleapis_com_nginx_request_*`
```

The script uses the REGEX_CONTAINS function to classify the API name for all the search service requests, so that the data studio dashboard could build latency, request, error reports for each api separately within the same service. When there is new services need to be added into the summary report, you should work on the similar script for the service nginx request log and union them together.

### Detail/Analytics report datasource
Here we use "search_combined" view as an example. This view combines search service application log and nginx request log information together by using the trace id. For the detail report of the new service, you should work on a similar script for the service application log and save as a new bigquery view, then create a separated detail report in datastudio and add the link into the summary report datastource (Refer to [Add new service into summary report](###Add new service into summary report) about how to do this) 

```bigquery script
select requestLog.timestamp as timestamp, requestLog.receiveTimeStamp as receiveTimeStamp, requestLog.httpRequest.status as Status, requestLog.httpRequest.latency as ResponseTime, entitlementsLog.httpRequest.latency as EntitlementsLatency,
searchLog.jsonPayload.applicationLog.onbehalfof, searchLog.jsonPayload.applicationLog.userid, searchLog.jsonPayload.applicationLog.slbaccountid, searchLog.jsonPayload.applicationLog.correlationid,
searchLog.jsonPayload.applicationLog.request.kind as kind, searchLog.jsonPayload.applicationLog.request.query as query, 
safe_cast(searchLog.jsonPayload.applicationLog.elasticSearchLatency as float64) as elasticSearchLatency, "P4D-EU" as env,
searchLog.resource.labels.module_id as service, searchLog.resource.labels.version_id as versionId,
safe_cast(split(searchLog.jsonPayload.applicationLog.geolocation.userLocation, ",")[SAFE_OFFSET(0)] as float64) as latitude,
safe_cast(split(searchLog.jsonPayload.applicationLog.geolocation.userLocation, ",")[SAFE_OFFSET(1)] as float64) as longitude,
searchLog.jsonPayload.applicationLog.geolocation.userCity as city, searchLog.jsonPayload.applicationLog.geolocation.userCountry as country, searchLog.jsonPayload.applicationLog.geolocation.userRegion as region,
CASE WHEN REGEXP_CONTAINS(requestLog.httpRequest.requestURL, '.*/index/schema.*') AND requestLog.httpRequest.requestMethod='GET' THEN 'getKindSchema'
     WHEN REGEXP_CONTAINS(requestLog.httpRequest.requestURL, '.*/index.*') AND requestLog.httpRequest.requestMethod='DELETE' THEN 'deleteIndex'
     WHEN REGEXP_CONTAINS(requestLog.httpRequest.requestURL, '.*/query(\?)+.*') AND requestLog.httpRequest.requestMethod='POST' THEN 'query'
     WHEN REGEXP_CONTAINS(requestLog.httpRequest.requestURL, '.*/query_with_cursor(\?).*') AND requestLog.httpRequest.requestMethod='POST' THEN 'query'
     ELSE 'others' END as APIName,
requestLog.resource.labels.project_id as projectId,
requestLog.labels.appengine_googleapis_com_trace_id as traceId
from `p4d-ddl-eu-services.p4d_datalake_search_all_logs.appengine_googleapis_com_nginx_request_*` as requestLog
INNER JOIN `p4d-ddl-eu-services.p4d_datalake_application_log.search_app_*` as searchLog ON requestLog.labels.appengine_googleapis_com_trace_id = searchLog.labels.appengine_googleapis_com_trace_id
INNER JOIN `p4d-ddl-eu-services.p4d_datalake_entitlements_all_logs.appengine_googleapis_com_nginx_request_*` as entitlementsLog ON requestLog.labels.appengine_googleapis_com_trace_id = entitlementsLog.labels.appengine_googleapis_com_trace_id
``` 

## Data Studio
Once the big query scripts are ready, please contact [Mingyang Zhu](mailto:mzhu9@slb.com) for the editor permission of the dashboard, and reconnect the data sources that has been modified or add new reports.

### Add new service into summary report
Refresh the "service_request" datasource in datastudio by reconnecting the datasource to the modified bigquery view, and the service should be added into the summary report automatically.

### Add detail/Analytics report for new service
Create a new datasource by connecting to your new bigquery view of the new service. Clone the exiting detail/analytics report as the starting point and add or delete report components to make it sound. Remember to add more CASE blocks of the report link into the "Detail Report Link" and "Analytics Report Link" fields in the "service_request" datasource, which looks like the following.

```Detail Report Link
CASE
WHEN Service='search' THEN 'https://datastudio.google.com/open/1VdLunUNNlci6OG3Gx8X6pv4czSN3T5Wv'
WHEN Service='indexer' THEN 'https://datastudio.google.com/open/15VPuCNjeVw26Ek2E6WxAtEPe-oNdsP-6'
ELSE 'https://datastudio.google.com/open/1h_edNOGn2aCefxuT17YXuHS9-nIqY5vP'
END
```