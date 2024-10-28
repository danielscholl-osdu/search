package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.model.InnerQueryNode;
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

    /**
     * Match example:
     *  "nested(data.NestedTest, nested(...."
     */
    private static Pattern isMultilevelNestedPattern = Pattern.compile("(nested\\s?\\()((.+?)nested\\s?\\()+");

    /**
     * Match example:
     *  "AND nested(data.NestedTest, nested(data.NestedTest.NestedInnerTest, (DateTimeInnerTest:(>2024) AND NumberInnerTest:(>14))))"
     * Groups:
     *  <operator>: "AND"
     *  <parentpath>: "data.NestedTest"
     *  <innernodes>:  "nested(data.NestedTest.NestedInnerTest, (DateTimeInnerTest:(>2024) AND NumberInnerTest:(>14))))"
     */
    private static Pattern multiLevelNestedPattern = Pattern.compile("((?<operator>AND|OR|NOT)(\\s|\\s\\())*(nested\\s?\\()(?<parentpath>.+?),\\s?(?<innernodes>.+?\\)\\)\\)+)");

    /**
     * Match example:
     *  "OR nested(data.NestedTest, (StringTest:\"test*\"))"
     * Groups:
     *  <operator>: "OR"
     *  <path>: "data.NestedTest"
     *  <query>: "(StringTest:\"test*\"))"
     */

    private static Pattern oneLevelNestedPattern = Pattern.compile("((?<operator>AND|OR|NOT)(\\s|\\s\\())*(nested\\s?\\()(?<path>.+?),\\s?(?<query>\\s?\\(.+)");
    /**
     * Match example:
     *  (NumberTest:
     * Groups:
     *  <incompletepath>: NumberTest
     */

    private static Pattern beginStringQueryNestedPattern = Pattern.compile("\\((?<incompletepath>\\S+?):");
    /**
     * Match example:
     *  AND StringTest:
     * Groups:
     *  <incompletepath>: StringTest
     */
    private static Pattern intermediateStringQueryNestedPattern = Pattern.compile("(AND|OR|NOT)\\s(?<incompletepath>\\S+?):");

    /**
     * Match example:
     *   AND TEXAS
     * Groups:
     *  <operator>: AND
     *  <query>: TEXAS
     */
    private static Pattern intermediatePattern = Pattern.compile("\\A(?<operator>AND|OR|NOT)(?<query>.+)");

    /**
     * Match example:
     *   (TEXAS)
     * Groups:
     *  <operator>: null
     *  <innernodes>: TEXAS
     *
     * Match example:
     *   AND (TEXAS)
     * Groups:
     *  <operator>: AND
     *  <innernodes>: TEXAS
     */
    private static Pattern innerNodePattern = Pattern.compile("(^\\(|^(?<operator>AND|OR|NOT)\\s*\\()(?<innernodes>.+?)\\)$");

    @Override
    public BoolQuery.Builder buildQueryBuilderFromQueryString(String query) {
        List<QueryNode> queryNodes = null;
        if (query.contains("nested(") || query.contains("nested (")) {
            queryNodes = parseQueryNodesFromQueryString(query);
        } else {
            queryNodes = Collections.singletonList(new QueryNode(query, null));
        }
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.boost(1.0F);
        for (QueryNode queryNode : queryNodes) {
            switch (queryNode.getOperator() != null ? queryNode.getOperator() : Operator.AND) {
                case AND:
          boolQueryBuilder.must(queryNode.toQueryBuilder().build());
                    break;
                case OR:
                    boolQueryBuilder.should(queryNode.toQueryBuilder().build());
                    break;
                case NOT:
                    boolQueryBuilder.mustNot(queryNode.toQueryBuilder().build());
                    break;
            }
        }
        return boolQueryBuilder;
    }

    @Override
    public List<QueryNode> parseQueryNodesFromQueryString(String queryString) {
        int height = 0;
        int position = 0;
        List<Integer> andPositions = new ArrayList();
        Pattern p = Pattern.compile("[\\s)]AND[\\s(]");
        Matcher m = p.matcher(queryString);
        while (m.find()) {
            andPositions.add(m.start());
        }
        List<Integer> orPositions = new ArrayList();
        p = Pattern.compile("[\\s)]OR[\\s(]");
        m = p.matcher(queryString);
        while (m.find()) {
            orPositions.add(m.start());
        }
        StringBuilder token = new StringBuilder();
        List<String> tokens = new ArrayList<>();
        boolean doubleQuoteStarted = false;
        char[] queryChars = queryString.toCharArray();
        for (char c : queryChars) {
            if (token.length() != 0 || c != ' ') {
                token.append(c);
            }
            if(c == '"') {
                if(doubleQuoteStarted) {
                    doubleQuoteStarted = false;
                }
                else if(hasMatchDoubleQuote(queryChars, position + 1)){
                    doubleQuoteStarted = true;
                }
            }
            if(!doubleQuoteStarted) {
                if (c == '(') {
                    height++;
                } else if (c == ')') {
                    if (height == 1 && token.length() > 0) {
                        tokens.add(token.toString());
                        token = new StringBuilder();
                    }
                    height--;
                    if (height < 0) {
                        throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed query",
                                String.format("Malformed closing parentheses in query part: \"%s\", at position: %d", queryString, position));
                    }
                } else if (height == 0 && token.length() > 0 && (andPositions.contains(position + 1) || orPositions.contains(position + 1))) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                }
            }
            position++;
        }

        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        if (height > 0) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed query",
                String.format("Malformed parentheses in query part: \"%s\", %d of closing brackets missing", queryString, height));
        }

        return transformStringTokensToQueryNode(tokens);
    }

    private boolean hasMatchDoubleQuote(char[] queryChars, int from) {
        for(int i = from; i < queryChars.length; i++) {
            if(queryChars[i] == '"')
                return true;
        }
        return false;
    }

    private List<QueryNode> transformStringTokensToQueryNode(List<String> tokens) {
        if (tokens.size() > 1) {
            tokens.set(0, defineLeadingTokenOperator(tokens.get(0), tokens.get(1)));
        }

        ArrayList<QueryNode> queryNodes = new ArrayList<>();
        for (String token : tokens) {
            Matcher innerNodeMatcher = innerNodePattern.matcher(token);
            Matcher isMultilevelNested = isMultilevelNestedPattern.matcher(token);
            Matcher multilevelNestedMatcher = multiLevelNestedPattern.matcher(token);
            Matcher oneLevelNestedMatcher = oneLevelNestedPattern.matcher(token);
            if(innerNodeMatcher.find()) {
                InnerQueryNode innerQueryNode = getInnerQueryNode(innerNodeMatcher);
                queryNodes.add(innerQueryNode);
            }
            else if (isMultilevelNested.find()) {
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
            stringQuery = stringQuery.replaceFirst(incompletePath, nestedPath + "." + incompletePath);
        }
        Matcher stringQueryMatcher = intermediateStringQueryNestedPattern.matcher(stringQuery);
        while (stringQueryMatcher.find()) {
            String incompletePath = stringQueryMatcher.group(INCOMPLETE_PATH_GROUP);
            stringQuery = stringQuery.replaceFirst(" " + incompletePath, " " + nestedPath + "." + incompletePath);
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

    private InnerQueryNode getInnerQueryNode(Matcher innerNodeMatcher) {
        String operand = innerNodeMatcher.group(OPERATOR_GROUP);
        String innerNodes = innerNodeMatcher.group(INNER_NODES_GROUP);
        return new InnerQueryNode(operand, parseQueryNodesFromQueryString(innerNodes));
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
