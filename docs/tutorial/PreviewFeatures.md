## Phrase completion

Feature available on OSDU deployments with autocomplete feature flag enabled and bagOfWords indexer feature enabled. 
Users can retrieve phrase completions along with or instead of the results that may help them build more accurate queries.
Suggestion behavior currently is based on completion suggester.

```json
POST /search/v2/query HTTP/1.1
{
  "kind": "osdu:wks:master-data--WellPlanningWellbore:1.0.0",
  "query": "awseastusa",
  "suggestPhrase": "someuniquesurveyprogramid",
}

Response:
{
    "results": [...],
    "aggregations": ...,
    "phraseSuggestions": [
        "osdu:master-data--SurveyProgram:SomeUniqueSurveyProgramID:"
    ],
    "totalCount": ...
}

```
