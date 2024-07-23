package org.opengroup.osdu.search.util;

import java.util.List;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SuggestionsQueryUtil {
    @Autowired
    private IFeatureFlag autocompleteFeatureFlag;
    private final String SUGGESTION_NAME = "autocomplete";

  public void getSuggestions(String suggestPhrase) {
    //        if (!autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME) ||
    // suggestPhrase == null || suggestPhrase == "") {
    //            return null;
    //        }
    //
    //        SuggestionBuilder suggestionBuilder = SuggestionBuilders.completion().options()
    //            "bagOfWords.autocomplete"
    //            ).text(suggestPhrase).skipDuplicates(true);
    //        SuggestBuilder suggestBuilder = new SuggestBuilder();
    //        suggestBuilder.addSuggestion(SUGGESTION_NAME, suggestionBuilder);
    //        return suggestBuilder;
    //    }
  }

    public List<String> getPhraseSuggestionsFromSearchResponse(int i) {
//        if (!autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)) {
//      return Collections.emptyList();
//        }
//
//        var suggestBlock = searchResponse.suggest();
//        if (suggestBlock == null) {
//            return null;
//        }
//        List<String> phraseSuggestions = new ArrayList<>();
//        var suggestion = suggestBlock.get(SUGGESTION_NAME);
//        for (Entry entry : suggestion) {
//            for (Entry.Option option : entry) {
//                phraseSuggestions.add(option.getText().toString());
//            }
//        }
        return null;
    }
}
