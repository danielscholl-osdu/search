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

package org.opengroup.osdu.search.smart.middleware;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.mockito.junit.MockitoJUnitRunner;

import org.opengroup.osdu.search.smart.models.AttributeCollection;
import org.opengroup.osdu.search.smart.models.Kinds;

@RunWith(MockitoJUnitRunner.class)
public class SyncFilterValuesServiceTest {

    @Mock
    Kinds kinds;
    @Mock
    private AttributeCollection attributes;
    
    @InjectMocks
    private SyncFilterValuesServiceImpl sut;

    @Test(expected = AppException.class)
    public void should_throw_Exception_when_cache_update_fails() {
        try {
            doThrow(new AppException(503, "", "")).when(kinds).cacheSync();
        } catch (IOException ex) {
            fail();
        }
        sut.updateCache();
    }
    
    @Test(expected = AppException.class)
    public void should_throw_Exception_when_attribute_cache_update_fails() throws URISyntaxException {
    	
        try {
         	Mockito.doNothing().when(kinds).cacheSync();
            doThrow(new URISyntaxException("","")).when(attributes).cacheSync();
        } catch (IOException ex) {
            fail();
        }
        sut.updateCache();
    }
    
    @Test
    public void should_not_throw_Exception_when_attribute_cache_update_successfully() {
    	
        try {
         	Mockito.doNothing().when(kinds).cacheSync();
         	Mockito.doNothing().when(attributes).cacheSync();
        } catch (IOException | URISyntaxException ex) {
            fail();
        }
        sut.updateCache();
    }
}
