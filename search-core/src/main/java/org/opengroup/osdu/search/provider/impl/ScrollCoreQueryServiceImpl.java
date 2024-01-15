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

package org.opengroup.osdu.search.provider.impl;

import static org.elasticsearch.rest.RestStatus.NOT_FOUND;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import org.apache.http.ContentTooLongException;
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
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IScrollQueryService;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IPerfLogger;
import org.opengroup.osdu.search.util.ResponseExceptionParser;
import org.opengroup.osdu.search.util.SearchRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScrollCoreQueryServiceImpl extends CoreQueryBase implements IScrollQueryService {

    private final TimeValue SEARCH_SCROLL_TIMEOUT = TimeValue.timeValueSeconds(90L);

    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private CursorCache cursorCache;
    @Inject
    private AuditLogger auditLogger;
    @Autowired
    private ResponseExceptionParser exceptionParser;
    @Autowired
    private IPerfLogger tracingLogger;

    private final MessageDigest digest;

    public ScrollCoreQueryServiceImpl() throws NoSuchAlgorithmException {
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
                        throw new AppException(429, "Too many requests", "Too many cursor requests, please re-try after some time.", e);
                    }
                    throw e;
                }
            } else {
                try {
                    CursorSettings cursorSettings = this.cursorCache.get(searchRequest.getCursor());
                    if (cursorSettings != null) {
                        if (!this.dpsHeaders.getUserEmail().equals(cursorSettings.getUserId())) {
                            throw new AppException(HttpServletResponse.SC_FORBIDDEN, "cursor issuer doesn't match the cursor consumer", "cursor sharing is forbidden");
                        }

                        executeCursorPaginationQuery(searchRequest, queryResponse, client, cursorSettings);
                    } else {
                        throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Can't find the given cursor", "The given cursor is invalid or expired");
                    }
                } catch (AppException e) {
                    throw e;
                } catch (ElasticsearchStatusException e) {
                    String invalidScrollMessage = "No search context found for id";
                    if (e.status() == NOT_FOUND
                            && (e.getMessage().startsWith(invalidScrollMessage)) || this.exceptionParser.parseException(e).stream().anyMatch(r -> r.contains(invalidScrollMessage)))
                        throw new AppException(HttpStatus.SC_BAD_REQUEST, "Can't find the given cursor", "The given cursor is invalid or expired", e);
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
                } catch (IOException e) {
                    if (e.getCause() instanceof ContentTooLongException) {
                        throw new AppException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Response is too long", "Elasticsearch response is too long, max is 100Mb", e);
                    }
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
                } catch (Exception e) {
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
                }
            }
            return queryResponse;
        }
    }

    private void executeCursorPaginationQuery(CursorQueryRequest searchRequest, CursorQueryResponse queryResponse, RestHighLevelClient client, CursorSettings cursorSettings) throws IOException {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(cursorSettings.getCursor());
        scrollRequest.scroll(SEARCH_SCROLL_TIMEOUT);
        Long startTime = 0L;
        SearchResponse searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
        Long latency = System.currentTimeMillis() - startTime;

        List<Map<String, Object>> results = getHitsFromSearchResponse(searchResponse);
        queryResponse.setTotalCount(searchResponse.getHits().getTotalHits().value);
        if (results != null) {
            queryResponse.setResults(results);
            queryResponse.setCursor(this.refreshCursorCache(searchResponse.getScrollId(), dpsHeaders.getUserEmail()));
            this.querySuccessAuditLogger(searchRequest);
        }
        int statusCode = searchResponse.status().getStatus();
        tracingLogger.log(searchRequest, latency, statusCode);
    }

    private CursorQueryResponse initCursorQuery(CursorQueryRequest searchRequest, RestHighLevelClient client) {
        return this.executeCursorQuery(searchRequest, client);
    }

    private CursorQueryResponse executeCursorQuery(CursorQueryRequest searchRequest, RestHighLevelClient client) throws AppException {

        SearchResponse searchResponse = this.makeSearchRequest(searchRequest, client);
        List<Map<String, Object>> results = this.getHitsFromSearchResponse(searchResponse);
        if (results != null) {
            return CursorQueryResponse.builder()
                    .cursor(refreshCursorCache(searchResponse.getScrollId(), dpsHeaders.getUserEmail()))
                    .results(results)
                    .totalCount(searchResponse.getHits().getTotalHits().value)
                    .build();
        }
        return CursorQueryResponse.getEmptyResponse();
    }

    @Override
    SearchRequest createElasticRequest(Query request, String index) throws AppException, IOException {

        // set the indexes to search against
        SearchRequest elasticSearchRequest = SearchRequestUtil.createSearchRequest(index);

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
            this.cursorCache.put(hashCursor, CursorSettings.builder().cursor(rawCursor).userId(userId).build());
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
