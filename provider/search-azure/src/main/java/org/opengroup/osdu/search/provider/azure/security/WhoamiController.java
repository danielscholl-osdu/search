//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.provider.azure.security;

import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@NoArgsConstructor
@Controller
public class WhoamiController {

    private SecurityContext securityContext;

    /*
    * NOTE [aaljain]: Code repetition in legal service as well
    */

    // Constructor made for unit testing
    WhoamiController(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @GetMapping(value = {"/", "/whoami"})
    @ResponseBody
    public String whoami() {
        // Added for unit testing
        if (securityContext == null)
            securityContext = SecurityContextHolder.getContext();
        final Authentication auth = securityContext.getAuthentication();

        String userName = auth.getName();
        String roles = String.valueOf(auth.getAuthorities());
        String details = String.valueOf(auth.getPrincipal());

        // NOTE [aaljain]: Inconsistent return string across services
        return "user: " + userName + "<BR>" +
                "roles: " + roles + "<BR>" +
                "details: " + details;
    }
}

