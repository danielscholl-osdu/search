package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfiguration {

    public static final String SEARCH_API_TAG_NAME = "search-api";
    public static final String HEALTH_CHECK_API_TAG_NAME = "health-check-api";
    public static final String INFO_API_TAG_NAME = "info";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String SECURITY_SCHEME = "bearer";

    public static final Tag SEARCH_API_TAG = new Tag().name(SEARCH_API_TAG_NAME).description("Service endpoints to search data in datalake");
    public static final Tag HEALTH_CHECK_API_TAG = new Tag().name(HEALTH_CHECK_API_TAG_NAME).description("Health Check API");
    public static final Tag INFO_API_TAG = new Tag().name(INFO_API_TAG_NAME).description("Version info endpoint");

    @Autowired
    private SwaggerConfigurationProperties configurationProperties;

    @Bean
    public OpenAPI customOpenAPI() {

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme(SECURITY_SCHEME)
                .bearerFormat(AUTHORIZATION_HEADER)
                .in(SecurityScheme.In.HEADER)
                .name(AUTHORIZATION_HEADER);
        final String securitySchemeName = AUTHORIZATION_HEADER;
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);
        Components components = new Components().addSecuritySchemes(securitySchemeName, securityScheme);

        OpenAPI openAPI = new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(apiInfo())
                .tags(tags());

        if(configurationProperties.isApiServerFullUrlEnabled())
            return openAPI;
        return openAPI
                .servers(Arrays.asList(new Server().url(configurationProperties.getApiServerUrl())));
    }

    private List<Tag> tags() {
        List<Tag> tags = new ArrayList<>();
        tags.add(SEARCH_API_TAG);
        tags.add(HEALTH_CHECK_API_TAG);
        tags.add(INFO_API_TAG);
        return tags;
    }

    private Info apiInfo() {
        return new Info()
                .title(configurationProperties.getApiTitle())
                .description(configurationProperties.getApiDescription())
                .version(configurationProperties.getApiVersion())
                .license(new License().name(configurationProperties.getApiLicenseName()).url(configurationProperties.getApiLicenseUrl()))
                .contact(new Contact().name(configurationProperties.getApiContactName()).email(configurationProperties.getApiContactEmail()));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getTags() != null && operation.getTags().contains(SEARCH_API_TAG_NAME)) {
                Parameter dataPartitionId = new Parameter()
                        .name(DpsHeaders.DATA_PARTITION_ID)
                        .description("Tenant Id")
                        .in("header")
                        .required(true)
                        .schema(new StringSchema());
                return operation.addParametersItem(dataPartitionId);
            }
            return operation;
        };
    }
}
