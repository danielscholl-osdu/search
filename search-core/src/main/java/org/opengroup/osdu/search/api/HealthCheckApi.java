package org.opengroup.osdu.search.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;

@RestController
@RequestMapping("/health")
@Tag(name = "health-check-api", description = "Health Check API")
public class HealthCheckApi {

	@Operation(summary = "${healthChecksApi.livenessCheck.summary}",
			description = "${healthChecksApi.livenessCheck.description}", tags = { "health-check-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = String.class)) })
	})
	@PermitAll
	@GetMapping("/liveness_check")
	public ResponseEntity<String> livenessCheck() {
		return new ResponseEntity<String>("Search Service is alive", HttpStatus.OK);
	}

	@Operation(summary = "${healthChecksApi.readinessCheck.summary}",
			description = "${healthChecksApi.readinessCheck.description}", tags = { "health-check-api" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = String.class)) })
	})
	@PermitAll
	@GetMapping("/readiness_check")
	public ResponseEntity<String> readinessCheck() {
		return new ResponseEntity<String>("Search Service is ready", HttpStatus.OK);
	}
}
