package org.opengroup.osdu.search.provider.reference.di;

import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class EntitlementsClientFactory extends AbstractFactoryBean<IEntitlementsFactory> {

	@Value("${AUTHORIZE_API}")
	private String AUTHORIZE_API;

	@Value("${AUTHORIZE_API_KEY:}")
	private String AUTHORIZE_API_KEY;

	@Override
	protected IEntitlementsFactory createInstance() throws Exception {

		return new EntitlementsFactory(EntitlementsAPIConfig
				.builder()
				.rootUrl(AUTHORIZE_API)
				.apiKey(AUTHORIZE_API_KEY)
				.build());
	}

	@Override
	public Class<?> getObjectType() {
		return IEntitlementsFactory.class;
	}
}