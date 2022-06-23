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

import lombok.ToString;
import lombok.extern.java.Log;

@Log
@ToString
public class AzureHTTPClient extends HTTPClient {

    private static String token = null;
    private static String defaultAccessToken = null;

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

    @Override
    public String getDefaultAccessToken() {
        if(defaultAccessToken == null) {
            try {
                defaultAccessToken = "Bearer " + JwtTokenUtil.getDefaultAccessToken();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return defaultAccessToken;
    }
}