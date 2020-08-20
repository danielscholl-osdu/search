package org.opengroup.osdu.search.provider.reference.provider.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CcsQueryRequest;
import org.opengroup.osdu.core.common.model.search.CcsQueryResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.search.provider.interfaces.ICcsQueryService;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.springframework.stereotype.Service;

// TODO: Remove this temporary implementation when ECE CCS is utilized
@Service
public class CcsQueryServiceImpl implements ICcsQueryService {

    @Inject
    private DpsHeaders dpsHeaders;

    @Inject
    private ITenantFactory tenantStorageFactory;

    @Inject
    private IElasticRepository elasticRepository;

    @Inject
    private IQueryService queryService;

    @Override
    public CcsQueryResponse makeRequest(final CcsQueryRequest ccsQueryRequest) throws Exception {
        List<String> accounts = Arrays.asList(dpsHeaders.getPartitionIdWithFallbackToAccountId().trim().split("\\s*,\\s*"));
        List<QueryResponse> tenantResponses = getTenantResponses(accounts, ccsQueryRequest);
        return convertQueryResponseToCcsQueryResponse(getCompoundResponse(tenantResponses));
    }

    private List<QueryResponse> getTenantResponses(final List<String> accounts, final CcsQueryRequest ccsQueryRequest) throws Exception {
        List<QueryResponse> tenantResponses = new ArrayList<>();
        if (Config.isSmartSearchCcsDisabled() || accounts.size() == 1) {
            TenantInfo tenant = tenantStorageFactory.getTenantInfo(this.dpsHeaders.getPartitionIdWithFallbackToAccountId());
            tenantResponses.add(queryService.queryIndex(convertCcsQueryRequestToQueryRequest(ccsQueryRequest),
                    elasticRepository.getElasticClusterSettings(tenant)));
        } else {
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<QueryResponse>> futureResponses = new ArrayList<>();
            for (String account : accounts) {
                futureResponses.add(executorService.submit(() -> {
                    TenantInfo tenant = tenantStorageFactory.getTenantInfo(account);
                    return queryService.queryIndex(convertCcsQueryRequestToQueryRequest(ccsQueryRequest),
                            elasticRepository.getElasticClusterSettings(tenant));
                }));
            }
            for (Future<QueryResponse> futureResponse : futureResponses) {
                // TODO: Check why it can be null, this doesn't look right and leads to redundant null checks in other services
                QueryResponse tenantResponse = futureResponse.get();
                if (tenantResponse != null) {
                    tenantResponses.add(tenantResponse);
                }
            }
            executorService.shutdown();
        }
        return tenantResponses;
    }

    private QueryResponse getCompoundResponse(final List<QueryResponse> tenantResponses) {
        QueryResponse response = new QueryResponse();
        if (tenantResponses.isEmpty()) {
            return response;
        } else {
            for (QueryResponse tenantResponse : tenantResponses) {
                response.setTotalCount(response.getTotalCount() + tenantResponse.getTotalCount());
            }
            tenantResponses.sort(Comparator.comparingLong(QueryResponse::getTotalCount).reversed());
            QueryResponse largestResponse = tenantResponses.remove(0);
            List<Map<String, Object>> results = new LinkedList<>();
            for (Map<String, Object> result : largestResponse.getResults()) {
                results.add(result);
                int index = largestResponse.getResults().indexOf(result);
                for (QueryResponse tenantResponse : tenantResponses) {
                    if (index < tenantResponse.getResults().size()) {
                        results.add(tenantResponse.getResults().get(index));
                    }
                }
            }
            response.setResults(results);
            return response;
        }
    }

    private QueryRequest convertCcsQueryRequestToQueryRequest(final CcsQueryRequest ccsQueryRequest) {
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setFrom(ccsQueryRequest.getFrom());
        queryRequest.setKind(ccsQueryRequest.getKind());
        queryRequest.setLimit(ccsQueryRequest.getLimit());
        queryRequest.setQuery(ccsQueryRequest.getQuery());
        queryRequest.setQueryAsOwner(ccsQueryRequest.isQueryAsOwner());
        return queryRequest;
    }

    private CcsQueryResponse convertQueryResponseToCcsQueryResponse(final QueryResponse queryResponse) {
        CcsQueryResponse ccsQueryResponse = new CcsQueryResponse();
        ccsQueryResponse.setResults(queryResponse.getResults());
        ccsQueryResponse.setTotalCount(queryResponse.getTotalCount());
        return ccsQueryResponse;
    }
}
