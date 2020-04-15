package org.opengroup.osdu.search.provider.byoc.utils;

import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.core.MultivaluedHashMap;

@Configuration
public class ConfigModule {

    @Bean
    ResteasyHttpHeaders resteasyHttpHeaders() {
        return new ResteasyHttpHeaders(new MultivaluedHashMap<>());
    }

}
