package org.opengroup.osdu.search.provider.azure.persistence.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.provider.azure.persistence.CosmosDBTenantInfo;
import org.opengroup.osdu.search.provider.azure.persistence.TenantInfoDoc;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class TenantFactoryImplTest {

    private final String[] ids = {"id1", "id2"};
    private final String[] serviceAccounts = {"service-account-1", "service-account-2"};

    @Mock
    private CosmosDBTenantInfo db;

    @InjectMocks
    private TenantFactoryImpl sut;

    @Before
    public void init() {
        List<TenantInfoDoc> tenantInfoDocs = new ArrayList<>();
        assertEquals(ids.length, serviceAccounts.length);
        for (int i = 0; i < ids.length; i++) {
            TenantInfoDoc tenantInfoDoc = new TenantInfoDoc(ids[i], serviceAccounts[i]);
            tenantInfoDocs.add(tenantInfoDoc);
        }
        doReturn(tenantInfoDocs).when(db).findAll();
    }

    @Test
    public void testExists_whenExistingTenantNameGiven() {
        for (String tenantName: ids) {
            assertTrue(sut.exists(tenantName));
        }
    }

    @Test
    public void testExists_whenNonExistingTenantNameGiven() {
        assertFalse(sut.exists("id-that-does-not-exist"));
        assertFalse(sut.exists(""));
    }

    @Test
    public void testGetTenantInfo_whenExistingTenantNameGiven() {
        for (int i = 0; i < ids.length; i++) {
            TenantInfo tenantInfo = sut.getTenantInfo(ids[i]);
            assertEquals(tenantInfo.getName(), ids[i]);
            assertEquals(tenantInfo.getServiceAccount(), serviceAccounts[i]);
        }
    }

    @Test
    public void testListTenantInfo() {
        List<TenantInfo> tenantInfoList = new ArrayList<TenantInfo> (sut.listTenantInfo());
        for (TenantInfo tenantInfo: tenantInfoList) {
            assertTrue(ArrayUtils.contains(ids, tenantInfo.getName()));
            assertTrue(ArrayUtils.contains(serviceAccounts, tenantInfo.getServiceAccount()));
        }
    }
}
