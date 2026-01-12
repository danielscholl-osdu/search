package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch.core.SearchRequest;

public interface IDetailedBadRequestMessageUtil {

  String getDetailedBadRequestMessage(SearchRequest searchRequest, Exception e);
}
