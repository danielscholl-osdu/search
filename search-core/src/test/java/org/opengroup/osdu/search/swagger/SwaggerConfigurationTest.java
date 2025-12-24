// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class SwaggerConfigurationTest {

    @Mock
    private SwaggerConfigurationProperties configProps;

    @InjectMocks
    private SwaggerConfiguration swaggerConfiguration;

    @BeforeEach
    void setup() {
        lenient().when(configProps.getApiTitle()).thenReturn("Search API");
        lenient().when(configProps.getApiDescription()).thenReturn("Search operations");
        lenient().when(configProps.getApiVersion()).thenReturn("1.0.0");
        lenient().when(configProps.getApiLicenseName()).thenReturn("Apache 2.0");
        lenient().when(configProps.getApiLicenseUrl()).thenReturn("http://license");
        lenient().when(configProps.getApiContactName()).thenReturn("Support");
        lenient().when(configProps.getApiContactEmail()).thenReturn("support@osdu.org");
        lenient().when(configProps.getApiServerUrl()).thenReturn("https://api.osdu.org");
    }

    @Test
    void customOpenAPI_buildsValidOpenAPI_whenFullUrlDisabled() {
        when(configProps.isApiServerFullUrlEnabled()).thenReturn(false);

        OpenAPI api = swaggerConfiguration.customOpenAPI();

        Info info = api.getInfo();
        assertNotNull(info);
        assertEquals("Search API", info.getTitle());
        assertEquals("1.0.0", info.getVersion());
        assertEquals("Apache 2.0", info.getLicense().getName());

        assertTrue(api.getTags().stream().anyMatch(t -> t.getName().equals("search-api")));
        assertTrue(api.getTags().stream().anyMatch(t -> t.getName().equals("health-check-api")));

        assertTrue(api.getComponents().getSecuritySchemes().containsKey("Authorization"));
        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get("Authorization");
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());

        assertEquals("https://api.osdu.org", api.getServers().get(0).getUrl());
    }

    @Test
    void customOpenAPI_skipsServerList_whenFullUrlEnabled() {
        when(configProps.isApiServerFullUrlEnabled()).thenReturn(true);
        OpenAPI api = swaggerConfiguration.customOpenAPI();
        assertNull(api.getServers());
    }

    @Test
    void operationCustomizer_addsDataPartitionHeaderParameter() {
        OperationCustomizer customizer = swaggerConfiguration.operationCustomizer();

        io.swagger.v3.oas.models.Operation op =
                new io.swagger.v3.oas.models.Operation().operationId("testOp");

        op = customizer.customize(op, mock(HandlerMethod.class));

        List<Parameter> params = op.getParameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        Parameter p = params.get(0);

        assertEquals(DpsHeaders.DATA_PARTITION_ID, p.getName());
        assertEquals("header", p.getIn());
        assertTrue(p.getRequired());
    }
}
