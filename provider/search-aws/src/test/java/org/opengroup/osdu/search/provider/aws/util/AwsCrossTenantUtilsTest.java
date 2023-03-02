package org.opengroup.osdu.search.provider.aws.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.search.service.IndexAliasService;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
    public void should_return_alias_when_searchingHundredSameKinds_01() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:2.0.0";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String ALIAS = String.format("a%d", KIND.hashCode());
        Map<String, String> ALIAS_MAP = new HashMap<>();
        ALIAS_MAP.put(KIND, ALIAS);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromAlias(KIND_COUNT, ALIAS);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(ALIAS_MAP);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_alias_when_searchingHundredSameKinds_02() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:2.*.*";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String ALIAS = String.format("a%d", KIND.hashCode());
        Map<String, String> ALIAS_MAP = new HashMap<>();
        ALIAS_MAP.put(KIND, ALIAS);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromAlias(KIND_COUNT, ALIAS);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(ALIAS_MAP);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_index_when_searchingHundredUnsupportedSameKinds_01() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:2.*";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromIndex(KIND_COUNT, INDEX);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(new HashMap<>());

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_index_when_searchingHundredUnsupportedSameKinds_02() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:2.*.0";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromIndex(KIND_COUNT, INDEX);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(new HashMap<>());

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_index_when_searchingHundredUnsupportedSameKinds_03() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:2.0.*";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromIndex(KIND_COUNT, INDEX);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(new HashMap<>());

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_index_when_searchingHundredUnsupportedSameKinds_04() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:*.0.0";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromIndex(KIND_COUNT, INDEX);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(new HashMap<>());

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_index_when_searchingHundredUnsupportedSameKinds_05() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:*.*.0";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromIndex(KIND_COUNT, INDEX);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(new HashMap<>());

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_index_when_searchingHundredUnsupportedSameKinds_06() {
        Integer KIND_COUNT = 300;
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant1:welldb-v2:wellbore:*.*.*";
        String INDEX = KIND.replace(":", "-");
        List<String> KINDS = Collections.nCopies(KIND_COUNT, KIND);
        String HUNDREDS_KINDS = getHundredsKinds(KIND_COUNT, KIND);
        String INDEX_NAME = getHundredsIndexNameFromIndex(KIND_COUNT, INDEX);

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(HUNDREDS_KINDS);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);
        when(indexAliasService.getIndicesAliases(KINDS)).thenReturn(new HashMap<>());

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_PartitionIdIndexName_when_searchingAllKinds_01() {
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

    @Test
    public void should_return_PartitionIdIndexName_when_searchingAllKinds_02() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "*:wks:*:*";
        String TENANT_KIND = "tenant1:wks:*:*";
        String INDEX = TENANT_KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s%s,%s", DATA_PARTITION_ID, "-wks-*-*", "-.*");

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);
        when(elasticIndexNameResolver.getIndexNameFromKind(TENANT_KIND)).thenReturn(INDEX);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_PartitionIdIndexName_when_searchingAllKinds_03() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "*:*:dataset--File.Generic:*";
        String TENANT_KIND = "tenant1:*:dataset--File.Generic:*";
        String INDEX = TENANT_KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s%s,%s", DATA_PARTITION_ID, "-*-dataset--File.Generic-*", "-.*");

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);
        when(elasticIndexNameResolver.getIndexNameFromKind(TENANT_KIND)).thenReturn(INDEX);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test
    public void should_return_PartitionIdIndexName_when_searchingAllKinds_04() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "*:*:*:1.0.0";
        String TENANT_KIND = "tenant1:*:*:1.0.0";
        String INDEX = TENANT_KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s%s,%s", DATA_PARTITION_ID, "-*-*-1.0.0", "-.*");

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);
        when(elasticIndexNameResolver.getIndexNameFromKind(TENANT_KIND)).thenReturn(INDEX);

        assertEquals(INDEX_NAME, awsCrossTenantUtils.getIndexName(searchRequest));
    }

    @Test(expected = BadRequestException.class)
    public void should_throw_BadRequestException_when_searchingAcrossTenants() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "tenant2:*:*:*";

        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);

        awsCrossTenantUtils.getIndexName(searchRequest); // Should throw an exception.
    }

    private String getHundredsKinds(Integer kindCount, String kind) {
        StringBuilder kindBuilder = new StringBuilder();
        for (int i = 0; i < kindCount; i++) {
            if (i == 0) {
                kindBuilder.append(kind);
            } else {
                kindBuilder.append("," + kind);
            }
        }
        return kindBuilder.toString();
    }

    private String getHundredsIndexNameFromAlias(Integer kindCount, String alias) {
        List<String> aliases = Collections.nCopies(kindCount, alias);
        StringBuilder aliasBuilder = new StringBuilder();
        aliases.forEach((item) -> aliasBuilder.append(item + ","));
        return String.format("%s%s", aliasBuilder, "-.*");
    }

    private String getHundredsIndexNameFromIndex(Integer kindCount, String index) {
        List<String> indexes = Collections.nCopies(kindCount, index);
        StringBuilder indexBuilder = new StringBuilder();
        indexes.forEach((item) -> indexBuilder.append(item + ","));
        return String.format("%s%s", indexBuilder, "-.*");
    }

}
