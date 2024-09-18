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
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class NestedQueryNode extends QueryNode {

  private String path;

  private List<QueryNode> innerNodes;

  public NestedQueryNode(
      @Nullable String queryString,
      @Nullable String operand,
      @Nullable List<QueryNode> innerNodes,
      String path) {
    super(queryString, operand);
    this.innerNodes = innerNodes;
    this.path = path;
  }

  @Override
  public Query.Builder toQueryBuilder() {
    if (Objects.nonNull(innerNodes) && !innerNodes.isEmpty()) {
      BoolQuery.Builder queryBuilder = new BoolQuery.Builder();
      for (QueryNode queryNode : innerNodes) {
        Query.Builder innerBuilder = queryNode.toQueryBuilder();
        switch (queryNode.operator != null ? queryNode.operator : Operator.AND) {
          case AND:
            queryBuilder.must(innerBuilder.build());
            break;
          case OR:
            queryBuilder.should(innerBuilder.build());
            break;
          case NOT:
            queryBuilder.mustNot(innerBuilder.build());
            break;
        }
      }
      return (Query.Builder)
          new Query.Builder()
              .nested(
                  n ->
                      n.path(path)
                          .query(new Query.Builder().bool(queryBuilder.build()).build())
                          .ignoreUnmapped(true));

    } else {
      String query = StringUtils.isNotBlank(queryString) ? queryString : "*";
      QueryStringQuery.Builder queryBuilder =
          new QueryStringQuery.Builder().query(query).allowLeadingWildcard(false);
      return (Query.Builder)
          new Query.Builder()
              .nested(
                  n -> n.path(path).query(queryBuilder.build()._toQuery()).ignoreUnmapped(true));
    }
  }
}
