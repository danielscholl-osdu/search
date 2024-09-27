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

package org.opengroup.osdu.search.model;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

@Getter
@Setter
public class QueryNode {

  protected String queryString;

  protected Operator operator;

  public QueryNode(String queryString, @Nullable String operator) {
    this.queryString = StringUtils.trimLeadingCharacter(queryString, ' ');
    this.operator = Operator.fromValue(operator);
  }

  public Query.Builder toQueryBuilder() {
    String query = (queryString != null && !queryString.trim().isEmpty()) ? queryString : "*";

    // the default set of values is the same as on the old Elasticsearch client
    QueryStringQuery.Builder queryStringQuery =
        new QueryStringQuery.Builder()
            .query(query)
            .allowLeadingWildcard(false)
            .fields(new ArrayList<>())
            .type(TextQueryType.BestFields)
            .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
            .maxDeterminizedStates(10000)
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
}
