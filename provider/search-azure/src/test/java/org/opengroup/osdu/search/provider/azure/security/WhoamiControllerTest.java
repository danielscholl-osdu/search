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

package org.opengroup.osdu.search.provider.azure.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class WhoamiControllerTest {

    private static final String userName = "username";
    private static final String roles = "roles";
    private static final String details = "details";

    @InjectMocks
    private WhoamiController sut;

    private class DummyPrincipal {
        @Override
        public String toString() {
            return details;
        }
    }

    public class DummyAuthority extends ArrayList {
        @Override
        public String toString() {
            return roles;
        }
    }

    @Before
    public void init() {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        doReturn(auth).when(securityContext).getAuthentication();
        doReturn(userName).when(auth).getName();
        doReturn(new DummyAuthority()).when(auth).getAuthorities();
        doReturn(new DummyPrincipal()).when(auth).getPrincipal();
        sut = new WhoamiController(securityContext);
    }

    @Test
    public void testWhoamiResponse() {
        String response = sut.whoami();
        assertEquals("user: " + userName + "<BR>roles: " + roles + "<BR>details: " + details, response);
    }
}