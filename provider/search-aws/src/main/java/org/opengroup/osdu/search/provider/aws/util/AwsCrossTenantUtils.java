package org.opengroup.osdu.search.provider.aws.util;

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

    @Override
    public String getIndexName(Query searchRequest) {
        StringBuilder builder = new StringBuilder();
        List<String> kinds = KindParser.parse(searchRequest.getKind());
        for(String kind : kinds) {
            String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
            String[] indexArr = index.split("-");
            indexArr[0] = dpsHeaders.getPartitionId();
            builder.append(String.join("-", indexArr) + ",");
        }
        builder.append("-.*"); // Exclude Lucene/ElasticSearch internal indices
        return builder.toString();
    }

}
