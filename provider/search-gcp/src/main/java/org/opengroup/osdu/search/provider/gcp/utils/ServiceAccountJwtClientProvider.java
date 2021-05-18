/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.provider.gcp.utils;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.core.gcp.GoogleIdToken.GcpServiceAccountJwtClient;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceAccountJwtClientProvider extends AbstractFactoryBean<IServiceAccountJwtClient> {

  private final SearchConfigurationProperties searchConfigurationProperties;

  @Override
  public Class<?> getObjectType() {
    return GcpServiceAccountJwtClient.class;
  }

  @Override
  protected IServiceAccountJwtClient createInstance() {
    GcpServiceAccountJwtClient serviceAccountJwtClient = new GcpServiceAccountJwtClient(
        searchConfigurationProperties.getGoogleAudiences());
    return serviceAccountJwtClient;
  }
}

