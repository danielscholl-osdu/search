package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.AUTOCOMPLETE_FEATURE_NAME;

@RunWith(MockitoJUnitRunner.class)
public class SuggestionsQueryUtilTests {

    private final String VALID_QUERY = """
        {
            "autocomplete": {
                "text": "aaa",
                "completion": {
                    "field": "bagOfWords.autocomplete",
                    "skip_duplicates": true
                }
            }
        }        
    """;
    private final String VALID_RESPONSE = """
        {
            "took" : 4,
            "timed_out" : false,
            "_shards" : {
              "total" : 1,
              "successful" : 1,
              "skipped" : 0,
              "failed" : 0
            },
            "hits" : {
              "total" : {
                "value" : 0,
                "relation" : "eq"
              },
              "max_score" : null,
              "hits" : [ ]
            },
            "suggest" : {
              "completion#autocomplete" : [
                {
                  "text" : "tes",
                  "offset" : 0,
                  "length" : 3,
                  "options" : [
                    {
                      "text" : "TEST",
                      "_index" : "bag_of_words",
                      "_type" : "_doc",
                      "_id" : "3",
                      "_score" : 1.0,
                      "_source" : {
                        "data" : {
                          "ExistenceKind" : "TEST"
                        }
                      }
                    }
                  ]
                }
              ]
            }
          }
    """;
    
    @Mock
    private IFeatureFlag autocompleteFeatureFlag;
    @InjectMocks
    private SuggestionsQueryUtil suggestionsQueryUtil;

    @Test
    public void suggestions_query_not_added_when_featureFlag_disabled() {
        when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(false);
        assertEquals(null, suggestionsQueryUtil.getSuggestions("aaa"));    
    }

    @Test
    public void suggestions_query_not_added_when_featureFlag_enabled() {
        when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(true);
        JsonElement expectedQuery = JsonParser.parseString(VALID_QUERY);
        assertEquals(expectedQuery, JsonParser.parseString(suggestionsQueryUtil.getSuggestions("aaa").toString()));    
    }

    @Test
    public void suggestions_are_returned_when_featureFlag_enabled() throws Exception {
        NamedXContentRegistry registry = new NamedXContentRegistry(getDefaultNamedXContents());
        XContentParser parser = JsonXContent.jsonXContent.createParser(registry, LoggingDeprecationHandler.INSTANCE, VALID_RESPONSE);
        when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(true);
        List<String> expectedSuggestions = new ArrayList<>() {{add("TEST");}};
        assertEquals(expectedSuggestions, suggestionsQueryUtil.getPhraseSuggestionsFromSearchResponse(SearchResponse.fromXContent(parser)));    
    }

    @Test
    public void suggestions_are_ignored_when_featureFlag_disabled() throws Exception { 
        NamedXContentRegistry registry = new NamedXContentRegistry(getDefaultNamedXContents());
        XContentParser parser = JsonXContent.jsonXContent.createParser(registry, LoggingDeprecationHandler.INSTANCE, VALID_RESPONSE);
        when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(false);
        assertEquals(null, suggestionsQueryUtil.getPhraseSuggestionsFromSearchResponse(SearchResponse.fromXContent(parser)));    
    }

    private List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        List<NamedXContentRegistry.Entry> entries = new ArrayList<>();
        entries.add(new NamedXContentRegistry.Entry(
            Suggest.Suggestion.class, 
            new ParseField("completion"),
            (parser, context) -> CompletionSuggestion.fromXContent(parser, (String) context))
        );
        return entries;
    }
}

