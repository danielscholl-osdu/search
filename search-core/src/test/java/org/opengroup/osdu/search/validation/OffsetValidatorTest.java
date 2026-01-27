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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.search.validation.OffsetValidator;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class OffsetValidatorTest {

    @Mock
    private QueryRequest queryRequest;

    @InjectMocks
    private OffsetValidator sut;

    private ConstraintValidatorContext constraintValidatorContext;

    @BeforeEach
    public void setup() {
        constraintValidatorContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        Mockito.lenient().when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }

    @Test
    public void should_returnTrue_when_offsetLimitCombination_isValid() {
        when(this.queryRequest.getFrom()).thenReturn(100);
        when(this.queryRequest.getLimit()).thenReturn(1000);
        when(QueryUtils.getResultSizeForQuery(queryRequest.getLimit())).thenReturn(100);

        assertTrue(sut.isValid(queryRequest, null));
    }

    @Test
    public void should_returnFalse_when_offsetLimitCombination_isInvalid() {
        when(this.queryRequest.getFrom()).thenReturn(10010);
        when(this.queryRequest.getLimit()).thenReturn(10);
        when(QueryUtils.getResultSizeForQuery(queryRequest.getLimit())).thenReturn(10);

        assertFalse(sut.isValid(queryRequest, constraintValidatorContext));

        when(this.queryRequest.getFrom()).thenReturn(9970);
        when(this.queryRequest.getLimit()).thenReturn(150);
        when(QueryUtils.getResultSizeForQuery(queryRequest.getLimit())).thenReturn(100);

        assertFalse(sut.isValid(queryRequest, constraintValidatorContext));
    }
}
