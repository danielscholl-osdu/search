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

import java.util.List;

import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.search.smart.attributes.AttributeLoader;
import org.opengroup.osdu.search.smart.models.Attribute;
import org.opengroup.osdu.search.smart.models.Filter;

public class AttributeParserLink extends ParserLinkBase {

	@Override
	protected String getFilter() {
		return "";
	}

	@Override
	public boolean canHandle(Filter filter) {
		for (Attribute attribute : AttributeLoader.getAttributes()) {
			if (attribute.getName().equals(filter.getName()))
				return true;
		}
		return false;
	}
	
	@Override
	public QueryRequest append(Filter filter, QueryRequest qr) {
		
		for (Attribute attribute : AttributeLoader.getAttributes()) {
			if (attribute.getName().equals(filter.getName())) {
				String value = filter.getValue();
				String smartQuery = getSmartQuery(value, attribute); 
				if (qr.getQuery().isEmpty()) {
					qr.setQuery(smartQuery);
				} else {
					qr.setQuery(new StringBuilder(qr.getQuery()).append(String.format(" AND %s", smartQuery)).toString());
				}
				return qr;
			}
		}
		return qr;
	}

	private String getSmartQuery(String inputValue, Attribute attribute) {
		StringBuilder stringBuilder = new StringBuilder();
		List<String> mapping = attribute.getSchemaMapping();
		for (String value : mapping) {
			if (stringBuilder.length() == 0) {
				stringBuilder.append("(").append(value).append(":\"").append(inputValue).append("\"");
				continue;
			}
			stringBuilder.append(" OR ").append(value).append(":\"").append(inputValue).append("\"");
		}
		
		stringBuilder.append(")");
		return stringBuilder.toString();
	}
}
