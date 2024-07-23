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
                      .build()); // QueryBuilders.query().bool(boolQueryBuilder.build());
    } else {
      String query = StringUtils.isNotBlank(queryString) ? queryString : "*";
      QueryStringQuery.Builder queryStringQueryBuilder =
          QueryBuilders.queryString().query(query).allowLeadingWildcard(false);
      return (Query.Builder) new Query.Builder().queryString(queryStringQueryBuilder.build());
    }
  }
}
