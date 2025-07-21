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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.internal.util.privilegedactions.NewInstance;

import org.opengroup.osdu.core.common.model.search.QueryRequest;

public class QueryRequestConstraintValidator implements ConstraintValidatorFactory {

	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		return run(NewInstance.action(key, "ConstraintValidator"));
	}

	@Override
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		// do nothing
	}

	private <T> T run(PrivilegedAction<T> action) {
		return System.getSecurityManager() != null ? AccessController.doPrivileged(action) : action.run();
	}

	public Validator getValidator() {
		Configuration<?> config = Validation.byDefaultProvider().configure();
		config.constraintValidatorFactory(this);
		ValidatorFactory factory = config.buildValidatorFactory();
		return factory.getValidator();
	}

	public String isValid(QueryRequest queryRequest) {
		String violationMessage = null;
		Set<ConstraintViolation<QueryRequest>> violations = getValidator().validate(queryRequest);
		if (violations.isEmpty())
			return null;

		for (ConstraintViolation<QueryRequest> violation : violations) {
			violationMessage = violation.getMessage();
		}
		return violationMessage;
	}
}
