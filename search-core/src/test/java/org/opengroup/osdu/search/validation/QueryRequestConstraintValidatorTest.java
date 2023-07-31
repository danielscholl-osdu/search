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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import javax.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.opengroup.osdu.core.common.model.search.QueryRequest;

public class QueryRequestConstraintValidatorTest {

	@Mock
	private QueryRequest queryRequest;

	@InjectMocks
	private QueryRequestConstraintValidator sut;

	private ConstraintValidatorContext constraintValidatorContext;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		constraintValidatorContext = mock(ConstraintValidatorContext.class);
		ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(
				ConstraintValidatorContext.ConstraintViolationBuilder.class);
		when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
	}

	@Test
	public void should_returnNull_when_offsetLimitCombination_isValid() {
		 QueryRequest queryRequest = new QueryRequest();  
		 queryRequest.setLimit(1);
		 queryRequest.setKind("tenant1:test:test:1.0.0");
		 assertNull(sut.isValid(queryRequest));		 
	}
	
	@Test
	public void should_returnErrorMessage_when_offsetLimitCombination_isNotValid() {
		 QueryRequest queryRequest = new QueryRequest();  
		 queryRequest.setLimit(100);
		 queryRequest.setFrom(100000);
		 queryRequest.setKind("tenant1:test:test:1.0.0");
		 String result = sut.isValid(queryRequest);
		 assertEquals("Invalid combination of limit and offset values, offset + limit cannot be greater than 10000",result);
	}
}
