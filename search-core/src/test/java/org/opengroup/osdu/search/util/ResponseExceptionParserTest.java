// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.util;

import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class ResponseExceptionParserTest {

    @Mock
    private JaxRsDpsLog log;

    @InjectMocks
    private ResponseExceptionParser parser;

    @Test
    void parseException_returnsReasonList_whenValidResponse() throws Exception {
        String json = "{ \"error\": { \"root_cause\": [{ \"reason\": \"index_not_found_exception\" }] } }";

        var entity = mock(org.apache.http.HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        Response response = mock(Response.class);
        when(response.getEntity()).thenReturn(entity);

        ResponseException rex = mock(ResponseException.class);
        when(rex.getResponse()).thenReturn(response);

        Exception cause = new Exception();
        cause.addSuppressed(rex);

        List<String> reasons = parser.parseException(cause);

        assertEquals(1, reasons.size());
        assertEquals("index_not_found_exception", reasons.get(0));
        verifyNoInteractions(log); // should not log any errors
    }

    @Test
    void parseException_logsError_whenInvalidJson() throws Exception {
        String invalidJson = "{ not-valid-json }";

        var entity = mock(org.apache.http.HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(invalidJson.getBytes()));

        Response response = mock(Response.class);
        when(response.getEntity()).thenReturn(entity);

        ResponseException rex = mock(ResponseException.class);
        when(rex.getResponse()).thenReturn(response);

        Exception cause = new Exception();
        cause.addSuppressed(rex);

        List<String> result = parser.parseException(cause);

        assertTrue(result.isEmpty());
        verify(log).error(startsWith("Unable to parse response exception content"), any(IOException.class));
    }

    @Test
    void parseException_returnsEmpty_whenNoResponseExceptionPresent() {
        Exception cause = new Exception();
        cause.addSuppressed(new RuntimeException("Some other error"));

        List<String> result = parser.parseException(cause);

        assertTrue(result.isEmpty());
        verifyNoInteractions(log);
    }

    @Test
    void parseException_returnsEmpty_whenCauseIsNullOrSuppressedNull() {
        assertTrue(parser.parseException((Exception) null).isEmpty());

        Exception cause = new Exception();
        assertTrue(parser.parseException(cause).isEmpty());
    }

    @Test
    void parseException_withAppException_delegatesToOriginalException() throws Exception {
        String json = "{ \"error\": { \"root_cause\": [{ \"reason\": \"shard_failure\" }] } }";

        var entity = mock(org.apache.http.HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        Response response = mock(Response.class);
        when(response.getEntity()).thenReturn(entity);

        ResponseException rex = mock(ResponseException.class);
        when(rex.getResponse()).thenReturn(response);

        Exception inner = new Exception();
        inner.addSuppressed(rex);

        AppException appEx = new AppException(500, "error", "desc", inner);

        List<String> reasons = parser.parseException(appEx);

        assertEquals(List.of("shard_failure"), reasons);
    }
}
