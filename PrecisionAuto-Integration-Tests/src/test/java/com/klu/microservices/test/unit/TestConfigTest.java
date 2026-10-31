package com.klu.microservices.test.unit;

import com.klu.microservices.test.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrecisionAuto Care — Testing Configuration Unit Tests (Offline)")
public class TestConfigTest {

    @Test
    @DisplayName("Central Gateway URL must be strictly configured as http://localhost:8080")
    void testGatewayBaseUrl() {
        assertEquals("http://localhost:8080", TestConfig.BASE_URL, "BASE_URL must match API Gateway port 8080");
    }

    @Test
    @DisplayName("Verify service prefix routes match Spring Cloud Gateway configuration")
    void testServiceRoutePrefixes() {
        assertEquals("/auth", TestConfig.AUTH_PREFIX);
        assertEquals("/vehicles", TestConfig.VEHICLES_PREFIX);
        assertEquals("/bookings", TestConfig.BOOKINGS_PREFIX);
        assertEquals("/records", TestConfig.RECORDS_PREFIX);
        assertEquals("/billing", TestConfig.BILLING_PREFIX);
    }

    @Test
    @DisplayName("Admin credentials must be non-null and properly populated")
    void testAdminCredentialsIntegrity() {
        assertNotNull(TestConfig.ADMIN_EMAIL);
        assertFalse(TestConfig.ADMIN_EMAIL.isBlank());
        assertTrue(TestConfig.ADMIN_EMAIL.contains("@"));

        assertNotNull(TestConfig.ADMIN_PASSWORD);
        assertFalse(TestConfig.ADMIN_PASSWORD.isBlank());
    }
}
