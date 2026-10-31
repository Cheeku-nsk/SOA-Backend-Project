package com.klu.microservices.test.integration;

import com.klu.microservices.test.config.RestAssuredConfig;
import com.klu.microservices.test.config.TestConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseIntegrationTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssuredConfig.setup();
    }

    @BeforeEach
    public void verifyGatewayAvailable() {
        Assumptions.assumeTrue(
                TestConfig.isGatewayOnline(),
                "API Gateway is unreachable at " + TestConfig.BASE_URL + ". Live microservice integration test skipped."
        );
    }
}
