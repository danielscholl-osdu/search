/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.search.model.QueryNode;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;

@ExtendWith(MockitoExtension.class)
public class SortParserUtilTest {

  private static final String ORDER = "ASC";
  private static final String SIMPLE_SORT_STRING =
      "data.Data.IndividualTypeProperties.SequenceNumber";
  private static final String SIMPLE_NESTED_SORT_STRING =
      "nested(data.NestedTest, NumberTest, min)";
  private static final String MALFORMED_NESTED_SORT_STRING = "nested(data.NestedTest, )";
  private static final String MALFORMED_MULTILEVEL_NESTED_SORT_STRING =
      "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest,))";
  private static final String MULTILEVEL_NESTED_SORT_STRING =
      "nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, NumberInnerTest, min))";
  private static final String MULTILEVEL_NESTED_SORT_STRING_WITH_SPACE =
      "nested (data.NestedTest,nested (data.NestedTest.NestedInnerTest,NumberInnerTest,min))";
  private static final String SIMPLE_SORT_FILTER =
      "nested(data.NestedTest, (InnerTestField:SomeValue))";
  private static final String NO_NESTED_BLOCK_FILTER = "simple string";
  private static final String topLevelOperatorFilter =
      "nested(data.NestedTest, (InnerTestField:SomeValue)) OR nested(data.NestedTest, (InnerTestField2:SomeValue))";
  private static final String LAST = "_last";

  @Mock private IFieldMappingTypeService fieldMappingTypeService;
  @Mock private IQueryParserUtil queryParserUtil;
  @InjectMocks private SortParserUtil sut;

  private SortParserUtil sortParserUtil =
      new SortParserUtil(fieldMappingTypeService, queryParserUtil);

  @Test
  public void testSimpleSortString() {
    SortOptions actualFieldSort = sortParserUtil.parseSort(SIMPLE_SORT_STRING, ORDER, null);

    SortOptions expectedFieldSort =
        new SortOptions.Builder()
            .field(
                f ->
                    f.field(SIMPLE_SORT_STRING)
                        .order(SortOrder.Asc)
                        .missing(LAST)
                        .unmappedType(FieldType.Keyword))
            .build();

    assertEquals(expectedFieldSort.toString(), actualFieldSort.toString());
  }

  @Test
  public void testScoreSortString() {
    String SCORE_FIELD = "_score";
    SortOptions actualFieldSort = sortParserUtil.parseSort(SCORE_FIELD, ORDER, null);

    SortOptions expectedFieldSort =
        new SortOptions.Builder().field(f -> f.field(SCORE_FIELD).order(SortOrder.Asc)).build();

    assertEquals(expectedFieldSort.toString(), actualFieldSort.toString());
  }

  @Test
  public void testSimpleNestedSortString() {
    SortOptions actualFieldSort = sortParserUtil.parseSort(SIMPLE_NESTED_SORT_STRING, ORDER, null);

    NestedSortValue.Builder nestedSortBuilder = new NestedSortValue.Builder();
    nestedSortBuilder.path("data.NestedTest");

    FieldSort.Builder fieldSort =
        new FieldSort.Builder()
            .field("data.NestedTest.NumberTest")
            .nested(nestedSortBuilder.build())
            .mode(SortMode.Min)
            .order(SortOrder.Asc)
            .missing(LAST)
            .unmappedType(FieldType.Keyword);
    SortOptions expectedFieldSort = SortOptions.of(s -> s.field(fieldSort.build()));

    assertEquals(expectedFieldSort.toString(), actualFieldSort.toString());
  }

  @Test
  public void testSimpleNestedSortStringWithFilter() {
    QueryParserUtil qUtil = new QueryParserUtil();
    List<QueryNode> nodes = qUtil.parseQueryNodesFromQueryString(SIMPLE_SORT_FILTER);
    when(this.queryParserUtil.parseQueryNodesFromQueryString(SIMPLE_SORT_FILTER)).thenReturn(nodes);

    SortOptions actualFileSort =
        this.sut.parseSort(SIMPLE_NESTED_SORT_STRING, ORDER, SIMPLE_SORT_FILTER);

    NestedSortValue.Builder nestedSortBuilder = new NestedSortValue.Builder();
    nestedSortBuilder
        .path("data.NestedTest")
        .filter(nodes.get(0).toQueryBuilder().build().nested().query());

    FieldSort.Builder fieldSort =
        new FieldSort.Builder()
            .field("data.NestedTest.NumberTest")
            .order(SortOrder.Asc)
            .nested(nestedSortBuilder.build())
            .mode(SortMode.Min)
            .missing(LAST)
            .unmappedType(FieldType.Keyword);
    SortOptions expectedFieldSort = SortOptions.of(s -> s.field(fieldSort.build()));

    assertEquals(expectedFieldSort.toString(), actualFileSort.toString());
  }

