package org.opengroup.osdu.search.util;

import joptsimple.internal.Strings;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.SortQuery;
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

    private Pattern oneLevelNestedSort = Pattern.compile("(nested\\()(?<path>.+?),\\s(?<field>.+?),\\s(?<mode>[^)]+)");
    private Pattern multilevelNestedPattern = Pattern.compile("(nested\\()(?<parentpath>.+?),\\s(?<innergroup>nested\\(.+)");

    @Autowired
    private IFieldMappingTypeService fieldMappingTypeService;

    @Override
    public FieldSortBuilder parseSort(String sortString, String sortOrder) {
        if (sortString.contains("nested(")) {
            return parseNestedSort(sortString, sortOrder);
        } else {
            if (sortString.equalsIgnoreCase(SCORE_FIELD)) {
                return new FieldSortBuilder(sortString)
                        .order(SortOrder.fromString(sortOrder));
            }
            return new FieldSortBuilder(sortString)
                    .order(SortOrder.fromString(sortOrder))
                    .missing("_last")
                    .unmappedType("keyword");
        }
    }

    public List<FieldSortBuilder> getSortQuery(RestHighLevelClient restClient, SortQuery sortQuery, String indexPattern) throws IOException {
        List<String> dataFields = new ArrayList<>();
        for (String field : sortQuery.getField()) {
            if (field.startsWith("data.")) dataFields.add(field + ".keyword");
        }

        if (dataFields.isEmpty()) {
            return getSortQuery(sortQuery);
        }

        Map<String, String> sortableFieldTypes = this.fieldMappingTypeService.getSortableTextFields(restClient, Strings.join(dataFields, ","), indexPattern);
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
    private List<FieldSortBuilder> getSortQuery(SortQuery sortQuery) {
        List<FieldSortBuilder> out = new ArrayList<>();

        for (int idx = 0; idx < sortQuery.getField().size(); idx++) {
            out.add(this.parseSort(sortQuery.getFieldByIndex(idx), sortQuery.getOrderByIndex(idx).name()));
        }
        return out;
    }

    private FieldSortBuilder parseNestedSort(String sortString, String sortOrder) {
        Matcher multilevelNestedMatcher = multilevelNestedPattern.matcher(sortString);
        String oneLevelSortString = sortString;
        NestedSortBuilder nestedSortBuilder = null;

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
            return new FieldSortBuilder(path + "." + field)
                    .setNestedSort(nestedSortBuilder)
                    .sortMode(SortMode.fromString(mode))
                    .order(SortOrder.fromString(sortOrder))
                    .missing("_last")
                    .unmappedType("keyword");
        }
        throw new AppException(HttpStatus.SC_BAD_REQUEST, String.format("Malformed nested sort : %s", sortString), BAD_SORT_MESSAGE);
    }

    private NestedSortBuilder parseNestedSortInDepth(String group) {
        Matcher multilevelNestedMatcher = multilevelNestedPattern.matcher(group);
        if (multilevelNestedMatcher.find()) {
            String path = multilevelNestedMatcher.group(PARENT_PATH);
            String innerGroup = multilevelNestedMatcher.group(INNER_GROUP);
            return new NestedSortBuilder(path).setNestedSort(parseNestedSortInDepth(innerGroup));
        }
        Matcher oneLevelMatcher = oneLevelNestedSort.matcher(group);
        if (oneLevelMatcher.find()) {
            String path = oneLevelMatcher.group(PATH_GROUP);
            return new NestedSortBuilder(path);
        }
        throw new AppException(HttpStatus.SC_BAD_REQUEST, String.format("Malformed nested sort group : %s", group),
                BAD_SORT_MESSAGE);
    }
}

