/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.search.provider.ibm.provider.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

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
import org.opengroup.osdu.search.util.ResponseExceptionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import static org.elasticsearch.rest.RestStatus.NOT_FOUND;

@Service
public class ScrollQueryServiceImpl extends QueryBase implements IScrollQueryService {

    private final TimeValue SEARCH_SCROLL_TIMEOUT = TimeValue.timeValueSeconds(90L);

    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private CursorCache cursorCache;
    @Inject
    private AuditLogger auditLogger;
    @Autowired
    private ResponseExceptionParser exceptionParser;

    private final MessageDigest digest;

    public ScrollQueryServiceImpl() throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance("MD5");
    }

    @Override
    public CursorQueryResponse queryIndex(CursorQueryRequest searchRequest) throws Exception {

    	//validateTenant(searchRequest);

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
                    String invalidScrollMessage = "No search context found for id";
                    if (e.status() == NOT_FOUND
                            && (e.getMessage().startsWith(invalidScrollMessage)) || this.exceptionParser.parseException(e).stream().anyMatch(r -> r.contains(invalidScrollMessage)))
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
                    .results(results)
                    .totalCount(searchResponse.getHits().getTotalHits().value)
                    .build();
        }
        return CursorQueryResponse.getEmptyResponse();
    }

    @Override
    SearchRequest createElasticRequest(Query request) throws AppException, IOException {

        // set the indexes to search against
        SearchRequest elasticSearchRequest = new SearchRequest(this.getIndex(request));

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
