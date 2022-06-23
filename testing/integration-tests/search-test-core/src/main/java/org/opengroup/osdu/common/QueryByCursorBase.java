package org.opengroup.osdu.common;

import com.google.gson.Gson;
import org.opengroup.osdu.request.CursorQuery;
import org.opengroup.osdu.request.SortQuery;
import org.opengroup.osdu.response.ErrorResponseMock;
import org.opengroup.osdu.response.ResponseMock;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.HTTPClient;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QueryByCursorBase extends TestsBase {

    private CursorQuery requestQuery = new CursorQuery();

    public QueryByCursorBase(HTTPClient httpClient) {
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

    public void i_limit_the_count_of_returned_results_to(int limit) {
        requestQuery.setLimit(limit);
    }

    public void i_send_None_with(String kind) {
        requestQuery.setKind(generateActualName(kind, timeStamp));
    }

    public void i_send_with(String query, String kind) {
        requestQuery.setQuery(query);
        requestQuery.setKind(generateActualName(kind, timeStamp));
    }

    public void i_set_the_fields_I_want_in_response_as(List<String> returnedFileds) {
        if(returnedFileds.contains("NULL")) requestQuery.setReturnedFields(null);
        else if (!returnedFileds.contains("All")) requestQuery.setReturnedFields(returnedFileds);
    }

    public void i_apply_geographical_query_on_field(String field) {
        spatialFilter.setField(field);
        requestQuery.setSpatialFilter(spatialFilter);
    }

    public void i_should_get_in_response_records(int resultCount) {
        String payload = requestQuery.toString();
        ResponseMock response = executeQuery(payload, headers, httpClient.getAccessToken(), ResponseMock.class);
        assertEquals(200, response.getResponseCode());
        assertEquals(resultCount, response.getResults().size());
    }

    public void i_should_get_response_with_reason_message_and_errors(List<Integer> codes, String type, String msg,
                                                                     String error) {
        String payload = requestQuery.toString();
        ErrorResponseMock response = executeQuery(payload, headers, httpClient.getAccessToken(), ErrorResponseMock.class);
        if (response.getErrors() != null) {
            assertEquals(generateActualName(error, timeStamp), response.getErrors().get(0));
        }
        assertEquals(type, response.getReason());
        assertEquals(generateActualName(msg, timeStamp), response.getMessage());
        assertTrue(codes.contains(response.getResponseCode()));
    }

    public void i_should_get_in_response_records_along_with_a_cursor(int resultCount) {
        String payload = requestQuery.toString();
        ResponseMock response = executeQuery(payload, headers, httpClient.getAccessToken(), ResponseMock.class);
        assertEquals(200, response.getResponseCode());
        assertEquals(resultCount, response.getResults().size());
        requestQuery.setCursor(response.getCursor());
    }

    public void i_sort_with(String sortJson) {
        SortQuery sortArg = (new Gson()).fromJson(sortJson, SortQuery.class);
        requestQuery.setSort(sortArg);
    }

    public void i_search_as(String isOwner) {
        requestQuery.setQueryAsOwner(Boolean.parseBoolean(isOwner));
    }

    public void i_should_get_records_in_right_order(String firstRecId, String lastRecId) {
        String actualFirstRecordId = generateActualName(firstRecId,timeStamp);
        String actualLastRecordId = generateActualName(lastRecId,timeStamp);
        String payload = requestQuery.toString();
        ResponseMock response = executeQuery(payload, headers, httpClient.getAccessToken(), ResponseMock.class);
        assertEquals(200, response.getResponseCode());
        assertNotNull(response.getResults());
        assertTrue(response.getResults().size() > 0);
        assertEquals(actualFirstRecordId, response.getResults().get(0).get("id").toString());
        assertEquals(actualLastRecordId, response.getResults().get(response.getResults().size()-1).get("id").toString());
    }

    public void i_set_an_invalid_cursor() {
        requestQuery.setCursor("invalid cursor");
    }

    public void i_should_get_in_response_records_using_search_as_mode(int resultCount) {
        String payload = requestQuery.toString();
        ResponseMock response = executeQuery(payload, headers, httpClient.getDefaultAccessToken(), ResponseMock.class);
        assertEquals(200, response.getResponseCode());
        assertEquals(resultCount, response.getResults().size());
    }
}
