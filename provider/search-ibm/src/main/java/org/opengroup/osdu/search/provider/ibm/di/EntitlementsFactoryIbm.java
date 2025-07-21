/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.search.provider.ibm.di;

import jakarta.inject.Inject;
import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
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

  @Inject
  private HttpResponseBodyMapper httpResponseBodyMapper;

  @Override
  protected IEntitlementsFactory createInstance() {

    return new EntitlementsFactory(EntitlementsAPIConfig
        .builder()
        .rootUrl(AUTHORIZE_API)
        .apiKey(AUTHORIZE_API_KEY).build(), httpResponseBodyMapper);
  }

  @Override
  public Class<?> getObjectType() {
    return IEntitlementsFactory.class;
  }
}
