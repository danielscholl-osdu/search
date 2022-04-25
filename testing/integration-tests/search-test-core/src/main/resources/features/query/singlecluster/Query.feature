Feature: Search with different queries
  To allow a user to find his data quickly, search should offer multiple ways to search data.

  Background:
    Given the elastic search is initialized with the following data
      | kind                                          | index                                         | mappingFile | recordFile | viewerGroup                  | ownerGroup                      |
      | tenant1:testquery<timestamp>:well:1.0.0       | tenant1-testquery<timestamp>-well-1.0.0       | records_1   | records_1  | data.default.viewers@tenant1 | data.default.owners@tenant1     |
      | tenant1:testquery<timestamp>:well:2.0.0       | tenant1-testquery<timestamp>-well-2.0.0       | records_2   | records_2  | data.default.viewers@tenant1 | data.default.testowners@tenant1 |
      | tenant1:testnestedquery<timestamp>:well:2.0.0 | tenant1-testnestedquery<timestamp>-well-2.0.0 | records_4   | records_4  | data.default.viewers@tenant1 | data.default.testowners@tenant1 |
      | tenant1:testintersectingquery<timestamp>:well:2.0.0 | tenant1-testintersectingquery<timestamp>-well-2.0.0 | records_5   | records_5  | data.default.viewers@tenant1 | data.default.testowners@tenant1 |

  Scenario Outline: Search data in a given kind
    When I send <query> with <kind>
    And I limit the count of returned results to <limit>
    And I set the offset of starting point as <offset>
    And I set the fields I want in response as <returned_fields>
    And I send request to tenant <tenant>
    Then I should get in response <count> records with <returned_fields>

    Examples:
      | tenant    | kind                                      | query                                | limit | offset | returned_fields | count |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4"      | None  | None   | All             | 1     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | None                                 | 0     | None   | NULL            | 3     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:2.0.0" | None                                 | 0     | None   | NULL            | 3     |
      | "tenant1" | ["tenant1:testquery<timestamp>:well:1.0.0", "tenant1:testquery<timestamp>:well:2.0.0"] | None | 0 | None | NULL     | 6     |
      ######################################Range Query test cases##########################################################################
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | "data.Rank:{1 TO 3}"                 | None  | None   | id,index        | 1     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | "data.Rank:[10 TO 20]"               | None  | None   | All             | 1     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | "data.Rank:>=2"                      | None  | None   | All             | 2     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | "data.Established:{* TO 2012-01-01}" | None  | None   | All             | 2     |
      ######################################Text Query test cases###########################################################################
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | "OSDU"                               | None  | None   | All             | 3     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:2.0.0" | "data.OriginalOperator:OFFICE6"      | None  | None   | All             | 1     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0" | ""OFFICE2" \| OFFICE3"               | None  | None   | All             | 1     |
      | "tenant1" | "tenant1:testquery<timestamp>:well:2.0.0" | "data.Well\*:(Data Lake Cloud)"      | None  | None   | All             | 3     |

  Scenario Outline: Search data in a given a kind with invalid inputs
    When I send <query> with <kind>
    And I limit the count of returned results to <limit>
    And I set the offset of starting point as <offset>
    And I send request to tenant <tenant>
    Then I should get <response_code> response with reason: <reponse_type>, message: <response_message> and errors: <errors>

    Examples:
      | tenant    | kind                                            | query                                                             | limit | offset | response_code | reponse_type    | response_message                                                                                 | errors                                           |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0"       | None                                                              | -1    | None   | 400           | "Bad Request"   | "Invalid parameters were given on search request"                                                | "'limit' must be equal or greater than 0"        |
      | "tenant1" | "invalid"                                       | None                                                              | 1     | None   | 400           | "Bad Request"   | "Invalid parameters were given on search request"                                                | "Not a valid record kind format. Found: invalid" |
      | "tenant1" | 123456789                                       | None                                                              | 1     | None   | 400           | "Bad Request"   | "Invalid parameters were given on search request"                                                | "Not a valid record kind type. Found: 123456789" |
      | "tenant1" | []                                              | None                                                              | 1     | None   | 400           | "Bad Request"   | "Invalid parameters were given on search request"                                                | "Record kind can't be null or empty. Found: []"  |
      | "tenant1" | "tenant1:testquery<timestamp>:well:1.0.0"       | None                                                              | 1     | -1     | 400           | "Bad Request"   | "Invalid parameters were given on search request"                                                | "'offset' must be equal or greater than 0"       |
      | "tenant2" | "tenant1:testquery<timestamp>:well:1.0.0"       | None                                                              | None  | None   | 401           | "Access denied" | "The user is not authorized to perform this action"                                              | ""                                               |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "nested(data.FacilityEvents, (EffectiveDateTime:[2019 TO 2026]))" | None  | None   | 400           | "Bad Request"   | "failed to create query: [nested] failed to find nested object under path [data.FacilityEvents]" | ""                                               |

  Scenario Outline: Search data across the kinds with bounding box inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define bounding box with points (<top_left_latitude>, <top_left_longitude>) and  (<bottom_right_latitude>, <bottom_right_longitude>)
    Then I should get in response <count> records

    Examples:
      | kind                                      | query                           | field                   | top_left_latitude | top_left_longitude | bottom_right_latitude | bottom_right_longitude | count |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None                            | "data.Location"         | 45                | -100               | 0                     | 0                      | 2     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None                            | "data.Location"         | 45                | -80                | 0                     | 0                      | 0     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location"         | 45                | -100               | 0                     | 0                      | 1     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location"         | 10                | -100               | 0                     | 0                      | 0     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None                            | "data.LocationGeoShape" | 45                | -100               | 0                     | 0                      | 2     |

  Scenario Outline: Search data across the kinds with invalid bounding box inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define bounding box with points (<top_left_latitude>, <top_left_longitude>) and  (<bottom_right_latitude>, <bottom_right_longitude>)
    Then I should get <response_code> response with reason: <reponse_type>, message: <response_message> and errors: <errors>

    Examples:
      | kind                                      | query                           | field           | top_left_latitude | top_left_longitude | bottom_right_latitude | bottom_right_longitude | response_code | reponse_type  | response_message                                  | errors                                                                   |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location" | 0                 | 0                  | 0                     | 0                      | 400           | "Bad Request" | "Invalid parameters were given on search request" | "top latitude cannot be the same as bottom latitude: 0.0 == 0.0"         |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location" | 0                 | -100               | -10                   | -100                   | 400           | "Bad Request" | "Invalid parameters were given on search request" | "left longitude cannot be the same as right longitude: -100.0 == -100.0" |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location" | 10                | -100               | 10                    | 0                      | 400           | "Bad Request" | "Invalid parameters were given on search request" | "top latitude cannot be the same as bottom latitude: 10.0 == 10.0"       |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location" | 45                | -100               | -95                   | 0                      | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'latitude' value is out of the range [-90, 90]"                         |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location" | 0                 | -100               | 10                    | 0                      | 400           | "Bad Request" | "Invalid parameters were given on search request" | "top corner is below bottom corner: 0.0 vs. 10.0"                        |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "data.OriginalOperator:OFFICE4" | "data.Location" | None              | None               | 0                     | 0                      | 400           | "Bad Request" | "Invalid parameters were given on search request" | "Invalid payload"                                                        |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None                            | "officeAddress" | 45                | -100               | 0                     | 0                      | 400           | "Bad Request" | "failed to find geo field [officeAddress]"        | ""                                                                       |

  Scenario Outline: Search data across the kinds with distance inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define focus coordinates as (<latitude>, <longitude>) and search in a <distance> radius
    Then I should get in response <count> records

    Examples:
      | kind                                      | query               | field           | latitude | longitude | distance | count |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "Under development" | "data.Location" | 0        | 0         | 20000000 | 3     |
      | "tenant1:testquery<timestamp>:*:*"        | "TEXAS OR TX"       | "data.Location" | 45       | -100      | 20000000 | 2     |

  Scenario Outline: Search data across the kinds with invalid distance inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define focus coordinates as (<latitude>, <longitude>) and search in a <distance> radius
    Then I should get <response_code> response with reason: <reponse_type>, message: <response_message> and errors: <errors>

    Examples:
      | kind                               | query          | field           | latitude | longitude | distance | response_code | reponse_type  | response_message                                  | errors                                              |
      | "tenant1:testquery<timestamp>:*:*" | "OFFICE - 2"   | "data.Location" | -45      | -200      | 1000     | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'longitude' value is out of the range [-180, 180]" |
      | "tenant1:testquery<timestamp>:*:*" | "TEXAS OR USA" | "data.Location" | -95      | -100      | 1000     | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'latitude' value is out of the range [-90, 90]"    |
      | "tenant1:testquery<timestamp>:*:*" | "Harris"       | "ZipCode"       | -45      | -200      | 1000     | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'longitude' value is out of the range [-180, 180]" |

  Scenario Outline: Search data across the kinds
    When I send <query> with <kind>
    And I limit the count of returned results to <limit>
    And I set the offset of starting point as <offset>
    And I set the fields I want in response as <returned_fields>
    And I send request to tenant <tenant>
    Then I should get in response <count> records

    Examples:
      | tenant    | kind                               | query                                      | limit | offset | returned_fields | count |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | None                                       | 1     | None   | All             | 1     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | None                                       | None  | 2      | All             | 4     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | None                                       | None  | None   | Country         | 6     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "OSDU OFFICE*"                             | None  | None   | All             | 6     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "SCHLUM OFFICE"                            | None  | None   | All             | 6     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | ""SCHLUM OFFICE""                          | None  | None   | All             | 0     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "data.Country:USA"                         | None  | None   | All             | 2     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "TEXAS AND OFFICE3"                        | None  | None   | All             | 1     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "data.OriginalOperator:OFFICE5 OR OFFICE2" | None  | None   | All             | 2     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "data.OriginalOperator:STI OR HT"          | None  | None   | All             | 0     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "_exists_:data.Basin"                      | None  | None   | All             | 4     |
      | "tenant1" | "tenant1:testquery<timestamp>:*:*" | "data.Well\*:"Data Lake Cloud""            | None  | None   | All             | 5     |


  Scenario Outline: Search data across the kinds with bounding box inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define bounding box with points (<top_left_latitude>, <top_left_longitude>) and  (<bottom_right_latitude>, <bottom_right_longitude>)
    Then I should get in response <count> records

    Examples:
      | kind                               | query | field           | top_left_latitude | top_left_longitude | bottom_right_latitude | bottom_right_longitude | count |
      | "tenant1:testquery<timestamp>:*:*" | None  | "data.Location" | 45                | -100               | 0                     | 0                      | 3     |
      | "tenant1:testquery<timestamp>:*:*" | None  | "data.Location" | 10                | -100               | 0                     | 0                      | 0     |

  Scenario Outline: Search data across the kinds with geo polygon inputs
    When I send <query> with <kind>
    And define geo polygon with following points <points_list>
    And I apply geographical query on field <field>
    Then I should get in response <count> records
    Examples:
      | kind                                      | query     | field                   | points_list                                                                                                        | count |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None      | "data.Location"         | (26.12362;-112.226716)  , (26.595873;-68.457186) , (52.273184;-93.593904)                                          | 2     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None      | "data.Location"         | (33.201112;-113.282863) , (33.456305;-98.269744) , (52.273184;-93.593904)                                          | 0     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | "OFFICE4" | "data.Location"         | (26.12362;-112.226716)  , (26.595873;-68.457186) , (52.273184;-93.593904)                                          | 1     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None      | "data.Location"         | (14.29056;72.18936)     , (22.13762;72.18936)    , (22.13762;77.18936) , (14.29056;77.18936) , (14.29056;72.18936) | 1     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None      | "data.LocationGeoShape" | (14.29056;72.18936)     , (22.13762;72.18936)    , (22.13762;77.18936) , (14.29056;77.18936) , (14.29056;72.18936) | 1     |

  Scenario Outline: Search data across the kinds with invalid geo polygon inputs
    When I send <query> with <kind>
    And define geo polygon with following points <points_list>
    And I apply geographical query on field <field>
    Then I should get <response_code> response with reason: <response_type>, message: <response_message> and errors: <errors>

    Examples:
      | kind                                      | query | field           | points_list                                                                | response_code | response_type | response_message                                  | errors                                           |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None  | "data.Location" | (26.595873;-68.457186)   , (52.273184;-93.593904)                          | 400           | "Bad Request" | "Invalid parameters were given on search request" | "too few points defined for geo polygon query"   |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None  | "data.Location" | (516.595873;-68.457186)  , (52.273184;-94.593904) , (95.273184;-93.593904) | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'latitude' value is out of the range [-90, 90]" |

  Scenario Outline: Search data and sort the results with the given sort fields and order
    When I send <query> with <kind>
    And I want the results sorted by <sort>
    Then I should get records in right order first record id: <first_record_id>, last record id: <last_record_id>
    Examples:
      | kind                                      | query       | sort                                                                         | first_record_id       | last_record_id        |
      | "tenant1:testquery<timestamp>:well:*"     | None        | {"field":["data.OriginalOperator","data.WellType"],"order":["ASC", "ASC"]}   | "test:well:1.0.0:1"   | "test:well:2.0.0:3"   |
      | "tenant1:testquery<timestamp>:well:*"     | None        | {"field":["id"],"order":["DESC"]}                                            | "test:well:2.0.0:3"   | "test:well:1.0.0:1"   |
      | "tenant1:testquery<timestamp>:well:*"     | None        | {"field":["namespace","data.Rank"],"order":["ASC","DESC"]}                   | "test:well:1.0.0:3"   | "test:well:2.0.0:1"   |
      | "tenant1:testnestedquery<timestamp>:well:2.0.0" | None  | {"field":["nested(data.VerticalMeasurements, VerticalMeasurement, min)"],"order":["ASC"]} | "test:well:1.0.0:2" | "test:well:1.0.0:1" |
      | "tenant1:testnestedquery<timestamp>:well:2.0.0" | None  | {"field":["nested(data.FacilityOperators, TerminationDateTime, min)"],"order":["DESC"]}   | "test:well:1.0.0:2" | "test:well:1.0.0:1" |

  Scenario Outline: Search data in a given kind with invalid sort field
    When I send <query> with <kind>
    And I want the results sorted by <sort>
    Then I should get <response_code> response with reason: <response_type>, message: <response_message> and errors: <errors>

    Examples:
      | kind                                  | query | sort                                          | response_code | response_type | response_message                                  | errors                                                            |
      | "tenant1:testquery<timestamp>:well:*" | None  | {"field":[],"order":["ASC"]}                  | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'sort.field' can not be null or empty"                           |
      | "tenant1:testquery<timestamp>:well:*" | None  | {"field":["id"],"order":[]}                   | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'sort.order' can not be null or empty"                           |
      | "tenant1:testquery<timestamp>:well:*" | None  | {"field":["id","data.Rank"],"order":["DESC"]} | 400           | "Bad Request" | "Invalid parameters were given on search request" | "'sort.field' and 'sort.order' size do not match"                 |
      | "tenant1:testquery<timestamp>:well:*" | None  | {"field":["id"],"order":[null]}               | 400           | "Bad Request" | "Invalid parameters were given on search request" | "Not a valid order option. It can only be either 'ASC' or 'DESC'" |

  Scenario Outline: Search data in a given kind with different searchAs modes
    When I send <query> with <kind>
    And I want to search as owner <is_owner>
    Then I should get in response <count> records

    Examples:
      | kind                                      | query     | is_owner | count |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None      | true     | 3     |
      | "tenant1:testquery<timestamp>:well:1.0.0" | None      | false    | 3     |
      | "tenant1:testquery<timestamp>:well:2.0.0" | None      | true     | 0     |
      | "tenant1:testquery<timestamp>:well:2.0.0" | None      | false    | 3     |
      | "tenant1:testquery<timestamp>:well:*"     | None      | false    | 6     |
      | "tenant1:testquery<timestamp>:well:*"     | None      | true     | 3     |
      | "tenant1:testquery<timestamp>:well:*"     | "OFFICE4" | true     | 1     |
      | "tenant1:testquery<timestamp>:well:*"     | None      | None     | 6     |

  Scenario Outline: Search data in a given kind with aggregateBy field
    When I send <query> with <kind>
    And I want to aggregate by <aggregateBy>
    Then I should get <count> unique values

    Examples:
      | kind                                            | query                                                          | aggregateBy                                              | count |
      | "tenant1:testquery<timestamp>:well:1.0.0"       | None                                                           | "namespace"                                              | 1     |
      | "tenant1:testquery<timestamp>:well:1.0.0"       | None                                                           | "type"                                                   | 1     |
      | "tenant1:testquery<timestamp>:well:1.0.0"       | "OFFICE4"                                                      | "data.Rank"                                              | 1     |
      | "tenant1:testquery<timestamp>:well:1.0.0"       | None                                                           | "data.Rank"                                              | 3     |
      | "tenant1:testnestedquery<timestamp>:well:2.0.0" | None                                                           | "nested(data.VerticalMeasurements, VerticalMeasurement)" | 2     |
      | "tenant1:testnestedquery<timestamp>:well:2.0.0" | nested(data.VerticalMeasurements, (VerticalMeasurement:(<15))) | "nested(data.VerticalMeasurements, VerticalMeasurement)" | 1     |

  Scenario Outline: Search data in a given kind with nested queries
    When I send <query> with <kind>
    And I send request to tenant <tenant>
    Then I should get in response <count> records

    Examples:
      | tenant    | kind                                            | query                                                                                                                                                                          | count |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "nested(data.VerticalMeasurements, (VerticalMeasurement:(>15.0) AND EffectiveDateTime:[2010-02-13T09:13:15.55+0000 TO 2021-02-13T09:13:15.55+0000]))"                          | 1     |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "nested(data.VerticalMeasurements, (VerticalMeasurement:(>15.0) OR EffectiveDateTime:[2010-02-13T09:13:15.55+0000 TO 2021-02-13T09:13:15.55+0000]))"                           | 2     |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "data.Source:"Example*" AND nested(data.VerticalMeasurements, (VerticalMeasurementDescription:"Example*"))"                                                                    | 2     |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "data.Source:"Example*" AND nested(data.VerticalMeasurements, (VerticalMeasurementDescription:"Example*")) AND data.FacilityName:"NOT EXISTING NAME""                          | 0     |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "nested(data.VerticalMeasurements, (VerticalMeasurement:(<15)))"                                                                                                               | 1     |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "nested(data.FacilityOperators, (TerminationDateTime:[2023 TO 2026] AND EffectiveDateTime:[* TO 2021])) NOT nested(data.VerticalMeasurements, (VerticalMeasurement:(>20000)))" | 1     |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "nested(data.FacilityOperators, (TerminationDateTime:[2023 TO 2026])) OR nested(data.VerticalMeasurements, (VerticalMeasurement:(>15)))"                                       | 2     |
      | "tenant1" | "tenant1:testnestedquery<timestamp>:well:2.0.0" | "nested(data.VerticalMeasurements, (VerticalMeasurementID:"Other*" AND VerticalMeasurement:(<30)))"                                                                          | 1     |

  Scenario Outline: Search data across the kinds with intersecting spatial filter inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define intersection polygon with points (<latitude1>, <longitude1>) and (<latitude2>, <longitude2>) and (<latitude3>, <longitude3>) and (<latitude4>, <longitude4>) and (<latitude5>, <longitude5>)
    Then I should get in response <count> records

    Examples:
      | kind                                                  | query | field                   | latitude1 | longitude1 | latitude2 | longitude2 | latitude3 | longitude3 | latitude4 | longitude4 | latitude5 | longitude5 | count |
      | "tenant1:testintersectingquery<timestamp>:well:2.0.0" | None  | "data.Wgs84Coordinates" | 10.75     | -8.60      | 10.73     | -2.47      | 1.01      | -2.47      | 1.01      | -8.60      | 10.75     | -8.60      | 1     |
      | "tenant1:testintersectingquery<timestamp>:well:2.0.0" | None  | "data.Wgs84Coordinates" | 80        | 0          | 80        | 1          | 90        | 1          | 90        | 0          | 80        | 0          | 0     |

  Scenario Outline: Search data across the kinds with intersecting coordinate inputs
    When I send <query> with <kind>
    And I apply geographical query on field <field>
    And define within polygon with points (<latitude1>, <longitude1>)
    Then I should get in response <count> records

    Examples:
      | kind                                                  | query | field                   | latitude1 | longitude1 | count |
      | "tenant1:testintersectingquery<timestamp>:well:2.0.0" | None  | "data.Wgs84Coordinates" | 10.75     | -8.60      | 1     |
      | "tenant1:testintersectingquery<timestamp>:well:2.0.0" | None  | "data.Wgs84Coordinates" | 80        | 0          | 0     |