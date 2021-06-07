package org.opengroup.osdu.search.util;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;

public interface IAggregationParserUtil {

    AbstractAggregationBuilder parseAggregation(String aggregation);
}
