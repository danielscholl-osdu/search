package org.opengroup.osdu.search.util;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilder;
import org.opengroup.osdu.search.model.QueryNode;

public interface IQueryParserUtil {

    QueryBuilder buildQueryBuilderFromQueryString(String query);

    List<QueryNode> parseQueryNodesFromQueryString(String queryString);

}
