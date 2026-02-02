package com.byul.wheresmymoney.backend.Common.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> healthCheck() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("message", "Application is running");
		response.put("timestamp", System.currentTimeMillis());
		return ResponseEntity.ok(response);
	}
}
