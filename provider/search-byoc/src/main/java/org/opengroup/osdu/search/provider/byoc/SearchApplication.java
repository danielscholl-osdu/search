package org.opengroup.osdu.search.provider.byoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"org.opengroup.osdu.search","org.opengroup.osdu.core.common","org.opengroup.osdu.is.core"})
@SpringBootApplication(
		exclude = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
		org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class})
public class SearchApplication {

	public static void main(String[] args) {

		SpringApplication.run(SearchApplication.class, args);

	}

}
