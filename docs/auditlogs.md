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
# Search service audit logs
This document presents the expected audit logs for each of API in Search service. __One and only one__ audit log is expected per request.
It is important to note audit logs __must not__ include technical information such as response codes, exceptions, stack trace, etc. 

## ``GET /api/search/v2/index/schema/{kind}``

- Action id: ``SE001``
- Action: ``READ``
- Operation: Get index's schema
- Outcome: ``SUCCESS``
- Description: User got index's schema successfully


## ``POST /api/search/v2/query_with_cursor``

- Action id: ``SE002``
- Action: ``READ``
- Operation: Query index with cursor
- Outcome: ``SUCCESS``
- Description: User queried index with cursor successfully

## ``POST /api/search/v2/query``

- Action id: ``SE003``
- Action: ``READ``
- Operation: Query index
- Outcome: ``SUCCESS``
- Description: User queried index successfully

## ``DELETE /api/search/v2/index/{kind}``

- Action id: ``SE004``
- Action: ``DELETE``
- Operation: Delete index
- Outcome: ``SUCCESS``
- Description: User deleted index successfully