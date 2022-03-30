package org.opengroup.osdu.search.model;

import io.micrometer.core.lang.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.List;
import java.util.Objects;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

public class InnerQueryNode extends QueryNode {
    private List<QueryNode> innerNodes;

    public InnerQueryNode(@Nullable String operand, @Nullable List<QueryNode> innerNodes) {
        super(null, operand);
        this.innerNodes = innerNodes;
    }

    @Override
    public QueryBuilder toQueryBuilder() {
        QueryBuilder queryBuilder;
        if (Objects.nonNull(innerNodes) && !innerNodes.isEmpty()) {
            queryBuilder = new BoolQueryBuilder();
            for (QueryNode queryNode : innerNodes) {
                QueryBuilder innerBuilder = queryNode.toQueryBuilder();
                switch (queryNode.operator != null ? queryNode.operator : Operator.AND) {
                    case AND:
                        ((BoolQueryBuilder) queryBuilder).must(innerBuilder);
                        break;
                    case OR:
                        ((BoolQueryBuilder) queryBuilder).should(innerBuilder);
                        break;
                    case NOT:
                        ((BoolQueryBuilder) queryBuilder).mustNot(innerBuilder);
                        break;
                }
            }
        } else {
            String query = StringUtils.isNotBlank(queryString) ? queryString : "*";
            queryBuilder = queryStringQuery(query).allowLeadingWildcard(false);
        }

        return queryBuilder;
    }
}
