/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.util;

import static org.opengroup.osdu.search.config.SearchConfigurationProperties.AUTOCOMPLETE_FEATURE_NAME;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import java.util.*;
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

  public List<String> getPhraseSuggestionsFromSearchResponse(
      SearchResponse<Map<String, Object>> searchResponse) {
    if (!autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)) {
      return null;
    }

    var suggestBlock = searchResponse.suggest();
    if (suggestBlock == null) {
      return null;
    }
    List<String> phraseSuggestions = new ArrayList<>();
    List<Suggestion<Map<String, Object>>> suggestions = suggestBlock.get(SUGGESTION_NAME);

    if (suggestions == null) {
      return new ArrayList<>();
    }
    for (Suggestion<Map<String, Object>> suggestion : suggestions) {
      if (Objects.isNull(suggestion.completion()) && suggestion.completion().options().isEmpty()) {
        continue;
      }
      for (CompletionSuggestOption<Map<String, Object>> completionSuggestOption :
          suggestion.completion().options()) {
        String text = completionSuggestOption.text();
        if (Objects.isNull(text)) {
          continue;
        }
        phraseSuggestions.add(text);
      }
    }
    return phraseSuggestions;
  }
}
