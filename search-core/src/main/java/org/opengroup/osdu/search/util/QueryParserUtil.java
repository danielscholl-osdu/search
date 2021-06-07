package org.opengroup.osdu.search.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpStatus;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.model.NestedQueryNode;
import org.opengroup.osdu.search.model.QueryNode;
import org.opengroup.osdu.search.model.QueryNode.Operator;
import org.springframework.stereotype.Component;

@Component
public class QueryParserUtil implements IQueryParserUtil {

    public static final String OPERATOR_GROUP = "operator";
    public static final String PATH_GROUP = "path";
    public static final String PARENT_PATH_GROUP = "parentpath";
    public static final String INNER_NODES_GROUP = "innernodes";
    public static final String QUERY_GROUP = "query";
    public static final String INCOMPLETE_PATH_GROUP = "incompletepath";

    private Pattern isMultilevelNestedPattern = Pattern.compile("(nested\\()((.+?)nested\\()+");
    private Pattern multiLevelNestedPattern =
        Pattern.compile("((?<operator>AND|OR|NOT)(\\s|\\s\\())*(nested\\()(?<parentpath>.+?),\\S*(?<innernodes>.+?\\)\\)\\)+)");
    private Pattern oneLevelNestedPattern = Pattern.compile("((?<operator>AND|OR|NOT)(\\s|\\s\\())*(nested\\()(?<path>.+?),\\S*(?<query>\\s\\(.+)");
    private Pattern beginStringQueryNestedPattern = Pattern.compile("\\((?<incompletepath>\\S+?):");
    private Pattern intermediateStringQueryNestedPattern = Pattern.compile("(AND|OR|NOT)\\s(?<incompletepath>\\S+?):");
    private Pattern intermediatePattern = Pattern.compile("\\A(?<operator>AND|OR|NOT)(?<query>.+)");

    @Override
    public QueryBuilder buildQueryBuilderFromQueryString(String query) {
        List<QueryNode> queryNodes = null;
        if (query.contains("nested(")) {
            queryNodes = parseQueryNodesFromQueryString(query);
        } else {
            queryNodes = Collections.singletonList(new QueryNode(query, null));
        }
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for (QueryNode queryNode : queryNodes) {
            switch (queryNode.getOperator() != null ? queryNode.getOperator() : Operator.AND) {
                case AND:
                    boolQueryBuilder.must(queryNode.toQueryBuilder());
                    break;
                case OR:
                    boolQueryBuilder.should(queryNode.toQueryBuilder());
                    break;
                case NOT:
                    boolQueryBuilder.mustNot(queryNode.toQueryBuilder());
                    break;
            }
        }
        return boolQueryBuilder;
    }

