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

    private Groups getGroups(String desId, String memberEmail) {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(memberEmail);
        return groups;
    }

    private void validateAppException(AppException e, int errorCode, String errorMessage) {
        assertEquals(errorCode, e.getError().getCode());
        assertThat(e.getError().getMessage(), containsString(errorMessage));
    }
}