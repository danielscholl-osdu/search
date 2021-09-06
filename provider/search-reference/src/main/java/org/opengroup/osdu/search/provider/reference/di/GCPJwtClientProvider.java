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

package org.opengroup.osdu.search.provider.reference.di;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

//TODO temp fix for policy integration
@Component
@RequiredArgsConstructor
public class GCPJwtClientProvider extends AbstractFactoryBean<IServiceAccountJwtClient> {

  @Value("${GOOGLE_AUDIENCES}")
  private String audience;

  @Override
  public Class<?> getObjectType() {
    return GcpServiceAccountJwtClient.class;
  }

  @Override
  protected IServiceAccountJwtClient createInstance() throws Exception {
    GcpServiceAccountJwtClient serviceAccountJwtClient = new GcpServiceAccountJwtClient(audience);
    return serviceAccountJwtClient;
  }
}
