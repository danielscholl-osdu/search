// Copyright © Microsoft Corporation
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;

@ExtendWith(MockitoExtension.class)
public class RedirectHttpRequestsHandlerTest {

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private ServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private RedirectHttpRequestsHandler filter;

    private static final String CRON_HEADER_NAME = "X-AppEngine-Cron";

    @BeforeEach
    void setUp() {
        filter = new RedirectHttpRequestsHandler();
        httpRequest = mock(HttpServletRequest.class);
        response = mock(ServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void doFilter_throwsServletException_whenNotHttpServletRequest() {
        ServletRequest nonHttp = mock(ServletRequest.class);

        assertThrows(ServletException.class,
                () -> filter.doFilter(nonHttp, response, chain));

        verifyNoInteractions(chain);
    }

    @Test
    void doFilter_throwsAppException302_whenSchemeHttp() {
        when(httpRequest.getScheme()).thenReturn("http");

        AppException ex = assertThrows(AppException.class,
                () -> filter.doFilter(httpRequest, response, chain));

        assertTrue(ex.getMessage().contains("HTTP is not supported"), "Should explain HTTP not supported");
        verifyNoMoreInteractions(chain);
    }
}
