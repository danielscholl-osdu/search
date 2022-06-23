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

package org.opengroup.osdu.search.provider.reference.provider.impl;

import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.springframework.stereotype.Service;

@Service
public class ProviderHeaderServiceImpl implements IProviderHeaderService {

  private static final String DATA_GROUPS = "X-Data-Groups";
  private static final String CRON_SERVICE = "X-AppEngine-Cron";
  private static final String DATA_ROOT_USER = "X-Data-Root-User";

  @Override
  public String getCronServiceHeader() {
    return CRON_SERVICE;
  }

  @Override
  public String getDataGroupsHeader() {
    return DATA_GROUPS;
  }

  @Override
  public String getDataRootUserHeader() { return DATA_ROOT_USER; }
}
