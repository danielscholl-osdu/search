package org.opengroup.osdu.search.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.AUTOCOMPLETE_FEATURE_NAME;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class SuggestionsQueryUtilTests {

  private final String VALID_QUERY =
      """
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
  private final String VALID_RESPONSE =
      """
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

  @Mock private IFeatureFlag autocompleteFeatureFlag;
  @InjectMocks private SuggestionsQueryUtil suggestionsQueryUtil;

  @Test
  public void suggestions_query_not_added_when_featureFlag_disabled() {
    when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(false);

    assertNull(suggestionsQueryUtil.getSuggestions("aaa"));
  }

  @Test
  public void suggestions_query_not_added_when_featureFlag_enabled() {
    when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(true);

    SearchRequest expectedQuery =
        SearchRequest.of(sr -> sr.suggest(s -> s.withJson(new StringReader(VALID_QUERY))));

    assertEquals(
        Objects.requireNonNull(expectedQuery.suggest()).toString(),
        suggestionsQueryUtil.getSuggestions("aaa").toString());
  }

  @Test
  public void suggestions_are_returned_when_featureFlag_enabled() throws Exception {
    SearchResponse<Map<String, Object>> searchResponseBuilder =
        SearchResponse.of(sr -> sr.withJson(new StringReader(VALID_RESPONSE)));

    when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(true);

    List<String> expectedSuggestions =
        new ArrayList<>() {
          {
            add("TEST");
          }
        };

    assertEquals(
        expectedSuggestions,
        suggestionsQueryUtil.getPhraseSuggestionsFromSearchResponse(searchResponseBuilder));
  }

  @Test
  public void suggestions_are_ignored_when_featureFlag_disabled() throws Exception {
    SearchResponse<Map<String, Object>> searchResponseBuilder =
        SearchResponse.of(sr -> sr.withJson(new StringReader(VALID_RESPONSE)));

    when(autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)).thenReturn(false);

    assertNull(suggestionsQueryUtil.getPhraseSuggestionsFromSearchResponse(searchResponseBuilder));
  }
}
