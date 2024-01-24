Feature: Search recursively on cursor with different queries
  To allow a user to find his data quickly, search should offer multiple ways to search data and iterate over all the results.

  Background:
    Given the schema is created with the following kind
      | kind                                           | schemaFile |
      | tenant1:search<timestamp>:test-data--Integration:1.0.1    | records_1  |
      | tenant1:search<timestamp>:test-data2--Integration:1.0.2    | records_2  |

  Scenario Outline: Ingest records for the given kind
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Examples:
      | kind                                          | recordFile  | acl                             |
      | "tenant1:search<timestamp>:test-data--Integration:1.0.1" | "records_1" |  "data.default.viewers@tenant1" |
      | "tenant1:search<timestamp>:test-data2--Integration:1.0.2"  | "records_2" | "data.default.viewers@tenant1"  |

  Scenario Outline: Search recursively page by page data across the kinds
    When I send <query> with <kind>
    And I limit the count of returned results to <limit>
    And I set the fields I want in response as <returned_fields>
    And I send request to tenant <q1_tenant>
    Then I should get in response <first_count> records along with a cursor
    And I send request to tenant <q2_tenant>
    Then I should get in response <final_count> records

    Examples:
      | q1_tenant | q2_tenant | kind                                | query                     | limit | returned_fields | first_count | final_count |
      | "tenant1" | "tenant1" | "tenant1:search<timestamp>:*:*" | None                      | 4     | All             | 4           | 2           |
      | "tenant1" | "tenant1" | "tenant1:search<timestamp>:*:*" | None                      | None  | All             | 6           | 0           |
      | "tenant1" | "tenant1" | "tenant1:search<timestamp>:*:*" | "TX OR TEXAS OR FRANCE"   | 1     | All             | 1           | 1           |
      | "tenant1" | "tenant1" | "tenant1:search<timestamp>:*:*" | "XdQQ6GCSNSBLTESTFAIL"    | 1     | All             | 0           | 0           |
      | "tenant1" | "tenant1" | "tenant1:search<timestamp>:*:*" | "\"OFFICE2\" \| OFFICE3 \| OFFICE5" | 1     | All             | 1           | 1           |

  Scenario Outline: Search recursively page by page data across the kinds with invalid inputs
    When I send <query> with <kind>
    And I limit the count of returned results to <limit>
    And I set an invalid cursor
    And I send request to tenant <tenant>
    Then I should get <response_code> response with reason: <reponse_type>, message: <response_message> and errors: <errors>

    Examples:
      | tenant    | kind                                       | query | limit | response_code | reponse_type                  | response_message                                  | errors                                    |
      | "tenant1" | "tenant1:search<timestamp>:test-data--Integration:1.0.1" | None  | None  | 400           | "Can't find the given cursor" | "The given cursor is invalid or expired"          | ""                                        |
      | "tenant1" | "*:*:*"                                    | None  | 0     | 400           | "Bad Request"                 | "Invalid parameters were given on search request" | "Not a valid record kind format. Found: *:*:*"   |
      | "tenant1" | "tenant1:search<timestamp>:test-data--Integration:1.0.1" | None  | -1    | 400           | "Bad Request"                 | "Invalid parameters were given on search request" | "'limit' must be equal or greater than 0" |

  Scenario Outline:  Search recursively page by page data across the kinds with invalid inputs and headers
    When I send <query> with <kind>
    And I limit the count of returned results to <limit>
    And I set the fields I want in response as <returned_fields>
    And I send request to tenant <q1_tenant>
    Then I should get in response <first_count> records along with a cursor
    And I send request to tenant <q2_tenant>
    Then I should get <response_code> response with reason: <reponse_type>, message: <response_message> and errors: <errors>

    Examples:
      | q1_tenant | q2_tenant | kind                                       | query | limit | returned_fields | first_count | response_code | reponse_type    | response_message                                    | errors |
      | "tenant1" | "tenant2" | "tenant1:search<timestamp>:test-data--Integration:1.0.1" | None  | 1     | All             | 1           | 401           | "Access denied" | "The user is not authorized to perform this action" | ""     |

  Scenario Outline: Search data across the kinds with bounding box inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define bounding box with points (<top_left_latitude>, <top_left_longitude>) and  (<bottom_right_latitude>, <bottom_right_longitude>)
    And I limit the count of returned results to <limit>
    And I send request to tenant <q1_tenant>
    Then I should get in response <first_count> records along with a cursor
    And I send request to tenant <q2_tenant>
    Then I should get in response <final_count> records

    Examples:
      | q1_tenant | q2_tenant | kind                                       | query  | limit | field           | top_left_latitude | top_left_longitude | bottom_right_latitude | bottom_right_longitude | first_count | final_count |
      | "tenant1" | "tenant1" | "tenant1:search<timestamp>:test-data--Integration:1.0.1" | None   | None  | "data.Location" | 45                | -100               | 0                     | 0                      | 2           | 0           |
      | "tenant1" | "tenant1" | "tenant1:search<timestamp>:test-data--Integration:1.0.1" | "data.OriginalOperator:OFFICE4" | 1     | "data.Location" | 45                | -110               | 0                     | 0                      | 1           | 0           |


  Scenario Outline: Search data and sort the results with the given sort fields and order
    When I send <query> with <kind>
    And I want the results sorted by <sort>
    Then I should get records in right order first record id: <first_record_id>, last record id: <last_record_id>
    Examples:
      | kind                                      | query       | sort                                                                         | first_record_id       | last_record_id        |
      | "tenant1:search<timestamp>:*:*"    | None        | {"field":["data.OriginalOperator"],"order":["ASC"]}                          | "tenant1:search<timestamp>:1"   | "tenant1:search<timestamp>:2.0.0:3"   |
      | "tenant1:search<timestamp>:*:*"    | None        | {"field":["id"],"order":["DESC"]}                                            | "tenant1:search<timestamp>:3"   | "tenant1:search<timestamp>:1"   |
      | "tenant1:search<timestamp>:*:*"    | None        | {"field":["namespace","data.Rank"],"order":["ASC","DESC"]}                   | "tenant1:search<timestamp>:3"   | "tenant1:search<timestamp>:2.0.0:1"   |
