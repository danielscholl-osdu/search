package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch._types.aggregations.AggregationBase;

public interface IAggregationParserUtil {

    AggregationBase.AbstractBuilder parseAggregation(String aggregation);
}
