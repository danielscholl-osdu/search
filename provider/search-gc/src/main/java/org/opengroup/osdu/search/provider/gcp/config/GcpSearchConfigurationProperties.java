/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.search.provider.gcp.config;

import lombok.Getter;
import lombok.Setter;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Primary
@Configuration
@ConfigurationProperties
@Getter
@Setter
public class GcpSearchConfigurationProperties extends SearchConfigurationProperties {

    private String authorizeApi;
    private String authorizeApiKey;

    private String redisSearchPassword;
    private Integer redisSearchExpiration = 60 * 60;
    private Boolean redisSearchWithSsl = false;
}
