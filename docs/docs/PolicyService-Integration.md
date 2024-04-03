# Policy Service Integration

Search service now supports data authorization checks via [Policy service](https://osdu.pages.opengroup.org/platform/security-and-compliance/policy/). Policy service allows dynamic policy evaluation on user requests and can be configured per data partition.

Search service combines the user query with the Elasticsearch Query DSL translated from the evaluation policy in Policy service to do search in one operation against Elasticsearch index. CSP must opt-in to delegate data access to Policy Service.      

## Steps to enable Policy service for a provider:

- Register policy for Search service, please look at policy service [documentation](https://osdu.pages.opengroup.org/platform/security-and-compliance/policy/) for more details. The default evaluation policy(data partition policy `osdu/partition/<data partition>/search.rego` or instance policy `osdu/instance/search.rego`) in Policy service is based on the current data ACLs and the user groups.

- Add and provide values for following runtime configuration in `application.properties`:
```
  service.policy.enabled=true
  service.policy.id=${policy_id}
  service.policy.endpoint=${policy_service_endpoint}
  policy.cache.timeout=<timeout_in_minutes>
  PARTITION_API=${partition_service_endpoint}
```
The policy id can be an instance based id (ie, osdu.instance.search) or partition based id (ie, osdu.partiton["%s"].search).

## Known Limitation for Azure Http Header Size

In  Microsoft Azure, there is a limitation for the Http Header size in Azure. For more information, see: https://learn.microsoft.com/en-US/troubleshoot/developer/webapps/iis/www-administration-management/http-bad-request-response-kerberos#cause

Consider If the user belongs to more than **2000** data groups. When Search calls Policy translate API, in the request header **'X-Data-Groups'** (>2000) values are passed. Microsoft Azure Application Gateway doesn't support such large headers. As a result, it returns a **'400 Request Header Or Cookie Too Large'** error before even reaching the policy service.

This error body is translated as input query for ElasticSearch, which results in ElasticSearch exception. As a result, the search API response will be:

```
{
    "code": 400,
    "reason": "Bad Request",
    "message": "Failed to derive xcontent"
}
```

### Workaround

Delete any stale or test groups associated with the user via the [Entitlements API](https://osdu.pages.opengroup.org/platform/security-and-compliance/entitlements/api/), such that the number of data groups is within the limit of 2000. 

For more information, see: https://learn.microsoft.com/en-US/troubleshoot/developer/webapps/iis/www-administration-management/http-bad-request-response-kerberos#workaround-1-decrease-the-number-of-active-directory-groups