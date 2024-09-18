package org.opengroup.osdu.search.util;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;

@RunWith(MockitoJUnitRunner.class)
@Ignore
//TODO:
public class AggregationParserUtilTest {

    public static final String SIMPLE_NESTED_FIELD = "data.Nested.NestedField";
    public static final String SIMPLE_NESTED_PATH = "data.Nested";

    public static final String MULTILEVEL_NESTED_FIELD = "data.Nested.NestedInner.NestedInnerField";
    public static final String NESTED_INNER_PATH = "data.Nested.NestedInner";
    public static final String PARENT_NESTED_PATH = "data.Nested";


    private String simpleAggregation = "data.SimpleField";
    private String nestedAggregation = "nested(data.Nested, NestedField)";
    private String multilevelNestedAggregation = "nested(data.Nested, nested(data.Nested.NestedInner, NestedInnerField)";
    private String nestedAggregationWithSpace = "nested (data.Nested,NestedField)";
    private String multilevelNestedAggregationWithSpace = "nested (data.Nested, nested(data.Nested.NestedInner,NestedInnerField)";

    @Mock
    private SearchConfigurationProperties properties;

    @InjectMocks
    private AggregationParserUtil aggregationParserUtil;

    @Before
    public void setUp() {
        when(properties.getAggregationSize()).thenReturn(1000);
        aggregationParserUtil = new AggregationParserUtil(properties);
    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void testSimpleAggregation() {
//        TermsAggregationBuilder actualTerms = (TermsAggregationBuilder) aggregationParserUtil.parseAggregation(simpleAggregation, sourceBuilder);
//
//        TermsAggregationBuilder expectedTerms = new TermsAggregationBuilder(AggregationParserUtil.TERM_AGGREGATION_NAME);
//        expectedTerms.field(simpleAggregation);
//        expectedTerms.size(1000);
//
//        assertEquals(expectedTerms, actualTerms);
//    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void testNestedAggregation() throws IOException {
//        NestedAggregationBuilder actualNested = (NestedAggregationBuilder) aggregationParserUtil.parseAggregation(nestedAggregation, sourceBuilder);
//
//        TermsAggregationBuilder expectedTerms = new TermsAggregationBuilder(AggregationParserUtil.TERM_AGGREGATION_NAME);
//        expectedTerms.field(SIMPLE_NESTED_FIELD);
//        expectedTerms.size(1000);
//
//        NestedAggregationBuilder expectedNested = new NestedAggregationBuilder(AggregationParserUtil.NESTED_AGGREGATION_NAME, SIMPLE_NESTED_PATH);
//        expectedNested.subAggregation(expectedTerms);
//
//        assertEquals(expectedNested, actualNested);
//    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void testMultilevelAggregation() {
//        NestedAggregationBuilder actualNested = (NestedAggregationBuilder) aggregationParserUtil.parseAggregation(multilevelNestedAggregation, sourceBuilder);
//
//        TermsAggregationBuilder expectedTerms = new TermsAggregationBuilder(AggregationParserUtil.TERM_AGGREGATION_NAME);
//        expectedTerms.field(MULTILEVEL_NESTED_FIELD);
//        expectedTerms.size(1000);
//
//        NestedAggregationBuilder expectedInnerNested = new NestedAggregationBuilder(AggregationParserUtil.NESTED_AGGREGATION_NAME, NESTED_INNER_PATH);
//        expectedInnerNested.subAggregation(expectedTerms);
//
//        NestedAggregationBuilder expectedParentNested = new NestedAggregationBuilder(AggregationParserUtil.NESTED_AGGREGATION_NAME, PARENT_NESTED_PATH);
//        expectedParentNested.subAggregation(expectedInnerNested);
//
//        assertEquals(expectedParentNested, actualNested);
//    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void testNestedAggregationWithSpace(){
//        NestedAggregationBuilder actualNested = (NestedAggregationBuilder) aggregationParserUtil.parseAggregation(nestedAggregationWithSpace, sourceBuilder);
//
//        TermsAggregationBuilder expectedTerms = new TermsAggregationBuilder(AggregationParserUtil.TERM_AGGREGATION_NAME);
//        expectedTerms.field(SIMPLE_NESTED_FIELD);
//        expectedTerms.size(1000);
//
//        NestedAggregationBuilder expectedNested = new NestedAggregationBuilder(AggregationParserUtil.NESTED_AGGREGATION_NAME, SIMPLE_NESTED_PATH);
//        expectedNested.subAggregation(expectedTerms);
//
//        assertEquals(expectedNested, actualNested);
//    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void testMultilevelAggregationWithSpace(){
//        NestedAggregationBuilder actualNested = (NestedAggregationBuilder) aggregationParserUtil.parseAggregation(multilevelNestedAggregationWithSpace, sourceBuilder);
//
//        TermsAggregationBuilder expectedTerms = new TermsAggregationBuilder(AggregationParserUtil.TERM_AGGREGATION_NAME);
//        expectedTerms.field(MULTILEVEL_NESTED_FIELD);
//        expectedTerms.size(1000);
//
//        NestedAggregationBuilder expectedInnerNested = new NestedAggregationBuilder(AggregationParserUtil.NESTED_AGGREGATION_NAME, NESTED_INNER_PATH);
//        expectedInnerNested.subAggregation(expectedTerms);
//
//        NestedAggregationBuilder expectedParentNested = new NestedAggregationBuilder(AggregationParserUtil.NESTED_AGGREGATION_NAME, PARENT_NESTED_PATH);
//        expectedParentNested.subAggregation(expectedInnerNested);
//
//        assertEquals(expectedParentNested, actualNested);
//    }
}
