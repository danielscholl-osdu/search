## Phrase completion

This feature is available on OSDU deployments with the autocomplete feature flag enabled
and the bagOfWords feature enabled on the indexer service.

By adding the json element **suggestPhrase** to the search query,
users can retrieve phrase completions along with or instead of the results that may
help them build more accurate queries.

Because this feature returns exact matching phrases 
it is not meant for spell correction or did-you-mean functionality.

Example query:
```http
POST /search/v2/query HTTP/1.1
{
  "kind": "osdu:wks:master-data--WellPlanningWellbore:1.0.0",
  "query": "awseastusa",
  "suggestPhrase": "someuniquesurveyprogramid"
}
```

Example Response:
```json
{
    "results": [],
    "aggregations": "...",
    "phraseSuggestions": [
        "osdu:master-data--SurveyProgram:SomeUniqueSurveyProgramID:"
    ],
    "totalCount": 12
}
```

This example shows that the service looks for the phrase being present across
multiple fields in the records and returns only the phrase actually present and not
the details of in what field the phrase is present.

## Limitations
The input can be a maximum of 50 characters in length.

There are also limitations based on how we have enabled indexing of fields.
For field attributes which have the indexing hint to be flattened, then only
the most nested attributes are indexed.

Same thing for all the meta attributes (e.g. kind, id, acls, tags etc.) as well,
completion suggester won't return.
