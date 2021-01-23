package org.opengroup.osdu.search.provider.reference.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class MongoDBConfig {

  private String mongoDbUrl;
  private String mongoDbUser;
  private String mongoDbPassword;

}
