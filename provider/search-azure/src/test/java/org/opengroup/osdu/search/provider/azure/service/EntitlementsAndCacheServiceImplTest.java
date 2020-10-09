package org.opengroup.osdu.search.provider.azure.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.EntitlementsException;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EntitlementsAndCacheServiceImplTest {

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private ICache<String, Groups> cache;

    @Mock
    private IEntitlementsFactory factory;

    @InjectMocks
    private EntitlementsAndCacheServiceImpl sut;

    @Before
    public void init() {
        cache.clearAll();
    }

    @Test(expected = AppException.class)
    public void testIsValidAcl_whenMissingGroups_throwsException() throws EntitlementsException {
        IEntitlementsService iEntitlementsService = mock(IEntitlementsService.class);

        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        doReturn(groups).when(iEntitlementsService).getGroups();
        doReturn(iEntitlementsService).when(factory).create(eq(dpsHeaders));

        try {
            sut.isValidAcl(dpsHeaders, new HashSet<>());
        } catch (AppException e) {
            int errorCode = 500;
            // TODO [aaljain]: Improve EntitlementsAndCacheServiceImpl to add better message logs
            String errorMessage = "Unknown error happened when validating ACL";
            validateAppException(e, errorCode, errorMessage);
            throw(e);
        }
    }

    @Test(expected = AppException.class)
    public void testIsValidAcl_whenInvalidEmailId_throwsException() throws EntitlementsException {
        IEntitlementsService iEntitlementsService = mock(IEntitlementsService.class);

        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        String invalidEmail = "invalidemail.com";
        String description = "Group description";
        String groupName = "groupName";
        GroupInfo groupInfo = getGroupInfo(invalidEmail, description, groupName);
        List<GroupInfo> groupInfoList = Arrays.asList(groupInfo);

        groups.setGroups(groupInfoList);

        doReturn(groups).when(iEntitlementsService).getGroups();
        doReturn(iEntitlementsService).when(factory).create(eq(dpsHeaders));

        try {
            sut.isValidAcl(dpsHeaders, new HashSet<>());
        } catch (AppException e) {
            int errorCode = 500;
            // TODO [aaljain]: Improve EntitlementsAndCacheServiceImpl to add better message logs
            String errorMessage = "Unknown error happened when validating ACL";
            validateAppException(e, errorCode, errorMessage);
            throw(e);
        }
    }

    @Test
    public void testIsValidAcl_whenDifferentDomain_thenReturnsFalse() throws EntitlementsException {
        IEntitlementsService iEntitlementsService = mock(IEntitlementsService.class);

        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        String invalidEmail = "group@domain.com";
        String description = "Group description";
        String groupName = "groupName";
        GroupInfo groupInfo = getGroupInfo(invalidEmail, description, groupName);
        List<GroupInfo> groupInfoList = Arrays.asList(groupInfo);

        groups.setGroups(groupInfoList);

        doReturn(groups).when(iEntitlementsService).getGroups();
        doReturn(iEntitlementsService).when(factory).create(eq(dpsHeaders));

        List<String> emails = Arrays.asList("emailid1@domain.com", "emailid2@otherdomain.com");
        Set<String> acls = new HashSet<String>(emails);

        boolean status = sut.isValidAcl(dpsHeaders, acls);

        assertFalse(status);
    }

    @Test
    public void testIsValidAcl_whenCorrectGroupAndAcl_thenReturnsTrue() throws EntitlementsException {
        IEntitlementsService iEntitlementsService = mock(IEntitlementsService.class);

        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        String invalidEmail = "group@domain.com";
        String description = "Group description";
        String groupName = "groupName";
        GroupInfo groupInfo = getGroupInfo(invalidEmail, description, groupName);
        List<GroupInfo> groupInfoList = Arrays.asList(groupInfo);

        groups.setGroups(groupInfoList);

        doReturn(groups).when(iEntitlementsService).getGroups();
        doReturn(iEntitlementsService).when(factory).create(eq(dpsHeaders));

        List<String> emails = Arrays.asList("emailid1@domain.com", "emailid2@domain.com");
        Set<String> acls = new HashSet<String>(emails);

        boolean status = sut.isValidAcl(dpsHeaders, acls);

        assertTrue(status);
    }

    private Groups getGroups(String desId, String memberEmail) {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(memberEmail);
        return groups;
    }

    private GroupInfo getGroupInfo(String email, String description, String name) {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setEmail(email);
        groupInfo.setDescription(description);
        groupInfo.setName(name);
        return groupInfo;
    }

    private void validateAppException(AppException e, int errorCode, String errorMessage) {
        assertEquals(errorCode, e.getError().getCode());
        assertThat(e.getError().getMessage(), containsString(errorMessage));
    }
}