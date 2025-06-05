### Running Acceptance Tests

You will need to have the following environment variables defined.

| name                                | value                                                                      | description                                          | sensitive? | source |
|-------------------------------------|----------------------------------------------------------------------------|------------------------------------------------------|------------|--------|
| `HOST`                              | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com`                 | Base URL for the OSDU platform                       | no         | -      |
| `SEARCH_HOST`                       | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/search/v2/`  | Base URL for the Search API service                  | no         | -      |
| `STORAGE_HOST`                      | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/storage/v2/` | Base URL for the Storage API service                 | no         | -      |
| `INDEXER_HOST`                      | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/indexer/v2/` | Base URL for the Indexer API service                 | no         | -      |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | eg `osdu`                                                                  | Primary partition ID used for testing tenant 1       | no         | -      |
| `DEFAULT_DATA_PARTITION_ID_TENANT2` | eg `non-exist`                                                             | Non-existing tenant name                             | no         | -      |
| `ENTITLEMENTS_DOMAIN`               | eg `group`                                                                 | Domain name for entitlements service                 | no         | -      |
| `GROUP_ID`                          | eg `group`                                                                 | Group i                                              | no         | -      |
| `LEGAL_TAG`                         | eg `osdu-demo-legaltag`                                                    | Legal tag                                            | no         | -      |
| `OTHER_RELEVANT_DATA_COUNTRIES`     | eg `US`                                                                    |                                                      | no         | -      |

Authentication can be provided as OIDC config:

| name                                            | value                                   | description                                             | sensitive? | source |
|------------------------------------------------|-----------------------------------------|----------------------------------------------------------|------------|--------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | Client ID for privileged user authentication            | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | Client secret for privileged user authentication        | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | URL of the OpenID Connect provider for authentication   | yes        | -      |

Or tokens can be used directly from env variables:

| name                    | value      | description                                       | sensitive? | source |
|------------------------|------------|----------------------------------------------------|------------|--------|
| `PRIVILEGED_USER_TOKEN` | `********` | Authentication token for privileged user access   | yes        | -      |

**Entitlements configuration for integration accounts**

| PRIVILEGED_USER                                                                                                                                   |
|---------------------------------------------------------------------------------------------------------------------------------------------------|
|  users<br/>service.entitlements.user<br/>service.search.user<br/>data.test1<br/>data.integration.test<br/>users@{tenant1}@{groupId}.com           |

Execute following command to build code and run all the integration tests:

```bash
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ (cd search-acceptance-test && mvn clean test)
```

**Cucumber Tagging Scenarios**

Tags being covered in acceptance test:

| Cucumber Tag | Covered |
|--------------------------------|--------|
| @default <br/> @health <br/> @autocomplete | YES |
| @xcollab | NO |

## License

Copyright © Google LLC

Copyright © EPAM Systems

Copyright © ExxonMobil

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
