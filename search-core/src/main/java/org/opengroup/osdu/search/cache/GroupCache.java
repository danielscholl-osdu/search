package org.opengroup.osdu.search.cache;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

public interface GroupCache extends ICache<String, Groups> {

    String getCacheKey(DpsHeaders headers);
}
