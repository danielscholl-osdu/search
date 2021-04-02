// Copyright © Amazon Web Services
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

package org.opengroup.osdu.search.provider.aws.provider.impl;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.builders.CoordinatesBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.locationtech.jts.geom.Coordinate;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AclRole;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.search.provider.aws.service.FieldMappingTypeService;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.opengroup.osdu.search.util.CrossTenantUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;


// TODO This class is a duplicate of byoc and gcp, should likely be moved to the core folder
abstract class QueryBase {

    @Inject
    DpsHeaders dpsHeaders;
    @Inject
    private JaxRsDpsLog log;
    @Inject
    private IProviderHeaderService providerHeaderService;
    @Inject
    private CrossTenantUtils crossTenantUtils;
    @Inject
    private FieldMappingTypeService fieldMappingTypeService;

    static final String AGGREGATION_NAME = "agg";
    private static final String GEO_SHAPE_INDEXED_TYPE = "geo_shape";

    // if returnedField contains property matching from excludes than query result will NOT include that property
    private final Set<String> excludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.X_ACL.getValue()));

    // queryableExcludes properties can be returned by query results
    private final Set<String> queryableExcludes = new HashSet<>(Arrays.asList(RecordMetaAttribute.INDEX_STATUS.getValue()));

    private final TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    private boolean useGeoShapeQuery = false;

    QueryBuilder buildQuery(String simpleQuery, SpatialFilter spatialFilter, boolean asOwner) throws AppException, IOException {

        QueryBuilder textQueryBuilder = null;
        QueryBuilder spatialQueryBuilder = null;
        QueryBuilder authorizationQueryBuilder;
        QueryBuilder queryBuilder = null;

        if (StringUtils.isNotEmpty(simpleQuery)) {
            textQueryBuilder = getSimpleQuery(simpleQuery);
        }

        // use only one of the spatial request
        if (spatialFilter != null) {
            if (useGeoShapeQuery) {
                if (spatialFilter.getByBoundingBox() != null) {
                    spatialQueryBuilder = getGeoShapeBoundingBoxQuery(spatialFilter);
                } else if (spatialFilter.getByDistance() != null) {
                    spatialQueryBuilder = getGeoShapeDistanceQuery(spatialFilter);
                } else if (spatialFilter.getByGeoPolygon() != null) {
                    spatialQueryBuilder = getGeoShapePolygonQuery(spatialFilter);
                }
            } else {
                if (spatialFilter.getByBoundingBox() != null) {
                    spatialQueryBuilder = getBoundingBoxQuery(spatialFilter);
                } else if (spatialFilter.getByDistance() != null) {
                    spatialQueryBuilder = getDistanceQuery(spatialFilter);
                } else if (spatialFilter.getByGeoPolygon() != null) {
                    spatialQueryBuilder = getGeoPolygonQuery(spatialFilter);
                }
            }
        }

        // apply authorization filters
        String groups = dpsHeaders.getHeaders().get(providerHeaderService.getDataGroupsHeader());
        String[] groupArray = groups.trim().split("\\s*,\\s*");
        if (asOwner) {
            authorizationQueryBuilder = boolQuery().minimumShouldMatch("1").should(termsQuery(
                    AclRole.OWNERS.getPath(), groupArray));
        } else {
            authorizationQueryBuilder = boolQuery().minimumShouldMatch("1").should(termsQuery(RecordMetaAttribute.X_ACL.getValue(), groupArray));
        }

        if (textQueryBuilder != null) {
            queryBuilder = boolQuery().must(textQueryBuilder);
        }
        if (spatialQueryBuilder != null) {
            queryBuilder = queryBuilder != null ? boolQuery().must(queryBuilder).must(spatialQueryBuilder) : boolQuery().must(spatialQueryBuilder);
        }
        if (authorizationQueryBuilder != null) {
            queryBuilder = queryBuilder != null ? boolQuery().must(queryBuilder).must(authorizationQueryBuilder) : boolQuery().must(authorizationQueryBuilder);
        }

        return queryBuilder;
    }

    private QueryBuilder getSimpleQuery(String searchQuery) {

        // if query is empty , then put *
        String query = StringUtils.isNotBlank(searchQuery) ? searchQuery : "*";
        return queryStringQuery(query).allowLeadingWildcard(false);
    }

    private QueryBuilder getBoundingBoxQuery(SpatialFilter spatialFilter) throws AppException {

        GeoPoint topLeft = new GeoPoint(spatialFilter.getByBoundingBox().getTopLeft().getLatitude(), spatialFilter.getByBoundingBox().getTopLeft().getLongitude());
        GeoPoint bottomRight = new GeoPoint(spatialFilter.getByBoundingBox().getBottomRight().getLatitude(), spatialFilter.getByBoundingBox().getBottomRight().getLongitude());
        return geoBoundingBoxQuery(spatialFilter.getField()).setCorners(topLeft, bottomRight);
    }

    private QueryBuilder getDistanceQuery(SpatialFilter spatialFilter) throws AppException {

        return geoDistanceQuery(spatialFilter.getField())
                .point(spatialFilter.getByDistance().getPoint().getLatitude(), spatialFilter.getByDistance().getPoint().getLongitude())
                .distance(spatialFilter.getByDistance().getDistance(), DistanceUnit.METERS);
    }

    private QueryBuilder getGeoPolygonQuery(SpatialFilter spatialFilter) throws AppException {

        List<GeoPoint> points = new ArrayList<>();
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new GeoPoint(point.getLatitude(), point.getLongitude()));
        }
        return geoPolygonQuery(spatialFilter.getField(), points);
    }

    private QueryBuilder getGeoShapePolygonQuery(SpatialFilter spatialFilter) throws IOException {

        List<Coordinate> points = new ArrayList<>();
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new Coordinate(point.getLongitude(), point.getLatitude()));
        }
        CoordinatesBuilder cb = new CoordinatesBuilder().coordinates(points);
        return geoWithinQuery(spatialFilter.getField(), new PolygonBuilder(cb));
    }

    private QueryBuilder getGeoShapeBoundingBoxQuery(SpatialFilter spatialFilter) throws IOException {

        Coordinate topLeft = new Coordinate(spatialFilter.getByBoundingBox().getTopLeft().getLongitude(), spatialFilter.getByBoundingBox().getTopLeft().getLatitude());
        Coordinate bottomRight = new Coordinate(spatialFilter.getByBoundingBox().getBottomRight().getLongitude(), spatialFilter.getByBoundingBox().getBottomRight().getLatitude());
        return geoWithinQuery(spatialFilter.getField(), new EnvelopeBuilder(topLeft, bottomRight));
    }

    private QueryBuilder getGeoShapeDistanceQuery(SpatialFilter spatialFilter) throws IOException {
        // broken functionality in Elasticsearch in current version, fixed in v7.7.0 onwards: https://github.com/elastic/elasticsearch/pull/53466
        throw new AppException(HttpServletResponse.SC_NOT_FOUND, "Distance query is not supported for GeoShape field", "Invalid request");
    }

    String getIndex(Query request) {
        return this.crossTenantUtils.getIndexName(request, this.dpsHeaders.getPartitionId());
    }

    List<Map<String, Object>> getHitsFromSearchResponse(SearchResponse searchResponse) {
        List<Map<String, Object>> results = new ArrayList<>();
        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.getHits().length != 0) {
            for (SearchHit searchHitFields : searchHits.getHits()) {
                Map<String, Object> hitFields = searchHitFields.getSourceAsMap();
                if (!searchHitFields.getHighlightFields().isEmpty()) {
                    for (HighlightField hf : searchHitFields.getHighlightFields().values()) {
                        if (!hf.getName().equalsIgnoreCase(RecordMetaAttribute.X_ACL.getValue())) {
                            Text[] fragments = hf.getFragments();
                            if (fragments.length > 0) {
                                hitFields.put(hf.getName(), fragments[0].toString());
                            }
                        }
                    }
                }
                results.add(hitFields);
            }
            return results;
        }

        return null;
    }

    List<AggregationResponse> getAggregationFromSearchResponse(SearchResponse searchResponse) {
        List<AggregationResponse> results = null;

        if (searchResponse.getAggregations() != null) {
            Terms kindAgg = searchResponse.getAggregations().get(AGGREGATION_NAME);
            if (kindAgg.getBuckets() != null) {
                results = new ArrayList<>();
                for (Terms.Bucket bucket : kindAgg.getBuckets()) {
                    results.add(AggregationResponse.builder().key(bucket.getKeyAsString()).count(bucket.getDocCount()).build());
                }
            }
        }

        return results;
    }

    SearchSourceBuilder createSearchSourceBuilder(Query request) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // build query: set query options and query
        QueryBuilder queryBuilder = buildQuery(request.getQuery(), request.getSpatialFilter(), request.isQueryAsOwner());
        sourceBuilder.size(QueryUtils.getResultSizeForQuery(request.getLimit()));
        sourceBuilder.query(queryBuilder);
        sourceBuilder.timeout(REQUEST_TIMEOUT);

        // set highlighter
        if (request.isReturnHighlightedFields()) {
            HighlightBuilder highlightBuilder = new HighlightBuilder().field("*", 200, 5);
            sourceBuilder.highlighter(highlightBuilder);
        }

        // sort: text is not suitable for sorting or aggregation, refer to: this: https://github.com/elastic/elasticsearch/issues/28638,
        // so keyword is recommended for unmappedType in general because it can handle both string and number.
        // It will ignore the characters longer than the threshold when sorting.
        if (request.getSort() != null) {
            for (int idx = 0; idx < request.getSort().getField().size(); idx++) {
                sourceBuilder.sort(new FieldSortBuilder(request.getSort().getFieldByIndex(idx))
                        .order(SortOrder.fromString(request.getSort().getOrderByIndex(idx).name()))
                        .missing("_last")
                        .unmappedType("keyword"));
            }
        }

        // set the return fields
        List<String> returnedFields = request.getReturnedFields();
        if (returnedFields == null) {
            returnedFields = new ArrayList<>();
        }
        Set<String> returnedFieldsSet = new HashSet<>(returnedFields);
        String[] includesArr = returnedFieldsSet.toArray(new String[returnedFieldsSet.size()]);

        // remove all matching returnedField and queryable from excludes
        Set<String> requestQueryableExcludes = new HashSet<>(queryableExcludes);
        Set<String> requestExcludes = new HashSet<>(excludes);
        requestQueryableExcludes.removeAll(returnedFields);
        requestExcludes.addAll(requestQueryableExcludes);
        String[] excludesArr = requestExcludes.toArray(new String[requestExcludes.size()]);

        sourceBuilder.fetchSource(includesArr, excludesArr);
        return sourceBuilder;
    }

    SearchResponse makeSearchRequest(Query searchRequest, RestHighLevelClient client) {
        Long startTime = 0L;
        SearchRequest elasticSearchRequest = null;
        SearchResponse searchResponse = null;

        try {
            if (searchRequest.getSpatialFilter() != null) {
                useGeoShapeQuery = this.useGeoShapeQuery(client, searchRequest, this.getIndex(searchRequest));
            }

            elasticSearchRequest = createElasticRequest(searchRequest);
            startTime = System.currentTimeMillis();
            searchResponse = client.search(elasticSearchRequest, RequestOptions.DEFAULT);
            return searchResponse;
        } catch (ElasticsearchStatusException e) {
            switch (e.status()) {
                case NOT_FOUND:
                    throw new AppException(HttpServletResponse.SC_NOT_FOUND, "Not Found", "Resource you are trying to find does not exists", e);
                case BAD_REQUEST:
                    throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "Invalid parameters were given on search request", e);
                case SERVICE_UNAVAILABLE:
                    throw new AppException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Search error", "Please re-try search after some time.", e);
                default:
                    throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
            }
        } catch (IOException e) {
            if (e.getMessage().startsWith("listener timeout after waiting for")) {
                throw new AppException(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Search error", String.format("Request timed out after waiting for %sm", REQUEST_TIMEOUT.getMinutes()), e);
            }
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
        } catch (Exception e) {
            throw new AppException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Search error", "Error processing search request", e);
        } finally {
            Long latency = System.currentTimeMillis() - startTime;
            String request = elasticSearchRequest != null ? elasticSearchRequest.source().toString() : searchRequest.toString();
            this.log.info(String.format("elastic latency: %s | elastic request-payload: %s", latency, request));
            this.auditLog(searchRequest, searchResponse);
        }
    }

    private boolean useGeoShapeQuery(RestHighLevelClient client, Query searchRequest, String index) throws IOException {
        Set<String> indexedTypes = this.fieldMappingTypeService.getFieldTypes(client, searchRequest.getSpatialFilter().getField(), index);
        // fallback to geo_point search if mixed type found for spatialFilter.field
        if (indexedTypes.isEmpty() || indexedTypes.size() > 1) return false;
        return indexedTypes.contains(GEO_SHAPE_INDEXED_TYPE);
    }

    abstract SearchRequest createElasticRequest(Query request) throws AppException, IOException;

    abstract void querySuccessAuditLogger(Query request);

    abstract void queryFailedAuditLogger(Query request);

    private void auditLog(Query searchRequest, SearchResponse searchResponse) {
        if (searchResponse != null && searchResponse.status() == RestStatus.OK) {
            this.querySuccessAuditLogger(searchRequest);
            return;
        }
        this.queryFailedAuditLogger(searchRequest);
    }
}