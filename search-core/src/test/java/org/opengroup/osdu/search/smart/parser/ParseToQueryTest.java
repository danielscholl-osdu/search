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

package org.opengroup.osdu.search.smart.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;

import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.search.smart.models.Filter;
import org.opengroup.osdu.search.smart.models.FilterCollection;
import org.opengroup.osdu.search.validation.QueryRequestConstraintValidator;

@ExtendWith(MockitoExtension.class)
public class ParseToQueryTest {
	
    @Mock
	QueryRequestConstraintValidator offsetConstraintValidator;
    @InjectMocks
    private ParseToQuery sut;

    @Test
    public void should_returnqueryForAllKindsAndLimitAs20_onAnyFilter(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);
        QueryRequest result = sut.parse(new ArrayList<>(),0,20);

        assertEquals(result.getKind(), "*:*:*:*");
        assertEquals(result.getLimit(), 20);
    }

    @Test
    public void should_returnTextQuery_when_filterNameIsEmpty(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);

        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();
        Filter f = new Filter();
        f.setName("");
        f.setValue("ash");
        col.getFilters().add(f);
        input.add(col);
        QueryRequest result = sut.parse(input,0,0);

        assertEquals("\"ash\"", result.getQuery());
    }

    @Test
    public void should_convertQueryWithSpaceToAbsoluteMatch_when_filterIsTextQuery(){
        when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);

        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();
        Filter f = new Filter();
        f.setName("Text");
        f.setValue("ash ketchum");
        col.getFilters().add(f);
        input.add(col);
        QueryRequest result = sut.parse(input,0,0);

        assertEquals("\"ash ketchum\"", result.getQuery());
    }

    @Test
    public void should_returnTypeQuery_when_filterNameIsType(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);

        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();
        Filter f = new Filter();
        f.setName("type");
        f.setValue("ash");
        col.getFilters().add(f);
        input.add(col);
        QueryRequest result = sut.parse(input,0,0);

        assertEquals(result.getQuery(), "");
        assertEquals(result.getKind(), "*:*:ash:*");
    }

    @Test
    public void should_returnEmptyQuery_when_noFiltersGiven(){
        when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);

        List<FilterCollection> input = new ArrayList<>();
        QueryRequest result = sut.parse(input,0,0);

        assertEquals(result.getQuery(), "");
        assertEquals(result.getKind(), "*:*:*:*");
    }

    @Test
    public void should_returnTypeQueryAndSourceQuery_when_filterNameIsTypeAndSource(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);

        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();

        Filter f = new Filter();
        f.setName("type");
        f.setValue("ash");
        col.getFilters().add(f);

        Filter f1 = new Filter();
        f1.setName("source");
        f1.setValue("s");
        col.getFilters().add(f1);

        input.add(col);
        QueryRequest result = sut.parse(input,0,0);

        assertEquals("", result.getQuery());
        assertEquals("*:s:ash:*", result.getKind());
    }

    @Test
    public void should_throwAppException_when_sameFilterUsedTwice(){
        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();
        Filter f = new Filter();
        f.setName("Type");
        col.getFilters().add(f);
        col.getFilters().add(f);
        input.add(col);

        try {
            sut.parse(input,0,0);
            fail("expected exception");
        }catch(AppException e){
            assertEquals(e.getError().getCode(), 400);
            assertEquals(e.getError().getMessage(), "Cannot use 'Type' filter more than once.");
        }
    }

    @Test
    public void should_beAbleToUseTextFilterTwice(){
        when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);

        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();
        Filter f = new Filter();
        f.setName("Text");
        f.setValue("ash");
        col.getFilters().add(f);
        col.getFilters().add(f);
        input.add(col);

        sut.parse(input,0,0);
        QueryRequest result = sut.parse(input,0,0);

        assertEquals("\"ash\" AND \"ash\"", result.getQuery());
    }
    
    @Test
    public void should_throwAppException_when_sameOffsetAndLimitIsGreaterThan10000(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);

        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();
        Filter f = new Filter();
        f.setName("text");
        col.getFilters().add(f);
        input.add(col);

        try {
            sut.parse(input,10000,10);
        }catch(AppException e){
            assertEquals(e.getError().getCode(), 400);
            assertEquals(e.getError().getMessage(), "Invalid combination of limit and offset values, offset + limit cannot be greater than 10000");
        }
    }
    
    @Test
    public void should_returnAttributeQuery_when_filterNameIsOperator(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);
    	
        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();

        Filter f = new Filter();
        f.setName("Operator");
        f.setValue("ash");
        col.getFilters().add(f);

        input.add(col);
        QueryRequest result = sut.parse(input,0,0);

        assertEquals("(data.Operator:\"ash\" OR data.OriginalOperator:\"ash\")", result.getQuery());
    }
    
    @Test
    public void should_returnAttributeQuery_when_filterNameIsOperatorAndField(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);
    	
        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();

        Filter f = new Filter();
        f.setName("Operator");
        f.setValue("ash");
        col.getFilters().add(f);

        Filter f1 = new Filter();
        f1.setName("Field");
        f1.setValue("ash1");
        col.getFilters().add(f1);
        
        input.add(col);
        QueryRequest result = sut.parse(input,0,0);

        assertEquals("(data.Operator:\"ash\" OR data.OriginalOperator:\"ash\") AND (data.field:\"ash1\" OR data.Field:\"ash1\")", result.getQuery());
    }
    
    @Test
    public void should_returnAttributeQuery_when_filterNameIsOperatorAndFieldAndText(){
    	when(this.offsetConstraintValidator.isValid(any())).thenReturn(null);
    	
        List<FilterCollection> input = new ArrayList<>();
        FilterCollection col = new FilterCollection();

        Filter f = new Filter();
        f.setName("Operator");
        f.setValue("ash");
        col.getFilters().add(f);

        Filter f1 = new Filter();
        f1.setName("Field");
        f1.setValue("ash1");
        col.getFilters().add(f1);

        Filter f3 = new Filter();
        f3.setName("text");
        f3.setValue("ash3");
        col.getFilters().add(f3);
        
        input.add(col);
        QueryRequest result = sut.parse(input,0,0);
        
        assertEquals("(data.Operator:\"ash\" OR data.OriginalOperator:\"ash\") AND (data.field:\"ash1\" OR data.Field:\"ash1\") AND \"ash3\"", result.getQuery());
    }
}
