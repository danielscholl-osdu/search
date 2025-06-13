/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
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

package org.opengroup.osdu.search.di;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.search.config.CorePlusSearchConfigurationProperties;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@RequiredArgsConstructor
public class EntitlementsClientFactory extends AbstractFactoryBean<IEntitlementsFactory> {

  private final HttpResponseBodyMapper httpResponseBodyMapper;
  private final CorePlusSearchConfigurationProperties configurationProperties;

  @Override
  protected IEntitlementsFactory createInstance() {

    return new EntitlementsFactory(EntitlementsAPIConfig
        .builder()
        .rootUrl(this.configurationProperties.getAuthorizeApi())
        .apiKey(this.configurationProperties.getAuthorizeApiKey())
        .build(), this.httpResponseBodyMapper);
  }

  @Override
  public Class<?> getObjectType() {
    return IEntitlementsFactory.class;
  }
}
