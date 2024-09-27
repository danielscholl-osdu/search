package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.model.QueryNode;

@RunWith(Theories.class)
public class QueryParserUtilTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private QueryParserUtil queryParserUtil = new QueryParserUtil();

    @DataPoints("VALID_QUERIES")
    public static List<ImmutablePair> validQueriesList() {
        InputStream inStream = QueryParserUtilTest.class.getResourceAsStream("/testqueries/valid-queries.json");
        return getPairsFromFile(inStream);
    }


    @DataPoints("NOT_VALID_QUERIES")
    public static List<ImmutablePair> notValidQueriesList() {
        InputStream inStream = QueryParserUtilTest.class.getResourceAsStream("/testqueries/not-valid-queries.json");
        return getPairsFromFile(inStream);
    }
    @Theory
    public void shouldReturnQueryBuilderForValidQueries(@FromDataPoints("VALID_QUERIES") ImmutablePair<String, String> pair) {
        BoolQuery.Builder queryBuilder =
        queryParserUtil.buildQueryBuilderFromQueryString(pair.getValue());
        BoolQuery actualResult = queryBuilder.build();
        JsonObject expectedQuery = getExpectedQuery(pair.getKey());
        SearchRequest expectedResult =
            SearchRequest.of(
                sr -> sr.query(q -> q.withJson(new StringReader(expectedQuery.toString()))));

        assertEquals(expectedResult.query().toString(), actualResult._toQuery().toString());
    }

    @Theory
    public void shouldReturnQueryNodesForValidQueries(@FromDataPoints("VALID_QUERIES") ImmutablePair<String, String> pair) {
        List<QueryNode> queryNodes = queryParserUtil.parseQueryNodesFromQueryString(pair.getValue());
        assertEquals(getClauseCounts(pair.getKey()), queryNodes.size());
    }

    @Theory
    public void shouldThrowExceptionForNotValidQueries(@FromDataPoints("NOT_VALID_QUERIES") ImmutablePair<String, String> pair) {
        exceptionRule.expect(AppException.class);
        exceptionRule.expectMessage(pair.getKey());
        queryParserUtil.buildQueryBuilderFromQueryString(pair.getValue());
    }

    private static List<ImmutablePair> getPairsFromFile(InputStream inStream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(br);
        Map<String, String> queriesMap = gson.fromJson(reader, Map.class);
        List<ImmutablePair> pairs =
            queriesMap.entrySet().stream().map(entry -> new ImmutablePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        return pairs;
    }

    private int getClauseCounts(String query) {
        InputStream inStream = QueryParserUtilTest.class.getResourceAsStream("/testqueries/top-level-nodes-count.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(br);
        Map<String, Double> queriesMap = gson.fromJson(reader, Map.class);
        return queriesMap.get(query).intValue();
    }

    private JsonObject getExpectedQuery(String queryFile) {
        InputStream inStream = this.getClass().getResourceAsStream("/testqueries/expected/" + queryFile + ".json");
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(br);
        return gson.fromJson(reader, JsonObject.class);
    }

}