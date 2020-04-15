package org.opengroup.osdu.search.middleware;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;

import com.google.common.base.Strings;

@Component("authorizationFilter")
@RequestScope
public class AuthorizationFilter {
	
    private static final String DATA_GROUP_PREFIX = "data.";

    private static final String PATH_SWAGGER = "/swagger.json";
    private static final String PATH_INDEX_API = "/index";
    private static final String PATH_CRON_HANDLERS = "cron-handlers";
    private static final String PATH_CCS = "/ccs/query";


    @Inject
    private IAuthorizationService authorizationService;

    @Inject
    private DpsHeaders requestHeaders;

    @Inject
    private IProviderHeaderService providerHeaderService;
    
    @Inject
    private HttpServletRequest request;
        
    public boolean hasPermission(String... requiredRoles) {    
        
        String path = request.getServletPath();

        if ("GET".equals(request.getMethod())) {
            if (path.equals(PATH_SWAGGER)) {
                return true;
            }
        }
    	

        try {
            checkApiAccess(requiredRoles, path, requestHeaders);
        } catch (AppException e) {
            if (Arrays.asList(requiredRoles).contains(SearchServiceRole.CRON) && "GET".equals(request.getMethod())) {
                if (path.contains(PATH_CRON_HANDLERS)) {
                    checkCronApiAccess(requestHeaders);
                    return true;
                }
            } else {
                return false;
            }
        }
        return true;
    }
    

    private void checkApiAccess(String[] requiredRoles, String path, DpsHeaders requestHeaders) {
        List<String> accountIds = validateAccountId(requestHeaders, path);
        List<String> dataGroups = new ArrayList<>();
        if (path.contains(PATH_CCS)) {
            requestHeaders.put(DpsHeaders.PRIMARY_PARTITION_ID, getPrimaryAccountId(accountIds));
            AuthorizationResponse authorizationResponse = authorizationService.authorizeAny(requestHeaders, requiredRoles);
            requestHeaders.put(DpsHeaders.USER_EMAIL, authorizationResponse.getUser());
            dataGroups.addAll(authorizationResponse.getGroups().getGroups()
                    .stream().filter(gInfo -> gInfo.getName().startsWith(DATA_GROUP_PREFIX)).map(GroupInfo::getEmail).distinct().collect(Collectors.toList()));
        } else {
            requestHeaders.put(DpsHeaders.PRIMARY_PARTITION_ID, getPrimaryAccountId(accountIds));
            // TODO: change this once client lib is updated with required method
            for (String accountId : accountIds) {
                requestHeaders.put(DpsHeaders.ACCOUNT_ID, accountId);
                requestHeaders.put(DpsHeaders.DATA_PARTITION_ID, accountId);
                AuthorizationResponse authorizationResponse = authorizationService.authorizeAny(requestHeaders, requiredRoles);
                requestHeaders.put(DpsHeaders.USER_EMAIL, authorizationResponse.getUser());

                dataGroups.addAll(authorizationResponse.getGroups().getGroups()
                        .stream().filter(gInfo -> gInfo.getName().startsWith(DATA_GROUP_PREFIX)).map(GroupInfo::getEmail).distinct().collect(Collectors.toList()));
            }
            requestHeaders.put(DpsHeaders.ACCOUNT_ID, StringUtils.join(accountIds, ","));
            requestHeaders.put(DpsHeaders.DATA_PARTITION_ID, StringUtils.join(accountIds, ","));
        }
        // don't proceed if data groups are empty
        if (dataGroups.isEmpty()) throw AppException.createForbidden("no data group found for user");
        requestHeaders.put(providerHeaderService.getDataGroupsHeader(), StringUtils.join(dataGroups, ','));

    }

    private List<String> validateAccountId(DpsHeaders requestHeaders, String path) {
        String accountHeader = requestHeaders.getPartitionIdWithFallbackToAccountId();
        String debuggingInfo = String.format("%s:%s", DpsHeaders.ACCOUNT_ID, accountHeader);

        if (Strings.isNullOrEmpty(accountHeader)) {
            throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad request", "invalid or empty data partition provided", debuggingInfo);
        }

        List<String> accountIds = Arrays.asList(accountHeader.trim().split("\\s*,\\s*"));
        if (accountIds.isEmpty()) {
            throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad request", "invalid or empty data partition provided", debuggingInfo);
        }

        // TODO: remove this once IndexApi methods are moved to indexer service
        if (path.contains(PATH_INDEX_API)) {
            if (accountIds.size() > 1) {
                throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad request", "multi-valued data partition not supported for the API", debuggingInfo);
            }
        }
        return accountIds;
    }


    private void checkCronApiAccess(DpsHeaders headersInfo) {
        String expectedCronHeaderValue = "true";
        String cronHeader = headersInfo.getHeaders().getOrDefault(providerHeaderService.getCronServiceHeader(), null);
        if(expectedCronHeaderValue.equalsIgnoreCase(cronHeader))return;
        throw AppException.createForbidden("invalid user agent, Engine Cron only");
    }

    String getPrimaryAccountId(List<String> accountIds) {
        if (accountIds.size() == 1) {
            return accountIds.get(0);
        }

        String primaryAccountId = null;
        for (String id : accountIds) {
            if (id.equalsIgnoreCase(TenantInfo.COMMON)) continue;
            primaryAccountId = id;
            break;
        }
        if (Strings.isNullOrEmpty(primaryAccountId)) primaryAccountId = accountIds.get(0);

        return primaryAccountId;
    }
    
}