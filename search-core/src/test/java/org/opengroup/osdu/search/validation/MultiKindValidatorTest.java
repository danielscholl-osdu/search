// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.search.validation;

import org.junit.Test;
import org.opengroup.osdu.core.common.model.search.validation.MultiKindValidator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MultiKindValidatorTest {

    @Test
    public void test_isValid_validatorRequest() {

        MultiKindValidator validator = new MultiKindValidator();
        assertTrue(validator.isValid("tenant:valid:kind:1.0.0", null));
        assertTrue(validator.isValid("*:*:*:*", null));
        assertTrue(validator.isValid("*:valid:kind:1.0.0", null));
        assertTrue(validator.isValid("tenant:*:kind:1.0.0", null));
        assertTrue(validator.isValid("tenant:valid:kind:*", null));
        assertTrue(validator.isValid("tenant:valid:*:1.0.0", null));
        assertTrue(validator.isValid("ten*:valid:kind:1*", null));
        assertFalse(validator.isValid("withoutcolon", null));
        assertFalse(validator.isValid("without:colon", null));
        assertFalse(validator.isValid("without:version:value", null));
        assertFalse(validator.isValid("123:321:value", null));
        assertFalse(validator.isValid("a:b:c:d:321", null));
    }

    @Test
    public void test_initialize_validator() {
        // for coverage purposes. Do nothing method!
        MultiKindValidator validator = new MultiKindValidator();
        validator.initialize(null);
    }
}