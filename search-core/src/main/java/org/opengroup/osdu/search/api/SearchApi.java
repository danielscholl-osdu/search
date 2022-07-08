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

package org.opengroup.osdu.search.api;

import io.swagger.annotations.*;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.*;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.search.config.CcsQueryConfig;
import org.opengroup.osdu.search.provider.interfaces.ICcsQueryService;
import org.opengroup.osdu.search.provider.interfaces.IQueryService;
import org.opengroup.osdu.search.provider.interfaces.IScrollQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Api(
        value = SwaggerDoc.SEARCH_TAG,
        authorizations = {@Authorization(value = SwaggerDoc.BEARER_AUTH),
                @Authorization(value = SwaggerDoc.GOOGLE_ID_AUTH)})
@RestController
@RequestScope
@RequestMapping("/")
@Validated
@ApiImplicitParams({
        @ApiImplicitParam(name = DpsHeaders.ACCOUNT_ID, value = SwaggerDoc.PARAMETER_ACCOUNT_ID, required = true, defaultValue = TenantInfo.COMMON, dataType = "string", paramType = "header"),
        @ApiImplicitParam(name = DpsHeaders.ON_BEHALF_OF, value = SwaggerDoc.PARAMETER_ONBEHALF_ACCOUNT_ID, dataType = "string", paramType = "header")})
public class SearchApi {

    @Inject
    private IQueryService queryService;
    @Inject
    private IScrollQueryService scrollQueryService;
    @Inject
    private ICcsQueryService ccsQueryService;
    @Inject
    private CcsQueryConfig ccsQueryConfig;

    @PostMapping("/query")
    @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "', '" + SearchServiceRole.USER + "')")
    @ApiOperation(
            value = SwaggerDoc.QUERY_POST_TITLE,
            nickname = SwaggerDoc.QUERY_OPERATION_ID,
            code = HttpServletResponse.SC_ACCEPTED,
            notes = SwaggerDoc.QUERY_POST_NOTES)
    @ApiResponses({
            @ApiResponse(
                    code = HttpServletResponse.SC_OK,
                    message = SwaggerDoc.QUERY_POST_RESPONSE_OK,
                    response = QueryResponse.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_BAD_REQUEST,
                    message = SwaggerDoc.QUERY_POST_RESPONSE_BAD_REQUEST,
                    response = AppError.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_FORBIDDEN,
                    message = SwaggerDoc.QUERY_POST_RESPONSE_NOT_AUTHORIZED,
                    response = AppError.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_BAD_GATEWAY,
                    message = SwaggerDoc.RESPONSE_BAD_GATEWAY,
                    response = String.class)})
    public ResponseEntity<QueryResponse> queryRecords(@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY) @RequestBody @Valid QueryRequest queryRequest) throws Exception {
            try{
                QueryResponse searchResponse = queryService.queryIndex(queryRequest);
                return new ResponseEntity<QueryResponse>(searchResponse, HttpStatus.OK);
            } catch (AppException e) {
                return handleIndexNotFoundException(e, QueryResponse.getEmptyResponse());
            }
    }

    @PostMapping("/query_with_cursor")
    @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "', '" + SearchServiceRole.USER + "')")
    @ApiOperation(
            value = SwaggerDoc.QUERY_WITH_CURSOR_POST_TITLE,
            nickname = SwaggerDoc.QUERY_WITH_CURSOR_OPERATION_ID,
            code = HttpServletResponse.SC_ACCEPTED,
            notes = SwaggerDoc.QUERY_WITH_CURSOR_POST_NOTES)
    @ApiResponses({
            @ApiResponse(
                    code = HttpServletResponse.SC_OK,
                    message = SwaggerDoc.QUERY_WITH_CURSOR_POST_RESPONSE_OK,
                    response = CursorQueryResponse.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_BAD_REQUEST,
                    message = SwaggerDoc.QUERY_WITH_CURSOR_POST_RESPONSE_BAD_REQUEST,
                    response = AppError.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_FORBIDDEN,
                    message = SwaggerDoc.QUERY_WITH_CURSOR_POST_RESPONSE_NOT_AUTHORIZED,
                    response = AppError.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_BAD_GATEWAY,
                    message = SwaggerDoc.RESPONSE_BAD_GATEWAY,
                    response = String.class)})
    public ResponseEntity<CursorQueryResponse> queryWithCursor(@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY) @RequestBody @Valid CursorQueryRequest queryRequest) throws Exception {
        try{
            CursorQueryResponse searchResponse = scrollQueryService.queryIndex(queryRequest);
            return new ResponseEntity<CursorQueryResponse>(searchResponse, HttpStatus.OK);
        } catch (AppException e) {
            return handleIndexNotFoundException(e, CursorQueryResponse.getEmptyResponse());
        }
    }

    // This endpoint is deprecated as of M10. In M11 this endpoint will be deleted
    @PostMapping("/ccs/query")
    @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "', '" + SearchServiceRole.USER + "')")
    @ApiOperation(
            value = SwaggerDoc.QUERY_POST_TITLE,
            nickname = SwaggerDoc.CCS_QUERY_OPERATION_ID,
            code = HttpServletResponse.SC_ACCEPTED,
            notes = SwaggerDoc.CCS_QUERY_NOTES)
    @ApiResponses({
            @ApiResponse(
                    code = HttpServletResponse.SC_OK,
                    message = SwaggerDoc.QUERY_POST_RESPONSE_OK,
                    response = CcsQueryResponse.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_BAD_REQUEST,
                    message = SwaggerDoc.QUERY_POST_RESPONSE_BAD_REQUEST,
                    response = AppError.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_FORBIDDEN,
                    message = SwaggerDoc.QUERY_POST_RESPONSE_NOT_AUTHORIZED,
                    response = AppError.class),
            @ApiResponse(
                    code = HttpServletResponse.SC_BAD_GATEWAY,
                    message = SwaggerDoc.RESPONSE_BAD_GATEWAY,
                    response = String.class)})
    @Deprecated
    public ResponseEntity<CcsQueryResponse> ccsQuery(@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY) @RequestBody @Valid CcsQueryRequest queryRequest) throws Exception {

        if (ccsQueryConfig.isDisabled()) {
            throw new AppException(HttpStatus.NOT_FOUND.value(), "This API has been deprecated", "Unable to perform action");
        }

        try{
            CcsQueryResponse searchResponse = ccsQueryService.makeRequest(queryRequest);
            return new ResponseEntity<CcsQueryResponse>(searchResponse, HttpStatus.OK);
        } catch (AppException e) {
            return handleIndexNotFoundException(e, new CcsQueryResponse());
        }
    }

    private <T> ResponseEntity<T>  handleIndexNotFoundException(AppException e, T response) {
        if (e.getError().getCode() == HttpStatus.NOT_FOUND.value()
                && e.getError().getMessage().equals("Resource you are trying to find does not exists"))
            return new ResponseEntity<>(response, HttpStatus.OK);
        throw e;
    }
}
