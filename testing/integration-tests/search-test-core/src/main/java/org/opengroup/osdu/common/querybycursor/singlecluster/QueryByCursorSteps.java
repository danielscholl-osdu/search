package org.opengroup.osdu.common.querybycursor.singlecluster;

import org.opengroup.osdu.common.QueryByCursorBase;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.HTTPClient;

public class QueryByCursorSteps extends QueryByCursorBase {


    public QueryByCursorSteps(HTTPClient httpClient) {
        super(httpClient);
    }

    @Override
    protected String getApi() {
        return Config.getSearchBaseURL() + "query_with_cursor";
    }

    @Override
    protected String getHttpMethod() {
        return "POST";
    }

}