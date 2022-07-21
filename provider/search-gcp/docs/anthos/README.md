## Service Configuration for Anthos

## Environment variables:

Define the following environment variables.

Must have:

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `SPRING_PROFILES_ACTIVE` | ex `anthos` | Spring profile that activate default configuration for GCP environment | false | - |
| `<ELASTICSEARCH_USER_ENV_VARIABLE_NAME>` | ex `user` | Elasticsearch user, name of that variable not defined at the service level, the name will be received through partition service. Each tenant can have it's own ENV name value, and it must be present in ENV of Indexer service, see [Partition properties set](#Properties-set-in-Partition-service)  | yes | - |
| `<ELASTICSEARCH_PASSWORD_ENV_VARIABLE_NAME>` | ex `password` | Elasticsearch password, name of that variable not defined at the service level, the name will be received through partition service. Each tenant can have it's own ENV name value, and it must be present in ENV of Indexer service, see [Partition properties set](#Properties-set-in-Partition-service) | false | - |

Defined in default application property file but possible to override:

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `service` | Logging prefix | no | - |
| `SERVER_SERVLET_CONTEXPATH` | `/api/search/v2/` | Servlet context path | no | - |
| `AUTHORIZE_API` | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint | no | output of infrastructure deployment |
| `REDIS_SEARCH_HOST` | ex `records-changed` | Redis host for search | no | https://console.cloud.google.com/memorystore/redis/instances |
| `REDIS_SEARCH_PORT` | ex `6379` | Redis host for search | no | https://console.cloud.google.com/memorystore/redis/instances |
| `GOOGLE_AUDIENCES` | ex `*****.apps.googleusercontent.com` | Client ID for getting access to cloud resources | yes | https://console.cloud.google.com/apis/credentials |
| `GOOGLE_APPLICATION_CREDENTIALS` | ex `/path/to/directory/service-key.json` | Service account credentials, you only need this if running locally | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
| `SECURITY_HTTPS_CERTIFICATE_TRUST` | ex `false` | Elastic client connection uses TrustSelfSignedStrategy(), if it is 'true' | false | output of infrastructure deployment |
| `PARTITION_API` | ex `http://localhost:8080/api/partition/v1` | Partition service endpoint | no | output of infrastructure deployment |
| `POLICY_API` | ex `http://localhost:8080/api/policy/v1/` | Police service endpoint | no | output of infrastructure deployment |
| `POLICY_ID` | ex `search` | policeId from ex `http://localhost:8080/api/policy/v1/policies`. Look at `POLICY_API` | no | - |
| `INDEXER_HOST` | ex `https://os-indexer-dot-opendes.appspot.com/api/indexer/v2/` | Indexer API endpoint | no | output of infrastructure deployment |

These variables define service behavior, and are used to switch between `anthos` or `gcp` environments, their overriding and usage in mixed mode was not tested.
Usage of spring profiles is preferred.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `PARTITION_AUTH_ENABLED` | ex `true` or `false` | Disable or enable auth token provisioning for requests to Partition service | no | - |
| `SERVICE_TOKEN_PROVIDER` | `GCP` or `OPENID` |Service account token provider, `GCP` means use Google service account `OPEIND` means use OpenId provider like `Keycloak` | no | - |

### Properties set in Partition service:

Note that properties can be set in Partition as `sensitive` in that case in property `value` should be present not value itself, but ENV variable name.
This variable should be present in environment of service that need that variable.

Example:
```
    "elasticsearch.port": {
      "sensitive": false, <- value not sensitive 
      "value": "9243"  <- will be used as is.
    },
      "elasticsearch.password": {
      "sensitive": true, <- value is sensitive 
      "value": "ELASTIC_SEARCH_PASSWORD_OSDU" <- service consumer should have env variable ELASTIC_SEARCH_PASSWORD_OSDU with elastic search password
    }
```



## Elasticsearch configuration

**prefix:** `elasticsearch`

It can be overridden by:

- through the Spring Boot property `elastic-search-properties-prefix`
- environment variable `ELASTIC_SEARCH_PROPERTIES_PREFIX`

**Propertyset:**

| Property | Description |
| --- | --- |
| elasticsearch.host | server URL |
| elasticsearch.port | server port |
| elasticsearch.user | username |
| elasticsearch.password | password |

<details><summary>Example of a definition for a single tenant</summary></details>

```

curl -L -X PATCH 'http://partition.com/api/partition/v1/partitions/opendes' -H 'data-partition-id: opendes' -H 'Authorization: Bearer ...' -H 'Content-Type: application/json' --data-raw '{
  "properties": {
    "elasticsearch.host": {
      "sensitive": false,
      "value": "elastic.us-central1.gcp.cloud.es.io"
    },
    "elasticsearch.port": {
      "sensitive": false,
      "value": "9243"
    },
    "elasticsearch.user": {
      "sensitive": true,
      "value": "<USER_ENV_VARIABLE_NAME>" <- (Not actual value, just name of env variable)
    },
      "elasticsearch.password": {
      "sensitive": true,
      "value": "<PASSWORD_ENV_VARIABLE_NAME>" <- (Not actual value, just name of env variable)
    }
  }
}'

```

### Running E2E Tests

You will need to have the following environment variables defined.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `ELASTIC_PASSWORD` | `********` | Password for Elasticsearch | yes | output of infrastructure deployment |
| `ELASTIC_USER_NAME` | `********` | User name for Elasticsearch | yes | output of infrastructure deployment |
| `ELASTIC_HOST` | ex `elastic.domain.com` | Host Elasticsearch | yes | output of infrastructure deployment |
| `ELASTIC_PORT` | ex `9243` | Port Elasticsearch | yes | output of infrastructure deployment |
| `INDEXER_HOST` | ex `https://os-indexer-dot-opendes.appspot.com/api/indexer/v2/` | Indexer API endpoint | no | output of infrastructure deployment |
| `DATA_GROUP` | `opendes` | The service account to this group and substitute | no | - |
| `ENTITLEMENTS_DOMAIN` | ex `opendes-gcp.projects.com` | OSDU R2 to run tests under  | no | - |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | ex `opendes` | HTTP Header 'Data-Partition-ID'  | no | - |
| `DEFAULT_DATA_PARTITION_ID_TENANT2` | ex `not-exist` | HTTP Header 'Data-Partition-ID' with not existing tenant  | no | - |
| `SEARCH_HOST` | ex `http://localhost:8080/api/search/v2/` | Endpoint of search service | no | - |
| `LEGAL_TAG` | ex `my-legal-tag` | a valid legal tag | no | - |  
| `SECURITY_HTTPS_CERTIFICATE_TRUST` | ex `false` | Elastic client connection uses TrustSelfSignedStrategy(), if it is 'true' | false | output of infrastructure deployment |
| `TEST_OPENID_PROVIDER_CLIENT_ID` | `********` | Client Id for `$INTEGRATION_TESTER` | yes | -- |
| `TEST_OPENID_PROVIDER_CLIENT_SECRET` | `********` |  | Client secret for `$INTEGRATION_TESTER` | -- |
| `TEST_OPENID_PROVIDER_URL` | `https://keycloak.com/auth/realms/osdu` | OpenID provider url | yes | -- |

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER | NO_DATA_ACCESS_TESTER | 
| ---  | ---   |
| users<br/>service.entitlements.user<br/>service.search.user<br/>data.test1<br/>data.integration.test<br/>users@{tenant1}@{domain}.com |

Execute following command to build code and run all the integration tests:

```bash
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ (cd testing/search-test-anthos/ && mvn clean test)
```
