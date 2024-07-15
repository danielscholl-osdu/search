package org.opengroup.osdu.search.util;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry;

import static org.opengroup.osdu.search.config.SearchConfigurationProperties.AUTOCOMPLETE_FEATURE_NAME;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

@Component
public class SuggestionsQueryUtil {
    @Autowired
    private IFeatureFlag autocompleteFeatureFlag;
    private final String SUGGESTION_NAME = "autocomplete";

    public SuggestBuilder getSuggestions(String suggestPhrase) {
        if (!autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME) || suggestPhrase == null || suggestPhrase == "") {
            return null;
        }
        SuggestionBuilder suggestionBuilder = SuggestBuilders.completionSuggestion(
            "bagOfWords.autocomplete"
            ).text(suggestPhrase).skipDuplicates(true);
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion(SUGGESTION_NAME, suggestionBuilder);
        return suggestBuilder;     
    }

    public List<String> getPhraseSuggestionsFromSearchResponse(SearchResponse searchResponse) {
        if (!autocompleteFeatureFlag.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME)) {
            return null;
        }

        Suggest suggestBlock = searchResponse.getSuggest();
        if (suggestBlock == null) {
            return null;
        }
        List<String> phraseSuggestions = new ArrayList<>();
        CompletionSuggestion suggestion = suggestBlock.getSuggestion(SUGGESTION_NAME);
        for (Entry entry : suggestion.getEntries()) {
            for (Entry.Option option : entry) {
                phraseSuggestions.add(option.getText().toString());
            }
        }
        return phraseSuggestions;
    }
}
