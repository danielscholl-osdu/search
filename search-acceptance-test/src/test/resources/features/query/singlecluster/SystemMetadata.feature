Feature: Search with different queries
  To allow a user to exclude indices of the system/meta data from the search results unless the indices (kinds) of the system/meta data are explicitly specified in the search query.

  Background:
    Given the schema is created with the following kind
      | kind                                                                  | schemaFile                |
      | tenant1:normal<timestamp>:test-data--Integration:2.0.0                | metadata_test_records_1   |
      | tenant1:normal<timestamp>:test-data--Integration:2.1.0                | metadata_test_records_2   |
      | .opendes:dot<timestamp>:test-data--Integration:2.0.0                  | metadata_test_records_3   |
      | .opendes:dot<timestamp>:test-data--Integration:2.1.0                  | metadata_test_records_4   |
      | system-meta-data:system<timestamp>:test-data--Integration:2.0.0       | metadata_test_records_5   |
      | system-meta-data:system<timestamp>:test-data--Integration:2.1.0       | metadata_test_records_6   |

  @default
  Scenario Outline: Ingest records for the given kind
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Examples:
      | kind                                                              | recordFile                    | acl                            |
      | "tenant1:normal<timestamp>:test-data--Integration:2.0.0"          | "metadata_test_records_1"     | "data.default.viewers@tenant1" |
      | "tenant1:normal<timestamp>:test-data--Integration:2.1.0"          | "metadata_test_records_2"     | "data.default.viewers@tenant1" |
      | ".opendes:dot<timestamp>:test-data--Integration:2.0.0"            | "metadata_test_records_3"     | "data.default.viewers@tenant1" |
      | ".opendes:dot<timestamp>:test-data--Integration:2.1.0"            | "metadata_test_records_4"     | "data.default.viewers@tenant1" |
      | "system-meta-data:system<timestamp>:test-data--Integration:2.0.0" | "metadata_test_records_5"     | "data.default.viewers@tenant1" |
      | "system-meta-data:system<timestamp>:test-data--Integration:2.1.0" | "metadata_test_records_6"     | "data.default.viewers@tenant1" |

  @default
  Scenario Outline: Search data in a given kind with aggregateBy field
    When I send <query> with <kind>
    And I want to aggregate by <aggregateBy>
    Then I should get <includedKinds> and should not get <excludedKinds> from aggregations
    Examples:
      | kind                                                                                            | query   | aggregateBy | includedKinds                                                                                                   | excludedKinds                                                                                                 |
      | "*:*:*:*"                                                                                       | ""      | "kind"      | "tenant1:normal<timestamp>:test-data--Integration:2.0.0,tenant1:normal<timestamp>:test-data--Integration:2.1.0" | ".opendes:dot<timestamp>:test-data--Integration:2.0.0,system-meta-data:system<timestamp>:test-data--Integration:2.0.0" |
      | "*:*:*:2.0.0"                                                                                   | ""      | "kind"      | "tenant1:normal<timestamp>:test-data--Integration:2.0.0" | ".opendes:dot<timestamp>:test-data--Integration:2.0.0,system-meta-data:system<timestamp>:test-data--Integration:2.0.0,tenant1:normal<timestamp>:test-data--Integration:2.1.0" |
      | "*:*:*:2.0.0,tenant1:normal<timestamp>:test-data--Integration:2.1.0"                            | ""      | "kind"      | "tenant1:normal<timestamp>:test-data--Integration:2.0.0,tenant1:normal<timestamp>:test-data--Integration:2.1.0" | ".opendes:dot<timestamp>:test-data--Integration:2.0.0,system-meta-data:system<timestamp>:test-data--Integration:2.0.0" |
      | ".opendes:dot<timestamp>:test-data--Integration:2.1.0"                                          | ""      | "kind"      | ".opendes:dot<timestamp>:test-data--Integration:2.1.0" | ".opendes:dot<timestamp>:test-data--Integration:2.0.0,system-meta-data:system<timestamp>:test-data--Integration:2.0.0" |
      | "system-meta-data:system<timestamp>:test-data--Integration:2.1.0"                               | ""      | "kind"      | "system-meta-data:system<timestamp>:test-data--Integration:2.1.0" | ".opendes:dot<timestamp>:test-data--Integration:2.0.0,system-meta-data:system<timestamp>:test-data--Integration:2.0.0" |
      | ".opendes:dot<timestamp>:test-data--Integration:2.1.0,tenant1:normal<timestamp>:*:*"            | ""      | "kind"      | ".opendes:dot<timestamp>:test-data--Integration:2.1.0,tenant1:normal<timestamp>:test-data--Integration:2.0.0,tenant1:normal<timestamp>:test-data--Integration:2.1.0" | ".opendes:dot<timestamp>:test-data--Integration:2.0.0,system-meta-data:system<timestamp>:test-data--Integration:2.0.0" |
      | "system-meta-data:system<timestamp>:test-data--Integration:2.1.0,tenant1:normal<timestamp>:*:*" | ""      | "kind"      | "system-meta-data:system<timestamp>:test-data--Integration:2.1.0,tenant1:normal<timestamp>:test-data--Integration:2.0.0,tenant1:normal<timestamp>:test-data--Integration:2.1.0" | ".opendes:dot<timestamp>:test-data--Integration:2.0.0,system-meta-data:system<timestamp>:test-data--Integration:2.0.0" |






