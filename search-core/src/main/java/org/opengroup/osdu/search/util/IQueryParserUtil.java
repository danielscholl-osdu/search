package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import java.util.List;
import org.opengroup.osdu.search.model.QueryNode;

public interface IQueryParserUtil {

    BoolQuery.Builder buildQueryBuilderFromQueryString(String query);

    List<QueryNode> parseQueryNodesFromQueryString(String queryString);

}
