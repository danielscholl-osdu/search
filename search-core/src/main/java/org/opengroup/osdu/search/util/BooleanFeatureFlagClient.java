/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.util;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.search.cache.FeatureFlagCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class BooleanFeatureFlagClient {
    @Lazy
    @Autowired
    private FeatureFlagCache cache;

    @Autowired
    private JaxRsDpsLog logger;

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private IPartitionFactory factory;

    @Autowired
    private IServiceAccountJwtClient tokenService;

    public boolean isEnabled(String featureName, boolean defaultValue) {
        String dataPartitionId = headers.getPartitionId();
        if (cache != null && cache.get(featureName) != null)
            return cache.get(featureName);

        boolean isEnabled = defaultValue;
        try {
            PartitionInfo partitionInfo = getPartitionInfo(dataPartitionId);
            isEnabled = getFeatureValue(partitionInfo, featureName, defaultValue);
        } catch (Exception e) {
            this.logger.error(String.format("PartitionService: Error getting %s for dataPartition with Id: %s. Turn on the feature flag by default.", featureName, dataPartitionId), e);
        }
        this.cache.put(featureName, isEnabled);
        return isEnabled;
    }

    private PartitionInfo getPartitionInfo(String dataPartitionId) throws PartitionException {
        try {
            DpsHeaders partitionHeaders = DpsHeaders.createFromMap(headers.getHeaders());
            partitionHeaders.put(DpsHeaders.AUTHORIZATION, this.tokenService.getIdToken(dataPartitionId));

            IPartitionProvider partitionProvider = this.factory.create(partitionHeaders);
            PartitionInfo partitionInfo = partitionProvider.get(dataPartitionId);
            return partitionInfo;
        } catch (PartitionException e) {
            if (e.getResponse() != null) {
                logger.error(String.format("Error getting partition info for data-partition: %s. Message: %s. ResponseCode: %s.", dataPartitionId, e.getResponse().getBody(), e.getResponse().getResponseCode()), e);
            } else {
                logger.error(String.format("Error getting partition info for data-partition: %s.", dataPartitionId), e);
            }
            throw e;
        }
    }

    private boolean getFeatureValue(PartitionInfo partitionInfo, String featureName, boolean defaultValue) {
        if(partitionInfo == null || partitionInfo.getProperties() == null)
            return defaultValue;

        if(partitionInfo.getProperties().containsKey(featureName)) {
            Property property = partitionInfo.getProperties().get(featureName);
            return Boolean.parseBoolean((String)property.getValue());
        }
        return defaultValue;
    }
}
