package com.klu.microservices.test.unit;

import com.klu.microservices.test.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrecisionAuto Care — Pricing & Invoicing Logic Unit Tests (Offline)")
public class PricingCalculationTest {

    @ParameterizedTest(name = "Service: {0} -> Labor: {1}, Parts: {2}")
    @CsvSource({
            "General Maintenance, 75.00, 45.00",
            "Oil & Fluid Service, 50.00, 65.00",
            "Brake Services, 120.00, 130.00",
            "Electrical Diagnostics, 95.00, 40.00",
            "A/C & Heating, 110.00, 85.00",
            "Engine Repair, 250.00, 180.00"
    })
    @DisplayName("Verify itemized labor and parts rates per service type")
    void testStandardServiceRates(String serviceType, String expectedLabor, String expectedParts) {
        BigDecimal labor = TestData.LABOR_RATES.get(serviceType);
        BigDecimal parts = TestData.PARTS_RATES.get(serviceType);

        assertNotNull(labor, "Labor rate must exist for " + serviceType);
        assertNotNull(parts, "Parts rate must exist for " + serviceType);

        assertEquals(new BigDecimal(expectedLabor), labor);
        assertEquals(new BigDecimal(expectedParts), parts);
    }

    @Test
    @DisplayName("Verify subtotal, 10% tax, and total calculation for standard service")
    void testStandardGeneralMaintenanceCalculation() {
        Map<String, Object> calc = TestData.calculateExpectedInvoice("General Maintenance", BigDecimal.ZERO);

        BigDecimal labor = (BigDecimal) calc.get("laborCharges");
        BigDecimal parts = (BigDecimal) calc.get("partsCharges");
        BigDecimal additional = (BigDecimal) calc.get("additionalCharges");
        BigDecimal subtotal = (BigDecimal) calc.get("subtotal");
        BigDecimal taxPercent = (BigDecimal) calc.get("taxPercentage");
        BigDecimal taxAmount = (BigDecimal) calc.get("taxAmount");
        BigDecimal totalAmount = (BigDecimal) calc.get("totalAmount");

        assertEquals(new BigDecimal("75.00"), labor);
        assertEquals(new BigDecimal("45.00"), parts);
        assertEquals(BigDecimal.ZERO.setScale(2), additional);
        assertEquals(new BigDecimal("120.00"), subtotal);
        assertEquals(new BigDecimal("10.00"), taxPercent);
        assertEquals(new BigDecimal("12.00"), taxAmount);
        assertEquals(new BigDecimal("132.00"), totalAmount);
    }

    @Test
    @DisplayName("Verify calculation with additional charges included")
    void testCalculationWithAdditionalCharges() {
        BigDecimal additional = new BigDecimal("25.50");
        Map<String, Object> calc = TestData.calculateExpectedInvoice("Oil & Fluid Service", additional);

        BigDecimal labor = (BigDecimal) calc.get("laborCharges");       // 50.00
        BigDecimal parts = (BigDecimal) calc.get("partsCharges");       // 65.00
        BigDecimal subtotal = (BigDecimal) calc.get("subtotal");         // 50 + 65 + 25.50 = 140.50
        BigDecimal taxAmount = (BigDecimal) calc.get("taxAmount");       // 14.05
        BigDecimal totalAmount = (BigDecimal) calc.get("totalAmount");   // 154.55

        assertEquals(new BigDecimal("50.00"), labor);
        assertEquals(new BigDecimal("65.00"), parts);
        assertEquals(new BigDecimal("140.50"), subtotal);
        assertEquals(new BigDecimal("14.05"), taxAmount);
        assertEquals(new BigDecimal("154.55"), totalAmount);
    }

    @Test
    @DisplayName("Verify default pricing for unrecognized service type")
    void testDefaultPricingFallback() {
        Map<String, Object> calc = TestData.calculateExpectedInvoice("Custom Customization Service", BigDecimal.ZERO);

        BigDecimal labor = (BigDecimal) calc.get("laborCharges"); // 80.00 default
        BigDecimal parts = (BigDecimal) calc.get("partsCharges"); // 50.00 default
        BigDecimal subtotal = (BigDecimal) calc.get("subtotal");   // 130.00

        assertEquals(new BigDecimal("80.00"), labor);
        assertEquals(new BigDecimal("50.00"), parts);
        assertEquals(new BigDecimal("130.00"), subtotal);
    }
}
