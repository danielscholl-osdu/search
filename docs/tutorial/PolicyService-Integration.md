Search service now supports data authorization checks via Policy service. Policy service allows dynamic policy evaluation on user requests and can
be configured per partition. 

By default, Search service utilizes Elastic's authorization query filter for data authorization checks. CSP must opt-in to delegate data access to Policy Service.      

Here are steps to enable Policy service for a provider:

- Enable policy configuration for desired partition:
  ```
  PATCH /api/partition/v1/partitions/{partitionId}
  {
    "properties": {
        "policy-service-enabled": {
            "sensitive": false,
            "value": "true"
        }
    }
  }
  ```

- Register policy for Search service, please look at policy service [documentation](https://community.opengroup.org/osdu/platform/security-and-compliance/policy#add-policy) for more details.  

- Add and provide values for following runtime configuration in `application.properties`
  ```
  service.policy.enabled=true
  POLICY_API=${policy_service_endpoint}
  PARTITION_API=${partition_service_endpoint}
  ```
  
- This is an experimental feature and at this moment has following limitations
    1. If the query has `returnedFields` set, it must contain all `acl, kind, legal` and `id`
    2. In the current implementation, totalCount represents the number of records matching user query before the search policy is applied
    3. Because the policy auth filter is applied outside of query handles, cursor may not point to the accurate data entry when using `query_with_cursor`