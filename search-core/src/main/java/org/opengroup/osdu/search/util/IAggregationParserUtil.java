package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import java.util.Map;

public interface IAggregationParserUtil {

  Map<String, Aggregation> parseAggregation(String aggregation);
}
