package org.opengroup.osdu.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SwaggerResponseMock extends ResponseBase {
    private String openapi;
    private Object info;
    private Object servers;
    private Object security;
    private Object tags;
    private Object paths;
    private Object components;
}
