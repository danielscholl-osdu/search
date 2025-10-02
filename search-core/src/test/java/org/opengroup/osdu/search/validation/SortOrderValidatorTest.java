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

import org.opengroup.osdu.core.common.model.search.validation.SortOrderValidator;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.search.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class SortOrderValidatorTest {

    @Mock
    private SortQuery sortQuery;

    @InjectMocks
    private SortOrderValidator sut;

    private ConstraintValidatorContext constraintValidatorContext;

    @Before
    public void setup() {
        initMocks(this);

        constraintValidatorContext = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }

    @Test
    public void should_returnTrue_when_sortOrder_isValid() {
        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        sortFields.add("namespace");
        when(this.sortQuery.getField()).thenReturn(sortFields);
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(SortOrder.ASC);
        sortOrders.add(SortOrder.DESC);
        when(this.sortQuery.getOrder()).thenReturn(sortOrders);

        assertTrue(sut.isValid(this.sortQuery, this.constraintValidatorContext));
    }

    @Test
    public void should_returnFalse_when_fieldIsNull() {
        when(this.sortQuery.getField()).thenReturn(null);

        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));
        verify(this.constraintValidatorContext).buildConstraintViolationWithTemplate(SwaggerDoc.SORT_FIELD_VALIDATION_NOT_EMPTY_MSG);
    }

    @Test
    public void should_returnFalse_when_fieldIsEmptyList() {
        List<String> sortFields = new ArrayList<>();
        when(this.sortQuery.getField()).thenReturn(sortFields);

        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));
        verify(this.constraintValidatorContext).buildConstraintViolationWithTemplate(SwaggerDoc.SORT_FIELD_VALIDATION_NOT_EMPTY_MSG);
    }

    @Test
    public void should_returnFalse_when_fieldListContainsInvalidItem() {
        List<String> sortFields = new ArrayList<>();
        sortFields.add("  ");
        sortFields.add("id");
        when(this.sortQuery.getField()).thenReturn(sortFields);

        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(SortOrder.ASC);
        sortOrders.add(SortOrder.DESC);
        when(this.sortQuery.getOrder()).thenReturn(sortOrders);

        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));
        verify(this.constraintValidatorContext).buildConstraintViolationWithTemplate(SwaggerDoc.SORT_FIELD_LIST_VALIDATION_NOT_EMPTY_MSG);
    }

    @Test
    public void should_returnFalse_when_orderIsNull() {
        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        sortFields.add("namespace");
        when(this.sortQuery.getField()).thenReturn(sortFields);
        when(this.sortQuery.getOrder()).thenReturn(null);

        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));
        verify(this.constraintValidatorContext).buildConstraintViolationWithTemplate(SwaggerDoc.SORT_ORDER_VALIDATION_NOT_EMPTY_MSG);
    }

    @Test
    public void should_returnFalse_when_orderIsEmptyList() {
        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        when(this.sortQuery.getField()).thenReturn(sortFields);

        List<SortOrder> sortOrders = new ArrayList<>();
        when(this.sortQuery.getOrder()).thenReturn(sortOrders);

        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));
        verify(this.constraintValidatorContext).buildConstraintViolationWithTemplate(SwaggerDoc.SORT_ORDER_VALIDATION_NOT_EMPTY_MSG);
    }

    @Test
    public void should_returnFalse_when_fieldAndOrderSizeNotMatch() {
        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        when(this.sortQuery.getField()).thenReturn(sortFields);
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(SortOrder.ASC);
        sortOrders.add(SortOrder.DESC);
        when(this.sortQuery.getOrder()).thenReturn(sortOrders);

        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));
        verify(this.constraintValidatorContext).buildConstraintViolationWithTemplate(SwaggerDoc.SORT_FIELD_ORDER_SIZE_NOT_MATCH);
    }

    @Test
    public void should_returnFalse_when_orderListHasNullValues() {
        when(this.sortQuery.getField()).thenReturn(null);
        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));

        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        when(this.sortQuery.getField()).thenReturn(sortFields);
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(null);
        when(this.sortQuery.getOrder()).thenReturn(sortOrders);

        assertFalse(sut.isValid(this.sortQuery, this.constraintValidatorContext));
        verify(this.constraintValidatorContext).buildConstraintViolationWithTemplate(SwaggerDoc.SORT_NOT_VALID_ORDER_OPTION);
    }
}
