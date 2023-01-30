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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.service.IEntitlementsExtensionService;

import javax.ws.rs.NotSupportedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AzureAuthorizationServiceTest {

    @Mock
    private IEntitlementsExtensionService entitlementsService;

    @Mock
    private Groups groups;

    @InjectMocks
    private AzureAuthorizationService sut;

    @Test(expected = AppException.class)
    public void should_throwAppException_when_givenGroupDoesNotExistForUser() {
        when(groups.any(any())).thenReturn(false);
        when(entitlementsService.getGroups(any())).thenReturn(groups);
        sut.authorizeAny(new DpsHeaders(), "a", "b");
    }

    @Test
    public void should_returnGroupsWithUserEmail_when_givenGroupExistsForUser() {
        when(groups.any(any())).thenReturn(true);
        when(entitlementsService.getGroups(any())).thenReturn(groups);
        sut.authorizeAny(new DpsHeaders(), "a", "b");
    }
}