    private List<QueryNode> parseQueryNodesFromQueryString(String queryString) {
        int height = 0;
        int position = 0;
        List<Integer> andPositions = new ArrayList();
        Pattern p = Pattern.compile("AND");
        Matcher m = p.matcher(queryString);
        while (m.find()) {
            andPositions.add(m.start());
        }
        List<Integer> orPositions = new ArrayList();
        p = Pattern.compile("OR");
        m = p.matcher(queryString);
        while (m.find()) {
            orPositions.add(m.start());
        }
        StringBuilder token = new StringBuilder();
        List<String> tokens = new ArrayList<>();
        for (char c : queryString.toCharArray()) {
            if (token.length() != 0 || c != ' ') {
                token.append(c);
            }
            if (c == '(') {
                height++;
            } else if (c == ')') {
                if (height == 1 && token.length() > 0) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                }
                height--;
                if(height < 0){
                    throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed query",
                        String.format("Malformed closing parentheses in query part: \"%s\", at position: %d", queryString,position));
                }
            } else if (height == 0 && token.length() > 0 && (andPositions.contains(position + 1) || orPositions.contains(position + 1))) {
                tokens.add(token.toString());
                token = new StringBuilder();
            }
            position++;
        }

        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        if(height > 0){
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed query",
                String.format("Malformed parentheses in query part: \"%s\", %d of closing brackets missing", queryString,height));
        }

        return transformStringTokensToQueryNode(tokens);
    }

    private List<QueryNode> transformStringTokensToQueryNode(List<String> tokens) {
        if (tokens.size() > 1) {
            tokens.set(0, defineLeadingTokenOperator(tokens.get(0), tokens.get(1)));
        }

        ArrayList<QueryNode> queryNodes = new ArrayList<>();
        for (String token : tokens) {
            Matcher isMultilevelNested = isMultilevelNestedPattern.matcher(token);
            Matcher multilevelNestedMatcher = multiLevelNestedPattern.matcher(token);
            Matcher oneLevelNestedMatcher = oneLevelNestedPattern.matcher(token);
            if (isMultilevelNested.find()) {
                multilevelNestedMatcher.find();
                NestedQueryNode nestedQueryNode = getMultilevelNestedQueryNode(multilevelNestedMatcher);
                queryNodes.add(nestedQueryNode);
            } else if (oneLevelNestedMatcher.find()) {
                NestedQueryNode nestedQueryNode = getOneLevelNestedQueryNode(oneLevelNestedMatcher);
                queryNodes.add(nestedQueryNode);
            } else {
                QueryNode simpleStringQuery = getSimpleStringQuery(token);
                queryNodes.add(simpleStringQuery);
            }
        }
        return queryNodes;
    }

    private QueryNode getSimpleStringQuery(String token) {
        Matcher intermediateMatcher = intermediatePattern.matcher(token);
        if (intermediateMatcher.find()) {
            String operator = intermediateMatcher.group(OPERATOR_GROUP);
            String queryString = intermediateMatcher.group(QUERY_GROUP);
            return new QueryNode(queryString, operator);
        } else {
            return new QueryNode(token, null);
        }
    }

    private NestedQueryNode getOneLevelNestedQueryNode(Matcher oneLevelNestedMatcher) {
        String boolOperator = oneLevelNestedMatcher.group(OPERATOR_GROUP);
        String nestedPath = oneLevelNestedMatcher.group(PATH_GROUP);
        String stringQuery = oneLevelNestedMatcher.group(QUERY_GROUP);
        Matcher beginQueryMatcher = beginStringQueryNestedPattern.matcher(stringQuery);
        if (beginQueryMatcher.find()) {
            String incompletePath = beginQueryMatcher.group(INCOMPLETE_PATH_GROUP);
            stringQuery = stringQuery.replace(incompletePath, nestedPath + "." + incompletePath);
        }
        Matcher stringQueryMatcher = intermediateStringQueryNestedPattern.matcher(stringQuery);
        while (stringQueryMatcher.find()) {
            String incompletePath = stringQueryMatcher.group(INCOMPLETE_PATH_GROUP);
            stringQuery = stringQuery.replace(incompletePath, nestedPath + "." + incompletePath);
        }
        stringQuery = trimTrailingBrackets(stringQuery);
        return new NestedQueryNode(stringQuery, boolOperator, null, nestedPath);
    }

    private NestedQueryNode getMultilevelNestedQueryNode(Matcher multilevelNestedMatcher) {
        String operand = multilevelNestedMatcher.group(OPERATOR_GROUP);
        String path = multilevelNestedMatcher.group(PARENT_PATH_GROUP);
        String innerNodes = trimTrailingBrackets(multilevelNestedMatcher.group(INNER_NODES_GROUP));
        return new NestedQueryNode(null, operand, parseQueryNodesFromQueryString(innerNodes), path);
    }

    private String defineLeadingTokenOperator(String leadingToken, String followingToken) {
        StringBuilder token = new StringBuilder(leadingToken);
        if (followingToken.startsWith(Operator.OR.getStringOperator())) {
            token.insert(0, Operator.OR.getStringOperator() + " ");
        } else {
            token.insert(0, Operator.AND.getStringOperator() + " ");
        }
        return token.toString();
    }

    private String trimTrailingBrackets(String string) {
        StringBuilder sb = new StringBuilder(string);
        int index = 0;
        int counter = 0;
        while (sb.length() > index) {
            if (sb.charAt(index) == '(') {
                counter++;
            }
            if (sb.charAt(index) == ')') {
                counter--;
            }
            while (counter < 0) {
                sb.deleteCharAt(index);
                counter++;
                index--;
            }
            index++;
        }
        return sb.toString();
    }
}
