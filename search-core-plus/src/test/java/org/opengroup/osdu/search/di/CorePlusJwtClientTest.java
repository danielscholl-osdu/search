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

package org.opengroup.osdu.search.di;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.auth.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CorePlusJwtClientTest {

    @Autowired
    CorePlusJwtClient client;

    @Test
    void getIdToken_returnsEmptyString_whenTokenProviderIsNull() {
        CorePlusJwtClient client = new CorePlusJwtClient();
        String token = client.getIdToken("any-service-account");
        assertEquals("", token);
    }

    @Test
    void getIdToken_returnsToken_whenTokenProviderPresent() {
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockProvider.getIdToken()).thenReturn("the-token");

        CorePlusJwtClient client = new CorePlusJwtClient();
        client.tokenProvider = mockProvider;
        String token = client.getIdToken("any-service-account");

        assertEquals("the-token", token);
        verify(mockProvider, times(1)).getIdToken();
    }
}
