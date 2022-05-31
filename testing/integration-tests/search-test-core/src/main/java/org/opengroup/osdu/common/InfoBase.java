package org.opengroup.osdu.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import joptsimple.internal.Strings;
import org.opengroup.osdu.response.InfoResponseMock;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.HTTPClient;

public class InfoBase extends TestsBase {

  private InfoResponseMock response;

  public InfoBase(HTTPClient httpClient) {
    super(httpClient);
  }

  @Override
  protected String getApi() {
    return Config.getSearchBaseURL() + "info";
  }

  @Override
  protected String getHttpMethod() {
    return "GET";
  }

  public void i_send_get_request_to_version_info_endpoint() {
    response =
        executeQuery(Strings.EMPTY, headers, httpClient.getAccessToken(), InfoResponseMock.class);
  }

  public void i_should_get_version_info_in_response() {
    assertEquals(200, response.getResponseCode());
    assertNotNull(response.getGroupId());
    assertNotNull(response.getArtifactId());
    assertNotNull(response.getVersion());
    assertNotNull(response.getBuildTime());
    assertNotNull(response.getBranch());
    assertNotNull(response.getCommitId());
    assertNotNull(response.getCommitMessage());
  }
}
