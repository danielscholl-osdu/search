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

package org.opengroup.osdu.search.middleware;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.search.context.UserContext;

@ExtendWith(MockitoExtension.class)
public class AuthorizationFilterTest {

    @Mock
    private IAuthorizationService authorizationService;
    @Mock
    private DpsHeaders requestHeaders;
    @Mock
    private HttpServletRequest request;
    @Mock
    private UserContext userContext;

    @InjectMocks
    private AuthorizationFilter authorizationFilter;

    private AuthorizationResponse buildAuthResponse(String user, List<GroupInfo> groupInfos) {
        Groups groups = new Groups();
        groups.setGroups(groupInfos);

        AuthorizationResponse response = mock(AuthorizationResponse.class);
        when(response.getUser()).thenReturn(user);
        when(response.getGroups()).thenReturn(groups);
        when(response.getUserAuthorizedGroupName()).thenReturn("authorized-group");
        return response;
    }

    private GroupInfo createGroupInfo(String name, String email) {
        GroupInfo info = new GroupInfo();
        info.setName(name);
        info.setEmail(email);
        return info;
    }

    @Test
    void hasPermission_returnsTrue_forSwaggerGetRequest() {
        when(request.getServletPath()).thenReturn("/swagger.json");
        when(request.getMethod()).thenReturn("GET");

        assertTrue(authorizationFilter.hasPermission("role1"));
    }

    @Test
    void hasPermission_doesNotShortCircuit_forGetNonSwaggerPath() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("GET");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1");

        GroupInfo dataGroup = createGroupInfo("data.viewers", "data.viewers@tenant1.example.com");
        AuthorizationResponse authResponse = buildAuthResponse("user@example.com", List.of(dataGroup));

        when(authorizationService.authorizeAny(any(DpsHeaders.class), any(String[].class)))
                .thenReturn(authResponse);

        assertTrue(authorizationFilter.hasPermission("role1"));
        verify(userContext).setDataGroups(anyList());
    }

    @Test
    void hasPermission_returnsTrue_whenUserHasDataGroups() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1");

        GroupInfo dataGroup = createGroupInfo("data.viewers", "data.viewers@tenant1.example.com");
        AuthorizationResponse authResponse = buildAuthResponse("user@example.com", List.of(dataGroup));

        when(authorizationService.authorizeAny(any(DpsHeaders.class), any(String[].class)))
                .thenReturn(authResponse);

        assertTrue(authorizationFilter.hasPermission("role1"));
        verify(userContext).setDataGroups(argThat(list -> list.contains("data.viewers@tenant1.example.com")));
        verify(userContext).setRootUser(false);
    }

    @Test
    void hasPermission_returnsFalse_whenAuthorizationThrowsAppException() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1");

        when(authorizationService.authorizeAny(any(DpsHeaders.class), any(String[].class)))
                .thenThrow(new AppException(403, "Forbidden", "unauthorized"));

        assertFalse(authorizationFilter.hasPermission("role1"));
    }

    @Test
    void hasPermission_returnsFalse_whenNoDataGroupsFound() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1");

        GroupInfo nonDataGroup = createGroupInfo("service.search.user", "service.search.user@tenant1.example.com");
        AuthorizationResponse authResponse = buildAuthResponse("user@example.com", List.of(nonDataGroup));

        when(authorizationService.authorizeAny(any(DpsHeaders.class), any(String[].class)))
                .thenReturn(authResponse);

        assertFalse(authorizationFilter.hasPermission("role1"));
    }

    @Test
    void hasPermission_setsRootUser_whenDataRootGroupPresent() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1");

        GroupInfo dataGroup = createGroupInfo("data.default.viewers", "data.default.viewers@tenant1.example.com");
        GroupInfo rootGroup = createGroupInfo("users.data.root", "users.data.root@tenant1.example.com");
        AuthorizationResponse authResponse = buildAuthResponse("admin@example.com", List.of(dataGroup, rootGroup));

        when(authorizationService.authorizeAny(any(DpsHeaders.class), any(String[].class)))
                .thenReturn(authResponse);

        assertTrue(authorizationFilter.hasPermission("role1"));
        verify(userContext).setRootUser(true);
    }

    @Test
    void hasPermission_returnsFalse_whenPartitionIdIsEmpty() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("");

        assertFalse(authorizationFilter.hasPermission("role1"));
    }

    @Test
    void hasPermission_returnsFalse_whenPartitionIdIsNull() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn(null);

        assertFalse(authorizationFilter.hasPermission("role1"));
    }

    @Test
    void hasPermission_authorizesEachPartition_whenMultipleAccountIds() {
        when(request.getServletPath()).thenReturn("/query");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1,tenant2");

        GroupInfo dataGroup = createGroupInfo("data.viewers", "data.viewers@example.com");
        AuthorizationResponse authResponse = buildAuthResponse("user@example.com", List.of(dataGroup));

        when(authorizationService.authorizeAny(any(DpsHeaders.class), any(String[].class)))
                .thenReturn(authResponse);

        assertTrue(authorizationFilter.hasPermission("role1"));
        verify(authorizationService, times(2)).authorizeAny(any(DpsHeaders.class), any(String[].class));
    }

    @Test
    void hasPermission_returnsFalse_whenMultiplePartitionsOnIndexApi() {
        when(request.getServletPath()).thenReturn("/index");
        when(request.getMethod()).thenReturn("POST");
        when(requestHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn("tenant1,tenant2");

        assertFalse(authorizationFilter.hasPermission("role1"));
    }
}
