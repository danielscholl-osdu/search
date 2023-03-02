package org.opengroup.osdu.search.util;

import org.elasticsearch.action.search.SearchRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class SearchRequestUtilTest {
    private SearchRequestUtil searchRequestUtil = new SearchRequestUtil();

    @Test
    public void setIgnoreUnavailable_to_true() {
        SearchRequest searchRequest = new SearchRequest("index1");
        searchRequestUtil.setIgnoreUnavailable(searchRequest, true);
        Assert.assertTrue(searchRequest.indicesOptions().ignoreUnavailable());
    }

    @Test
    public void setIgnoreUnavailable_to_false() {
        SearchRequest searchRequest = new SearchRequest("index1");
        searchRequestUtil.setIgnoreUnavailable(searchRequest, false);
        Assert.assertFalse(searchRequest.indicesOptions().ignoreUnavailable());
    }
}
