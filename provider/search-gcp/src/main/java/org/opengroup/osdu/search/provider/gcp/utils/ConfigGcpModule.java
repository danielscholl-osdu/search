package org.opengroup.osdu.search.provider.gcp.utils;

import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.core.MultivaluedHashMap;

@Configuration
public class ConfigGcpModule {

    @Bean
    ResteasyHttpHeaders resteasyHttpHeaders() {
        return new ResteasyHttpHeaders(new MultivaluedHashMap<>());
    }

}