  @Test
  public void testMultilevelNestedSortString() {
    SortOptions actualFileSort =
        sortParserUtil.parseSort(MULTILEVEL_NESTED_SORT_STRING, ORDER, null);

    NestedSortValue.Builder child = new NestedSortValue.Builder();
    child.path("data.NestedTest.NestedInnerTest");
    NestedSortValue.Builder parent = new NestedSortValue.Builder();
    parent.path("data.NestedTest").nested(child.build());

    FieldSort.Builder fieldSort = new FieldSort.Builder();
    fieldSort
        .field("data.NestedTest.NestedInnerTest.NumberInnerTest")
        .order(SortOrder.Asc)
        .nested(parent.build())
        .mode(SortMode.Min)
        .missing(LAST)
        .unmappedType(FieldType.Keyword);
    SortOptions expectedFieldSort = SortOptions.of(s -> s.field(fieldSort.build()));

    assertEquals(actualFileSort.toString(), expectedFieldSort.toString());
  }

  @Test
  public void testMultilevelNestedSortStringWithSpace() {
    SortOptions actualFileSort =
        sortParserUtil.parseSort(MULTILEVEL_NESTED_SORT_STRING_WITH_SPACE, ORDER, null);

    NestedSortValue.Builder child = new NestedSortValue.Builder();
    child.path("data.NestedTest.NestedInnerTest");
    NestedSortValue.Builder parent = new NestedSortValue.Builder();
    parent.path("data.NestedTest").nested(child.build());

    FieldSort.Builder fieldSort = new FieldSort.Builder();
    fieldSort
        .field("data.NestedTest.NestedInnerTest.NumberInnerTest")
        .order(SortOrder.Asc)
        .nested(parent.build())
        .mode(SortMode.Min)
        .missing(LAST)
        .unmappedType(FieldType.Keyword);
    SortOptions expectedFieldSort = SortOptions.of(s -> s.field(fieldSort.build()));

    assertEquals(expectedFieldSort.toString(), actualFileSort.toString());
  }

  @Test
  public void testMalformedNestedSortString() {
    assertThrows(AppException.class, () -> {
      sortParserUtil.parseSort(MALFORMED_NESTED_SORT_STRING, ORDER, null);
    });
  }

  @Test
  public void testMalformedMultilevelNestedSortString() {
    assertThrows(AppException.class, () -> {
      sortParserUtil.parseSort(MALFORMED_MULTILEVEL_NESTED_SORT_STRING, ORDER, null);
    });
  }

  @Test
  public void testSortFilterWithoutNestedContext() {
    assertThrows(AppException.class, () -> {
      this.sut.parseSort(SIMPLE_NESTED_SORT_STRING, ORDER, NO_NESTED_BLOCK_FILTER);
    });
  }

  @Test
  public void testSortFilterTopLevelOperator() {
    assertThrows(AppException.class, () -> {
      this.sut.parseSort(SIMPLE_NESTED_SORT_STRING, ORDER, topLevelOperatorFilter);
    });
  }

  @Test
  public void should_return_validSortQuery_given_sortFields() throws IOException {
    ElasticsearchClient restClient = mock(ElasticsearchClient.class);

    SortQuery sort = new SortQuery();
    List<String> sortFields = new ArrayList<>();
    sortFields.add("id");
    sortFields.add("namespace");
    sort.setField(sortFields);
    List<org.opengroup.osdu.core.common.model.search.SortOrder> sortOrders = new ArrayList<>();
    sortOrders.add(org.opengroup.osdu.core.common.model.search.SortOrder.ASC);
    sortOrders.add(org.opengroup.osdu.core.common.model.search.SortOrder.DESC);
    sort.setOrder(sortOrders);

    List<SortOptions> sortQuery =
        this.sut.getSortQuery(
            restClient, sort, "osdu:wks:work-product-component--wellboremarkerset:1.0.0");

    assertNotNull(sortQuery);
    assertEquals(2, sortQuery.size());
  }

  @Test
  public void should_return_validSortQuery_given_dataSortFields() throws IOException {
    ElasticsearchClient restClient = mock(ElasticsearchClient.class);

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
    when(this.fieldMappingTypeService.getSortableTextFields(any(), any(), any()))
        .thenReturn(keywordMap);

    List<SortOptions> sortQuery =
        this.sut.getSortQuery(
            restClient, sort, "osdu:wks:work-product-component--wellboremarkerset:1.0.0");

    assertNotNull(sortQuery);
    assertEquals(2, sortQuery.size());
    assertEquals("data.Country.keyword", sortQuery.get(0).field().field());
    assertEquals("data.ProductionRate", sortQuery.get(1).field().field());
  }
}
