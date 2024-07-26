package org.opengroup.osdu.search.util;

import static org.opengroup.osdu.search.config.SearchConfigurationProperties.AUTOCOMPLETE_FEATURE_NAME;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Suggester;

import java.util.*;

import co.elastic.clients.elasticsearch.core.search.Suggestion;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SuggestionsQueryUtil {

  @Autowired private IFeatureFlag autocompleteFeatureFlag;
  private final String SUGGESTION_NAME = "autocomplete";

  public Suggester getSuggestions(String suggestPhrase) {
    if (!autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)
        || Objects.isNull(suggestPhrase)
        || suggestPhrase.isEmpty()) {
      return null;
    }
    return Suggester.of(
        s ->
            s.suggesters(
                SUGGESTION_NAME,
                fs ->
                    fs.completion(c -> c.field("bagOfWords.autocomplete").skipDuplicates(true))
                        .text(suggestPhrase)));
  }

      public List<String> getPhraseSuggestionsFromSearchResponse(SearchResponse<Map<String, Object>> searchResponse) {
          if (!autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)) {
            return Collections.emptyList();
          }

          var suggestBlock = searchResponse.suggest();
          if (suggestBlock == null) {
              return null;
          }
          List<String> phraseSuggestions = new ArrayList<>();
          List<Suggestion<Map<String, Object>>> suggestions = suggestBlock.get(SUGGESTION_NAME);

          for(Suggestion<Map<String, Object>> suggestion : suggestions){
          }

          //        var suggestion = suggestBlock.get(SUGGESTION_NAME);
  //        for (Entry entry : suggestion) {
  //            for (Entry.Option option : entry) {
  //                phraseSuggestions.add(option.getText().toString());
  //            }
  //        }
          return null;
      }
}
