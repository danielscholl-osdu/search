package org.opengroup.osdu.search.provider.gcp.provider.impl;

import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;
import org.springframework.stereotype.Service;

//TODO: Move to search-gcp once available
@Service
public class ProviderHeaderServiceImpl implements IProviderHeaderService {
    private static final String DATA_GROUPS = "X-Data-Groups";
    private static final String CRON_SERVICE = "X-AppEngine-Cron";


    @Override
    public String getCronServiceHeader() {
        return CRON_SERVICE;
    }

    @Override
    public String getDataGroupsHeader() {
        return DATA_GROUPS;
    }
}
