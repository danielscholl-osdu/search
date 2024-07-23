package org.opengroup.osdu.search.model;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBase.AbstractBuilder;
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
  public NestedQuery.Builder toQueryBuilder() {
    AbstractBuilder queryBuilder;
    if (Objects.nonNull(innerNodes) && !innerNodes.isEmpty()) {
      queryBuilder = new BoolQuery.Builder();
      for (QueryNode queryNode : innerNodes) {
        NestedQuery.Builder innerBuilder = queryNode.toQueryBuilder();
        switch (queryNode.operator != null ? queryNode.operator : Operator.AND) {
          case AND:
            ((BoolQuery.Builder) queryBuilder).must(innerBuilder.build()._toQuery());
            break;
          case OR:
            ((BoolQuery.Builder) queryBuilder).should(innerBuilder.build()._toQuery());
            break;
          case NOT:
            ((BoolQuery.Builder) queryBuilder).mustNot(innerBuilder.build()._toQuery());
            break;
        }
      }
      return new NestedQuery.Builder()
          .path(path)
          .query(((BoolQuery.Builder) queryBuilder).build()._toQuery())
          .scoreMode(ChildScoreMode.Avg)
          .ignoreUnmapped(true);

    } else {
      String query = StringUtils.isNotBlank(queryString) ? queryString : "*";
      queryBuilder = new QueryStringQuery.Builder().query(query).allowLeadingWildcard(false);
      return new NestedQuery.Builder()
          .path(path)
          .query(((QueryStringQuery.Builder) queryBuilder).build()._toQuery())
          .scoreMode(ChildScoreMode.Avg)
          .ignoreUnmapped(true);
    }
  }
}
