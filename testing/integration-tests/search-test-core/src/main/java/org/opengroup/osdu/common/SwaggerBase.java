package org.opengroup.osdu.common;

import joptsimple.internal.Strings;
import org.opengroup.osdu.response.SwaggerResponseMock;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.HTTPClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SwaggerBase extends TestsBase {

  private SwaggerResponseMock response;

  public SwaggerBase(HTTPClient httpClient) {
    super(httpClient);
  }

  @Override
  protected String getApi() {
    return Config.getSearchBaseURL() + "swagger";
  }

  @Override
  protected String getHttpMethod() {
    return "GET";
  }

  public void i_send_get_request_to_swagger_endpoint() {
    response = executeQuery(Strings.EMPTY, headers, httpClient.getAccessToken(), SwaggerResponseMock.class);
  }

  public void i_should_get_openapi_spec_in_response() {
    assertNotNull(response.getOpenapi());
    assertNotNull(response.getInfo());
    assertNotNull(response.getServers());
    assertNotNull(response.getSecurity());
    assertNotNull(response.getTags());
    assertNotNull(response.getPaths());
    assertNotNull(response.getComponents());
    assertEquals(200, response.getResponseCode());
  }

}
