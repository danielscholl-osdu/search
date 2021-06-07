package org.opengroup.osdu.search.util;

import org.elasticsearch.search.sort.FieldSortBuilder;

public interface ISortParserUtil {

    FieldSortBuilder parseSort(String sortString, String sortOrder);
}
