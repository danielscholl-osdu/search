package org.opengroup.osdu.search.util;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengroup.osdu.core.common.model.http.AppException;

public class QueryParserUtilTest {


    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private String simpleStringQuery = "simple query";
    private String simpleQueryWithBools = "TEXAS OR TX";
    private String combinedSimpleQueries = "(data.First:\"some data\") AND (data.Second:\"other data\")";

    private String combinedSimpleAndNestedQuery =
        "data.First:\"Example*\" OR nested(data.NestedTest, (StringTest:\"Example*\")) AND data.Second:\"test string\"";
    private String expectedFirstStringQuery = "data.First:\"Example*\" ";
    private String expectedNestedStringQuery = "(data.NestedTest.StringTest:\"Example*\")";
    private String expectedSecondStringQuery = "data.Second:\"test string\"";

    private String simpleNestedPath = "data.NestedTest";
    private String simpleNestedQuery = "nested(" + simpleNestedPath + ", (NumberTest:(12345.0 OR 0) AND StringTest:\"test string\"))";
    private String rangeSimpleNestedQuery = "nested(" + simpleNestedPath + ", (MarkerMeasuredDepth:(>0)))";
    private String rangeSimpleCombinedNestedQuery = "nested(" + simpleNestedPath + ", (NumberTest:(>12345.0) AND SecondNumber:[0 TO 100]))";
    private String expectedSimpleNestedQueryString =
        "(" + simpleNestedPath + ".NumberTest:(12345.0 OR 0) AND " + simpleNestedPath + ".StringTest:\"test string\")";
    private String expectedRangeSimpleCombinedNestedQueryString =
        "(" + simpleNestedPath + ".NumberTest:(>12345.0) AND " + simpleNestedPath + ".SecondNumber:[0 TO 100])";
    private String expectedRangeSimpleNestedQueryString = "(" + simpleNestedPath + ".MarkerMeasuredDepth:(>0))";

    private String multilevelFirstLevelNestedPath = "data.FirstLevel";
    private String multilevelSecondLevelNestedPath = "data.FirstLevel.SecondLevel";
    private String multilevelThirdLevelNestedPath = "data.FirstLevel.SecondLevel.ThirdLevel";
    private String queryStringForMultilevelNestedQuery = "(ThirdLevelNumberTest:\"12345.0\")";
    private String expectedQueryStringForMultilevelNestedQuery = "(data.FirstLevel.SecondLevel.ThirdLevel.ThirdLevelNumberTest:\"12345.0\")";
    private String multileveledNestedQuery =
        "(nested(" + multilevelFirstLevelNestedPath +
            ", nested(" + multilevelSecondLevelNestedPath +
            ", nested(" + multilevelThirdLevelNestedPath + ", " + queryStringForMultilevelNestedQuery + "))))";

    private String combinedParentNested = "data.ParentNested";
    private String combinedFirstInnerNested = "data.ParentNested.FirstInnerNested";
    private String combinedSecondInnerNested = "data.ParentNested.SecondInnerNested";
    private String combinedMultilevelNestedQuery = "nested(" + combinedParentNested
        + ", nested(" + combinedFirstInnerNested + ", (NumberTest:(12345.0 OR 0) AND StringTest:\"test string\"))"
        + " OR nested(" + combinedSecondInnerNested + ", (NumberTest:\"12345.0\" AND StringTest:\"test string\")))";
    private String expectedCombinedFirstInnerNestedQueryString =
        "(" + combinedFirstInnerNested + ".NumberTest:(12345.0 OR 0) AND " + combinedFirstInnerNested + ".StringTest:\"test string\")";
    private String expectedCombinedSecondInnerNestedQueryString =
        "(" + combinedSecondInnerNested + ".NumberTest:\"12345.0\" AND " + combinedSecondInnerNested + ".StringTest:\"test string\")";

    private String combinedNestedQueryWithNot = "nested(" + combinedParentNested
        + ", nested(" + combinedFirstInnerNested + ", (NumberTest:(12345.0 OR 0) AND StringTest:\"test string\"))"
        + " NOT nested(" + combinedSecondInnerNested + ", (NumberTest:\"12345.0\" AND StringTest:\"test string\")))";

    private String incompleteParenthesesQuery = "nested(" + simpleNestedPath + ", (NumberTest:(12345.0 OR 0) AND StringTest:\"test string\"";
    private String malformedParenthesesQuery = "nested(" + simpleNestedPath + ", ))NumberTest:(12345.0 OR 0) AND StringTest:\"test string\"))";

    private QueryParserUtil queryParserUtil = new QueryParserUtil();

