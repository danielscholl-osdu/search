//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.provider.azure.provider.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.service.IEntitlementsExtensionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AzureAuthorizationServiceTest {

    @Mock
    private IEntitlementsExtensionService entitlementsService;

    @Mock
    private Groups groups;

    @InjectMocks
    private AzureAuthorizationService sut;

    @Test
    public void should_throwAppException_when_givenGroupDoesNotExistForUser() {
        when(groups.any(any())).thenReturn(false);
        when(entitlementsService.getGroups(any())).thenReturn(groups);
        DpsHeaders dpsHeaders = new DpsHeaders();

        assertThrows(AppException.class, () -> sut.authorizeAny(dpsHeaders, "a", "b"));
    }

    @Test
    public void should_returnGroupsWithUserEmail_when_givenGroupExistsForUser() {
        when(groups.any(any())).thenReturn(true);
        when(entitlementsService.getGroups(any())).thenReturn(groups);

        AuthorizationResponse authorizationResponse = sut.authorizeAny(new DpsHeaders(), "a", "b");

        assertNotNull(authorizationResponse);
    }

    @Test
    public void should_throwNotImplementedException_when_authorizeAnyForPartition() {
        DpsHeaders dpsHeaders = new DpsHeaders();
        String expectedMessage = "authorizeAny not implemented for azure";
        NotImplementedException notImplementedException = assertThrows(NotImplementedException.class,
                () -> sut.authorizeAny("test-partition1", dpsHeaders, "a", "b"));

        assertEquals(expectedMessage, notImplementedException.getMessage());
    }
}
