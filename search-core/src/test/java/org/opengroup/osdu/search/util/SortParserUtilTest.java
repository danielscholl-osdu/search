package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.search.model.QueryNode;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SortParserUtilTest {

    private final String order = "ASC";
    private final String simpleSortString = "data.Data.IndividualTypeProperties.SequenceNumber";
    private final String simpleNestedSortString = "nested(data.NestedTest, NumberTest, min)";
    private final String malformedNestedSortString = "nested(data.NestedTest, )";
    private final String malformedMultilevelNestedSortString = "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest,))";
    private final String multilevelNestedSortString = "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, NumberInnerTest, min))";
    private final String multilevelNestedSortStringWithSpace = "nested (data.NestedTest,nested (data.NestedTest.NestedInnerTest,NumberInnerTest,min))";
    private final String simpleSortFilter = "nested(data.NestedTest, (InnerTestField:SomeValue))";
    private final String noNestedBlockFilter = "simple string";
    private final String topLevelOperatorFilter = "nested(data.NestedTest, (InnerTestField:SomeValue)) OR nested(data.NestedTest, (InnerTestField2:SomeValue))";

    private SortParserUtil sortParserUtil = new SortParserUtil();

    @Mock
    private IFieldMappingTypeService fieldMappingTypeService;
    @Mock
    private IQueryParserUtil queryParserUtil;
    @InjectMocks
    private SortParserUtil sut;

    @Test
    public void testSimpleSortString() {
        FieldSortBuilder actualFieldSortBuilder = sortParserUtil.parseSort(simpleSortString, order, null);
        FieldSortBuilder expectedSortBuilder = new FieldSortBuilder(simpleSortString)
            .order(SortOrder.valueOf(order))
            .missing("_last")
            .unmappedType("keyword");
        assertEquals(expectedSortBuilder, actualFieldSortBuilder);
    }

    @Test
    public void testScoreSortString() {
        String SCORE_FIELD = "_score";
        FieldSortBuilder actualFieldSortBuilder = sortParserUtil.parseSort(SCORE_FIELD, order, null);
        FieldSortBuilder expectedSortBuilder = new FieldSortBuilder(SCORE_FIELD)
                .order(SortOrder.valueOf(order));
        assertEquals(expectedSortBuilder, actualFieldSortBuilder);
    }

    @Test
    public void testSimpleNestedSortString() {
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(simpleNestedSortString, order, null);
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
    public void testSimpleNestedSortStringWithFilter() {
        QueryParserUtil qUtil = new QueryParserUtil();
        List<QueryNode> nodes = qUtil.parseQueryNodesFromQueryString(simpleSortFilter);
        when(this.queryParserUtil.parseQueryNodesFromQueryString(simpleSortFilter)).thenReturn(nodes);
        FieldSortBuilder actualFileSortBuilder = this.sut.parseSort(simpleNestedSortString, order, simpleSortFilter);
        NestedSortBuilder nestedSortBuilder = new NestedSortBuilder("data.NestedTest").setFilter(((NestedQueryBuilder) nodes.get(0).toQueryBuilder()).query());
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
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(multilevelNestedSortString, order, null);

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

    @Test
    public void testMultilevelNestedSortStringWithSpace() {
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(multilevelNestedSortStringWithSpace, order, null);

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
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(malformedNestedSortString, order, null);
    }

    @Test(expected = AppException.class)
    public void testMalformedMultilevelNestedSortString() {
        FieldSortBuilder actualFileSortBuilder = sortParserUtil.parseSort(malformedMultilevelNestedSortString, order, null);
    }

    @Test(expected = AppException.class)
    public void testSortFilterWithoutNestedContext() {
        QueryParserUtil qUtil = new QueryParserUtil();
        List<QueryNode> nodes = qUtil.parseQueryNodesFromQueryString(simpleSortFilter);
        when(this.queryParserUtil.parseQueryNodesFromQueryString(simpleSortFilter)).thenReturn(nodes);
        FieldSortBuilder actualFileSortBuilder = this.sut.parseSort(simpleNestedSortString, order, noNestedBlockFilter);
    }

    @Test(expected = AppException.class)
    public void testSortFilterTopLevelOperator() {
        QueryParserUtil qUtil = new QueryParserUtil();
        List<QueryNode> nodes = qUtil.parseQueryNodesFromQueryString(simpleSortFilter);
        System.out.println(nodes);
        when(this.queryParserUtil.parseQueryNodesFromQueryString(simpleSortFilter)).thenReturn(nodes);
        FieldSortBuilder actualFileSortBuilder = this.sut.parseSort(simpleNestedSortString, order, topLevelOperatorFilter);
    }

    @Test
    public void should_return_validSortQuery_given_sortFields() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);

        SortQuery sort = new SortQuery();
        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        sortFields.add("namespace");
        sort.setField(sortFields);
        List<org.opengroup.osdu.core.common.model.search.SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(org.opengroup.osdu.core.common.model.search.SortOrder.ASC);
        sortOrders.add(org.opengroup.osdu.core.common.model.search.SortOrder.DESC);
        sort.setOrder(sortOrders);

        List<FieldSortBuilder> sortQuery = this.sut.getSortQuery(restClient, sort, "osdu:wks:work-product-component--wellboremarkerset:1.0.0");
        assertNotNull(sortQuery);
        assertEquals(2, sortQuery.size());
    }

    @Test
    public void should_return_validSortQuery_given_dataSortFields() throws IOException {
        RestHighLevelClient restClient = mock(RestHighLevelClient.class);

        SortQuery sort = new SortQuery();
        List<String> sortFields = new ArrayList<>();
        sortFields.add("data.Country");
        sortFields.add("data.ProductionRate");
        sort.setField(sortFields);
        List<org.opengroup.osdu.core.common.model.search.SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(org.opengroup.osdu.core.common.model.search.SortOrder.ASC);
        sortOrders.add(org.opengroup.osdu.core.common.model.search.SortOrder.DESC);
        sort.setOrder(sortOrders);

        Map<String, String> keywordMap = new HashMap<>();
        keywordMap.put("data.Country", "data.Country.keyword");
        when(this.fieldMappingTypeService.getSortableTextFields(any(), any(), any())).thenReturn(keywordMap);

        List<FieldSortBuilder> sortQuery = this.sut.getSortQuery(restClient, sort, "osdu:wks:work-product-component--wellboremarkerset:1.0.0");
        assertNotNull(sortQuery);
        assertEquals(2, sortQuery.size());
        assertEquals("data.Country.keyword", sortQuery.get(0).getFieldName());
        assertEquals("data.ProductionRate", sortQuery.get(1).getFieldName());
    }
}
