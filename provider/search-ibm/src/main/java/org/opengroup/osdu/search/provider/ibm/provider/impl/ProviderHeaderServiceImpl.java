/**
 * Copyright 2020 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.search.provider.ibm.provider.impl;

import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.springframework.stereotype.Service;

//TODO: Move to org.opengroup.osdu.search.search-gcp once available
@Service
public class ProviderHeaderServiceImpl implements IProviderHeaderService {
    private static final String DATA_GROUPS = "X-Data-Groups";
    private static final String CRON_SERVICE = "X-AppEngine-Cron";


    @Override
    public String getCronServiceHeader() {
        return CRON_SERVICE;
    }

    @Override
    public String getDataGroupsHeader() {
        return DATA_GROUPS;
    }
}
