// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.search.provider.byoc.provider.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.elasticsearch.search.sort.SortBuilders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.search.provider.interfaces.IScrollQueryService;
import org.opengroup.osdu.search.util.CrossTenantUtils;
import org.opengroup.osdu.search.util.QueryResponseUtil;
import org.opengroup.osdu.search.util.ResponseExceptionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.RestStatus.NOT_FOUND;

@Service
public class ScrollQueryServiceImpl extends QueryBase implements IScrollQueryService {

    private final TimeValue SEARCH_SCROLL_TIMEOUT = TimeValue.timeValueSeconds(90L);

    @Inject
    private ElasticClientHandler elasticClientHandler;

    @Inject
    private CursorCache cursorCache;

    @Inject
    private CrossTenantUtils crossTenantUtils;

    @Inject
    private AuditLogger auditLogger;

    @Inject
    private QueryResponseUtil queryResponseUtil;
    @Autowired
    private ResponseExceptionParser exceptionParser;

    private final MessageDigest digest;

    ScrollQueryServiceImpl() throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance("MD5");
    }

    @Override
    public CursorQueryResponse queryIndex(CursorQueryRequest searchRequest) throws Exception {

        CursorQueryResponse queryResponse = CursorQueryResponse.getEmptyResponse();

        try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
            if (Strings.isNullOrEmpty(searchRequest.getCursor())) {
                try {
                    return this.initCursorQuery(searchRequest, client);
                } catch (AppException e) {
                    if (this.exceptionParser.parseException(e).stream().anyMatch(r -> r.contains("Trying to create too many scroll contexts. Must be less than or equal to:"))) {
                        throw new AppException(429, "Too many request", "Too many cursor request, please re-try after some time.", e);
                    }
                    throw e;
                }
            } else {
                try {
                    CursorSettings cursorSettings = null;//this.cursorCache.get(searchRequest.getCursor());
                    if (cursorSettings != null) {
                        if (!this.dpsHeaders.getUserEmail().equals(cursorSettings.getUserId())) {
                            throw new AppException(HttpServletResponse.SC_FORBIDDEN, "cursor issuer doesn't match the cursor consumer", "cursor sharing is forbidden");
                        }

                        SearchScrollRequest scrollRequest = new SearchScrollRequest(cursorSettings.getCursor());
                        scrollRequest.scroll(SEARCH_SCROLL_TIMEOUT);
                        SearchResponse searchScrollResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);

                        List<Map<String, Object>> results = getHitsFromSearchResponse(searchScrollResponse);
                        queryResponse.setTotalCount(searchScrollResponse.getHits().getTotalHits().value);
                        if (results != null) {
                            queryResponse.setResults(results);
                            queryResponse.setCursor(this.refreshCursorCache(searchScrollResponse.getScrollId(), dpsHeaders.getUserEmail()));

                            this.querySuccessAuditLogger(searchRequest);
                        }
                    } else {
                        throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Can't find the given cursor", "The given cursor is invalid or expired");
                    }
                } catch (AppException e) {
                    throw e;
                } catch (ElasticsearchStatusException e) {
                    if (e.status() == NOT_FOUND && e.getMessage().startsWith("No search context found for id"))
                        throw new AppException(HttpStatus.SC_BAD_REQUEST, "Can't find the given cursor", "The given cursor is invalid or expired", e);
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
                } catch (Exception e) {
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
                }
            }
            return queryResponse;
        }
    }

    private CursorQueryResponse initCursorQuery(CursorQueryRequest searchRequest, RestHighLevelClient client) {
        CursorQueryResponse queryResponse = this.executeCursorQuery(searchRequest, client);
        return queryResponse;
    }

    private CursorQueryResponse executeCursorQuery(CursorQueryRequest searchRequest, RestHighLevelClient client) throws AppException {

        SearchResponse searchResponse = this.makeSearchRequest(searchRequest, client);
        List<Map<String, Object>> results = this.getHitsFromSearchResponse(searchResponse);
        if (results != null) {
            return CursorQueryResponse.builder()
                    .cursor(refreshCursorCache(searchResponse.getScrollId(), dpsHeaders.getUserEmail()))
                    .results(queryResponseUtil.getQueryResponseResults(results))
                    .totalCount(searchResponse.getHits().getTotalHits().value)
                    .build();
        }
        return CursorQueryResponse.getEmptyResponse();
    }

    @Override
    SearchRequest createElasticRequest(Query request) throws AppException {

        // set the indexes to org.opengroup.osdu.search.search against
        String index = this.crossTenantUtils.getIndexName(request);
        SearchRequest elasticSearchRequest = new SearchRequest(index);

        // build query
        SearchSourceBuilder sourceBuilder = this.createSearchSourceBuilder(request);

        // Optimize Scroll request if users wants to iterate over all documents regardless of order
        if (request.getSort() == null) {
            sourceBuilder.sort(SortBuilders.scoreSort());
            sourceBuilder.sort(SortBuilders.fieldSort("_doc"));
        }

        elasticSearchRequest.source(sourceBuilder);
        elasticSearchRequest.scroll(new Scroll(SEARCH_SCROLL_TIMEOUT));

        return elasticSearchRequest;
    }

    String refreshCursorCache(String rawCursor, String userId) {
        if (rawCursor != null) {
            this.digest.update(rawCursor.getBytes());
            String hashCursor = DatatypeConverter.printHexBinary(this.digest.digest()).toUpperCase();
            //this.cursorCache.put(hashCursor, CursorSettings.builder().cursor(rawCursor).userId(userId).build());
            return hashCursor;
        }
        return null;
    }

    @Override
    void querySuccessAuditLogger(Query request) {
        this.auditLogger.queryIndexWithCursorSuccess(Lists.newArrayList(request.toString()));
    }

    @Override
    void queryFailedAuditLogger(Query request) {
        this.auditLogger.queryIndexWithCursorFailed(Lists.newArrayList(request.toString()));
    }
}
