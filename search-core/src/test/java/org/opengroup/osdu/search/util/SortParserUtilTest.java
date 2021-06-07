package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;

import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.opengroup.osdu.core.common.model.http.AppException;

public class SortParserUtilTest {

    private final String order = "ASC";
    private final String simpleSortString = "data.Data.IndividualTypeProperties.SequenceNumber";
    private final String simpleNestedSortString = "nested(data.NestedTest, NumberTest, min)";
    private final String malformedNestedSortString = "nested(data.NestedTest, )";
    private final String multilevelNestedSortString = "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, NumberInnerTest, min))";
    private final String malformedMultilevelNestedSortString = "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest,))";

    private SortParserUtil sortParserUtil = new SortParserUtil();

    @Test
    public void testSimpleSortString() {
        FieldSortBuilder actualFieldSortBuilder = sortParserUtil.parseSort(simpleSortString, order);
        FieldSortBuilder expectedSortBuilder = new FieldSortBuilder(simpleSortString)
            .order(SortOrder.valueOf(order))
            .missing("_last")
            .unmappedType("keyword");
        assertEquals(expectedSortBuilder, actualFieldSortBuilder);
    }

    @Test
    public void testSimpleNestedSortString() {
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(simpleNestedSortString, order);
        NestedSortBuilder nestedSortBuilder = new NestedSortBuilder("data.NestedTest");
        FieldSortBuilder expectedSortBuilder = new FieldSortBuilder("data.NestedTest.NumberTest")
            .order(SortOrder.valueOf(order))
            .setNestedSort(nestedSortBuilder)
            .sortMode(SortMode.fromString("min"))
            .missing("_last")
            .unmappedType("keyword");

        assertEquals(expectedSortBuilder, actualFileSortBuilder);
    }

    @Test
    public void testMultilevelNestedSortString() {
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(multilevelNestedSortString, order);

        NestedSortBuilder child = new NestedSortBuilder("data.NestedTest.NestedInnerTest");
        NestedSortBuilder parent = new NestedSortBuilder("data.NestedTest")
            .setNestedSort(child);

        FieldSortBuilder expectedSortBuilder = new FieldSortBuilder("data.NestedTest.NestedInnerTest.NumberInnerTest")
            .order(SortOrder.valueOf(order))
            .setNestedSort(parent)
            .sortMode(SortMode.fromString("min"))
            .missing("_last")
            .unmappedType("keyword");

        assertEquals(expectedSortBuilder, actualFileSortBuilder);
    }

    @Test(expected = AppException.class)
    public void testMalformedNestedSortString() {
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(malformedNestedSortString, order);
    }

    @Test(expected = AppException.class)
    public void testMalformedMultilevelNestedSortString() {
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(malformedMultilevelNestedSortString, order);
    }
}