package org.opengroup.osdu.search.provider.aws.util;

import com.google.api.client.util.Strings;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.service.IndexAliasService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.util.KindParser;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AwsCrossTenantUtils extends CrossTenantUtils {
    // For details, please refer to implementation of class
    // org.opengroup.osdu.core.common.model.search.validation.MultiKindValidator
    private static final int MAX_INDEX_NAME_LENGTH = 3840;

    @Inject
    private DpsHeaders dpsHeaders;
    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private IndexAliasService indexAliasService;

    // This override method forces the kind used in the index search to use the same partition id as
    // the request's partition id header. This is to prevent data from other tenants (aka. partition)
    // to appear when searching data in a given tenant.
    @Override
    public String getIndexName(Query searchRequest) throws BadRequestException {
        List<String> kinds = KindParser.parse(searchRequest.getKind());
        String index = getIndexName(kinds, new HashMap<>());
        if(index.length() <= MAX_INDEX_NAME_LENGTH) {
            return index;
        }
        else {
            Map<String, String> aliases = this.indexAliasService.getIndicesAliases(kinds);
            return getIndexName(kinds, aliases);
        }
    }

    private String getIndexName(List<String> kinds, Map<String, String> aliases) {
        StringBuilder builder = new StringBuilder();
        for(String kind : kinds) {
            if(aliases.containsKey(kind) && !Strings.isNullOrEmpty(aliases.get(kind))) {
                String alias = aliases.get(kind);
                builder.append(alias);
            }
            else {
                String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
                String[] indexArr = index.split("-");
                if (indexArr[0].equalsIgnoreCase("*")) {
                    indexArr[0] = dpsHeaders.getPartitionId();
                }
                else if (indexArr[0].equalsIgnoreCase(dpsHeaders.getPartitionId()) == false) {
                    throw new BadRequestException("Invalid kind in search request.");
                }
                builder.append(String.join("-", indexArr));
            }
            builder.append(",");
        }
        builder.append("-.*"); // Exclude Lucene/ElasticSearch internal indices
        return builder.toString();
    }
}
