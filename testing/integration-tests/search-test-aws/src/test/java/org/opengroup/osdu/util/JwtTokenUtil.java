// Copyright © Amazon Web Services
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


import org.opengroup.osdu.core.aws.cognito.AWSCognitoClient;

class JwtTokenUtil {
    private final static String COGNITO_CLIENT_ID_PROPERTY = "AWS_COGNITO_CLIENT_ID";
    private final static String COGNITO_AUTH_FLOW_PROPERTY = "AWS_COGNITO_AUTH_FLOW";
    private final static String COGNITO_AUTH_PARAMS_USER_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_USER";
    private final static String COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_PASSWORD";

    static String getAccessToken() {
        String clientId = System.getProperty(COGNITO_CLIENT_ID_PROPERTY, System.getenv(COGNITO_CLIENT_ID_PROPERTY));
        String authFlow = System.getProperty(COGNITO_AUTH_FLOW_PROPERTY, System.getenv(COGNITO_AUTH_FLOW_PROPERTY));
        String user = System.getProperty(COGNITO_AUTH_PARAMS_USER_PROPERTY, System.getenv(COGNITO_AUTH_PARAMS_USER_PROPERTY));
        String password = System.getProperty(COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY, System.getenv(COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY));

        AWSCognitoClient client = new AWSCognitoClient(clientId, authFlow, user, password);
        return client.getToken();
    }
}
