/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.search.provider.ibm.di;

import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class EntitlementsFactoryIbm extends AbstractFactoryBean<IEntitlementsFactory> {
	@Value("${AUTHORIZE_API}")
	private String AUTHORIZE_API;

	@Value("${AUTHORIZE_API_KEY:#{null}}")
	private String AUTHORIZE_API_KEY;

	@Override
	protected IEntitlementsFactory createInstance() throws Exception {

		return new EntitlementsFactory(
				EntitlementsAPIConfig.builder().rootUrl(AUTHORIZE_API).apiKey(AUTHORIZE_API_KEY).build());
	}

	@Override
	public Class<?> getObjectType() {
		return IEntitlementsFactory.class;
	}
}
