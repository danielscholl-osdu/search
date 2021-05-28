package org.opengroup.osdu.search.provider.gcp.di;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;
//TODO temp fix for policy integration
@Component
@RequiredArgsConstructor
public class GCPJwtClientProvider extends AbstractFactoryBean<IServiceAccountJwtClient> {

    @Value("${GOOGLE_AUDIENCES}")
    private String audience;

    @Override
    public Class<?> getObjectType() {
        return GcpServiceAccountJwtClient.class;
    }

    @Override
    protected IServiceAccountJwtClient createInstance() throws Exception {
        GcpServiceAccountJwtClient serviceAccountJwtClient = new GcpServiceAccountJwtClient(audience);
        return serviceAccountJwtClient;
    }
}
