package org.opengroup.osdu.search.provider.gcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"org.opengroup.osdu"})
@SpringBootApplication(
        exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class})
public class SearchGcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchGcpApplication.class, args);
    }
}
