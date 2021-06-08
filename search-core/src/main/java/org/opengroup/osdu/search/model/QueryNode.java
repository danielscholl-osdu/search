package org.opengroup.osdu.search.model;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilder;

@Getter
@Setter
public class QueryNode {

    protected String queryString;

    protected Operator operator;

    public QueryNode(String queryString, @Nullable String operator) {
        this.queryString = Strings.trimLeadingCharacter(queryString,' ');
        this.operator = Operator.fromValue(operator);
    }

    public QueryBuilder toQueryBuilder() {
        String query = StringUtils.isNotBlank(queryString) ? queryString : "*";
        return queryStringQuery(query).allowLeadingWildcard(false);
    }

    public enum Operator {
        OR("OR"),
        AND("AND"),
        NOT("NOT");
        private String stringOperator;

        Operator(String operator) {
            this.stringOperator = operator;
        }

        public static Operator fromValue(String stringOperator) {
            for (Operator operator : Operator.values()) {
                if (operator.stringOperator.equalsIgnoreCase(stringOperator)) {
                    return operator;
                }
            }
            return null;
        }

        public String getStringOperator() {
            return stringOperator;
        }
    }

}
