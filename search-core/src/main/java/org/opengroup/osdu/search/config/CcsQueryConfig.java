package org.opengroup.osdu.search.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="ccs.query.endpoints")
@Getter
@Setter
public class CcsQueryConfig {

    private boolean disabled = false;
}
