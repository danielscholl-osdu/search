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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.search.smart.models.Filter;
import org.opengroup.osdu.search.smart.models.FilterCollection;
import org.opengroup.osdu.search.validation.QueryRequestConstraintValidator;

public class ParseToQuery {
	
	@Inject
	QueryRequestConstraintValidator offsetConstraintValidator;
	
	private static final List<ParserLinkBase> filters = new ArrayList<>();
	private static final List<String> returnFields = new ArrayList<>();

	static {
		filters.add(new TypeParserLink());
		filters.add(new SourceParserLink());
		filters.add(new AttributeParserLink());
		filters.add(new DefaultParserLink());

		returnFields.add("id");
		returnFields.add("kind");
	}

	public QueryRequest parse(List<FilterCollection> input, int offset, int limit) {
		QueryRequest output = new QueryRequest();
		output.setKind("*:%s:%t:*");
		Set<String> usedFilters = new HashSet<>();

		for (FilterCollection fc : input) {
			for (Filter filter : fc.getFilters()) {
				if (usedFilters.contains(filter.getName()))
					throw new AppException(400, "Bad request", String.format("Cannot use '%s' filter more than once.", filter.getName()));

				if(!"Text".equalsIgnoreCase(filter.getName()))
					usedFilters.add(filter.getName());
				addToQuery(output, filter);
			}
		}
		
		//TODO::  We will revisit this later for performance issue.
		//output.setReturnHighlightedFields(true);
		//Commented the below line to get all the "Data" attributes for Smart Search integration with DMApp "View Data".
		// output.getReturnedFields().addAll(returnFields);
		output.setKind(output.getKind().replace("%s", "*").replace("%t", "*"));
		output.setLimit(limit);
		
		output.setFrom(offset);
		String offsetStatus = offsetConstraintValidator.isValid(output);
		if(offsetStatus!=null)
			throw new AppException(400, "Bad request", offsetStatus);

		return output;
	}

	private void addToQuery(QueryRequest output, Filter filter) {
		for (ParserLinkBase link : filters) {
			if (link.canHandle(filter)) {
				link.append(filter, output);
				break;
			}
		}
	}
}
