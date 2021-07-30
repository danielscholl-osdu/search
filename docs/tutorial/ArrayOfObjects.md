## Searching by an array of objects

### Table of contents <a name="TOC"></a>
- [Introduction](#introduction)
- [Schemas](#schemas)
    * [Indexing hints](#indexing-hints)
      + [Default](#default-hint)
      + [Flattened](#flattened-hint)
      + [Nested](#nested-hint)
- [Query](#query)
    * [Default](#default-query)
    * [Flattened](#flattened-query)
    * [Nested](#nested-query)
      + [Examples](#nested-query-examples)
        - [Range query](#range-query-examples)
        - [Text query](#text-query-examples)
        - [Combination with non-nested queries](#combination-query-examples)
        - [Multilevel-Nested query](#multilevel-query-examples)
- [Sort](#sort)
    * [Default](#default-sort)
    * [Flattened](#flattened-sort)
    * [Nested](#nested-sort)
- [Aggregation](#aggregation)
    * [Default](#default-aggregation)
    * [Flattened](#flattened-aggregation)
    * [Nested](#nested-aggregation)
- [Make an array of objects searchable ](#update-schema)

## Introduction <a name="introduction"></a>
Since 0.9.0 version the Search API provides a mechanism to search data structured in an array of objects. Search capabilities directly related to schema definitions,
this tutorial describes how you can define an array of objects in schemas and how search should be performed for each definition. 

## Schemas <a name="schemas"></a>
Since 0.8.0 version The indexer service supports special indexing hints in schemas that allow you to define different elasticsearch mapping types for an array of objects.

### Indexing hints <a name="indexing-hints"></a> 
In this section, we will take a closer look at each type of array, how it can be defined in the schema, and how it will be represented in elasticsearch. 
Special property `x-osdu-indexing` with values `nested` or ` flattened` used to define an array of objects representation in elasticsearch:
```json
"x-osdu-indexing": {
    "type": "nested"
}
```
```json
"x-osdu-indexing": {
    "type": "flattened"
}
```
For example, we will take an artificial schema that is not in the platform, because it is small and contains every type of hint, schemas can be queried with the following request: 

```
curl --location --request GET 'https://<SCHEMA-API>/api/schema-service/v1/schema/<TENANT>:wks:ArraysOfObjectsTestCollection:5.0.0' \
--header 'Data-Partition-Id: <TENANT>' \
--header 'Authorization: <TOKEN>'
```


```json
{
    "x-osdu-inheriting-from-kind": [],
    "x-osdu-license": "Copyright 2021, The Open Group \\nLicensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 . Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "description": "Array of objects testing schema",
    "title": "Test",
    "type": "object",
    "required": [
        "kind",
        "acl",
        "legal"
    ],
    "properties": {
        "data": {
            "allOf": [
                {
                    "type": "object",
                    "properties": {
                        "NestedTest": {
                            "x-osdu-indexing": {
                                "type": "nested"
                            },
                            "description": "nested type test",
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "NumberTest": {
                                        "description": "number test",
                                        "type": "number"
                                    },
                                    "DateTimeTest": {
                                        "format": "date-time",
                                        "description": "date and time test",
                                        "x-osdu-frame-of-reference": "DateTime",
                                        "type": "string"
                                    },
                                    "StringTest": {
                                        "description": "string test",
                                        "type": "string"
                                    },
                                  "NestedInnerTest": {
                                        "x-osdu-indexing": {
                                            "type": "nested"
                                        },
                                        "description": "nested type test",
                                        "type": "array",
                                        "items": {
                                            "type": "object",
                                            "properties": {
                                                "DateTimeInnerTest": {
                                                    "format": "date-time",
                                                    "description": "date and time test",
                                                    "x-osdu-frame-of-reference": "DateTime",
                                                    "type": "string"
                                                },
                                                "StringInnerTest": {
                                                    "description": "string test",
                                                    "type": "string"
                                                },
                                                "NumberInnerTest": {
                                                    "description": "number test",
                                                    "type": "number"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        "FlattenedTest": {
                            "x-osdu-indexing": {
                                "type": "flattened"
                            },
                            "description": "flattened type test",
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "NumberTest": {
                                        "description": "number test",
                                        "type": "number"
                                    },
                                    "DateTimeTest": {
                                        "format": "date-time",
                                        "description": "date and time test",
                                        "x-osdu-frame-of-reference": "DateTime",
                                        "type": "string"
                                    },
                                    "StringTest": {
                                        "description": "string test",
                                        "type": "string"
                                    }
                                }
                            }
                        },
                        "ObjectTest": {
                            "description": "default object type test",
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "NumberTest": {
                                        "description": "number test",
                                        "type": "number"
                                    },
                                    "DateTimeTest": {
                                        "format": "date-time",
                                        "description": "date and time test",
                                        "x-osdu-frame-of-reference": "DateTime",
                                        "type": "string"
                                    },
                                    "StringTest": {
                                        "description": "string test",
                                        "type": "string"
                                    }
                                }
                            }
                        }
                    }
                }
            ]
        }
    }
}
```

#### Default <a name="default-hint"></a>
By default, if there are no hints in the schema, such an array will show up as an `object` type in elasticsearch without any analysis.
More about request options [Default query](#default-query)
```json
"ObjectTest": {
       "description": "default object type test",
       "type": "array",
       "items": {
           "type": "object",
           "properties": {
               "NumberTest": {
                   "description": "number test",
                   "type": "number"
               },
               "DateTimeTest": {
                   "format": "date-time",
                   "description": "date and time test",
                   "x-osdu-frame-of-reference": "DateTime",
                   "type": "string"
               },
               "StringTest": {
                   "description": "string test",
                   "type": "string"
               }
           }
       }
   }
```

There will be no inner properties details in mapping, search capabilities of an object will not be possible.
In elasticsearch this will look like:
```json
{
    "<TENANT>-wks-arraysofobjectstestcollection-5.0.0": {
        "aliases": {},
        "mappings": {
            "properties": {
                        .....
                        "ObjectTest": {
                            "type": "object"
                        }
                    }
                },
```

#### Flattened <a name="flattened-hint"></a>

If the default behavior doesn't suit your needs, and you need to do searches, but you don't want to overcomplicate, `flattened` hint in schema may help.
As you can see in this schema `FlattenedTest` array have `x-osdu-indexing` hint with `"type": "flattened"`.
More about request options [Flattened queries](#flattened-query)

```json
"FlattenedTest": {
    "x-osdu-indexing": {
        "type": "flattened"
    },
    "description": "flattened type test",
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "NumberTest": {
                "description": "number test",
                "type": "number"
            },
            "DateTimeTest": {
                "format": "date-time",
                "description": "date and time test",
                "x-osdu-frame-of-reference": "DateTime",
                "type": "string"
            },
            "StringTest": {
                "description": "string test",
                "type": "string"
            }
        }
    }
}
```

There will be no inner properties details in mapping, but unlike default(object) type searching by inner properties will be possible.
More details about `flattened` type can be found at [Flattened type](https://www.elastic.co/guide/en/elasticsearch/reference/master/flattened.html).
In elasticsearch this will look like:
```json
{
    "<TENANT>-wks-arraysofobjectstestcollection-5.0.0": {
        "aliases": {},
        "mappings": {
            "dynamic": "false",
            "properties": {
                        .....
                        "FlattenedTest": {
                            "type": "flattened"
                        }
                    }
                },
```

#### Nested <a name="nested-hint"></a>
If you need maximum search capabilities a `nested` can suit your needs, this allows not only searching by properties of objects, but also taking advantage of their types.  
Since the `nested` type treats all objects in the array as separate objects, it provides capabilities such as: 
1. Search the array for a specific object that fully matches the search query.
2. Use ranged queries for `number` and `date` properties.
3. Use wildcard requests for `text` field types.
4. Use multileveled nesting (like in example, array of objects with `nested` hint can contain inner `nested` array of objects) 

More about request options [Nested queries](#nested-query)

```json
"NestedTest": {
    "x-osdu-indexing": {
        "type": "nested"
    },
    "description": "nested type test",
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "NumberTest": {
                "description": "number test",
                "type": "number"
            },
            "DateTimeTest": {
                "format": "date-time",
                "description": "date and time test",
                "x-osdu-frame-of-reference": "DateTime",
                "type": "string"
            },
            "StringTest": {
                "description": "string test",
                "type": "string"
            },
            "NestedInnerTest": {
                "x-osdu-indexing": {
                    "type": "nested"
                },
                "description": "nested type test",
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "DateTimeInnerTest": {
                            "format": "date-time",
                            "description": "date and time test",
                            "x-osdu-frame-of-reference": "DateTime",
                            "type": "string"
                        },
                        "StringInnerTest": {
                            "description": "string test",
                            "type": "string"
                        },
                        "NumberInnerTest": {
                            "description": "number test",
                            "type": "number"
                        }
                    }
                }
            }
        }
    }
},
```

In elasticsearch this will look like :

```json
    "<TENANT>-wks-arraysofobjectstestcollection-5.0.0": {
        "aliases": {},
        "mappings": {
            "dynamic": "false",
            "properties": {
                        .....
                         "NestedTest": {
                             "type": "nested",
                             "properties": {
                                 "DateTimeTest": {
                                     "type": "date"
                                 },
                                 "NumberTest": {
                                     "type": "double"
                                 },
                                 "StringTest": {
                                     "type": "text"
                                 },
                                 "NestedInnerTest": {
                                      "type": "nested",
                                      "properties": {
                                          "DateTimeInnerTest": {
                                              "type": "date"
                                          },
                                          "NumberInnerTest": {
                                              "type": "double"
                                          },
                                          "StringInnerTest": {
                                              "type": "text"
                                          }
                                      }
                                 }
                             }
                         },
                    }
                },
```

## Queries <a name="query"></a>

### Default <a name="default-query"></a>
Search queries for properties of `ObjectTest` cannot be executed, but it will return in response to other requests:

```
curl --location --request POST 'https://<SEARCH-URL>/api/search/v2/query' \
--header 'data-partition-id: <TENANT>' \
--header 'Content-Type: application/json' \
--header 'Authorization: <TOKEN>' \
--data-raw '{
    "kind": "odesprod:wks:ArraysOfObjectsTestCollection:5.0.0"
}'
```

Response will be:
```json
{
    "results": [
        {
            "data": {
                ...
                "ObjectTest": [
                    {
                        "NumberTest": "12345",
                        "DateTimeTest": "2020-02-13T09:13:15.55Z",
                        "StringTest": "test string"
                    },
                    {
                        "NumberTest": "567890",
                        "DateTimeTest": "2020-02-13T09:13:15.55Z",
                        "StringTest": "test string"
                    }
                ]
            },
            "kind": "odesprod:wks:ArraysOfObjectsTestCollection:5.0.0",
            "namespace": "odesprod:wks",
            ...
    "totalCount": 1
}
```

### Flattened queries <a name="flattened-query"></a>
Flattened type has its pros and cons, is easy to use for the end user, and the query syntax remains the same, but all properties in this type are "flattened", which means they are not treated as separate objects.
Each property becomes a "leaf" of its parent index, and their types are ignored, properties treated as [keywords](https://www.elastic.co/guide/en/elasticsearch/reference/current/keyword.html).
In short, this means that the benefits of the type cannot be used:
1. It's not possible to use range query like `(>12345)` or `[0 TO 100]` or `[20100101 TO 20141231]` for number or date types. 
2. It's not possible to use wildcard queries, such request won't find records with `StringTest` properties with value `test string`:
```json
{
    "query":"data.FlattenedTest.StringTest:\"test*\""
}
```
3. There is no way to search for a specific object in an array:
```json
                "FlattenedTest": [
                    {
                        "NumberTest": "1",
                        "DateTimeTest": "2020-02-13T09:13:15.55Z",
                        "StringTest": "first string"
                    },
                    {
                        "NumberTest": "100",
                        "DateTimeTest": "2020-02-13T09:13:15.55Z",
                        "StringTest": "second string"
                    }
                ],
```
Query like: 
```json
{
    "query":"data.FlattenedTest.NumberTest:\"1\" AND data.FlattenedTest.StringTest:\"second string\""
}
```
Will return such records, although `NumberTest` and `StringTest` doesn't relate to 1 specific object.

### Nested queries <a name="nested-query"></a>
Nested with advantages brings complexity, it has a serious impact on performance and produce separate documents e.g. 100 WellLog records with 100 Curves each will produce 100x100 documents under Elastic index.
The nested query require the user to know the properties types, path, etc. due to their syntax.

#### Nested query examples <a name="nested-query-examples"></a>

The nested query has a special syntax, it must start with the key `nested`, then the `path` and the search property must be specified: 
```json
nested(<path>, (<property>:<value>))
```
Brackets, commas, spaces are required. 

##### Range query <a name="range-query-examples"></a>
```json
{
    "query":"nested(data.NestedTest, (NumberTest:(>1)))"
}
```
```json
{
    "query":"nested(data.NestedTest, (NumberTest:[1 TO 20000]))"
}
```
```json
{
    "query":"nested(data.NestedTest, (DateTimeTest:(>2019)))"
}
```
##### Text query <a name="text-query-examples"></a>
```json
{
    "query":"nested(data.NestedTest, (StringTest:(test*)))"
}
```
```json
{
    "query":"nested(data.NestedTest, (StringTest:\"test*\"))"
}
```
##### Combination with non-nested queries <a name="combination-query-examples"></a>
Such combination will find record with specific objects in the array:
```json
{
    "query":"nested(data.NestedTest, (NumberTest:[1 TO 20000] AND StringTest:\"test*\"))"
}
```
Nested can be combined with non-nested values in any order with operators `AND` `OR` `NOT`, combinations are limited only by the size of the request body: 
```json
{
    "query":"nested(data.NestedTest, (NumberTest:[1 TO 20000])) AND data.FlattenedTest.StringTest:\"test string\""
}
```
```json
{
    "query": "data.FlattenedTest.StringTest:\"test string\" OR nested(data.NestedTest, (NumberTest:[1 TO 20000]))"
}
```
```json
{
    "query":"nested(data.NestedTest, (NumberTest:[1 TO 20000])) NOT data.FlattenedTest.StringTest:\"test string\""
}
```
```
{
   "query": <nested> AND <not-nested> OR <nested> NOT <not-nested>....etc
}
```
##### Multilevel-Nested query <a name="multilevel-query-examples"></a>
In example [schema](#indexing-hints) you may see that `NestedTest` contains another nested array of objects, those inner objects also can be found via search query:
```json
{
    "query":"nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, (DateTimeInnerTest:(>2024) AND NumberInnerTest:(>14))))"
}
```
Query must be in same format:
```
nested(<root-path>, nested(<inner-path>, (<inner-property>:<value>)))
```
Such query can also be combined with non-nested queries, other nested queries, etc. 

### Sort <a name="sort"></a>

#### Default <a name="default-sort"></a>
Sorting by default array type is not possible, because elasticsearch is unaware about inner objects values.

#### Flattened <a name="flattened-sort"></a>
Flattened type can be used for sorting, since the elasticsearch `keyword` type is suitable for it, the syntax remains the same as for non-array properties :
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "sort": {
        "field": [
             "data.FlattenedTest.NumberTest"
        ],
        "order": [
            "DESC"
        ]
    }
}
```
More info at [fielddata](https://www.elastic.co/guide/en/elasticsearch/reference/7.8/fielddata.html)

#### Nested <a name="nested-sort"></a>
Sorting by nested type is possible, but special syntax is required, similar to the syntax of nested queries:<br/>
```nested(path, field, mode)```<br/>
It start with the keyword `nested` followed by `path` and `field`, `mode` attribute used to select which value of the nested field elasticsearch should sort by, possible values are:<br/>
`min` - sort by minimum value in the array .<br/>
`max` - sort by maximum value in the array.<br/>
`avg` - sort by average value in the array.<br/>
Examples:
1. Sorting by 1 nested property
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "sort": {
        "field": [
             "nested(data.NestedTest, NumberTest, min)"
        ],
        "order": [
            "ASC"
        ]
    }
}
```
2. Sorting by multilevel nested property
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "sort": {
        "field": [
             "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, DateTimeInnerTest, max))"
        ],
        "order": [
            "DESC"
        ]
    }
}
```
3. Sorting combination
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "sort": {
        "field": [
             "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, DateTimeInnerTest, max))",
             "nested(data.NestedTest, NumberTest, min)"
        ],
        "order": [
            "ASC","DESC"
        ]
    }
}
```
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "sort": {
        "field": [
            "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, DateTimeInnerTest, max))",
            "data.FlattenedTest.NumberTest"
        ],
        "order": [
            "ASC",
            "DESC"
        ]
    }
}
```
More info at [nested-sorting](https://www.elastic.co/guide/en/elasticsearch/reference/7.8/sort-search-results.html#nested-sorting)

### Aggregation <a name="aggregation"></a>

#### Default <a name="default-aggregation"></a>
Aggregation by default array type is not possible, because elasticsearch is unaware about inner objects values.

#### Flattened <a name="flattened-aggregation"></a>
Flattened type can be used for aggregation, the syntax remains the same:
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "aggregateBy": "data.FlattenedTest.NumberTest"
}
```
Result:
```json
    ...
    "aggregations": [
        {
            "key": "12",
            "count": 2
        },
        {
            "key": "20",
            "count": 2
        }
    ...
    ],
```

#### Nested <a name="nested-aggregation"></a>
Aggregation by nested array type possible, but a special syntax is required:
```
nested(path, field)
```
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "aggregateBy": "nested(data.NestedTest, NumberTest)"
}
```
Result:
```json
    ...
    "aggregations": [
        {
            "key": "1.0",
            "count": 2
        },
        {
            "key": "2.0",
            "count": 2
        },
        ...
    ],
```
Multilevel nested aggregation is also supported:
```json
{
    "kind": "{{data-partition-id}}:wks:ArraysOfObjectsTestCollection:5.0.0",
    "aggregateBy": "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, NumberInnerTest))"
}
```
### Make an array of objects searchable  <a name="update-schema"></a>

It is possible that after defining the schema and loading the records, you realize that some array of object properties was introduced without the necessary hints in the schemas. 
If they should be searchable but are currently not, this can be fixed in a few steps:
 
1. Update related schema with the Schema service with hints that suit your needs, as you can see we have added `x-osdu-indexing` hint to the `ObjectTest` array:
```
curl --location --request PUT 'https://<SCHEMA_URL>/api/schema-service/v1/schema/' \
--header 'Data-Partition-Id: <DATA_PARTITION>' \
--header 'Content-Type: application/json' \
--data-raw '{
    "schemaInfo": {
        "schemaIdentity": {
            "authority": "<DATA_PARTITION>",
            "source": "wks",
            "entityType": "ArraysOfObjectsTestCollection",
            "schemaVersionMajor": 5,
            "schemaVersionMinor": 0,
            "schemaVersionPatch": 0
        },
        "status": "DEVELOPMENT"
    },
    "schema": {
        "x-osdu-license": "Copyright 2021, The Open Group \\nLicensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 . Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.",
        "$schema": "http://json-schema.org/draft-07/schema#",
        "description": "Array of objects testing schema",
        "title": "Test",
        "type": "object",
        "required": [
            "kind",
            "acl",
            "legal"
        ],
        "properties": {
            "data": {
                "allOf": [
                    {
                        "type": "object",
                        "properties": {
                            "NestedTest": {
                                "description": "nested type test",
                                "type": "array",
                                "x-osdu-indexing": {
                                    "type": "nested"
                                },
                                "items": {
                                    "type": "object",
                                    "properties": {
                                        "NestedInnerTest": {
                                            "x-osdu-indexing": {
                                                "type": "nested"
                                            },
                                            "description": "nested type test",
                                            "type": "array",
                                            "items": {
                                                "type": "object",
                                                "properties": {
                                                    "StringInnerTest": {
                                                        "description": "string test",
                                                        "type": "string"
                                                    },
                                                    "DateTimeInnerTest": {
                                                        "format": "date-time",
                                                        "description": "date and time test",
                                                        "x-osdu-frame-of-reference": "DateTime",
                                                        "type": "string"
                                                    },
                                                    "NumberInnerTest": {
                                                        "description": "number test",
                                                        "type": "number"
                                                    }
                                                }
                                            }
                                        },
                                        "DateTimeTest": {
                                            "description": "date and time test",
                                            "type": "string",
                                            "format": "date-time",
                                            "x-osdu-frame-of-reference": "DateTime"
                                        },
                                        "NumberTest": {
                                            "description": "number test",
                                            "type": "number"
                                        },
                                        "StringTest": {
                                            "description": "string test",
                                            "type": "string"
                                        }
                                    }
                                }
                            },
                            "FlattenedTest": {
                                "description": "flattened type test",
                                "type": "array",
                                "x-osdu-indexing": {
                                    "type": "flattened"
                                },
                                "items": {
                                    "type": "object",
                                    "properties": {
                                        "DateTimeTest": {
                                            "description": "date and time test",
                                            "type": "string",
                                            "format": "date-time",
                                            "x-osdu-frame-of-reference": "DateTime"
                                        },
                                        "NumberTest": {
                                            "description": "number test",
                                            "type": "number"
                                        },
                                        "StringTest": {
                                            "description": "string test",
                                            "type": "string"
                                        }
                                    }
                                }
                            },
                            "ObjectTest": {
                                "description": "default object type test",
                                "type": "array",
                                "x-osdu-indexing": {
                                    "type": "nested"
                                },
                                "items": {
                                    "type": "object",
                                    "properties": {
                                        "DateTimeTest": {
                                            "description": "date and time test",
                                            "type": "string",
                                            "format": "date-time",
                                            "x-osdu-frame-of-reference": "DateTime"
                                        },
                                        "NumberTest": {
                                            "description": "number test",
                                            "type": "number"
                                        },
                                        "StringTest": {
                                            "description": "string test",
                                            "type": "string"
                                        }
                                    }
                                }
                            }
                        }
                    }
                ]
            }
        },
        "x-osdu-inheriting-from-kind": []
    }
}'
```

2. Perform a forced reindexing task using the Indexer service: 
```
curl --location --request POST '<INDEXER_URL>/api/indexer/v2/reindex?force_clean=true' \
--header 'data-partition-id: <DATA_PARTITION>' \
--header 'correlation-id: 74c20433-544f-46e3-a215-c059b2ca6810' \
--header 'Content-Type: application/json' \
--data-raw '{
  "kind":"<DATA_PARTITION>:wks:ArraysOfObjectsTestCollection:5.0.0"
}'
```

3. After some time (depends on the number of records in kind) this array of objects becomes searchable:
```json
{
    "query":"nested(data.ObjectTest, (NumberTest:[1 TO 20000]))"
}
```
This is an unusual scenario, but it can come in handy for solving such a problem. 