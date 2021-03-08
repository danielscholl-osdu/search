/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.search.provider.ibm.cache;

import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.util.Crc32c;
import org.opengroup.osdu.search.cache.GroupCache;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("groupsCache")
public class GroupCacheImpl extends VmCache<String, Groups> implements GroupCache {

    public GroupCacheImpl() {
        super(30, 1000);
    }

    public String getCacheKey(DpsHeaders headers) {
        String key = String.format("entitlement-groups:%s:%s", headers.getPartitionIdWithFallbackToAccountId(),
                headers.getAuthorization());
        return Crc32c.hashToBase64EncodedString(key);
    }
}
