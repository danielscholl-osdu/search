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
import java.time.Duration;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.ResponseHeadersFactory;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.Request;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIDRequestFilter implements Filter {
	@Inject
	private DpsHeaders requestHeaders;
	private ResponseHeadersFactory responseHeadersFactory = new ResponseHeadersFactory();

	// defaults to * for any front-end, string must be comma-delimited if more than one domain
	@Value("${ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS:*}")
	String ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS;

	@Inject
	private JaxRsDpsLog logger;

	private static final String OPTIONS_STRING = "OPTIONS";
	private static final String FOR_HEADER_NAME = "frame-of-reference";

	@Override
	public void init(FilterConfig filterConfig) {
		//None
	}

	@Override
	public void destroy() {
		//None
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest;
		if (request instanceof HttpServletRequest) {
			httpRequest = (HttpServletRequest)request;
		} else {
			throw new ServletException("Request is not HttpServletRequest");
		}

		long startTime;
		Object property = httpRequest.getAttribute("starttime");

		if(property == null) {
			startTime = System.currentTimeMillis();
		} else {
			startTime = (long)property;
		}

		String path = httpRequest.getServletPath();
		if (path.endsWith("/liveness_check") || path.endsWith("/readiness_check"))
			return;

		String fetchConversionHeader = ((HttpServletRequest) request).getHeader(FOR_HEADER_NAME);
		if (!Strings.isNullOrEmpty(fetchConversionHeader)) {
			this.requestHeaders.put(FOR_HEADER_NAME, fetchConversionHeader);
		}
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		this.requestHeaders.addCorrelationIdIfMissing();

		Map<String, String> responseHeaders = responseHeadersFactory.getResponseHeaders(ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS);
		for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
			httpResponse.addHeader(header.getKey(), header.getValue().toString());
		}
		httpResponse.addHeader(DpsHeaders.CORRELATION_ID, this.requestHeaders.getCorrelationId());

		// This block handles the OPTIONS preflight requests performed by Swagger. We
		// are also enforcing requests coming from other origins to be rejected.
		if (httpRequest.getMethod().equalsIgnoreCase(OPTIONS_STRING)) {
			httpResponse.setStatus(HttpStatus.SC_OK);
		}
		chain.doFilter(request, response);

		logger.request(Request.builder()
				.requestMethod(httpRequest.getMethod())
				.latency(Duration.ofMillis(System.currentTimeMillis() - startTime))
				.requestUrl(httpRequest.getRequestURI().toLowerCase())
				.Status(httpResponse.getStatus())
				.ip(httpRequest.getRemoteAddr())
				.build());
	}
}
