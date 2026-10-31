package com.klu.microservices.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("PrecisionAuto Care — Test Project Initialization Test")
class PrecisionAutoIntegrationTestsApplicationTests {

	@Test
	@DisplayName("Test project application class loads properly")
	void contextLoads() {
		assertNotNull(PrecisionAutoIntegrationTestsApplication.class);
	}
}
