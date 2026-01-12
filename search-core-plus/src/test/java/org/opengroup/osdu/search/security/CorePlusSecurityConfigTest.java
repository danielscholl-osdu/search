/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CorePlusSecurityConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(CorePlusSecurityConfig.class);

    @Test
    void filterChain_disablesCsrf_andDisablesHttpBasic() {

        contextRunner.run(context -> {
            SecurityFilterChain chain = context.getBean(SecurityFilterChain.class);

            List<? extends Class<?>> filters =
                    chain.getFilters().stream()
                            .map(Object::getClass)
                            .toList();
            assertThat(filters)
                    .noneMatch(CsrfFilter.class::isAssignableFrom);
            assertThat(filters)
                    .noneMatch(BasicAuthenticationFilter.class::isAssignableFrom);
        });
    }
}
