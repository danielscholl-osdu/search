// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.search.middleware;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ValidationException;
import javassist.NotFoundException;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.config.SearchConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.opengroup.osdu.search.util.Constants.LARGE_ERROR_MESSAGE;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionMapperTest {

    @Mock
    private JaxRsDpsLog log;
    @Mock
    private SearchConfigurationProperties configurationProperties;
    @InjectMocks
    private GlobalExceptionMapper sut;

    @Test
    public void should_useValuesInAppExceptionInResponse_When_AppExceptionIsHandledByGlobalExceptionMapper() {

        AppException exception = new AppException(409, "any reason", "any message");

        ResponseEntity<Object> response = sut.handleAppException(exception);
        assertEquals(409, response.getStatusCode().value());
        assertEquals(exception.getError(), response.getBody());
    }

    @Test
    public void should_use404ValueInResponse_When_NotFoundExceptionIsHandledByGlobalExceptionMapper() {

        NotFoundException exception = new NotFoundException("any message");

         	ResponseEntity<Object> response = sut.handleNotFoundException(exception);
         	assertEquals(404, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().toString().contains("any message"));
    }

//    @Test
//    public void should_use405ValueInResponse_When_NotAllowedExceptionIsHandledByGlobalExceptionMapper() {
//
//        NotAllowedException exception = new NotAllowedException("any message");
//
//        Response response = sut.toResponse(exception);
//        assertEquals(405, response.getStatus());
//        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());
//        assertNotNull(response.getEntity());
//    }

//    @Test
//    public void should_use415ValueInResponse_When_NotSupportedExceptionIsHandledByGlobalExceptionMapper() {
//
//        NotSupportedException exception = new NotSupportedException("any message");
//
//        Response response = sut.toResponse(exception);
//        assertEquals(415, response.getStatus());
//        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());
//        assertNotNull(response.getEntity());
//    }

    @Test
    public void should_useGenericValuesInResponse_When_ExceptionIsHandledByGlobalExceptionMapper() {

        Exception exception = new Exception("any message");

        ResponseEntity<Object> response = sut.handleGeneralException(exception);
        assertEquals(500, response.getStatusCode().value());
        assertEquals("AppError(code=500, reason=Server error., message=An unknown error has occurred., errors=null, debuggingInfo=null, originalException=java.lang.Exception: any message)", response.getBody().toString());
    }

    @Test
    public  void should_useBadRequestInResponse_When_JsonProcessingExceptionIsHandledByGlobalExceptionMapper() {
        JsonProcessingException exception = new JsonParseException(null, "any message");

        ResponseEntity<Object> response = sut.handleJsonProcessingException(exception);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode().value());
    }

    @Test
    public  void should_useBadRequestInResponse_When_handleUnrecognizedPropertyExceptionIsHandledByGlobalExceptionMapper() {
        UnrecognizedPropertyException exception = mock(UnrecognizedPropertyException.class);

        ResponseEntity<Object> response = sut.handleUnrecognizedPropertyException(exception);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode().value());
    }

    @Test
    public  void should_useBadRequestInResponse_When_handleValidationExceptionIsHandledByGlobalExceptionMapper() {
        ValidationException exception = new ValidationException();

        ResponseEntity<Object> response = sut.handleValidationException(exception);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode().value());
    }

    @Test
    public  void should_useBadRequestInResponse_When_handleAccessDeniedExceptionIsHandledByGlobalExceptionMapper() {
        AccessDeniedException exception = new AccessDeniedException("Access is denied.");

        ResponseEntity<Object> response = sut.handleAccessDeniedException(exception);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusCode().value());
    }

    @Test
    public void should_returnNullResponse_when_BrokenPipeIOExceptionIsCaptured() {
        IOException ioException = new IOException("Broken pipe");

        ResponseEntity response = this.sut.handleIOException(ioException);

        assertNull(response);
    }

    @Test
    public void should_returnServiceUnavailable_when_IOExceptionIsCaptured() {
        IOException ioException = new IOException("Not broken yet");

        ResponseEntity response = this.sut.handleIOException(ioException);

        assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatusCode().value());
    }

    @Test
    public void should_truncateMessage_when_hugeExceptionIsCaptureed() {
        AppException appException = new AppException(HttpStatus.SC_BAD_REQUEST, "Too many clauses", LARGE_ERROR_MESSAGE);
        when(this.configurationProperties.getMaxExceptionLogMessageLength()).thenReturn(5000);
        ResponseEntity response = this.sut.handleAppException(appException);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode().value());
        String loggingMsg = LARGE_ERROR_MESSAGE.substring(0, this.configurationProperties.getMaxExceptionLogMessageLength());

        AppException apException = new AppException(HttpStatus.SC_BAD_REQUEST, "Too many clauses", loggingMsg);
        verify(this.log).warning(eq(loggingMsg), any(AppException.class));
    }

    @Test
    public void should_returnBadRequest_whenMethodArgumentNotValid() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("queryRequest", "kind", "kind cannot be empty");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Object> response = sut.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatusCode.valueOf(400), mock(WebRequest.class));

        assertNotNull(response);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode().value());
        assertInstanceOf(ObjectNode.class, response.getBody());
        ObjectNode body = (ObjectNode) response.getBody();
        assertEquals(400, body.get("code").asInt());
        assertEquals("Bad Request", body.get("reason").asText());
        assertTrue(body.has("errors"));
    }

    @Test
    public void should_returnBadRequest_whenMethodArgumentNotValid_noFieldErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<Object> response = sut.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatusCode.valueOf(400), mock(WebRequest.class));

        assertNotNull(response);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode().value());
        ObjectNode body = (ObjectNode) response.getBody();
        assertNotNull(body);
        assertFalse(body.has("errors"));
    }

    @Test
    public void should_returnBadRequest_whenHttpMessageNotReadable() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("Cannot read", (Throwable) null, null);

        ResponseEntity<Object> response = sut.handleHttpMessageNotReadable(
                exception, new HttpHeaders(), HttpStatusCode.valueOf(400), mock(WebRequest.class));

        assertNotNull(response);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    public void should_handleNonStandardStatusCode_inAppException() {
        AppException exception = new AppException(499, "Custom status", "custom message");

        ResponseEntity<Object> response = sut.handleAppException(exception);
        assertEquals(499, response.getStatusCode().value());
        // Non-standard codes (HttpStatus.resolve() returns null) produce body(e) instead of body(e.getError())
        assertNotNull(response.getBody());
    }

    @Test
    public void should_logSuppressedResponseExceptions() {
        ResponseException suppressedResponseException = mock(ResponseException.class);
        when(suppressedResponseException.getMessage()).thenReturn("Elasticsearch shard failure");

        Exception cause = new Exception("root cause");
        cause.addSuppressed(suppressedResponseException);

        AppException appException = new AppException(HttpStatus.SC_BAD_REQUEST, "bad", "bad request", cause);

        sut.handleAppException(appException);

        verify(log).error(anyString(), any(ResponseException.class));
    }
}
