package org.opengroup.osdu.search.util;

import org.elasticsearch.action.search.SearchRequest;

public interface IDetailedBadRequestMessageUtil {

    String getDetailedBadRequestMessage(SearchRequest searchRequest, Exception e);
}
