/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.search.provider.ibm.cache;

import java.util.Map;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.search.cache.IFieldTypeMappingCache;
import org.springframework.stereotype.Component;

@Component
public class FieldTypeMappingCache extends VmCache<String, Map> implements IFieldTypeMappingCache {

    /*public FieldTypeMappingCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST, @Value("${REDIS_SEARCH_PORT}") final int REDIS_SEARCH_PORT) {
        super(REDIS_SEARCH_HOST, REDIS_SEARCH_PORT, 1440 * 60, String.class, HashSet.class);
    }*/
	
	public FieldTypeMappingCache() {
		super(5 * 60, 1000);
	}
	
}