/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.search.provider.ibm.provider.impl;

import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.springframework.stereotype.Service;

//TODO: Move to org.opengroup.osdu.search.search-gcp once available
@Service
public class ProviderHeaderServiceImpl implements IProviderHeaderService {
    private static final String DATA_GROUPS = "X-Data-Groups";
    private static final String CRON_SERVICE = "X-AppEngine-Cron";
    private static final String DATA_ROOT_USER = "X-Data-Root-User";

    @Override
    public String getCronServiceHeader() {
        return CRON_SERVICE;
    }

    @Override
    public String getDataGroupsHeader() {
        return DATA_GROUPS;
    }

    @Override
    public String getDataRootUserHeader() { return DATA_ROOT_USER; }
}
