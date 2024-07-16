Feature: Search with collaboration header different queries
  To allow a user to find his data quickly, search should offer multiple ways to search data.

  Background:
    Given the schema is created with the following kind
      | kind                                                          | schemaFile |
      | tenant1:search-collab<timestamp>:test-data--Integration:1.0.6 | records_6  |
      | tenant1:search-collab<timestamp>:test-data--Integration:1.0.7 | records_7  |
      | tenant1:search-collab<timestamp>:test-data--Integration:1.0.8 | records_8  |

  @xcollab
  Scenario Outline: Ingest records for the given kind with xcollaboration header
    When I ingest records with the <recordFile> with <acl> for a given <kind> with <xcollaboration> header
    Examples:
      | kind                                                            | recordFile  | acl                            | xcollaboration                                            |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.6" | "records_6" | "data.default.viewers@tenant1" | "id=96d5550e-2b5e-4b84-825c-646339ee5fc7,application=pws" |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.7" | "records_7" | "data.default.viewers@tenant1" | ""                                                        |

  @xcollab
  Scenario Outline: Search data in a given kind with xcollaboration header
    When I send <query> with <kind>
    And I send request with <xcollaboration> header
    Then I should get in response <count> records with <returned_fields>

    Examples:
      | kind                                                            | query | returned_fields | count | xcollaboration                                            |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.6" | None  | All             | 3     | "id=96d5550e-2b5e-4b84-825c-646339ee5fc7,application=pws" |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.6" | None  | All             | 0     | ""                                                        |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.7" | None  | All             | 3     | ""                                                        |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.7" | None  | All             | 0     | "id=96d5550e-2b5e-4b84-825c-646339ee5fc7,application=pws" |

  @xcollab
  Scenario Outline: Ingest records for the given kind with xcollaboration header
    When I ingest records with the <recordFile> with <acl> for a given <kind> with <xcollaboration> header
    Examples:
      | kind                                                            | recordFile  | acl                            | xcollaboration                                            |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.8" | "records_8" | "data.default.viewers@tenant1" | ""                                                        |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.8" | "records_9" | "data.default.viewers@tenant1" | "id=96d5550e-2b5e-4b84-825c-646339ee5fc7,application=pws" |

  @xcollab
  Scenario Outline: Search data in a given kind with xcollaboration header
    When I send <query> with <kind>
    And I send request with <xcollaboration> header
    Then I should get in response <count> records with <returned_fields>

    Examples:
      | kind                                                            | query                  | returned_fields | count | xcollaboration                                            |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.8" | "data.Field:SOR"       | All             | 3     | ""                                                        |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.8" | "data.Field:SOR"       | All             | 0     | "id=96d5550e-2b5e-4b84-825c-646339ee5fc7,application=pws" |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.8" | "data.Field:Namespace" | All             | 0     | ""                                                        |
      | "tenant1:search-collab<timestamp>:test-data--Integration:1.0.8" | "data.Field:Namespace" | All             | 3     | "id=96d5550e-2b5e-4b84-825c-646339ee5fc7,application=pws" |
