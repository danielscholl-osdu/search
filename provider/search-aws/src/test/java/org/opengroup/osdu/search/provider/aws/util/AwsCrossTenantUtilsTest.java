package org.opengroup.osdu.search.provider.aws.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.util.KindParser;
import org.opengroup.osdu.search.service.IndexAliasService;
import org.springframework.security.core.parameters.P;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AwsCrossTenantUtilsTest {

    @InjectMocks
    AwsCrossTenantUtils awsCrossTenantUtils;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;

    @Mock
    private IndexAliasService indexAliasService;

    @Mock
    private Query searchRequest;

    @Test
    public void should_return_IndexName_when_partitionIdAndKindMatches() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:*:*:*";
        String INDEX = KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s,%s", INDEX, "-.*");
        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_IndexNameWithHyphen_when_partitionIdAndKindMatches() {
        String DATA_PARTITION_ID = "tenant-1";
        String KIND = "tenant-1:*:*:*";
        String INDEX = KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s,%s", INDEX, "-.*");
        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_alias_when_searchingHundredSameKindsAndKindMatches() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:*:*:*";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String ALIAS = String.format("a%d", KIND.hashCode());
        Map<String, String> ALIAS_MAP = new HashMap<>();
        ALIAS_MAP.put(KIND, ALIAS);

        StringBuilder kindBuilder = new StringBuilder();
        for (int i = 0; i < KIND_COUNT; i++) {
            if (i == 0) {
                kindBuilder.append(KIND);
            } else {
                kindBuilder.append("," + KIND);
            }
        }
        String HUNDREDS_KINDS = kindBuilder.toString();

        List<String> ALIASES = Collections.nCopies(KIND_COUNT, ALIAS);
        StringBuilder aliasBuilder = new StringBuilder();
        ALIASES.forEach((alias) -> aliasBuilder.append(alias + ","));
        String INDEX_NAME = String.format("%s%s", aliasBuilder, "-.*");

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(ALIAS_MAP);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_alias_when_searchingHundredSameKinds() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "*:*:*:*";
        String TENANT_KIND = "tenant1:*:*:*";
        String INDEX = TENANT_KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String ALIAS = String.format("a%d", TENANT_KIND.hashCode());
        Map<String, String> ALIAS_MAP = new HashMap<>();
        ALIAS_MAP.put(KIND, ALIAS);

        StringBuilder kindBuilder = new StringBuilder();
        for (int i = 0; i < KIND_COUNT; i++) {
            if (i == 0) {
                kindBuilder.append(KIND);
            } else {
                kindBuilder.append("," + KIND);
            }
        }
        String HUNDREDS_KINDS = kindBuilder.toString();

        List<String> ALIASES = Collections.nCopies(KIND_COUNT, ALIAS);
        StringBuilder aliasBuilder = new StringBuilder();
        ALIASES.forEach((alias) -> aliasBuilder.append(alias + ","));
        String INDEX_NAME = String.format("%s%s", aliasBuilder, "-.*");

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(TENANT_KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(ALIAS_MAP);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_PartitionIdIndexName_when_searchingAllKinds() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "*:*:*:*";
        String TENANT_KIND = "tenant1:*:*:*";
        String INDEX = TENANT_KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s%s,%s", DATA_PARTITION_ID, "-*-*-*", "-.*");
        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);
        when(elasticIndexNameResolver.getIndexNameFromKind(TENANT_KIND)).thenReturn(INDEX);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test(expected = BadRequestException.class)
    public void should_throw_BadRequestException_when_searchingAcrossTenants() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant2:*:*:*";
        String INDEX = KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s,%s", INDEX, "-.*");
        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);

        awsCrossTenantUtils.getIndexName(searchRequest); // Should throw an exception.
    }
}
