package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.util.AggregationParserUtil.NESTED_AGGREGATION_NAME;
import static org.opengroup.osdu.search.util.AggregationParserUtil.TERM_AGGREGATION_NAME;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.util.NamedValue;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;

@RunWith(MockitoJUnitRunner.class)
public class AggregationParserUtilTest {

  public static final String SIMPLE_NESTED_FIELD = "data.Nested.NestedField";
  public static final String SIMPLE_NESTED_PATH = "data.Nested";

  public static final String MULTILEVEL_NESTED_FIELD = "data.Nested.NestedInner.NestedInnerField";
  public static final String NESTED_INNER_PATH = "data.Nested.NestedInner";
  public static final String PARENT_NESTED_PATH = "data.Nested";

  private static final String SIMPLE_AGGREGATION = "data.SimpleField";
  private static final String NESTED_AGGREGATION = "nested(data.Nested, NestedField)";
  private static final String MULTILEVEL_NESTED_AGGREGATION =
      "nested(data.Nested, nested(data.Nested.NestedInner, NestedInnerField)";
  private static final String NESTED_AGGREGATION_WITH_SPACE = "nested (data.Nested,NestedField)";
  private static final String MULTILEVEL_NESTED_AGGREGATION_WITH_SPACE =
      "nested (data.Nested, nested(data.Nested.NestedInner,NestedInnerField)";

  @Mock
  private SearchConfigurationProperties properties;

  @InjectMocks
  private AggregationParserUtil aggregationParserUtil;

  @Before
  public void setUp() {
    when(properties.getAggregationSize()).thenReturn(1000);
    aggregationParserUtil = new AggregationParserUtil(properties);
  }

  @Test
  public void testSimpleAggregation() {
    Map<String, Aggregation> actualAggregation =
        aggregationParserUtil.parseAggregation(SIMPLE_AGGREGATION);

    TermsAggregation expectedAggregation = getTermsAggregation(SIMPLE_AGGREGATION);

    assertEquals(
        expectedAggregation.toString(),
        actualAggregation.get(TERM_AGGREGATION_NAME).terms().toString());
  }

  @Test
  public void testNestedAggregation() {
    Map<String, Aggregation> actualAggregation =
        aggregationParserUtil.parseAggregation(NESTED_AGGREGATION);

    TermsAggregation termsAggregation = getTermsAggregation(SIMPLE_NESTED_FIELD);
    Aggregation expectedAggregation =
        new Aggregation.Builder()
            .nested(na -> na.path(SIMPLE_NESTED_PATH))
            .aggregations(Map.of(TERM_AGGREGATION_NAME, termsAggregation._toAggregation()))
            .build();

    assertEquals(
        expectedAggregation.toString(), actualAggregation.get(NESTED_AGGREGATION_NAME).toString());
  }

  @Test
  public void testMultilevelAggregation() {
    Map<String, Aggregation> actualAggregation =
        aggregationParserUtil.parseAggregation(MULTILEVEL_NESTED_AGGREGATION);

    TermsAggregation termsAggregation = getTermsAggregation(MULTILEVEL_NESTED_FIELD);
    Aggregation nestedInnerAggregation =
        new Aggregation.Builder()
            .nested(n -> n.path(NESTED_INNER_PATH))
            .aggregations(Map.of(TERM_AGGREGATION_NAME, termsAggregation._toAggregation()))
            .build();
    Aggregation parentNestedAggregation =
        new Aggregation.Builder()
            .nested(n -> n.path(PARENT_NESTED_PATH))
            .aggregations(NESTED_AGGREGATION_NAME, nestedInnerAggregation)
            .build();

    assertEquals(
        parentNestedAggregation.toString(),
        actualAggregation.get(NESTED_AGGREGATION_NAME).toString());
  }

  @Test
  public void testNestedAggregationWithSpace() {
    Map<String, Aggregation> actualAggregation =
        aggregationParserUtil.parseAggregation(NESTED_AGGREGATION_WITH_SPACE);

    TermsAggregation termsAggregation = getTermsAggregation(SIMPLE_NESTED_FIELD);
    Aggregation expectedAggregation =
        new Aggregation.Builder()
            .nested(na -> na.path(SIMPLE_NESTED_PATH))
            .aggregations(Map.of(TERM_AGGREGATION_NAME, termsAggregation._toAggregation()))
            .build();

    assertEquals(
        expectedAggregation.toString(), actualAggregation.get(NESTED_AGGREGATION_NAME).toString());
  }

  @Test
  public void testMultilevelAggregationWithSpace() {
    Map<String, Aggregation> actualAggregation =
        aggregationParserUtil.parseAggregation(MULTILEVEL_NESTED_AGGREGATION_WITH_SPACE);

    TermsAggregation termsAggregation = getTermsAggregation(MULTILEVEL_NESTED_FIELD);
    Aggregation nestedInnerAggregation =
        new Aggregation.Builder()
            .nested(n -> n.path(NESTED_INNER_PATH))
            .aggregations(Map.of(TERM_AGGREGATION_NAME, termsAggregation._toAggregation()))
            .build();
    Aggregation parentNestedAggregation =
        new Aggregation.Builder()
            .nested(n -> n.path(PARENT_NESTED_PATH))
            .aggregations(NESTED_AGGREGATION_NAME, nestedInnerAggregation)
            .build();

    assertEquals(
        parentNestedAggregation.toString(),
        actualAggregation.get(NESTED_AGGREGATION_NAME).toString());
  }

  private static TermsAggregation getTermsAggregation(String simpleAggregation) {
    return TermsAggregation.of(
        ta ->
            ta.field(simpleAggregation)
                .size(1000)
                .minDocCount(1)
                .showTermDocCountError(false)
                .order(
                    List.of(
                        NamedValue.of("_count", SortOrder.Desc),
                        NamedValue.of("_key", SortOrder.Asc))));
  }
}
