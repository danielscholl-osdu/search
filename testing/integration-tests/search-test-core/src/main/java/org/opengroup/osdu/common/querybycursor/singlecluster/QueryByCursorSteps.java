package org.opengroup.osdu.common.querybycursor.singlecluster;

import org.opengroup.osdu.common.QueryByCursorBase;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

import java.util.Map;

public class QueryByCursorSteps extends QueryByCursorBase {


    public QueryByCursorSteps(HTTPClient httpClient) {
        super(httpClient);
    }
    public QueryByCursorSteps(HTTPClient httpClient, ElasticUtils elasticUtils) {
        super(httpClient, elasticUtils);
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