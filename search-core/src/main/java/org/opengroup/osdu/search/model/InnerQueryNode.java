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

public class InnerQueryNode extends QueryNode {
  private List<QueryNode> innerNodes;

  public InnerQueryNode(@Nullable String operand, @Nullable List<QueryNode> innerNodes) {
    super(null, operand);
    this.innerNodes = innerNodes;
  }

  @Override
  public Query.Builder toQueryBuilder() {
    if (Objects.nonNull(innerNodes) && !innerNodes.isEmpty()) {
      BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
      boolQueryBuilder.boost(1.0F);
      for (QueryNode queryNode : innerNodes) {
        Query.Builder innerBuilder = queryNode.toQueryBuilder();

        switch (queryNode.operator != null ? queryNode.operator : Operator.AND) {
          case AND:
            boolQueryBuilder.must(innerBuilder.build());
            break;
          case OR:
            boolQueryBuilder.should(innerBuilder.build());
            break;
          case NOT:
            boolQueryBuilder.mustNot(innerBuilder.build());
            break;
        }
      }
      return (Query.Builder)
          new Query.Builder()
              .bool(
                  boolQueryBuilder
                      .build());
    } else {
      String query = StringUtils.isNotBlank(queryString) ? queryString : "*";
      QueryStringQuery.Builder queryStringQueryBuilder =
          QueryBuilders.queryString().query(query).allowLeadingWildcard(false);
      return (Query.Builder) new Query.Builder().queryString(queryStringQueryBuilder.build());
    }
  }
}
