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

package org.opengroup.osdu.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static org.opengroup.osdu.util.Utility.beautifyJsonString;

@Slf4j
@ToString
public class AzureHTTPClient extends HTTPClient {

    private static String token = null;

    @Override
    public synchronized String getAccessToken() {
        if(token == null) {
            try {
                token = "Bearer " + JwtTokenUtil.getAccessToken();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return token;
    }

    public ClientResponse send(String httpMethod, String url, String payLoad, Map<String, String> headers, String token) {
        log.info("in Azure send method");
        ClientResponse response = null;
        Client client = getClient();
        client.setConnectTimeout(300000);
        client.setReadTimeout(300000);
        WebResource webResource = client.resource(url);
        int count = 1;
        int MaxRetry = 3;
        while (count < MaxRetry) {
            try {
                headers.put("correlation-id", headers.getOrDefault("correlation-id", UUID.randomUUID().toString()));
                //removing Auth header before logging
                headers.remove("Authorization");
                log.info(String.format("Request URL: %s %s\nHeaders: %s\nPayload: %s", httpMethod, url, headers, beautifyJsonString(payLoad)));
                log.info(String.format("Attempt: #%s/%s, CorrelationId: %s", count, MaxRetry, headers.get("correlation-id")));
                response = this.getClientResponse(httpMethod, payLoad, webResource, headers, token);
                if (response.getStatusInfo().getFamily().equals(Response.Status.Family.valueOf("SERVER_ERROR"))) {
                    count++;
                    Thread.sleep(5000);
                    continue;
                } else {
                    log.info("sending response from azure send method");
                    break;
                }
            } catch (Exception ex) {
                log.error("Send request error",ex);
                count++;
                if (count == MaxRetry) {
                    throw new AssertionError("Error: Send request error", ex);
                }
            } finally {
                //log response body
                if (response != null) {
                    log.info(String.format("This is the response received : %s\nHeaders: %s\nStatus code: %s", response, response.getHeaders(), response.getStatus()));
                }
            }
        }
        return response;
    }
}
