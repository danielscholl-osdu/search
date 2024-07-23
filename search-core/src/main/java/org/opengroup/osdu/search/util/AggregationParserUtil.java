package org.opengroup.osdu.search.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.elastic.clients.elasticsearch._types.aggregations.AggregationBase;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AggregationParserUtil implements IAggregationParserUtil {

    public static final String TERM_AGGREGATION_NAME = "agg";
    public static final String NESTED_AGGREGATION_NAME = "nested";
    private static final String PATH_GROUP = "path";
    private static final String FIELD_GROUP = "field";
    private static final String BAD_AGGREGATION_MESSAGE =
        "Must be in format: nested(<path>, <field>) OR nested(<parent_path>, .....nested(<child_path>, <field>))";

    private final Pattern nestedAggregationPattern = Pattern.compile("(nested\\s?\\()(?<path>.+?),\\s?(?<field>[^)]+)");

    private final SearchConfigurationProperties configurationProperties;

    @Override
    public AggregationBase.AbstractBuilder parseAggregation(String aggregation) {
        TermsAggregation.Builder termsAggregationBuilder = new TermsAggregation.Builder().name(TERM_AGGREGATION_NAME);
        if (aggregation.contains("nested(") || aggregation.contains("nested (")) {
            Matcher nestedMatcher = nestedAggregationPattern.matcher(aggregation);
            if (nestedMatcher.find()) {
                return parseNestedInDepth(aggregation, termsAggregationBuilder);
            } else {
                throw new AppException(HttpStatus.SC_BAD_REQUEST, String.format("Malformed nested aggregation : %s", aggregation),
                    BAD_AGGREGATION_MESSAGE);
            }
        } else {
            termsAggregationBuilder.field(aggregation);
            termsAggregationBuilder.size(configurationProperties.getAggregationSize());
            return termsAggregationBuilder;
        }
    }

    private AggregationBase.AbstractBuilder parseNestedInDepth(String aggregation, TermsAggregation.Builder termsAggregationBuilder) {
        Matcher nestedMatcher = nestedAggregationPattern.matcher(aggregation);
        String pathGroup = null;
        String fieldGroup = null;
        if (nestedMatcher.find()) {
            pathGroup = nestedMatcher.group(PATH_GROUP);
            fieldGroup = nestedMatcher.group(FIELD_GROUP);
            nestedMatcher.reset(fieldGroup);
            if (nestedMatcher.find()) {
                NestedAggregation.of(na -> na.name(NESTED_AGGREGATION_NAME).path(pathGroup).)
                return new NestedAggregationBuilder(NESTED_AGGREGATION_NAME, pathGroup).subAggregation(parseNestedInDepth(fieldGroup, termsAggregationBuilder));
            } else {
                NestedAggregationBuilder nestedAggregationBuilder = new NestedAggregationBuilder(NESTED_AGGREGATION_NAME, pathGroup);
                termsAggregationBuilder.field(pathGroup + "." + fieldGroup);
                termsAggregationBuilder.size(configurationProperties.getAggregationSize());
                nestedAggregationBuilder.subAggregation(termsAggregationBuilder);
                return nestedAggregationBuilder;
            }
        } else {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, String.format("Malformed nested aggregation : %s", aggregation),
                BAD_AGGREGATION_MESSAGE);
        }
    }
}
