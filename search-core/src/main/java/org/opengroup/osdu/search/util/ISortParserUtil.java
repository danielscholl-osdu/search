package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import java.io.IOException;
import java.util.List;
import org.opengroup.osdu.core.common.model.search.SortQuery;

public interface ISortParserUtil {

    SortOptions parseSort(String sortString, String sortOrder, String sortFilter);

    List<SortOptions> getSortQuery(ElasticsearchClient restClient, SortQuery sortQuery, String indexPattern) throws IOException;
}
