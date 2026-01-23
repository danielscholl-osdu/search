// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.smart.models;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.provider.interfaces.IAttributesCache;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class AttributeCollectionTest {

    @Mock private SearchConfigurationProperties searchConfig;
    @Mock private JaxRsDpsLog log;
    @Mock private DpsHeaders headers;
    @Mock private IAttributesCache<String, Set<String>> cache;
    @Mock private IServiceAccountJwtClient serviceAccountJwtClient;

    @InjectMocks private AttributeCollection attributeCollection;

    @BeforeEach
    void setup() {
        Mockito.lenient().when(searchConfig.getDeployedServiceId()).thenReturn("search-svc");
        Mockito.lenient().when(headers.getPartitionId()).thenReturn("test-partition");
    }

    @Test
    void getAllAttributes_returnsCachedData_whenPresent() throws IOException {
        String cacheKey = "search-svc-test-partition-attr1";
        Set<String> cached = new HashSet<>(Set.of("val1", "val2"));
        when(cache.get(cacheKey)).thenReturn(cached);

        Set<String> result = attributeCollection.getAllAttributes("test-partition", "attr1");

        assertEquals(cached, result);
        verify(cache, times(1)).get(cacheKey);
    }

    @Test
    void getAllAttributes_returnsEmptyAndLogsWarning_whenCacheEmpty() throws IOException {
        when(cache.get(anyString())).thenReturn(null);
        Set<String> result = attributeCollection.getAllAttributes("test-partition", "attrX");
        assertTrue(result.isEmpty());
    }

    @Test
    void checkOrGetAuthorizationHeader_returnsExistingAuthHeader_forLocalEnv() {
        when(searchConfig.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.LOCAL);
        when(headers.getAuthorization()).thenReturn("Bearer abc");

        String result = attributeCollection.checkOrGetAuthorizationHeader();
        assertEquals("Bearer abc", result);
    }

    @Test
    void checkOrGetAuthorizationHeader_throwsException_whenAuthHeaderEmpty() {
        when(searchConfig.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.LOCAL);
        when(headers.getAuthorization()).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> attributeCollection.checkOrGetAuthorizationHeader());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, ex.getError().getCode());
    }

    @Test
    void checkOrGetAuthorizationHeader_returnsToken_whenNotLocalEnv() {
        when(searchConfig.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.CLOUD);
        when(serviceAccountJwtClient.getIdToken("test-partition")).thenReturn("xyz-token");

        String result = attributeCollection.checkOrGetAuthorizationHeader();
        assertEquals("Bearer xyz-token", result);
    }

    @Test
    void getPartionedList_splitsLargeSetIntoChunks() {
        Set<String> input = new LinkedHashSet<>();
        for (int i = 1; i <= 25; i++) input.add("i" + i);

        List<List<String>> parts = attributeCollection.getPartionedList(input, 10);
        assertEquals(3, parts.size());
        assertEquals(10, parts.get(0).size());
    }
}
