// Copyright © Amazon Web Services
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

package org.opengroup.osdu.search.provider.aws.cache;

import org.opengroup.osdu.core.common.model.search.CursorSettings;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class ElasticCredentialsCacheImplTest {


    private final String s = "s";
    private final CursorSettings o = new CursorSettings("Cursor", "ID");

    @Test
    public void go_Through_Test() throws Exception{                   
        ElasticCredentialsCacheImpl cacheImpl = new ElasticCredentialsCacheImpl();                                                                
        cacheImpl.put(s, o);
        assertNull(cacheImpl.get(s));
        cacheImpl.delete(s);
        assertNull(cacheImpl.get(s));
        cacheImpl.clearAll();                                                                                                                                                                                   
    }

}