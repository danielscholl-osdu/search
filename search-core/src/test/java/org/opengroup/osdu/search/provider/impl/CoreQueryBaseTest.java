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

package org.opengroup.osdu.search.provider.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.opengroup.osdu.core.common.http.CollaborationContextFactory;
import org.opengroup.osdu.core.common.model.search.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.annotation.Resource;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.opengroup.osdu.search.context.UserContext;
import org.opengroup.osdu.search.policy.service.IPolicyService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.IQueryParserUtil;
import org.opengroup.osdu.search.util.IDetailedBadRequestMessageUtil;
import org.opengroup.osdu.search.util.IQueryPerformanceLogger;
import org.opengroup.osdu.search.util.SuggestionsQueryUtil;
import org.opengroup.osdu.search.util.GeoQueryBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CoreQueryBaseTest.Config.class)
public class CoreQueryBaseTest {

    @MockBean
    UserContext userContext;

    @MockBean
    SearchConfigurationProperties searchConfigurationProperties;

    @TestConfiguration
    static class Config {
        @Bean
        CoreQueryBase coreQueryBase() {
            return new CoreQueryBase() {
                @Override
                SearchRequest.Builder createElasticRequest(Query request, String index) throws IOException {
                    SearchRequest.Builder b = createSearchSourceBuilder(request);
                    b.index(index);
                    return b;
                }
                @Override void querySuccessAuditLogger(Query request) {}
                @Override void queryFailedAuditLogger(Query request) {}
            };
        }

        @MockBean DpsHeaders dpsHeaders;
        @MockBean JaxRsDpsLog log;
        @MockBean
        CrossTenantUtils crossTenantUtils;
        @MockBean
        IPolicyService iPolicyService;
        @MockBean IQueryParserUtil queryParserUtil;
        @MockBean
        ISortParserUtil sortParserUtil;
        @MockBean IDetailedBadRequestMessageUtil detailedBadRequestMessageUtil;
        @MockBean ElasticLoggingConfig elasticLoggingConfig;
        @MockBean IQueryPerformanceLogger tracingLogger;
        @MockBean GeoQueryBuilder geoQueryBuilder;
        @MockBean SuggestionsQueryUtil suggestionsQueryUtil;
        @MockBean IFeatureFlag featureFlag;
        @MockBean CollaborationContextFactory collaborationContextFactory;
    }

    @Resource CoreQueryBase coreQueryBase;
    @Resource CrossTenantUtils crossTenantUtils;
    @Resource IQueryParserUtil queryParserUtil;
    @Resource ElasticLoggingConfig elasticLoggingConfig;
    @Resource IFeatureFlag featureFlag;
    @Resource ISortParserUtil sortParserUtil;
    @Resource IQueryPerformanceLogger tracingLogger;
    @Resource SuggestionsQueryUtil suggestionsQueryUtil;

    @BeforeEach
    void setup() {
        when(elasticLoggingConfig.getEnabled()).thenReturn(false);
        when(elasticLoggingConfig.getThreshold()).thenReturn(Long.MAX_VALUE);
    }

    @Test
    void createSearchSourceBuilder_buildsIncludesExcludesAndHighlight() throws Exception {
        Query request = mock(Query.class);

        when(request.getQuery()).thenReturn("kind:foo");
        when(request.getReturnedFields()).thenReturn(Arrays.asList("a", "b"));
        when(request.getHighlightedFields()).thenReturn(Collections.singletonList("title"));
        when(request.getLimit()).thenReturn(5);
        when(request.getSuggestPhrase()).thenReturn(null);
        when(request.isTrackTotalCount()).thenReturn(false);
        when(request.isQueryAsOwner()).thenReturn(false);
        when(request.getSort()).thenReturn(null);
        when(request.getSpatialFilter()).thenReturn(null);

        when(featureFlag.isFeatureEnabled(any())).thenReturn(false);
        when(queryParserUtil.buildQueryBuilderFromQueryString(any()))
                .thenReturn(new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder());
        when(suggestionsQueryUtil.getSuggestions(any())).thenReturn(null);

        var built = coreQueryBase.createSearchSourceBuilder(request).build();

        assertEquals(QueryUtils.getResultSizeForQuery(5), built.size());
    }

    @Test
    void makeSearchRequest_mapsListenerTimeoutIOException_to504() throws Exception {
        Query request = mock(Query.class);
        when(request.getLimit()).thenReturn(1);
        when(request.getQuery()).thenReturn("kind:foo");
        when(request.getSort()).thenReturn(null);
        when(request.getSpatialFilter()).thenReturn(null);
        when(request.isQueryAsOwner()).thenReturn(false);
        when(request.getSuggestPhrase()).thenReturn(null);
        when(request.isTrackTotalCount()).thenReturn(false);
        when(request.getReturnedFields()).thenReturn(Collections.emptyList());
        when(request.getHighlightedFields()).thenReturn(null);

        when(featureFlag.isFeatureEnabled(any())).thenReturn(false);
        when(crossTenantUtils.getIndexName(any())).thenReturn("idx");
        when(sortParserUtil.getSortQuery(any(), any(), anyString())).thenReturn(Collections.emptyList());
        when(queryParserUtil.buildQueryBuilderFromQueryString(any())).thenReturn(new BoolQuery.Builder());

        ElasticsearchClient esClient = mock(ElasticsearchClient.class);
        when(esClient.search(any(SearchRequest.class), ArgumentMatchers.<Type>any()))
                .thenThrow(new IOException("listener timeout after waiting for 1m"));

        AppException ex = assertThrows(AppException.class, () -> coreQueryBase.makeSearchRequest(request, esClient));

        assertEquals(504, ex.getError().getCode());
        assertEquals("Search error", ex.getError().getReason());
        assertTrue(ex.getError().getMessage().contains("Request timed out"));

        verify(tracingLogger).log(eq(request), anyLong(), anyInt());
    }
}
