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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.middleware.RequestHeadersTestUtil.setupRequestHeaderMock;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.annotation.security.RolesAllowed;
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
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.Request;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.mockito.junit.MockitoJUnitRunner;

/* These tests indirectly test the AuthorizationFilter that is called via the 
 * @PreAuthorize annotations on API methods. They verify the behavior
 * on the HTTP level using the CorrelatonIDREquestFilter to process mocked
 * requests.
 */

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationRequestFilterTest {

    private static final String ROLE1 = "role1";
    private static final String ROLE2 = "role2";

    @Mock
    private IAuthorizationService authorizationService;
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
        doNothing().when(logger).request(any(Request.class));

        dpsHeaders = new DpsHeaders();
        dpsHeaders.put(DpsHeaders.ACCOUNT_ID, "tenant1");
        dpsHeaders.put(DpsHeaders.AUTHORIZATION, "Bearer geer.fereferv.cefe=");
        dpsHeaders.put(DpsHeaders.CONTENT_TYPE, "application/json");
        dpsHeaders.addCorrelationIdIfMissing();
    	when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpResponse.getStatus()).thenReturn(200);


        Method method = this.getClass().getMethod("rolesAllowedTestMethod");
    }



    @Test
    public void should_skipFilter_when_requestingSwaggerEndpoint() throws Exception {
        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getServletPath()).thenReturn("/swagger.js");
        when(httpRequest.getRequestURI()).thenReturn("http://foobar/");
        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");

        sut.doFilter(httpRequest, httpResponse, filterChain);

        verify(this.resourceInfo, never()).getResourceMethod();
    }

    @Test
    public void should_authenticateRequest_when_resourceIsRolesAllowedAnnotated() throws Exception {
        Groups groups = new Groups();
        GroupInfo group1 = new GroupInfo();
        group1.setName("data.group.1");
        groups.getGroups().add(group1);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder().groups(groups).user("user.1").build();

        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getServletPath()).thenReturn("/query");
        when(httpRequest.getRequestURI()).thenReturn("http://foobar/query");

        HashMap<String,String> headers =new HashMap<String, String>();
        setupRequestHeaderMock(headers, httpRequest);
        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");

        sut.doFilter(httpRequest, httpResponse, filterChain);

        verify(this.resourceInfo, never()).getResourceMethod();

        assertNotNull(httpResponse.containsHeader(DpsHeaders.CORRELATION_ID));
        assertNotNull(dpsHeaders.getCorrelationId());
    }

    @Test
    public void should_authenticateRequest_when_resourceIsRolesAllowedAnnotated_and_slbAccountIdHasSpaces() throws Exception {
        Groups groups = new Groups();
        GroupInfo group1 = new GroupInfo();
        group1.setName("data.group.1");
        groups.getGroups().add(group1);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder().groups(groups).user("user.1").build();

        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getServletPath()).thenReturn("/query");
        when(httpRequest.getRequestURI()).thenReturn("http://foobar/query");

        HashMap<String,String> headers =new HashMap<String, String>();
        headers.put(DpsHeaders.ACCOUNT_ID, " tenant1, common ");
        dpsHeaders.put(DpsHeaders.ACCOUNT_ID, " tenant1, common ");
        setupRequestHeaderMock(headers, httpRequest);

        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");

        sut.doFilter(httpRequest, httpResponse, filterChain);

        assertNotNull(httpResponse.containsHeader(DpsHeaders.CORRELATION_ID));
        assertNotNull(dpsHeaders.getCorrelationId());
    }

    @Test
    public void should_throwException_when_dataGroupsMissing() throws Exception {
        Groups groups = new Groups();
        GroupInfo group1 = new GroupInfo();
        group1.setName("service.group.1");
        groups.getGroups().add(group1);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder().groups(groups).user("user.1").build();

        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getServletPath()).thenReturn("/query");
        when(httpRequest.getRequestURI()).thenReturn("http://foobar/query");

        HashMap<String,String> headers =new HashMap<String, String>();
        setupRequestHeaderMock(headers, httpRequest);
        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");

        try {
        	sut.doFilter(httpRequest, httpResponse, filterChain);
        } catch (AppException e) {
            assertEquals(e.getError().getCode(), HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Test
    public void should_throwException_given_multipleAccountId_forNonQueryApi() throws Exception {

        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getServletPath()).thenReturn("/index/schema");
        when(httpRequest.getRequestURI()).thenReturn("http://foobar/index/schema");

        HashMap<String,String> headers =new HashMap<String, String>();
        headers.put(DpsHeaders.ACCOUNT_ID, "tenant1,common");
        dpsHeaders.put(DpsHeaders.ACCOUNT_ID, "tenant1,common");
        setupRequestHeaderMock(headers, httpRequest);
        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");


        try {
        	sut.doFilter(httpRequest, httpResponse, filterChain);
        } catch (AppException e) {
            assertEquals(e.getError().getCode(), HttpServletResponse.SC_BAD_REQUEST);
            assertEquals(e.getError().getMessage(), "multi-valued data partition not supported for the API");
        }
    }

    @Test
    public void should_throwException_given_emptyAccountId() throws Exception {

        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getServletPath()).thenReturn("/query");
        when(httpRequest.getRequestURI()).thenReturn("http://foobar/query");

        HashMap<String,String> headers = new HashMap<String, String>();
        headers.put(DpsHeaders.ACCOUNT_ID, null);
        dpsHeaders.put(DpsHeaders.ACCOUNT_ID, null);
        setupRequestHeaderMock(headers, httpRequest);
        org.springframework.test.util.ReflectionTestUtils.setField(sut, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");

        try {
        	sut.doFilter(httpRequest, httpResponse, filterChain);
        } catch (AppException e) {
            assertEquals(e.getError().getCode(), HttpServletResponse.SC_BAD_REQUEST);
            assertEquals(e.getError().getMessage(), "invalid or empty data partition provided");
        }
    }

    @RolesAllowed({ROLE1, ROLE2})
    public void rolesAllowedTestMethod() {
        // do nothing
    }
}
