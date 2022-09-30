Search service now supports data authorization checks via Policy service. Policy service allows dynamic policy evaluation on user requests and can be configured per partition.

Search service combines the user query with the Elasticsearch Query DSL translated from the evaluation policy in Policy service to do search in one operation against Elasticsearch index. CSP must opt-in to delegate data access to Policy Service.      

Here are steps to enable Policy service for a provider:

- Register policy for Search service, please look at policy service [documentation](https://community.opengroup.org/osdu/platform/security-and-compliance/policy#add-policy) for more details. The default evaluation policy("osdu.instance.search") in Policy service is based on the current data ACLs and the user groups.

- Add and provide values for following runtime configuration in `application.properties`
  ```
  service.policy.enabled=true
  service.policy.id=${policy_id}
  service.policy.endpoint=${policy_service_endpoint}
  policy.cache.timeout=<timeout_in_minutes>
  PARTITION_API=${partition_service_endpoint}
  ```
The policy id can be an instance based id (ie, osdu.instance.search) or partition based id (ie, osdu.partiton["%s"].search).