    @Test
    public void shouldReturnQueryBuilderWithSimpleQuery() {
        BoolQueryBuilder queryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(simpleStringQuery);
        List<QueryBuilder> mustQueries = queryBuilder.must();
        QueryStringQueryBuilder stringQueryBuilder = (QueryStringQueryBuilder) mustQueries.get(0);

        assertNotNull(stringQueryBuilder);
        assertEquals(simpleStringQuery, stringQueryBuilder.queryString());
    }

    @Test
    public void shouldReturnSingleQueryBuilderFormSimpleQueryWithBool() {
        BoolQueryBuilder queryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(simpleQueryWithBools);
        List<QueryBuilder> mustQueries = queryBuilder.must();
        assertEquals(1, mustQueries.size());
        QueryStringQueryBuilder stringQueryBuilder = (QueryStringQueryBuilder) mustQueries.get(0);

        assertNotNull(stringQueryBuilder);
        assertEquals(simpleQueryWithBools, stringQueryBuilder.queryString());
    }

    @Test
    public void shouldReturnSingleQueryBuilderForCombinedQuery() {
        BoolQueryBuilder queryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(combinedSimpleQueries);
        List<QueryBuilder> mustQueries = queryBuilder.must();

        assertEquals(1, mustQueries.size());

        QueryStringQueryBuilder stringQueryBuilder = (QueryStringQueryBuilder) mustQueries.get(0);

        assertEquals(combinedSimpleQueries, stringQueryBuilder.queryString());
    }

    @Test
    public void shouldReturnCombinedNestedAndSimpleQueryBuilder() {
        BoolQueryBuilder actualBoolQueryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(combinedSimpleAndNestedQuery);
        List<QueryBuilder> must = actualBoolQueryBuilder.must();
        assertEquals(1, must.size());

        QueryStringQueryBuilder expectedFirstQuery = queryStringQuery(expectedFirstStringQuery).allowLeadingWildcard(false);
        NestedQueryBuilder expectedNestedQuery =
            nestedQuery(simpleNestedPath, queryStringQuery(expectedNestedStringQuery).allowLeadingWildcard(false), ScoreMode.Avg);
        QueryStringQueryBuilder expectedSecondQuery = queryStringQuery(expectedSecondStringQuery).allowLeadingWildcard(false);

        BoolQueryBuilder expected = boolQuery().should(expectedFirstQuery).should(expectedNestedQuery).must(expectedSecondQuery);
        assertEquals(expected, actualBoolQueryBuilder);
    }

    @Test
    public void shouldReturnNestedQueryBuilderForNestedQuery() {
        BoolQueryBuilder queryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(simpleNestedQuery);
        List<QueryBuilder> mustQueries = queryBuilder.must();

        assertEquals(1, mustQueries.size());

        NestedQueryBuilder nestedQueryBuilder = (NestedQueryBuilder) mustQueries.get(0);
        NestedQueryBuilder expectedNestedQuery =
            nestedQuery(simpleNestedPath, queryStringQuery(expectedSimpleNestedQueryString).allowLeadingWildcard(false), ScoreMode.Avg);

        assertEquals(expectedNestedQuery, nestedQueryBuilder);
    }

    @Test
    public void shouldReturnNestedQueryBuilderForRangeNestedQuery() {
        BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(rangeSimpleNestedQuery);
        List<QueryBuilder> mustQueries = boolQueryBuilder.must();
        assertEquals(1, mustQueries.size());

        NestedQueryBuilder nestedQueryBuilder = (NestedQueryBuilder) mustQueries.get(0);
        NestedQueryBuilder expectedNestedQuery =
            nestedQuery(simpleNestedPath, queryStringQuery(expectedRangeSimpleNestedQueryString).allowLeadingWildcard(false), ScoreMode.Avg);

        assertEquals(expectedNestedQuery, nestedQueryBuilder);
    }

    @Test
    public void shouldReturnNestedQueryBuilderForCombinedRangeNestedQuery() {
        BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(rangeSimpleCombinedNestedQuery);
        List<QueryBuilder> mustQueries = boolQueryBuilder.must();
        assertEquals(1, mustQueries.size());

        NestedQueryBuilder nestedQueryBuilder = (NestedQueryBuilder) mustQueries.get(0);
        NestedQueryBuilder expectedNestedQuery =
            nestedQuery(simpleNestedPath, queryStringQuery(expectedRangeSimpleCombinedNestedQueryString).allowLeadingWildcard(false), ScoreMode.Avg);

        assertEquals(expectedNestedQuery, nestedQueryBuilder);
    }

