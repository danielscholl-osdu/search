package org.opengroup.osdu.search.middleware;


import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.search.context.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component("authorizationFilter")
@RequestScope
public class AuthorizationFilter {

    private static final String DATA_GROUP_PREFIX = "data.";
    private static final String DATA_ROOT_GROUP = "users.data.root";

    private static final String PATH_SWAGGER = "/swagger.json";
    private static final String PATH_INDEX_API = "/index";

    @Inject
    private IAuthorizationService authorizationService;

    @Inject
    private DpsHeaders requestHeaders;

    @Inject
    private HttpServletRequest request;

    @Inject
    private UserContext userContext;

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
            return false;
        }
        return true;
    }


    private void checkApiAccess(String[] requiredRoles, String path, DpsHeaders requestHeaders) {
        List<String> accountIds = validateAccountId(requestHeaders, path);
        List<String> dataGroups = new ArrayList<>();
        boolean dataRootUser = false;
        requestHeaders.put(DpsHeaders.PRIMARY_PARTITION_ID, getPrimaryAccountId(accountIds));
        // TODO: change this once client lib is updated with required method
        for (String accountId : accountIds) {
            requestHeaders.put(DpsHeaders.ACCOUNT_ID, accountId);
            requestHeaders.put(DpsHeaders.DATA_PARTITION_ID, accountId);
            AuthorizationResponse authorizationResponse = authorizationService.authorizeAny(requestHeaders, requiredRoles);
            requestHeaders.put(DpsHeaders.USER_EMAIL, authorizationResponse.getUser());

            for (GroupInfo gInfo : authorizationResponse.getGroups().getGroups()) {
                if (gInfo.getName().startsWith(DATA_GROUP_PREFIX)) {
                    dataGroups.add(gInfo.getEmail());
                }

                if (gInfo.getName().equals(DATA_ROOT_GROUP)) {
                    dataRootUser = true;
                }
            }
        }
        requestHeaders.put(DpsHeaders.ACCOUNT_ID, StringUtils.join(accountIds, ","));
        requestHeaders.put(DpsHeaders.DATA_PARTITION_ID, StringUtils.join(accountIds, ","));

        // don't proceed if data groups are empty
        if (dataGroups.isEmpty()) {
            throw AppException.createForbidden("no data group found for user");
        }
        // Store groups and root user flag in UserContext
        userContext.setDataGroups(dataGroups);
        userContext.setRootUser(dataRootUser);
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
