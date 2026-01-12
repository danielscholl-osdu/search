/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.search.provider.ibm.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticCredentialsCacheImpl implements IElasticCredentialsCache<String, ClusterSettings> {

	private VmCache<String, ClusterSettings> cache;

	public ElasticCredentialsCacheImpl(@Value("${ELASTIC_CACHE_EXPIRATION}") final String ELASTIC_CACHE_EXPIRATION,
			@Value("${MAX_CACHE_VALUE_SIZE}") final String MAX_CACHE_VALUE_SIZE) {
		cache = new VmCache<>(Integer.parseInt(ELASTIC_CACHE_EXPIRATION) * 60, Integer.parseInt(MAX_CACHE_VALUE_SIZE));
	}

	@Override
	public void clearAll() {

		this.cache.clearAll();

	}

	@Override
	public void delete(String s) {
		this.cache.delete(s);

	}

	@Override
	public ClusterSettings get(String s) {

		return this.cache.get(s);
	}

	@Override
	public void put(String s, ClusterSettings o) {
		this.cache.put(s, o);

	}

}
