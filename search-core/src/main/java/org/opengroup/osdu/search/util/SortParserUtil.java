package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.NestedSortValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.search.model.NestedQueryNode;
import org.opengroup.osdu.search.model.QueryNode;
import org.opengroup.osdu.search.service.IFieldMappingTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SortParserUtil implements ISortParserUtil {

    private static final String SCORE_FIELD = "_score";
    private static final String BAD_SORT_MESSAGE =
            "Must be in format: nested(<path>, <field>, <mode>) OR nested(<parent_path>, .....nested(<child_path>, <field>, <mode>))";
    private static final String PATH_GROUP = "path";
    private static final String FIELD_GROUP = "field";
    private static final String MODE_GROUP = "mode";
    private static final String PARENT_PATH = "parentpath";
    private static final String INNER_GROUP = "innergroup";

    private Pattern oneLevelNestedSort = Pattern.compile("(nested\\s?\\()(?<path>.+?),\\s?(?<field>.+?),\\s?(?<mode>[^)]+)");
    private Pattern multilevelNestedPattern = Pattern.compile("(nested\\s?\\()(?<parentpath>.+?),\\s?(?<innergroup>nested\\s?\\(.+)");

    @Autowired
    private IFieldMappingTypeService fieldMappingTypeService;
    
    @Autowired
    private IQueryParserUtil queryParserUtil;

    @Override
    public SortOptions parseSort(String sortString, String sortOrder, String sortFilter) {
        if (sortString.contains("nested(") || sortString.contains("nested (")) {
            return parseNestedSort(sortString, sortOrder, sortFilter);
        } else {
            if (sortString.equalsIgnoreCase(SCORE_FIELD)) {
                return new SortOptions.Builder().field(f -> f.field(sortString)
                        .order(SortOrder.valueOf(sortOrder))).build();
            }
            return new SortOptions.Builder().field(f->f.field(sortString)
                    .order(SortOrder.valueOf(sortOrder))
                    .missing("_last")
                    .unmappedType(FieldType.valueOf("keyword"))).build();
        }
    }

    public List<FieldSortBuilder> getSortQuery(ElasticsearchClient restClient, SortQuery sortQuery, String indexPattern) throws IOException {
        List<String> dataFields = new ArrayList<>();
        for (String field : sortQuery.getField()) {
            if (field.startsWith("data.")) dataFields.add(field + ".keyword");
        }

        if (dataFields.isEmpty()) {
            return getSortQuery(sortQuery);
        }

        Map<String, String> sortableFieldTypes = this.fieldMappingTypeService.getSortableTextFields(restClient, String.join(",", dataFields), indexPattern);
        List<String> sortableFields = new LinkedList<>();
        sortQuery.getField().forEach(field -> {
            if (sortableFieldTypes.containsKey(field)) {
                sortableFields.add(sortableFieldTypes.get(field));
            } else {
                sortableFields.add(field);
            }
        });
        sortQuery.setField(sortableFields);
        return getSortQuery(sortQuery);
    }

    // sort: text is not suitable for sorting or aggregation, refer to: this: https://github.com/elastic/elasticsearch/issues/28638,
    // so keyword is recommended for unmappedType in general because it can handle both string and number.
    // It will ignore the characters longer than the threshold when sorting.
    private List<SortOptions> getSortQuery(SortQuery sortQuery) {
        List<SortOptions> out = new ArrayList<>();

        for (int idx = 0; idx < sortQuery.getField().size(); idx++) {
            out.add(this.parseSort(sortQuery.getFieldByIndex(idx), sortQuery.getOrderByIndex(idx).name(), sortQuery.getFilterByIndex(idx)));
        }
        return out;
    }

    private NestedQuery.Builder buildQueryBuilderFromQueryString(String sortNestedFilter) {
        List<QueryNode> nodes = queryParserUtil.parseQueryNodesFromQueryString(sortNestedFilter);
        if (nodes.size() != 1 || !(nodes.get(0) instanceof NestedQueryNode)) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, String.format("Top level sort filter must be in nested context : %s", sortNestedFilter), BAD_SORT_MESSAGE);
        }
        NestedQueryNode topNode = (NestedQueryNode) nodes.get(0);

        return topNode.toQueryBuilder();
    }

    private SortOptions parseNestedSort(String sortString, String sortOrder, String sortNestedFilter) {
        Matcher multilevelNestedMatcher = multilevelNestedPattern.matcher(sortString);
        String oneLevelSortString = sortString;
        NestedSortValue nestedSortBuilder = null;
        QueryBuilder filterSortQuery = Objects.isNull(sortNestedFilter) ? null : buildQueryBuilderFromQueryString(sortNestedFilter);


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
                nestedSortBuilder = new NestedSortBuilder(path);
            }
            if (!Objects.isNull(filterSortQuery)) {
                nestedSortBuilder = nestedSortBuilder.setFilter(filterSortQuery);
            }
            return new FieldSortBuilder(path + "." + field)
                    .setNestedSort(nestedSortBuilder)
                    .sortMode(SortMode.fromString(mode))
                    .order(SortOrder.fromString(sortOrder))
                    .missing("_last")
                    .unmappedType("keyword");
        }
        throw new AppException(HttpStatus.SC_BAD_REQUEST, String.format("Malformed nested sort : %s", sortString), BAD_SORT_MESSAGE);
    }

    private NestedSortValue parseNestedSortInDepth(String group) {
        Matcher multilevelNestedMatcher = multilevelNestedPattern.matcher(group);
        if (multilevelNestedMatcher.find()) {
            String path = multilevelNestedMatcher.group(PARENT_PATH);
            String innerGroup = multilevelNestedMatcher.group(INNER_GROUP);
            return new NestedSortValue.Builder().path(path).nested(parseNestedSortInDepth(innerGroup)).build();
        }
        Matcher oneLevelMatcher = oneLevelNestedSort.matcher(group);
        if (oneLevelMatcher.find()) {
            String path = oneLevelMatcher.group(PATH_GROUP);
            return new NestedSortValue.Builder().path(path).build();
        }
        throw new AppException(HttpStatus.SC_BAD_REQUEST, String.format("Malformed nested sort group : %s", group),
                BAD_SORT_MESSAGE);
    }
}

