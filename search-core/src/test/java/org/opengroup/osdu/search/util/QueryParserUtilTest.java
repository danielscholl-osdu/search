package org.opengroup.osdu.search.util;

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
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.model.QueryNode;

import static org.junit.jupiter.api.Assertions.*;

public class QueryParserUtilTest {

    private QueryParserUtil queryParserUtil = new QueryParserUtil();

    @ParameterizedTest
    @MethodSource("validQueriesList")
    @DisplayName("Should return query builder for valid query: {0}")
    public void shouldReturnQueryBuilderForValidQueries(ImmutablePair<String, String> pair) {

            BoolQuery.Builder builder = queryParserUtil.buildQueryBuilderFromQueryString(pair.getValue());
            BoolQuery actual = builder.build();

            JsonObject expectedQuery = getExpectedQuery(pair.getKey());
            SearchRequest expected = SearchRequest.of(
                    sr -> sr.query(q -> q.withJson(new StringReader(expectedQuery.toString())))
            );
            assertEquals(expected.query().toString(), actual._toQuery().toString());
        }

    @ParameterizedTest
    @MethodSource("validQueriesList")
    @DisplayName("Should return query builder for valid query: {0}")
    public void shouldReturnQueryNodesForValidQueries(ImmutablePair<String, String> pair) {
            List<QueryNode> nodes = queryParserUtil.parseQueryNodesFromQueryString(pair.getValue());
            assertEquals(getClauseCounts(pair.getKey()), nodes.size());
    }

    @Test
    public void shouldThrowExceptionForNotValidQueries() {
        notValidQueriesList().forEach(pair -> {
            AppException ex = assertThrows(
                    AppException.class,
                    () -> queryParserUtil.buildQueryBuilderFromQueryString(pair.getValue())
            );
            assertTrue(
                    ex.getMessage().contains(pair.getKey()),
                    () -> "Expected message to contain: '" + pair.getKey() + "' but got: '" + ex.getMessage() + "'"
            );
        });
    }

    private static List<ImmutablePair<String, String>> validQueriesList() {
        InputStream in = QueryParserUtilTest.class.getResourceAsStream("/testqueries/valid-queries.json");
        return getPairsFromFile(in);
    }

    private static List<ImmutablePair<String, String>> notValidQueriesList() {
        InputStream in = QueryParserUtilTest.class.getResourceAsStream("/testqueries/not-valid-queries.json");
        return getPairsFromFile(in);
    }

    private static List<ImmutablePair<String, String>> getPairsFromFile(InputStream inStream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(br);
        Map<String, String> map = gson.fromJson(reader, Map.class);
        return map.entrySet().stream()
                .map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
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
