package org.opengroup.osdu.search.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "elastic.latency.log")
@Getter
@Setter
@ToString
public class ElasticLoggingConfig {

    private Boolean enabled = false;
    private Long threshold = 200L;
}
