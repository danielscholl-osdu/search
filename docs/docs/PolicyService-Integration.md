# Policy Service Integration

Search service now supports data authorization checks via [Policy service](https://osdu.pages.opengroup.org/platform/security-and-compliance/policy/). Policy service allows dynamic policy evaluation on user requests and can be configured per data partition.

Search service combines the user query with the Elasticsearch Query DSL translated from the evaluation policy in Policy service to do search in one operation against Elasticsearch index. CSP must opt-in to delegate data access to Policy Service.      

## Steps to enable Policy service for a provider:

- Register policy for Search service, please look at policy service [documentation](https://osdu.pages.opengroup.org/platform/security-and-compliance/policy/) for more details. The default evaluation policy(data partition policy `osdu/partition/<data partition>/search.rego` or instance policy `osdu/instance/search.rego`) in Policy service is based on the current data ACLs and the user groups.

- Add and provide values for following runtime configuration in `application.properties`:
```
  featureFlag.policy.enabled=true
  service.policy.id=${policy_id}
  service.policy.endpoint=${policy_service_endpoint}
  service.policy.cache.timeout=<timeout_in_minutes>
```
The policy id can be an instance based id (ie, osdu.instance.search) or partition based id (ie, osdu.partiton["%s"].search).
