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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javassist.NotFoundException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@RestController
@RequestScope
public class GlobalExceptionMapper extends ResponseEntityExceptionHandler {

    @Autowired
    private JaxRsDpsLog logger;

    @ExceptionHandler(AppException.class)
    protected ResponseEntity<Object> handleAppException(AppException e) {
        return this.getErrorResponse(e);
    }

    @ExceptionHandler(JsonProcessingException.class)
    protected ResponseEntity<Object> handleJsonProcessingException(JsonProcessingException e) {
        return this.getErrorResponse(
                new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid JSON format on the request", e.getMessage(), e));
    }

    // ResponseEntityExceptionHandler already has a default implementation for handling MethodArgumentNotValidException, so we are overriding it
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatus status, WebRequest request) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getDefaultMessage());
        }
        this.logger.warning("Invalid parameters were given on search request", e);
        HttpStatus httpStatus = HttpStatus.resolve(org.apache.http.HttpStatus.SC_BAD_REQUEST);
        return new ResponseEntity<>(this.getValidationResponse(errors), httpStatus);
    }

    // Yes, if we are extending ResponseEntityExceptionHandler, this is being caught as HttpMessageNotReadableException type, and not as UnrecognizedPropertyException type
    // ResponseEntityExceptionHandler already has a default implementation for handling HttpMessageNotReadableException, so we are overriding it
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpHeaders headers, HttpStatus status, WebRequest request) {
        AppException appException = new AppException(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Invalid parameters were given on search request", e);
        this.logger.warning(appException.getError().getMessage(), appException);
        HttpStatus httpStatus = HttpStatus.resolve(appException.getError().getCode());
        return new ResponseEntity<>(appException.getError(), httpStatus);
    }

    @ExceptionHandler(UnrecognizedPropertyException.class)
    protected ResponseEntity<Object> handleUnrecognizedPropertyException(UnrecognizedPropertyException e) {
        return this.getErrorResponse(
                new AppException(HttpStatus.BAD_REQUEST.value(), "Unrecognized field \"" + e.getPropertyName() + "\" found on request ", e.getMessage(), e));
    }

    @ExceptionHandler(ValidationException.class)
    protected ResponseEntity<Object> handleValidationException(ValidationException e) {
        return this.getErrorResponse(
                new AppException(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Invalid parameters were given on search request", e));
    }

    @ExceptionHandler(NotFoundException.class)
    protected ResponseEntity<Object> handleNotFoundException(NotFoundException e) {
        return this.getErrorResponse(
                new AppException(HttpStatus.NOT_FOUND.value(), "Resource not found.", e.getMessage(), e));
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException e) {
        return this.getErrorResponse(
                new AppException(HttpStatus.UNAUTHORIZED.value(), "Access denied", "The user is not authorized to perform this action", e));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneralException(Exception e) {
        return this.getErrorResponse(
                new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server error.",
                        "An unknown error has occurred.", e));
    }

    private ResponseEntity<Object> getErrorResponse(AppException e) {

        String exceptionMsg = e.getError().getMessage();

        if (e.getError().getCode() > 499) {
            this.logger.error(exceptionMsg, e);
        } else {
            this.logger.warning(exceptionMsg, e);
        }

        // Support for non standard HttpStatus Codes
        HttpStatus httpStatus = HttpStatus.resolve(e.getError().getCode());
        if (httpStatus == null) {
            return ResponseEntity.status(e.getError().getCode()).body(e);
        } else {
            return new ResponseEntity<>(e.getError(), httpStatus);
        }
    }

    private ObjectNode getValidationResponse(List<String> errors) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("code", org.apache.http.HttpStatus.SC_BAD_REQUEST);
        node.put("reason", "Bad Request");
        node.put("message", "Invalid parameters were given on search request");
        if (!errors.isEmpty()) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (String error : errors) {
                arrayNode.add(error);
            }
            node.set("errors", arrayNode);
        }
        return node;
    }
}
