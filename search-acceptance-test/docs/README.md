### Running Acceptance Tests

You will need to have the following environment variables defined.

| name                                 | value                                            | description                                                                                                                                                                                                                    | sensitive?                              | source |
|--------------------------------------|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------|--------|
| `HOST`                               | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com`                                                   | -                                                                                                                                                                  | no                                     | -      |
| `SEARCH_HOST`                        | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/search/v2/`                                                   | -                                                                                                                                                   | no                                     | -      |
| `STORAGE_HOST`                       | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/storage/v2/`                                                   | -                                                                                                                                                  | no                                     | -      |
| `INDEXER_HOST`                       | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/indexer/v2/`                                                   | -                                                                                                                                                  | no                                     | -      |
| `DEFAULT_DATA_PARTITION_ID_TENANT1`  | eg `osdu`                                        | Partition Id used for testing                                                                                                                                                                                                  | no                                     | -      |
| `DEFAULT_DATA_PARTITION_ID_TENANT2`  | eg `non-exist`                                   |                                                                                                                                                                                                                                | no                                     | -      |
| `ENTITLEMENTS_DOMAIN`                | eg `group`                                   |                                                                                                                                                                                                                                    | no                                     | -      |
| `GROUP_ID`                | eg `group`                                   |                                                                                                                                                                                                                                    | no                                     | -      |
| `LEGAL_TAG`                | eg `osdu-demo-legaltag`                                   |                                                                                                                                                                                                                                    | no                                     | -      |
| `OTHER_RELEVANT_DATA_COUNTRIES`                | eg `US`                                   |                                                                                                                                                                                                                                    | no                                     | -      |


Authentication can be provided as OIDC config:

| name                                            | value                                   | description                   | sensitive? | source |
|-------------------------------------------------|-----------------------------------------|-------------------------------|------------|--------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | PRIVILEGED_USER Client Id     | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | PRIVILEGED_USER Client secret | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | OpenID provider url           | yes        | -      |

Or tokens can be used directly from env variables:

| name                    | value      | description           | sensitive? | source |
|-------------------------|------------|-----------------------|------------|--------|
| `PRIVILEGED_USER_TOKEN` | `********` | PRIVILEGED_USER_TOKEN Token | yes        | -      |


**Entitlements configuration for integration accounts**

| PRIVILEGED_USER                                                                                                                                   |
|---------------------------------------------------------------------------------------------------------------------------------------------------|
|  users<br/>service.entitlements.user<br/>service.search.user<br/>data.test1<br/>data.integration.test<br/>users@{tenant1}@{groupId}.com |


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
