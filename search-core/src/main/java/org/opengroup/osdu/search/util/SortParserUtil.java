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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.search.model.NestedQueryNode;
import org.opengroup.osdu.search.model.QueryNode;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SortParserUtil implements ISortParserUtil {

  private static final String SCORE_FIELD = "_score";
  private static final String BAD_SORT_MESSAGE =
      "Must be in format: nested(<path>, <field>, <mode>) OR nested(<parent_path>, .....nested(<child_path>, <field>, <mode>))";
  private static final String PATH_GROUP = "path";
  private static final String FIELD_GROUP = "field";
  private static final String MODE_GROUP = "mode";
  private static final String PARENT_PATH = "parentpath";
  private static final String INNER_GROUP = "innergroup";

  private Pattern oneLevelNestedSort =
      Pattern.compile("(nested\\s?\\()(?<path>.+?),\\s?(?<field>.+?),\\s?(?<mode>[^)]+)");
  private Pattern multilevelNestedPattern =
      Pattern.compile("(nested\\s?\\()(?<parentpath>.+?),\\s?(?<innergroup>nested\\s?\\(.+)");

  private final IFieldMappingTypeService fieldMappingTypeService;
  private final IQueryParserUtil queryParserUtil;

  @Override
  public SortOptions parseSort(String sortString, String sortOrder, String sortFilter) {
    if (sortString.contains("nested(") || sortString.contains("nested (")) {
      return parseNestedSort(sortString, sortOrder, sortFilter);
    } else {
      if (sortString.equalsIgnoreCase(SCORE_FIELD)) {
        return new SortOptions.Builder()
            .field(f -> f.field(sortString).order(SortOrder.valueOf(sortOrder)))
            .build();
      }
      return new SortOptions.Builder()
          .field(
              f ->
                  f.field(sortString)
                      .order(
                          SortOrder.Asc.jsonValue().equalsIgnoreCase(sortOrder)
                              ? SortOrder.Asc
                              : SortOrder.Desc)
                      .missing("_last")
                      .unmappedType(FieldType.Keyword))
          .build();
    }
  }

  public List<SortOptions> getSortQuery(
      ElasticsearchClient restClient, SortQuery sortQuery, String indexPattern) throws IOException {
    List<String> dataFields = new ArrayList<>();
    for (String field : sortQuery.getField()) {
      if (field.startsWith("data.")) dataFields.add(field + ".keyword");
    }

    if (dataFields.isEmpty()) {
      return getSortQuery(sortQuery);
    }

    Map<String, String> sortableFieldTypes =
        this.fieldMappingTypeService.getSortableTextFields(
            restClient, String.join(",", dataFields), indexPattern);
    List<String> sortableFields = new LinkedList<>();
    sortQuery
        .getField()
        .forEach(
            field -> {
              if (sortableFieldTypes.containsKey(field)) {
                sortableFields.add(sortableFieldTypes.get(field));
              } else {
                sortableFields.add(field);
              }
            });
    sortQuery.setField(sortableFields);
    return getSortQuery(sortQuery);
  }

  // sort: text is not suitable for sorting or aggregation, refer to: this:
  // https://github.com/elastic/elasticsearch/issues/28638,
  // so keyword is recommended for unmappedType in general because it can handle both string and
  // number.
  // It will ignore the characters longer than the threshold when sorting.
  private List<SortOptions> getSortQuery(SortQuery sortQuery) {
    List<SortOptions> out = new ArrayList<>();

    for (int idx = 0; idx < sortQuery.getField().size(); idx++) {
      out.add(
          this.parseSort(
              sortQuery.getFieldByIndex(idx),
              sortQuery.getOrderByIndex(idx).name(),
              sortQuery.getFilterByIndex(idx)));
    }
    return out;
  }

  private Query.Builder buildQueryBuilderFromQueryString(String sortNestedFilter) {
    List<QueryNode> nodes = queryParserUtil.parseQueryNodesFromQueryString(sortNestedFilter);
    if (nodes.size() != 1 || !(nodes.get(0) instanceof NestedQueryNode)) {
      throw new AppException(
          HttpStatus.SC_BAD_REQUEST,
          String.format("Top level sort filter must be in nested context : %s", sortNestedFilter),
          BAD_SORT_MESSAGE);
    }
    NestedQueryNode topNode = (NestedQueryNode) nodes.get(0);

    return topNode.toQueryBuilder();
  }

  private SortOptions parseNestedSort(
      String sortString, String sortOrder, String sortNestedFilter) {

    Matcher multilevelNestedMatcher = multilevelNestedPattern.matcher(sortString);
    String oneLevelSortString = sortString;
    NestedSortValue.Builder nestedSortBuilder = null;

    Query.Builder filterSortQuery =
        Objects.isNull(sortNestedFilter)
            ? null
            : buildQueryBuilderFromQueryString(sortNestedFilter);

    if (multilevelNestedMatcher.find()) {

      nestedSortBuilder = parseNestedSortInDepth(sortString);
      multilevelNestedMatcher.reset(sortString);

      while (multilevelNestedMatcher.find()) {
        String innerGroup = multilevelNestedMatcher.group(INNER_GROUP);
        oneLevelSortString = innerGroup;
        multilevelNestedMatcher.reset(oneLevelSortString);
      }
    }

    Matcher oneLevelNestedMatcher = oneLevelNestedSort.matcher(oneLevelSortString);
    if (oneLevelNestedMatcher.find()) {

      String path = oneLevelNestedMatcher.group(PATH_GROUP);
      String field = oneLevelNestedMatcher.group(FIELD_GROUP);
      String mode = oneLevelNestedMatcher.group(MODE_GROUP);

      if (Objects.isNull(nestedSortBuilder)) {
        nestedSortBuilder = new NestedSortValue.Builder();
        nestedSortBuilder.path(path);
      }

      if (filterSortQuery != null) {
        nestedSortBuilder.filter(filterSortQuery.build().nested().query());
      }

      FieldSort.Builder fieldSort =
          new FieldSort.Builder()
              .field(path + "." + field)
              .nested(nestedSortBuilder.build())
              .mode(SortMode.valueOf(capitalizeFirstLetter(mode)))
              .order(SortOrder.valueOf(capitalizeFirstLetter(sortOrder)))
              .missing("_last")
              .unmappedType(FieldType.Keyword);

      return SortOptions.of(s -> s.field(fieldSort.build()));
    }

    throw new AppException(
        HttpStatus.SC_BAD_REQUEST,
        "Malformed nested sort : %s".formatted(sortString),
        BAD_SORT_MESSAGE);
  }

  private NestedSortValue.Builder parseNestedSortInDepth(String group) {
    Matcher multilevelNestedMatcher = multilevelNestedPattern.matcher(group);

    if (multilevelNestedMatcher.find()) {
      String path = multilevelNestedMatcher.group(PARENT_PATH);
      String innerGroup = multilevelNestedMatcher.group(INNER_GROUP);

      return new NestedSortValue.Builder()
          .path(path)
          .nested(parseNestedSortInDepth(innerGroup).build());
    }

    Matcher oneLevelMatcher = oneLevelNestedSort.matcher(group);
    if (oneLevelMatcher.find()) {
      String path = oneLevelMatcher.group(PATH_GROUP);
      return new NestedSortValue.Builder().path(path);
    }

    throw new AppException(
        HttpStatus.SC_BAD_REQUEST,
        "Malformed nested sort group : %s".formatted(group),
        BAD_SORT_MESSAGE);
  }

  private static String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }
}
