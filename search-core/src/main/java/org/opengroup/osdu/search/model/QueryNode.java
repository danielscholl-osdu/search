package org.opengroup.osdu.search.model;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class QueryNode {

  protected String queryString;

  protected Operator operator;

  public QueryNode(String queryString, @Nullable String operator) {
    this.queryString = StringUtils.trimLeadingCharacter(queryString, ' ');
    this.operator = Operator.fromValue(operator);
  }

  //  public AbstractBuilder toQueryBuilder() {
  //    String query = isNotBlank(queryString) ? queryString : "*";
  //    return new QueryStringQuery.Builder().query(query).allowLeadingWildcard(false);
  //  }

  public Query.Builder toQueryBuilder() {
    String query = (queryString != null && !queryString.trim().isEmpty()) ? queryString : "*";
    QueryStringQuery.Builder queryStringQuery =
        new QueryStringQuery.Builder()
            .query(query)
            .allowLeadingWildcard(false)
            .type(TextQueryType.BestFields)
            .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
            .maxDeterminizedStates(1000)
            .allowLeadingWildcard(false)
            .enablePositionIncrements(true)
            .fuzziness("AUTO")
            .fuzzyPrefixLength(0)
            .fuzzyMaxExpansions(50)
            .phraseSlop(0.0)
            .escape(false)
            .autoGenerateSynonymsPhraseQuery(true)
            .fuzzyTranspositions(true)
            .boost(1.0f);

    return (Query.Builder) new Query.Builder().queryString(queryStringQuery.build());
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

  private static boolean isNotBlank(String str) {
    return str != null && !str.isBlank();
  }
}
