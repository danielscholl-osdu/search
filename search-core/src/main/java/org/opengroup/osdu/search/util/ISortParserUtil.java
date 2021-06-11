package org.opengroup.osdu.search.util;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.opengroup.osdu.core.common.model.search.SortQuery;

import java.io.IOException;
import java.util.List;

public interface ISortParserUtil {

    FieldSortBuilder parseSort(String sortString, String sortOrder);

    List<FieldSortBuilder> getSortQuery(RestHighLevelClient restClient, SortQuery sortQuery, String indexPattern) throws IOException;
}