    @Test
    public void shouldReturnCombinedMultilevelNestedQuery() {
        BoolQueryBuilder queryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(combinedMultilevelNestedQuery);
        List<QueryBuilder> mustQueries = queryBuilder.must();

        assertEquals(1, mustQueries.size());

        NestedQueryBuilder parentNestedQueryBuilder = (NestedQueryBuilder) mustQueries.get(0);
        BoolQueryBuilder boolNestedQueryBuilder = (BoolQueryBuilder) parentNestedQueryBuilder.query();

        List<QueryBuilder> innerShould = boolNestedQueryBuilder.should();
        assertEquals(2, innerShould.size());

        NestedQueryBuilder firstInnerNestedQuery = (NestedQueryBuilder) innerShould.get(0);
        NestedQueryBuilder secondInnerNestedQuery = (NestedQueryBuilder) innerShould.get(1);

        NestedQueryBuilder expectedFirstInnerNestedQuery =
            nestedQuery(combinedFirstInnerNested, queryStringQuery(expectedCombinedFirstInnerNestedQueryString).allowLeadingWildcard(false), ScoreMode.Avg);

        NestedQueryBuilder expectedSecondInnerNestedQuery =
            nestedQuery(combinedSecondInnerNested, queryStringQuery(expectedCombinedSecondInnerNestedQueryString).allowLeadingWildcard(false), ScoreMode.Avg);

        assertEquals(expectedFirstInnerNestedQuery, firstInnerNestedQuery);
        assertEquals(expectedSecondInnerNestedQuery, secondInnerNestedQuery);
    }

    @Test
    public void shouldReturnMultilevelNestedQuery() {
        BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(multileveledNestedQuery);
        List<QueryBuilder> must = boolQueryBuilder.must();
        assertEquals(1, must.size());
        NestedQueryBuilder firstLevel = (NestedQueryBuilder) must.get(0);

        NestedQueryBuilder expectedThirdLevel =
            nestedQuery(multilevelThirdLevelNestedPath, queryStringQuery(expectedQueryStringForMultilevelNestedQuery).allowLeadingWildcard(false),
                ScoreMode.Avg);
        NestedQueryBuilder expectedSecondLevel = nestedQuery(multilevelSecondLevelNestedPath, boolQuery().must(expectedThirdLevel), ScoreMode.Avg);
        NestedQueryBuilder expectedFirstLevel = nestedQuery(multilevelFirstLevelNestedPath, boolQuery().must(expectedSecondLevel), ScoreMode.Avg);

        assertEquals(expectedFirstLevel, firstLevel);
    }

    @Test
    public void shouldReturnMustAndMustNotQueryBuilderForQueryWithNot() {
        BoolQueryBuilder boolQueryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(combinedNestedQueryWithNot);
        List<QueryBuilder> must = boolQueryBuilder.must();

        assertEquals(1, must.size());

        NestedQueryBuilder parentNestedQueryBuilder = (NestedQueryBuilder) must.get(0);
        BoolQueryBuilder boolNestedQueryBuilder = (BoolQueryBuilder) parentNestedQueryBuilder.query();

        List<QueryBuilder> innerMust = boolNestedQueryBuilder.must();
        assertEquals(1, innerMust.size());

        List<QueryBuilder> innerMustNot = boolNestedQueryBuilder.mustNot();
        assertEquals(1, innerMustNot.size());

        NestedQueryBuilder MustInnerNestedQuery = (NestedQueryBuilder) innerMust.get(0);
        NestedQueryBuilder MustNotInnerNestedQuery = (NestedQueryBuilder) innerMustNot.get(0);

        NestedQueryBuilder expectedFirstInnerNestedQuery =
            nestedQuery(combinedFirstInnerNested, queryStringQuery(expectedCombinedFirstInnerNestedQueryString).allowLeadingWildcard(false), ScoreMode.Avg);

        NestedQueryBuilder expectedSecondInnerNestedQuery =
            nestedQuery(combinedSecondInnerNested, queryStringQuery(expectedCombinedSecondInnerNestedQueryString).allowLeadingWildcard(false), ScoreMode.Avg);

        assertEquals(expectedFirstInnerNestedQuery, MustInnerNestedQuery);
        assertEquals(expectedSecondInnerNestedQuery, MustNotInnerNestedQuery);
    }

    @Test
    public void shouldThrowAppExceptionWhenParenthesesIncomplete() {
        exceptionRule.expect(AppException.class);
        exceptionRule.expectMessage("2 of closing brackets missing");
        queryParserUtil.buildQueryBuilderFromQueryString(incompleteParenthesesQuery);
    }

    @Test
    public void shouldThrowAppExceptionWhenParenthesesMalformed() {
        exceptionRule.expect(AppException.class);
        exceptionRule.expectMessage("Malformed closing parentheses in query part:");
        exceptionRule.expectMessage("at position: 25");
        queryParserUtil.buildQueryBuilderFromQueryString(malformedParenthesesQuery);
    }

}