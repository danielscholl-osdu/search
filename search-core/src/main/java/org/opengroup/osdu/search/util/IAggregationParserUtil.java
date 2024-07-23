package org.opengroup.osdu.search.util;

import co.elastic.clients.elasticsearch._types.aggregations.AggregationBase;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;

public interface IAggregationParserUtil {

    AggregationBase.AbstractBuilder parseAggregation(String aggregation);
}
