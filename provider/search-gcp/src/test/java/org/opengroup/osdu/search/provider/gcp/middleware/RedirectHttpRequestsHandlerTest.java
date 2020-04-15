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

package org.opengroup.osdu.search.provider.gcp.middleware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.search.middleware.RedirectHttpRequestsHandler;
import org.opengroup.osdu.search.provider.gcp.provider.impl.ProviderHeaderServiceImpl;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Config.class})
public class RedirectHttpRequestsHandlerTest {

    @Mock
    private ContainerRequestContext context;
    
    @Mock
    ProviderHeaderServiceImpl providerHeaderService;

    @InjectMocks
    private RedirectHttpRequestsHandler sut;
    
	@Mock
	private HttpServletRequest httpRequest;
	
	@Mock
	private HttpServletResponse httpResponse;
	
	@Mock
	private FilterChain filterChain;

    @Before
    public void setup() throws Exception {
        mockStatic(Config.class);
        
        doNothing().when(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    public void should_throwAppException302WithHttpsLocation_when_client_isNotUsingHttps() throws Exception {
        when(httpRequest.getScheme()).thenReturn("http");
        when(Config.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.CLOUD);
        try {
            sut.doFilter(httpRequest, httpResponse, filterChain);
            fail("should throw");
        } catch (AppException e) {
            assertEquals(302, e.getError().getCode());
        }
    }

    /*
    @Test
    public void should_not_throwAppExceptionWithHttpsLocation_when_client_isACronJobNotUsingHttps() throws Exception {
        Map<String,String> headers=new HashMap<>();
        headers.put(providerHeaderService.getCronServiceHeader(),"true");
        setupRequestHeaderMock(headers, httpRequest);
        when(httpRequest.getScheme()).thenReturn("http");
        when(Config.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.CLOUD);
        try {
        	sut.doFilter(httpRequest, httpResponse, filterChain);
        } catch (AppException e) {
            fail("should not throw");
        }
    }
    */
}