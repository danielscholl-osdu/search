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

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;

//@Component
public class RedirectHttpRequestsHandler implements Filter {

    @Inject
    private IProviderHeaderService providerHeaderService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

    	HttpServletRequest httpRequest = null;
    	if (request instanceof HttpServletRequest) {
    		httpRequest = (HttpServletRequest)request;
    	} else {
    		throw new ServletException("Request is not HttpServletRequest");
    	}
        //Skip if it is a cron job
        String expectedCronHeaderValue = "true";
        String cronHeader = httpRequest.getHeader(providerHeaderService.getCronServiceHeader());
        if (expectedCronHeaderValue.equalsIgnoreCase(cronHeader)) return;

        // return 302 redirect if http connection is attempted
        if ("http".equals(httpRequest.getScheme())) {
            throw new AppException(302, "Redirect", "HTTP is not supported. Use HTTPS.");
        }
    }
}