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

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.smart.models.Attribute;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public class AttributeMappingReaderTest {

    private AttributeMappingReader attributeMappingReader;


    @Test
    public void should_return_correct_Attributes_when_valid_file() throws Exception {
        attributeMappingReader = spy(new AttributeMappingReader());
        assertDoesNotThrow(() ->
                attributeMappingReader.convertJsonIntoAttributes("testattributes/fileWithCorrectAttributes.json")
        );
    }

    @Test
    public void should_throw_expection_when_invalid_json_file() throws Exception {
        attributeMappingReader = spy(new AttributeMappingReader());
        assertThrows(AppException.class, () ->
                attributeMappingReader.convertJsonIntoAttributes("testattributes/invalidAttributeFile.json")
        );
    }

    @Test
    public void should_throw_exception_when_invalid_file() throws Exception {
        attributeMappingReader = spy(new AttributeMappingReader());
        AppException exception = assertThrows(AppException.class, () -> {
            attributeMappingReader.convertJsonIntoAttributes("abcd");
        });
    }

    @Test
    public void should_return_attributes_when_input_file_passed() throws Exception {
        attributeMappingReader = spy(AttributeMappingReader.class);
        doReturn(new LinkedList<>()).when(attributeMappingReader).convertJsonIntoAttributes("attributemapping.json");
        List<Attribute> retList = attributeMappingReader.read();
        assertEquals(0, retList.size());
    }

}
