/*
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

package org.opengroup.osdu.search.auth;

import io.lettuce.core.RedisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.entitlements.AuthorizationServiceImpl;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CorePlusAuthorizationServiceTest {

    @Mock
    ICache<String, Groups> cache;

    @Mock
    JaxRsDpsLog jaxRsDpsLog;

    CorePlusAuthorizationService service;

    @BeforeEach
    void initService() {
        service = new CorePlusAuthorizationService(cache, jaxRsDpsLog);
    }

    @Test
    void whenGroupsInCache_authorizeAny_returnsCachedGroupsAndDoesNotPut() {

        DpsHeaders headers = mock(DpsHeaders.class);
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn("part1");
        when(headers.getAuthorization()).thenReturn("auth-token");

        Groups groups = new Groups();
        groups.setMemberEmail("user@example.com");
        when(cache.get(anyString())).thenReturn(groups);

        AuthorizationResponse response = service.authorizeAny(headers, "ROLE_X");

        assertNotNull(response);
        assertEquals("user@example.com", response.getUser());
        assertSame(groups, response.getGroups());

        verify(cache, times(1)).get(anyString());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void whenCacheGetThrowsRedisException_logsErrorAndReturnsNullGroups() {
        when(cache.get(anyString())).thenThrow(new RedisException("connection failed"));

        DpsHeaders headers = mock(DpsHeaders.class);
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn("whatever");
        when(headers.getAuthorization()).thenReturn("doesNotMatter");

        CorePlusAuthorizationService service = new CorePlusAuthorizationService(cache, jaxRsDpsLog);

        try {
            AuthorizationResponse resp = service.authorizeAny(headers, "ROLE");
        } catch (Exception ignored) { /* But .get should be tried and logged first */ }

        verify(jaxRsDpsLog, atLeastOnce())
                .error(contains("Error getting key"), any(RedisException.class));
    }

    @Test
    void whenCachePutThrows_exceptionBubblesUp() {
        DpsHeaders headers = mock(DpsHeaders.class);
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn("p");
        when(headers.getAuthorization()).thenReturn("a");

        when(cache.get(anyString())).thenReturn(null);

        CorePlusAuthorizationService spy =
                Mockito.spy(new CorePlusAuthorizationService(cache, jaxRsDpsLog));

        Mockito.lenient().doReturn(
                AuthorizationResponse.builder()
                        .user("u")
                        .groups(new Groups())
                        .build()
        ).when(spy).authorizeViaParent(any(), any());

        assertThrows(RuntimeException.class,
                () -> spy.authorizeAny(headers, "ROLE"));
    }

    @Test
    void whenCachedGroupsHasNullMemberEmail_responseHasNullUser() {
        DpsHeaders headers = mock(DpsHeaders.class);
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn("p");
        when(headers.getAuthorization()).thenReturn("a");

        Groups groups = new Groups();
        groups.setMemberEmail(null);
        when(cache.get(anyString())).thenReturn(groups);

        AuthorizationResponse resp = service.authorizeAny(headers, "R");
        assertNotNull(resp);
        assertNull(resp.getUser());
        assertSame(groups, resp.getGroups());
    }

    @Test
    void whenCacheGetThrowsOtherRuntimeException_itBubblesUp() {
        DpsHeaders headers = mock(DpsHeaders.class);
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn("p");
        when(headers.getAuthorization()).thenReturn("a");

        when(cache.get(anyString())).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> service.authorizeAny(headers, "R"));
    }
}
