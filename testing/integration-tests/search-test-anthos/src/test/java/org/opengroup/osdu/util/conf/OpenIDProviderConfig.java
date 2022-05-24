/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.util.conf;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

public class OpenIDProviderConfig {

    private String clientId;
    private String url;
    private String clientSecret;
    private final String[] scopes = {"openid"};
    private static final OpenIDProviderConfig openIDProviderConfig = new OpenIDProviderConfig();
    private static OIDCProviderMetadata providerMetadata;

    public static OpenIDProviderConfig Instance() {
        try {
            openIDProviderConfig.clientId = System.getProperty("TEST_OPENID_PROVIDER_CLIENT_ID", System.getenv("TEST_OPENID_PROVIDER_CLIENT_ID"));
            openIDProviderConfig.url = System.getProperty("TEST_OPENID_PROVIDER_URL", System.getenv("TEST_OPENID_PROVIDER_URL"));
            openIDProviderConfig.clientSecret = System.getProperty("TEST_OPENID_PROVIDER_CLIENT_SECRET", System.getenv("TEST_OPENID_PROVIDER_CLIENT_SECRET"));
            Issuer issuer = new Issuer(openIDProviderConfig.url);
            OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(issuer);
            HTTPRequest httpRequest = request.toHTTPRequest();
            HTTPResponse httpResponse = httpRequest.send();
            providerMetadata = OIDCProviderMetadata.parse(httpResponse.getContentAsJSONObject());
        } catch (Exception e) {
            throw new RuntimeException("Malformed token provider configuration", e);
        }
        return openIDProviderConfig;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String[] getScopes() {
        return scopes;
    }

    public OIDCProviderMetadata getProviderMetadata() {
        return providerMetadata;
    }
}
