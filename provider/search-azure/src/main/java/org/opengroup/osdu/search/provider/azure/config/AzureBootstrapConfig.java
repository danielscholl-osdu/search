//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.provider.azure.config;

import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.inject.Named;

@Configuration
public class AzureBootstrapConfig {

    @Value("${elastic.cache.expiration}")
    private Integer elasticCacheExpiration;

    @Value("${elastic.cache.maxSize}")
    private Integer elasticCacheMaxSize;

    @Value("${azure.keyvault.url}")
    private String keyVaultURL;

    @Value("${AUTHORIZE_API}")
    private String entitlementsAPIEndpoint;

    @Value("${AUTHORIZE_API_KEY}")
    private String entitlementsAPIKey;

    @Bean
    @Named("KEY_VAULT_URL")
    public String getKeyVaultURL() {
        return keyVaultURL;
    }

    @Bean
    @Named("ELASTIC_CACHE_EXPIRATION")
    public Integer getElasticCacheExpiration() {
        return elasticCacheExpiration;
    }

    @Bean
    @Named("MAX_CACHE_VALUE_SIZE")
    public Integer getElasticCacheMaxSize() {
        return elasticCacheMaxSize;
    }

    @Autowired
    private HttpResponseBodyMapper httpResponseBodyMapper;

    @Bean
    public IEntitlementsFactory entitlementsFactory() {
        EntitlementsAPIConfig apiConfig = EntitlementsAPIConfig.builder()
                .apiKey(entitlementsAPIKey)
                .rootUrl(entitlementsAPIEndpoint)
                .build();
        return new EntitlementsFactory(apiConfig, httpResponseBodyMapper);
    }

}
