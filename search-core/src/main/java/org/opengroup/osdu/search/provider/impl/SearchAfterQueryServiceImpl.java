// Copyright © Schlumberger
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
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.http.ContentTooLongException;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.search.cache.SearchAfterSettingsCache;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.model.SearchAfterSettings;
import org.opengroup.osdu.search.provider.interfaces.ISearchAfterQueryService;
import org.opengroup.osdu.search.util.ElasticClientHandler;
import org.opengroup.osdu.search.util.IQueryPerformanceLogger;
import org.opengroup.osdu.search.util.ISortParserUtil;
import org.opengroup.osdu.search.util.ResponseExceptionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class SearchAfterQueryServiceImpl extends CoreQueryBase implements ISearchAfterQueryService {

    private final Time SEARCH_AFTER_TIMEOUT = Time.of(t -> t.time("90s"));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private SearchAfterSettingsCache searchAfterSettingsCache;
    @Inject
    private AuditLogger auditLogger;
    @Inject
    private JaxRsDpsLog logger;
    @Autowired
    private ResponseExceptionParser exceptionParser;
    @Autowired
    private IQueryPerformanceLogger tracingLogger;
    @Autowired
    private ISortParserUtil sortParserUtil;

    private final MessageDigest digest;

    public SearchAfterQueryServiceImpl() throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance("MD5");
    }

    @Override
    SearchRequest.Builder createElasticRequest(Query request, String index) throws AppException, IOException {
        // build query
        SearchRequest.Builder searchSourceBuilder = this.createSearchSourceBuilder(request);
        // Add PIT for SearchAfter Query. [indicesOptions] cannot be used with point in time
        String pitId = openPointInTime(index, this.elasticClientHandler.getOrCreateRestClient());
        searchSourceBuilder.pit(pit -> pit.id(pitId).keepAlive(SEARCH_AFTER_TIMEOUT));
        searchSourceBuilder.searchType(SearchType.QueryThenFetch).batchedReduceSize(512L);

        // All PIT search requests add an implicit sort tiebreaker field called _shard_doc
        if (request.getSort() == null) {
            for (SortOptions sortOption : getDefaultSortOptions()) {
                searchSourceBuilder.sort(sortOption);
            }
        }

        return searchSourceBuilder;
    }

    @Override
    void querySuccessAuditLogger(Query request) {
        this.auditLogger.queryIndexWithCursorSuccess(Lists.newArrayList(request.toString()));
    }

    @Override
    void queryFailedAuditLogger(Query request) {
        this.auditLogger.queryIndexWithCursorFailed(Lists.newArrayList(request.toString()));
    }

    @Override
    public CursorQueryResponse queryIndex(CursorQueryRequest searchRequest) throws Exception {
        CursorQueryResponse queryResponse;
        try {
            ElasticsearchClient client = this.elasticClientHandler.getOrCreateRestClient();
            if (Strings.isNullOrEmpty(searchRequest.getCursor())) {
                queryResponse = this.initPaginationQueryQuery(searchRequest, client);
            }
            else {
                SearchAfterSettings cursorSettings = this.searchAfterSettingsCache.get(searchRequest.getCursor());
                if (cursorSettings != null) {
                    checkAuthority(cursorSettings);
                    if (!cursorSettings.isClosed()) {
                        queryResponse = executeCursorPaginationQuery(searchRequest, client, cursorSettings);
                    } else {
                        queryResponse = CursorQueryResponse.getEmptyResponse();
                        queryResponse.setTotalCount(cursorSettings.getTotalCount());
                        this.searchAfterSettingsCache.delete(searchRequest.getCursor());
                    }
                } else {
                    throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Can't find the given cursor", "The given cursor is invalid or expired");
                }
            }

            return queryResponse;
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
        }
        catch (IOException e) {
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
        }
        catch (Exception e) {
            throw new AppException(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    "Search error",
                    "Error processing search request",
                    e);
        }
    }

    @Override
    public void close(String cursor) throws Exception {
        SearchAfterSettings cursorSettings = this.searchAfterSettingsCache.get(cursor);
        if(cursorSettings == null)
            return;

        checkAuthority(cursorSettings);
        this.searchAfterSettingsCache.delete(cursor);
        if(!cursorSettings.isClosed()) {
            ElasticsearchClient client = this.elasticClientHandler.getOrCreateRestClient();
            closePointInTime(cursorSettings.getPitId(), client);
        }
    }
    private List<SortOptions> getSortOptions(CursorQueryRequest searchRequest, ElasticsearchClient client) throws IOException {
        List<SortOptions> sortOptionsList;
        if (searchRequest.getSort() != null) {
            String index = getIndex(searchRequest);
            sortOptionsList = new ArrayList<>(this.sortParserUtil.getSortQuery(client, searchRequest.getSort(), index));
        } else {
            sortOptionsList = getDefaultSortOptions();
        }
        return sortOptionsList;
    }

    private List<SortOptions> getDefaultSortOptions() {
        // All PIT search requests add an implicit sort tiebreaker field called _shard_doc
        List<SortOptions> sortOptions = new ArrayList<>();
        sortOptions.add(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
        return sortOptions;
    }

    private String openPointInTime(String index, ElasticsearchClient client) throws IOException {
        OpenPointInTimeRequest openRequest = OpenPointInTimeRequest.of(builder ->
                builder.index(index)
                        .ignoreUnavailable(true)
                        .keepAlive(SEARCH_AFTER_TIMEOUT));
        OpenPointInTimeResponse openResponse = client.openPointInTime(openRequest);
        return openResponse.id();
    }

    private void checkAuthority(SearchAfterSettings cursorSettings) {
        if (!this.dpsHeaders.getUserEmail().equals(cursorSettings.getUserId())) {
            throw new AppException(HttpServletResponse.SC_FORBIDDEN, "cursor issuer doesn't match the cursor consumer", "cursor sharing is forbidden");
        }
    }

    private void closePointInTime(String pitId, ElasticsearchClient client) {
        ClosePointInTimeRequest closeRequest = ClosePointInTimeRequest.of(builder -> builder.id(pitId));
        try {
            client.closePointInTime(closeRequest);
        }
        catch(Exception ex) {
            logger.warning("Failed to close point in time", ex);
        }
    }

    private CursorQueryResponse initPaginationQueryQuery(CursorQueryRequest searchRequest, ElasticsearchClient client) throws IOException  {
        Long startTime = System.currentTimeMillis();

        // Set TrackTotalCount = true to get the total count in the first query
        searchRequest.setTrackTotalCount(true);
        SearchResponse<Map<String, Object>> searchResponse = this.makeSearchRequest(searchRequest, client);
        CursorQueryResponse response = processSearchResponse(searchResponse, searchRequest, client, null);

        Long latency = System.currentTimeMillis() - startTime;
        tracingLogger.log(searchRequest, latency, 200);

        return response;
    }

    private CursorQueryResponse executeCursorPaginationQuery(CursorQueryRequest searchRequest, ElasticsearchClient client, SearchAfterSettings cursorSettings) throws IOException {
        Long startTime = System.currentTimeMillis();

        // build query
        searchRequest.setSort(cursorSettings.getSortQuery());
        SearchRequest.Builder sourceBuilder = this.createSearchSourceBuilder(searchRequest);

        // The following 3 statements are required to support pagination with search_after
        List<SortOptions> sortOptionsList = this.getSortOptions(searchRequest, client);
        List<FieldValue> fieldValues = this.toFieldValues(cursorSettings.getSearchAfterValues());
        sourceBuilder.pit(pit -> pit.id(cursorSettings.getPitId()).keepAlive(SEARCH_AFTER_TIMEOUT));
        sourceBuilder.sort(sortOptionsList)
                     .searchAfter(fieldValues);
        sourceBuilder.searchType(SearchType.QueryThenFetch).batchedReduceSize(512L);

        SearchRequest elasticSearchRequest = sourceBuilder.build();
        SearchResponse<Map<String, Object>> searchResponse = client.search(elasticSearchRequest, (Type) Map.class);

        CursorQueryResponse response = processSearchResponse(searchResponse, searchRequest, client, cursorSettings);

        Long latency = System.currentTimeMillis() - startTime;
        tracingLogger.log(searchRequest, latency, 200);

        return response;
    }

    private CursorQueryResponse processSearchResponse(SearchResponse<Map<String, Object>> searchResponse, CursorQueryRequest searchRequest, ElasticsearchClient client, SearchAfterSettings cursorSettings) throws IOException {
        CursorQueryResponse queryResponse = CursorQueryResponse.getEmptyResponse();

        // We enforce trackTotalCount to be true in the first call only
        long totalCount = (cursorSettings != null)
                ? cursorSettings.getTotalCount()
                : searchResponse.hits().total().value();
        queryResponse.setTotalCount(totalCount);

        boolean isCursorClosed = closePointInTimeIfNeeded(searchRequest, searchResponse, client);
        List<Map<String, Object>> results = this.getHitsFromSearchResponse(searchResponse);
        if (results != null && !results.isEmpty()) {
            this.querySuccessAuditLogger(searchRequest);
            String cursor = this.refreshCursorCache(searchResponse, searchRequest, isCursorClosed, cursorSettings);
            queryResponse.setCursor(cursor);
            queryResponse.setResults(results);
        }
        else if(searchRequest.getCursor() != null) {
            this.searchAfterSettingsCache.delete(searchRequest.getCursor());
        }

        return queryResponse;
    }

    private boolean closePointInTimeIfNeeded(CursorQueryRequest searchRequest, SearchResponse<Map<String, Object>>  searchResponse, ElasticsearchClient client) {
        String pitId = searchResponse.pitId();
        HitsMetadata<Map<String, Object>> searchHits = searchResponse.hits();
        if (pitId != null && (searchHits == null || searchHits.hits() == null || searchHits.hits().size() < searchRequest.getLimit())) {
            closePointInTime(pitId, client);
            return true;
        }
        return false;
    }

    String refreshCursorCache(SearchResponse<Map<String, Object>> searchResponse, CursorQueryRequest searchRequest, boolean isCursorClosed, SearchAfterSettings cursorSettings) throws JsonProcessingException {
        String pitId = searchResponse.pitId();
        if (pitId != null) {
            this.digest.update(pitId.getBytes());
            String hashCursor = DatatypeConverter.printHexBinary(this.digest.digest()).toUpperCase();
            List<FieldValue> searchAfterValues;
            HitsMetadata<Map<String, Object>> searchHits = searchResponse.hits();
            if (searchHits.hits() != null && !searchHits.hits().isEmpty()) {
                int length = searchHits.hits().size();
                searchAfterValues = searchHits.hits().get(length -1).sort();
            }
            else {
                searchAfterValues = new ArrayList<>();
            }
            if(cursorSettings == null) {
                cursorSettings = SearchAfterSettings
                        .builder()
                        .userId(dpsHeaders.getUserEmail())
                        .sortQuery(searchRequest.getSort())
                        .totalCount(searchResponse.hits().total().value()).build();
            }
            cursorSettings.setPitId(pitId);
            cursorSettings.setSearchAfterValues(this.toEntries(searchAfterValues));
            cursorSettings.setClosed(isCursorClosed);
            this.searchAfterSettingsCache.put(hashCursor, cursorSettings);
            return hashCursor;
        }
        else {
            return null;
        }
    }

    private List<Map.Entry<String, Object>> toEntries(List<FieldValue> fieldValues) {
        if(fieldValues == null) {
            return new ArrayList<>();
        }

        List<Map.Entry<String, Object>> entries = new ArrayList();
        for(FieldValue fieldValue : fieldValues) {
            fieldValue._toJsonString();
            Map.Entry<String, Object> entry;
            if(fieldValue.isDouble() || fieldValue.isLong() || fieldValue.isBoolean()) {
                entry = new AbstractMap.SimpleEntry<>(fieldValue._kind().name(), String.valueOf(fieldValue._get()));
            }
            else {
                entry = new AbstractMap.SimpleEntry<>(fieldValue._kind().name(), fieldValue._get());
            }
            entries.add(entry);
        }
        return entries;
    }

    private List<FieldValue> toFieldValues(List<Map.Entry<String, Object>> entries) {
        if(entries == null) {
            return new ArrayList<>();
        }

        List<FieldValue> fieldValues = new ArrayList();
        for(Map.Entry<String, Object> kindValue : entries) {
            FieldValue fieldValue;
            switch (kindValue.getKey()) {
                case "Double":
                    fieldValue = FieldValue.of(Double.parseDouble((String)kindValue.getValue()));
                    break;
                case "Long":
                    fieldValue = FieldValue.of(Long.parseLong((String)kindValue.getValue()));
                    break;
                case "Boolean":
                    fieldValue = FieldValue.of(Boolean.parseBoolean((String)kindValue.getValue()));
                    break;
                case "String":
                    fieldValue = FieldValue.of((String) kindValue.getValue());
                    break;
                case "Null":
                    fieldValue = FieldValue.NULL;
                    break;
                default:
                    fieldValue = FieldValue.of(kindValue.getValue());
            }
            fieldValues.add(fieldValue);
        }
        return fieldValues;
    }
}
