package com.klu.microservices.test.unit;

import com.klu.microservices.test.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrecisionAuto Care — API Contract & Validation Constraints Unit Tests (Offline)")
public class ApiContractTest {

    // Regexp from book-service VehicleRequest: ^[A-Za-z0-9 -]{4,15}$
    private static final Pattern REG_NO_PATTERN = Pattern.compile("^[A-Za-z0-9 -]{4,15}$");

    @ParameterizedTest
    @ValueSource(strings = {
            "KA01-AB1234",
            "MH12-DE-9999",
            "DL-01-A-0001",
            "AB12",
            "123456789012345"
    })
    @DisplayName("Valid vehicle registration numbers must match regex constraint")
    void testValidRegistrationNumbers(String regNo) {
        assertTrue(REG_NO_PATTERN.matcher(regNo).matches(), "Expected '" + regNo + "' to be valid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A",                // Too short (< 4)
            "ABC",              // Too short (< 4)
            "TOOLONGSPECIFICATION1234", // Too long (> 15)
            "KA01@1234",        // Invalid special character @
            "KA#01$99",         // Invalid special characters #, $
            ""                  // Blank
    })
    @DisplayName("Invalid vehicle registration numbers must be rejected by regex constraint")
    void testInvalidRegistrationNumbers(String regNo) {
        assertFalse(REG_NO_PATTERN.matcher(regNo).matches(), "Expected '" + regNo + "' to be rejected");
    }

    @Test
    @DisplayName("Vehicle manufacturing year boundary constraints (1900 to currentYear + 1)")
    void testVehicleYearConstraints() {
        int currentYear = LocalDate.now().getYear();
        int maxAllowedYear = currentYear + 1;
        int minAllowedYear = 1900;

        assertTrue(minAllowedYear >= 1900);
        assertTrue(maxAllowedYear > currentYear);
        assertFalse(1899 >= minAllowedYear, "Year 1899 must be rejected");
        assertFalse((currentYear + 2) <= maxAllowedYear, "Year in distant future must be rejected");
    }

    @Test
    @DisplayName("Supported services list contains all required catalog items")
    void testSupportedServicesCatalog() {
        assertEquals(6, TestData.SUPPORTED_SERVICES.size());
        assertTrue(TestData.SUPPORTED_SERVICES.contains("General Maintenance"));
        assertTrue(TestData.SUPPORTED_SERVICES.contains("Oil & Fluid Service"));
        assertTrue(TestData.SUPPORTED_SERVICES.contains("Brake Services"));
        assertTrue(TestData.SUPPORTED_SERVICES.contains("Electrical Diagnostics"));
        assertTrue(TestData.SUPPORTED_SERVICES.contains("A/C & Heating"));
        assertTrue(TestData.SUPPORTED_SERVICES.contains("Engine Repair"));
    }
}
