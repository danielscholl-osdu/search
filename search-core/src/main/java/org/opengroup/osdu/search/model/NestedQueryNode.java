package org.opengroup.osdu.search.model;

import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

public class NestedQueryNode extends QueryNode {

    private String path;

    private List<QueryNode> innerNodes;

    public NestedQueryNode(@Nullable String queryString, @Nullable String operand, @Nullable List<QueryNode> innerNodes, String path) {
        super(queryString, operand);
        this.innerNodes = innerNodes;
        this.path = path;
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
        return nestedQuery(path, queryBuilder, ScoreMode.Avg).ignoreUnmapped(true);
    }
}
