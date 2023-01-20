package org.opengroup.osdu.search.provider.aws.util;

import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.util.KindParser;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class AwsCrossTenantUtils extends CrossTenantUtils {

    @Inject
    DpsHeaders dpsHeaders;
    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;

    // This override method forces the kind used in the index search to use the same partition id as
    // the request's partition id header. This is to prevent data from other tenants (aka. partition)
    // to appear when searching data in a given tenant.
    @Override
    public String getIndexName(Query searchRequest) throws BadRequestException {
        StringBuilder builder = new StringBuilder();
        List<String> kinds = KindParser.parse(searchRequest.getKind());
        for(String kind : kinds) {
            String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
            String[] indexArr = index.split("-");
            if (indexArr[0] == "*") {
                indexArr[0] = dpsHeaders.getPartitionId();
            }
            else if (indexArr[0].equalsIgnoreCase(dpsHeaders.getPartitionId()) == false) {
                throw new BadRequestException("Invalid kind in search request.");
            }
            builder.append(String.join("-", indexArr) + ",");
        }
        builder.append("-.*"); // Exclude Lucene/ElasticSearch internal indices
        return builder.toString();
    }

}
