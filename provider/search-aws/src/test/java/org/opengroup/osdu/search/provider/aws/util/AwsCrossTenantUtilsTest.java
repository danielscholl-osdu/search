package org.opengroup.osdu.search.provider.aws.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;

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
    public void should_return_PartitionIdIndexName_when_searchingAllKinds() {
        String DATA_PARTITION_ID = "tenant1";
        String KIND = "*:*:*:*";
        String INDEX = KIND.replace(":", "-");
        String INDEX_NAME = String.format("%s%s,%s", DATA_PARTITION_ID, "-*-*-*", "-.*");
        when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION_ID);
        when(searchRequest.getKind()).thenReturn(KIND);
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);

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
        when(elasticIndexNameResolver.getIndexNameFromKind(KIND)).thenReturn(INDEX);

        awsCrossTenantUtils.getIndexName(searchRequest); // Should throw an exception.
    }
}
