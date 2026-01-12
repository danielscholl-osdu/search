package org.opengroup.osdu.search.provider.ibm.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.springframework.stereotype.Component;

@Component
public class GroupCacheImpl extends VmCache<String, Groups> {

    public GroupCacheImpl() {
        super(30, 1000);
    }
}
