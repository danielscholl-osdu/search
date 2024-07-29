package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.util.NamedValue;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;
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

  private final Pattern nestedAggregationPattern =
      Pattern.compile("(nested\\s?\\()(?<path>.+?),\\s?(?<field>[^)]+)");

  private final SearchConfigurationProperties configurationProperties;

  @Override
  public Map<String, Aggregation> parseAggregation(String aggregation) {
    TermsAggregation.Builder termsAggregationBuilder = new TermsAggregation.Builder();
    Map<String, Aggregation> aggregationMap = new HashMap<>();
    if (aggregation.contains("nested(") || aggregation.contains("nested (")) {
      Matcher nestedMatcher = nestedAggregationPattern.matcher(aggregation);
      if (nestedMatcher.find()) {
        aggregationMap.put(NESTED_AGGREGATION_NAME, parseNestedInDepth(aggregation, termsAggregationBuilder));
        return aggregationMap;
      } else {
        throw new AppException(
            HttpStatus.SC_BAD_REQUEST,
            String.format("Malformed nested aggregation : %s", aggregation),
            BAD_AGGREGATION_MESSAGE);
      }
    } else {
      termsAggregationBuilder.field(aggregation);
      termsAggregationBuilder.size(configurationProperties.getAggregationSize());
      aggregationMap.put(TERM_AGGREGATION_NAME, termsAggregationBuilder.build()._toAggregation());
      return aggregationMap;
    }
  }

  private Aggregation parseNestedInDepth(
      String aggregation, TermsAggregation.Builder termsAggregationBuilder) {
    Matcher nestedMatcher = nestedAggregationPattern.matcher(aggregation);
    Deque<Aggregation> builderStack = new LinkedList<>();
    String pathGroup;
    String fieldGroup;

    while (nestedMatcher.find()) {
      pathGroup = nestedMatcher.group(PATH_GROUP);
      fieldGroup = nestedMatcher.group(FIELD_GROUP);

      Aggregation nestedAggregation =
          AggregationBuilders.nested().path(pathGroup).build()._toAggregation();
      builderStack.push(nestedAggregation);

      nestedMatcher.reset(fieldGroup);
      if (!nestedMatcher.find()) {
        Aggregation nestedAggregationLast =
            AggregationBuilders.nested().path(pathGroup).build()._toAggregation();
        termsAggregationBuilder.field(pathGroup + "." + fieldGroup);
        termsAggregationBuilder.size(configurationProperties.getAggregationSize());
        termsAggregationBuilder.minDocCount(1);
        termsAggregationBuilder.showTermDocCountError(false);
        termsAggregationBuilder.order(
            List.of(NamedValue.of("_count", SortOrder.Desc), NamedValue.of("_key", SortOrder.Asc)));

        Aggregation resultAggregation =
            new Aggregation.Builder()
                .nested(nestedAggregationLast.nested())
                .aggregations(
                    Map.of(TERM_AGGREGATION_NAME, termsAggregationBuilder.build()._toAggregation()))
                .build();
        builderStack.pop();
        builderStack.push(resultAggregation);
        break;
      }

      aggregation = fieldGroup;
      nestedMatcher = nestedAggregationPattern.matcher(aggregation);
    }

    if (builderStack.isEmpty()) {
      throw new AppException(
          400,
          String.format("Malformed nested aggregation: %s", aggregation),
          "Bad aggregation format");
    }

    Aggregation finalAggregation = builderStack.pop();

    Aggregation.Builder resultAggregation = new Aggregation.Builder();

    while (!builderStack.isEmpty()) {
      Aggregation parent = builderStack.pop();
      parent =
          resultAggregation
              .nested(parent.nested())
              .aggregations(NESTED_AGGREGATION_NAME, finalAggregation)
              .build();
      finalAggregation = parent;
      resultAggregation = new Aggregation.Builder();
    }

    return finalAggregation;
  }
}
