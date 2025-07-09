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

package org.opengroup.osdu.search.smart.attributes;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;

import org.opengroup.osdu.search.smart.models.Attribute;

public class AttributeLoaderTest {
	
	@Test
	public void should_return_list_of_attributes_provided_in_attribute_mapping_json() {
		List<Attribute> ls = AttributeLoader.getAttributes();
		assertFalse("The attribute list should not be empty", ls.isEmpty());
	}
}
