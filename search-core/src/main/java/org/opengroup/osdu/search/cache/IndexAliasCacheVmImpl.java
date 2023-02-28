package org.opengroup.osdu.search.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class IndexAliasCacheVmImpl implements IndexAliasCache{
    private VmCache<String, String> cache;

    @Inject
    private TenantInfo tenant;

    public IndexAliasCacheVmImpl() {
        // The index alias won't be changed once it is created
        cache = new VmCache<>(24 * 3600, 2000);
    }

    @Override
    public void put(String s, String o) {
        this.cache.put(getKey(s), o);
    }

    @Override
    public String get(String s) {
        return this.cache.get(getKey(s));
    }

    @Override
    public void delete(String s) {
        this.cache.delete(getKey(s));
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }

    private String getKey(String kind) {
        return tenant.getDataPartitionId() + "-" + kind;
    }
}
