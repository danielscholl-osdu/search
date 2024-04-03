# Search Service Audit Logs

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