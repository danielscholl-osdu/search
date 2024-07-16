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

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
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

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestScope
@RequestMapping("/")
@Validated
@Tag(name = "search-api", description = "Service endpoints to search data in datalake")
public class SearchApi {

    @Inject
    private IQueryService queryService;
    @Inject
    private IScrollQueryService scrollQueryService;

    @Operation(summary = "${searchApi.queryRecords.summary}", description = "${searchApi.queryRecords.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "search-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = { @Content(schema = @Schema(implementation = QueryResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid parameters were given on request",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "502", description = "Search service scale-up is taking longer than expected. Wait 10 seconds and retry.",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class))})
    })
    @PostMapping("/query")
    @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "', '" + SearchServiceRole.USER + "')")
    public ResponseEntity<QueryResponse> queryRecords(@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY) @RequestBody @Valid QueryRequest queryRequest) throws Exception {
        QueryResponse searchResponse = queryService.queryIndex(queryRequest);
        return new ResponseEntity<QueryResponse>(searchResponse, HttpStatus.OK);
    }

    @Operation(summary = "${searchApi.queryWithCursor.summary}", description = "${searchApi.queryWithCursor.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "search-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = { @Content(schema = @Schema(implementation = CursorQueryResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid parameters were given on request",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "502", description = "Search service scale-up is taking longer than expected. Wait 10 seconds and retry.",  content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class))})
    })
    @PostMapping("/query_with_cursor")
    @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "', '" + SearchServiceRole.USER + "')")
    @ApiOperation(
            value = SwaggerDoc.QUERY_WITH_CURSOR_POST_TITLE,
            nickname = SwaggerDoc.QUERY_WITH_CURSOR_OPERATION_ID,
            code = HttpServletResponse.SC_ACCEPTED,
            notes = SwaggerDoc.QUERY_WITH_CURSOR_POST_NOTES)
    public ResponseEntity<CursorQueryResponse> queryWithCursor(
        @NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY) @RequestBody @Valid CursorQueryRequest queryRequest) throws Exception {
        CursorQueryResponse searchResponse = scrollQueryService.queryIndex(queryRequest);
        return new ResponseEntity<CursorQueryResponse>(searchResponse, HttpStatus.OK);
    }
}
