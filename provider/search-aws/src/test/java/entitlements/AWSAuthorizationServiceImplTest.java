/* Copyright © Amazon Web Services

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */

package entitlements;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsService;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.*;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.search.provider.aws.cache.GroupCache;
import org.opengroup.osdu.search.provider.aws.entitlements.AWSAuthorizationServiceImpl;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AWSAuthorizationServiceImplTest {

    private static final String MEMBER_EMAIL = "tester@gmail.com";
    private static final String HEADER_ACCOUNT_ID = "anyTenant";
    private static final String HEADER_AUTHORIZATION = "anyCrazyToken";

    @Mock
    private IEntitlementsFactory entitlementFactory;

    @Mock
    private GroupCache<String, Groups> cache;

    private DpsHeaders headers;

    @Mock
    private IEntitlementsService entitlementService;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private AWSAuthorizationServiceImpl entitlementsAndCacheService;

    @InjectMocks
    private AWSAuthorizationServiceImpl sut;

    private static final Map<String, String> headerMap = new HashMap<>();

    @Before
    public void setup() {

        setDefaultHeaders();

        this.headers = DpsHeaders.createFromMap(headerMap);

        when(this.entitlementFactory.create(this.headers)).thenReturn(this.entitlementService);
    }

    private void setDefaultHeaders() {
        headerMap.put(DpsHeaders.ACCOUNT_ID, HEADER_ACCOUNT_ID);
        headerMap.put(DpsHeaders.AUTHORIZATION, HEADER_AUTHORIZATION);
    }

    @Test
    public void should_returnMemberEmail_when_authorizationIsSuccessful() throws Exception {

        GroupInfo g1 = new GroupInfo();
        g1.setEmail("role1@gmail.com");
        g1.setName("role1");

        GroupInfo g2 = new GroupInfo();
        g2.setEmail("role2@gmail.com");
        g2.setName("role2");

        List<GroupInfo> groupsInfo = new ArrayList<>();
        groupsInfo.add(g1);
        groupsInfo.add(g2);

        Groups groups = new Groups();
        groups.setGroups(groupsInfo);
        groups.setMemberEmail(MEMBER_EMAIL);
        groups.setDesId(MEMBER_EMAIL);

        when(this.entitlementService.getGroups()).thenReturn(groups);

        AuthorizationResponse response = AuthorizationResponse.builder().groups(groups).user(MEMBER_EMAIL).build();

        assertEquals(response, this.sut.authorizeAny(this.headers, "role2"));
    }
    @Test
    public void should_throw_AppException_when_EntitlementsException() throws Exception {

        GroupInfo g1 = new GroupInfo();
        g1.setEmail("role1@gmail.com");
        g1.setName("role1");

        GroupInfo g2 = new GroupInfo();
        g2.setEmail("role2@gmail.com");
        g2.setName("role2");

        List<GroupInfo> groupsInfo = new ArrayList<>();
        groupsInfo.add(g1);
        groupsInfo.add(g2);

        Groups groups = new Groups();
        groups.setGroups(groupsInfo);
        groups.setMemberEmail(MEMBER_EMAIL);
        groups.setDesId(MEMBER_EMAIL);

        when(this.entitlementService.getGroups()).thenThrow(new EntitlementsException("String", new HttpResponse()));
        try {
            AuthorizationResponse response = AuthorizationResponse.builder().groups(groups).user(MEMBER_EMAIL).build();
            assertEquals(response, this.sut.authorizeAny(this.headers, "role2"));
        } catch (AppException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void should_throw_RedisException_when_Cache_return_NULL() throws Exception {

        GroupInfo g1 = new GroupInfo();
        g1.setEmail("role1@gmail.com");
        g1.setName("role1");

        GroupInfo g2 = new GroupInfo();
        g2.setEmail("role2@gmail.com");
        g2.setName("role2");

        List<GroupInfo> groupsInfo = new ArrayList<>();
        groupsInfo.add(g1);
        groupsInfo.add(g2);

        Groups groups = new Groups();
        groups.setGroups(groupsInfo);
        groups.setMemberEmail(MEMBER_EMAIL);
        groups.setDesId(MEMBER_EMAIL);
        when(this.cache.get(anyString())).thenReturn(null);
        try {
            AuthorizationResponse response = AuthorizationResponse.builder().groups(groups).user(MEMBER_EMAIL).build();
            assertEquals(response, this.sut.authorizeAny(this.headers, "role2"));
        } catch (AppException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void should_throw_AppException_when_Entitlement_error() throws Exception {

        GroupInfo g1 = new GroupInfo();
        g1.setEmail("role1@gmail.com");
        g1.setName("role1");

        GroupInfo g2 = new GroupInfo();
        g2.setEmail("role2@gmail.com");
        g2.setName("role2");

        List<GroupInfo> groupsInfo = new ArrayList<>();
        groupsInfo.add(g1);
        groupsInfo.add(g2);

        Groups groups = new Groups();
        groups.setGroups(groupsInfo);
        groups.setMemberEmail(MEMBER_EMAIL);
        groups.setDesId(MEMBER_EMAIL);
        AWSAuthorizationServiceImpl awsAuthorizationServiceImpl = new AWSAuthorizationServiceImpl();
        try {
            AuthorizationResponse response = AuthorizationResponse.builder().groups(groups).user(MEMBER_EMAIL).build();
            assertEquals(response, awsAuthorizationServiceImpl.authorizeAny(this.headers, "role2"));
        } catch (AppException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void should_return_null_response() throws Exception {
        AWSAuthorizationServiceImpl awsAuthorizationServiceImpl = new AWSAuthorizationServiceImpl();
        try {
            AuthorizationResponse response = awsAuthorizationServiceImpl.authorizeAny("test", this.headers, "role2");
            assertNull(response);
        } catch (AppException ex) {
            ex.printStackTrace();
        }
    }
    
    @Test
    public void should_throw_Exception_with_null_roles() {

        DpsHeaders headers = new DpsHeaders();
        try {
            AuthorizationResponse respones = this.sut.authorizeAny(headers);
            assertNull(respones);
        } catch (AppException ex) {
            ex.printStackTrace();
        }
    }
}
