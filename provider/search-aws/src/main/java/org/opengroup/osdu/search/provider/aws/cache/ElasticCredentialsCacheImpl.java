/* Copyright © Amazon Web Services

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */

package org.opengroup.osdu.search.provider.aws.cache;

import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ElasticCredentialsCacheImpl implements IElasticCredentialsCache<Object, Object> {

    // NOTE: this class is empty because ElasticCredentials are not used with the AWS implementation.
    // Instead, we're using the AWS Request Signing Apache Interceptor library to handle authentication.
    // This class is required to build, so we have it here, but empty.

    @Override
    public void put(Object o, Object o2) {
        // NOTE: this class is empty because ElasticCredentials are not used with the AWS implementation.
    }

    @Override
    public Object get(Object o) {
        return null;
    }

    @Override
    public void delete(Object o) {
        // NOTE: this class is empty because ElasticCredentials are not used with the AWS implementation.
    }

    @Override
    public void clearAll() {
        // NOTE: this class is empty because ElasticCredentials are not used with the AWS implementation.
    }
}
