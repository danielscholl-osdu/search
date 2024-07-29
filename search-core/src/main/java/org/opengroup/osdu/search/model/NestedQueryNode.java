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
            ((BoolQuery.Builder) queryBuilder).must(innerBuilder.build());
            break;
          case OR:
            ((BoolQuery.Builder) queryBuilder).should(innerBuilder.build());
            break;
          case NOT:
            ((BoolQuery.Builder) queryBuilder).mustNot(innerBuilder.build());
            break;
        }
      }
      NestedQuery nestedQuery =
          new NestedQuery.Builder()
              .path(path)
              .query(new Query.Builder().bool(queryBuilder.build()).build())
              .ignoreUnmapped(true).build();

      return (Query.Builder) new Query.Builder().nested(nestedQuery);

    } else {
      String query = StringUtils.isNotBlank(queryString) ? queryString : "*";
      QueryStringQuery.Builder queryBuilder =
          new QueryStringQuery.Builder().query(query).allowLeadingWildcard(false);
      NestedQuery nestedQuery =
          new NestedQuery.Builder()
              .path(path)
              .query(queryBuilder.build()._toQuery())
              .ignoreUnmapped(true)
              .build();
      return (Query.Builder) new Query.Builder().nested(nestedQuery);
    }
  }
}
