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

package org.opengroup.osdu.search.smart.models;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

public class FilterTest {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void should_beValid_when_nameIsBlank(){
        Filter sut = getSut();
        sut.setName("");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(0, violations.size());
    }

    @Test
    public void should_throwInvalid_when_nameIsNonAlphaNumeric(){
        Filter sut = getSut();
        sut.setName("ash(");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(1, violations.size());
    }
    @Test
    public void should_throwInvalid_when_nameIsOver15Chars(){
        Filter sut = getSut();
        sut.setName("ashashashashashashasashash");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(1, violations.size());
    }
    @Test
    public void should_beOk_when_nameIsType(){
        Filter sut = getSut();
        sut.setName("ash");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(0, violations.size());
    }


    @Test
    public void should_throwInvalid_when_valueIsBlank(){
        Filter sut = getSut();
        sut.setValue("");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(2, violations.size());
    }
    @Test
    public void should_throwInvalid_when_valueIsOver50Chars(){
        Filter sut = getSut();
        sut.setValue("tomorrowtomorrowtomorrowtomorrowtomorrowtomorrowtomorrowtomorrowtomorrow");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(1, violations.size());
    }
    @Test
    public void should_beOk_when_valueIsType(){
        Filter sut = getSut();
        sut.setValue("tomorrowtomorrow");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(0, violations.size());
    }


    @Test
    public void should_throwInvalid_when_operatorIsBlank(){
        Filter sut = getSut();
        sut.setOperator("");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(2, violations.size());
    }
    @Test
    public void should_throwInvalid_when_operatorIsOver3Chars(){
        Filter sut = getSut();
        sut.setOperator("tomorrow");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(1, violations.size());
    }
    @Test
    public void should_beOk_when_operatorIsType(){
        Filter sut = getSut();
        sut.setOperator("eq");
        Set<ConstraintViolation<Filter>> violations = validator.validate(sut);

        assertEquals(0, violations.size());
    }

    private Filter getSut() {
        Filter sut = new Filter();
        sut.setName("type");
        sut.setValue("wells");
        sut.setOperator("or");
        return sut;
    }
}
