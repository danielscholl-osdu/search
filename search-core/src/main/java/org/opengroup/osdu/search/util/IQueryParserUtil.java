package org.opengroup.osdu.search.util;

import org.elasticsearch.index.query.QueryBuilder;

public interface IQueryParserUtil {

    QueryBuilder buildQueryBuilderFromQueryString(String query);

}
