package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.model.http.AppException;

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
        BoolQueryBuilder queryBuilder = (BoolQueryBuilder) queryParserUtil.buildQueryBuilderFromQueryString(pair.getValue());
        JsonObject expectedQuery = getExpectedQuery(pair.getKey());
        JsonParser jsonParser = new JsonParser();
        JsonObject actualQuery = jsonParser.parse(queryBuilder.toString()).getAsJsonObject();
        assertEquals(expectedQuery, actualQuery);
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

    private JsonObject getExpectedQuery(String queryFile) {
        InputStream inStream = this.getClass().getResourceAsStream("/testqueries/expected/" + queryFile + ".json");
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(br);
        return gson.fromJson(reader, JsonObject.class);
    }

}