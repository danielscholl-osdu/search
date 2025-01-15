/*
 *  Copyright 2017-2019 © Schlumberger
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.provider.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import org.apache.http.ContentTooLongException;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.search.cache.CursorCache;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.provider.interfaces.IScrollQueryService;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IQueryPerformanceLogger;
import org.opengroup.osdu.search.util.ResponseExceptionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScrollCoreQueryServiceImpl extends CoreQueryBase implements IScrollQueryService {

  private static final Time SEARCH_SCROLL_TIMEOUT = Time.of(t -> t.time("90s"));

  @Inject private ElasticClientHandler elasticClientHandler;
  @Inject private CursorCache cursorCache;
  @Inject private AuditLogger auditLogger;
  @Autowired private ResponseExceptionParser exceptionParser;
  @Autowired private IQueryPerformanceLogger tracingLogger;

  private final MessageDigest digest;

  public ScrollCoreQueryServiceImpl() throws NoSuchAlgorithmException {
    this.digest = MessageDigest.getInstance("MD5");
  }

  @Override
  public CursorQueryResponse queryIndex(CursorQueryRequest searchRequest) throws Exception {

    CursorQueryResponse queryResponse = CursorQueryResponse.getEmptyResponse();
    try {
      ElasticsearchClient client = this.elasticClientHandler.getOrCreateRestClient();
      if (Strings.isNullOrEmpty(searchRequest.getCursor())) {
        try {
          return this.initCursorQuery(searchRequest, client);
        } catch (AppException e) {
          if (this.exceptionParser.parseException(e).stream()
              .anyMatch(
                  r ->
                      r.contains(
                          "Trying to create too many scroll contexts. Must be less than or equal to:"))) {
            throw new AppException(
                429,
                "Too many requests",
                "Too many cursor requests, please re-try after some time.",
                e);
          }
          throw e;
        }
      } else {
        try {
          CursorSettings cursorSettings = this.cursorCache.get(searchRequest.getCursor());
          if (cursorSettings != null) {
            if (!this.dpsHeaders.getUserEmail().equals(cursorSettings.getUserId())) {
              throw new AppException(
                  HttpServletResponse.SC_FORBIDDEN,
                  "cursor issuer doesn't match the cursor consumer",
                  "cursor sharing is forbidden");
            }

            executeCursorPaginationQuery(searchRequest, queryResponse, client, cursorSettings);
          } else {
            throw new AppException(
                HttpServletResponse.SC_BAD_REQUEST,
                "Can't find the given cursor",
                "The given cursor is invalid or expired");
          }
        } catch (AppException e) {
          throw e;
        } catch (ElasticsearchException e) {
          String invalidScrollMessage = "No search context found for id";
          if (e.status() == 404 && (e.getMessage().startsWith(invalidScrollMessage))
              || this.exceptionParser.parseException(e).stream()
                  .anyMatch(r -> r.contains(invalidScrollMessage)))
            throw new AppException(
                HttpStatus.SC_BAD_REQUEST,
                "Can't find the given cursor",
                "The given cursor is invalid or expired",
                e);
          throw new AppException(
              HttpStatus.SC_INTERNAL_SERVER_ERROR,
              "Search error",
              "Error processing search request",
              e);
        } catch (IOException e) {
          if (e.getCause() instanceof ContentTooLongException) {
            throw new AppException(
                HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                "Response is too long",
                "Elasticsearch response is too long, max is 100Mb",
                e);
          }
          throw new AppException(
              HttpStatus.SC_INTERNAL_SERVER_ERROR,
              "Search error",
              "Error processing search request",
              e);
        } catch (Exception e) {
          throw new AppException(
              HttpStatus.SC_INTERNAL_SERVER_ERROR,
              "Search error",
              "Error processing search request",
              e);
        }
      }
      return queryResponse;
    } catch (AppException e) {
      throw e;
    }
  }

  private void executeCursorPaginationQuery(
      CursorQueryRequest searchRequest,
      CursorQueryResponse queryResponse,
      ElasticsearchClient client,
      CursorSettings cursorSettings)
      throws IOException {

    ScrollRequest scrollRequest =
        ScrollRequest.of(
            sr -> sr.scrollId(cursorSettings.getCursor()).scroll(SEARCH_SCROLL_TIMEOUT));

    long startTime = System.currentTimeMillis();
    ScrollResponse<Map<String, Object>> scrollResponse =
        client.scroll(scrollRequest, (Type) Map.class);
    Long latency = System.currentTimeMillis() - startTime;

    List<Map<String, Object>> results = getHitsFromSearchResponse(scrollResponse);
    queryResponse.setTotalCount(scrollResponse.hits().total().value());
    if (!results.isEmpty()) {
      queryResponse.setResults(results);
      queryResponse.setCursor(
          this.refreshCursorCache(scrollResponse.scrollId(), dpsHeaders.getUserEmail()));
      this.querySuccessAuditLogger(searchRequest);
    }

    tracingLogger.log(searchRequest, latency, 200);
  }

  private CursorQueryResponse initCursorQuery(
      CursorQueryRequest searchRequest, ElasticsearchClient client) {
    return this.executeCursorQuery(searchRequest, client);
  }

  private CursorQueryResponse executeCursorQuery(
      CursorQueryRequest searchRequest, ElasticsearchClient client) throws AppException {

    SearchResponse<Map<String, Object>> searchResponse =
        this.makeSearchRequest(searchRequest, client);
    List<Map<String, Object>> results = this.getHitsFromSearchResponse(searchResponse);
    if (!results.isEmpty()) {
      return CursorQueryResponse.builder()
          .cursor(refreshCursorCache(searchResponse.scrollId(), dpsHeaders.getUserEmail()))
          .results(results)
          .totalCount(searchResponse.hits().total().value())
          .build();
    }
    return CursorQueryResponse.getEmptyResponse();
  }

  @Override
  SearchRequest.Builder createElasticRequest(Query request, String index)
      throws AppException, IOException {
    // build query
    SearchRequest.Builder searchSourceBuilder = this.createSearchSourceBuilder(request);
    searchSourceBuilder
            .index(index)
            .allowNoIndices(true)
            .expandWildcards(ExpandWildcard.Open, ExpandWildcard.Closed)
            .ignoreUnavailable(true)
            .ignoreThrottled(true);
    searchSourceBuilder.searchType(SearchType.QueryThenFetch);
    searchSourceBuilder.batchedReduceSize(512L).ccsMinimizeRoundtrips(true);

    // Optimize Scroll request if users wants to iterate over all documents regardless of order
    if (request.getSort() == null) { 
      searchSourceBuilder
          .sort(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))))
          .sort(SortOptions.of(so -> so.doc(d -> d.order(SortOrder.Asc))));
    }
    searchSourceBuilder.scroll(SEARCH_SCROLL_TIMEOUT);

    return searchSourceBuilder;
  }

  String refreshCursorCache(String rawCursor, String userId) {
    if (rawCursor != null) {
      this.digest.update(rawCursor.getBytes());
      String hashCursor = DatatypeConverter.printHexBinary(this.digest.digest()).toUpperCase();
      this.cursorCache.put(
          hashCursor, CursorSettings.builder().cursor(rawCursor).userId(userId).build());
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
