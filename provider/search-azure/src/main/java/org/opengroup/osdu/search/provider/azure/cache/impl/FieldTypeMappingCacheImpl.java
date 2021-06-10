package org.opengroup.osdu.search.provider.azure.cache.impl;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FieldTypeMappingCacheImpl extends VmCache<String, Map> implements IFieldTypeMappingCache {

    public FieldTypeMappingCacheImpl() {
        super(1440 * 60, 1000);
    }
}