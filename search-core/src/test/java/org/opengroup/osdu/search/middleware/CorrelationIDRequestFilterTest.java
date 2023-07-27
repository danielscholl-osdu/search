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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.opengroup.osdu.search.middleware.RequestHeadersTestUtil.setupRequestHeaderMock;

import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.mockito.junit.MockitoJUnitRunner;

import org.opengroup.osdu.core.common.model.http.Request;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationIDRequestFilterTest {


    @Mock
    private ResourceInfo resourceInfo;


    @InjectMocks
    private CorrelationIDRequestFilter sut;

    @Mock
    private DpsHeaders dpsHeaders;


	@Mock
	private HttpServletRequest httpRequest;

	@Mock
	private HttpServletResponse httpResponse;

	@Mock
	private FilterChain filterChain;

    @Mock
    private JaxRsDpsLog logger;

    @Before
    public void setup() throws Exception {
        doNothing().when(filterChain).doFilter(httpRequest, httpResponse);

        dpsHeaders = new DpsHeaders();
        dpsHeaders.addCorrelationIdIfMissing();

    	when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpResponse.getStatus()).thenReturn(200);

        doNothing().when(logger).request(any(Request.class));

    }

	@Test
	public void should_notAddCorrelationIdInHttpHeaders_when_correlationIdIsAlreadyProvidedByTheUser() throws Exception {
		final String CORRELATION_ID = "any previous correlation id";

        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getServletPath()).thenReturn("/query");
        when(httpRequest.getRequestURI()).thenReturn("http://foobar/query");

        HashMap<String,String> headers =new HashMap<String, String>();
        headers.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);
        setupRequestHeaderMock(headers, httpRequest);
        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");

		sut.doFilter(httpRequest, httpResponse, filterChain);

        assertNotNull(httpResponse.containsHeader(DpsHeaders.CORRELATION_ID));
        assertNotNull(dpsHeaders.getCorrelationId());
	}

}
