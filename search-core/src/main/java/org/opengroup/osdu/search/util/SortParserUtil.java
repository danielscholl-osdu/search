package org.opengroup.osdu.search.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpStatus;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.stereotype.Component;

@Component
public class SortParserUtil implements ISortParserUtil {

    private static final String BAD_SORT_MESSAGE =
        "Must be in format: nested(<path>, <field>, <mode>) OR nested(<parent_path>, .....nested(<child_path>, <field>, <mode>))";
    private static final String PATH_GROUP = "path";
    private static final String FIELD_GROUP = "field";
    private static final String MODE_GROUP = "mode";
    private static final String PARENT_PATH = "parentpath";
    private static final String INNER_GROUP = "innergroup";

    private Pattern oneLevelNestedSort = Pattern.compile("(nested\\()(?<path>.+?),\\s(?<field>.+?),\\s(?<mode>[^)]+)");
    private Pattern multilevelNestedPattern = Pattern.compile("(nested\\()(?<parentpath>.+?),\\s(?<innergroup>nested\\(.+)");

    @Override
    public FieldSortBuilder parseSort(String sortString, String sortOrder) {
        if (sortString.contains("nested(")) {
            return parseNestedSort(sortString, sortOrder);
        } else {
            return new FieldSortBuilder(sortString)
                .order(SortOrder.fromString(sortOrder))
                .missing("_last")
                .unmappedType("keyword");
        }
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

