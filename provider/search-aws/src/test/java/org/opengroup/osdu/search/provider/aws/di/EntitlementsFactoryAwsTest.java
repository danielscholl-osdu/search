/**
 * Copyright 2020 IBM Corp. All Rights Reserved.
 * Copyright 2020 © Amazon Web Services
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.provider.aws.di;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class EntitlementsFactoryAwsTest {

    @Test
    public void entitlementsFactory_test(){
        EntitlementsFactoryAws entitlementFactory = new EntitlementsFactoryAws();

        assertEquals(entitlementFactory.createInstance().getClass(), EntitlementsFactory.class);
        assertEquals(entitlementFactory.getObjectType(), IEntitlementsFactory.class);

    }
}
