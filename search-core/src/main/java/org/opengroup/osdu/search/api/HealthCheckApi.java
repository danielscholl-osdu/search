package org.opengroup.osdu.search.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;

@RestController
@RequestMapping("/health")
public class HealthCheckApi {

	@PermitAll
	@GetMapping("/liveness_check")
	public ResponseEntity<String> livenessCheck() {
		return new ResponseEntity<String>("Search Service is alive", HttpStatus.OK);
	}

	@PermitAll
	@GetMapping("/readiness_check")
	public ResponseEntity<String> readinessCheck() {
		return new ResponseEntity<String>("Search Service is ready", HttpStatus.OK);
	}
}
