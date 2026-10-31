package com.klu.microservices.bill.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class InvoicePricingServiceTest {

    private InvoicePricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new InvoicePricingService();
        ReflectionTestUtils.setField(pricingService, "taxPercentage", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(pricingService, "defaultLabor", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(pricingService, "defaultParts", new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should correctly calculate charges for General Maintenance")
    void testGeneralMaintenanceCalculation() {
        InvoicePricingService.PricingCalculation calc = pricingService.calculate("General Maintenance", BigDecimal.ZERO);

        assertEquals(new BigDecimal("75.00"), calc.getLaborCharges());
        assertEquals(new BigDecimal("45.00"), calc.getPartsCharges());
        assertEquals(new BigDecimal("0.00"), calc.getAdditionalCharges());
        assertEquals(new BigDecimal("120.00"), calc.getSubtotal());
        assertEquals(new BigDecimal("10.00"), calc.getTaxPercentage());
        assertEquals(new BigDecimal("12.00"), calc.getTaxAmount()); // 10% of 120.00
        assertEquals(new BigDecimal("132.00"), calc.getTotalAmount()); // 120.00 + 12.00
    }

    @Test
    @DisplayName("Should correctly calculate charges for Brake Services with additional charges")
    void testBrakeServicesWithAdditionalCharges() {
        BigDecimal additional = new BigDecimal("25.00");
        InvoicePricingService.PricingCalculation calc = pricingService.calculate("Brake Services", additional);

        assertEquals(new BigDecimal("120.00"), calc.getLaborCharges());
        assertEquals(new BigDecimal("130.00"), calc.getPartsCharges());
        assertEquals(new BigDecimal("25.00"), calc.getAdditionalCharges());
        // Subtotal = 120 + 130 + 25 = 275.00
        assertEquals(new BigDecimal("275.00"), calc.getSubtotal());
        // Tax = 10% of 275.00 = 27.50
        assertEquals(new BigDecimal("27.50"), calc.getTaxAmount());
        // Total = 275.00 + 27.50 = 302.50
        assertEquals(new BigDecimal("302.50"), calc.getTotalAmount());
    }

    @Test
    @DisplayName("Should use default rates for unrecognized custom service types")
    void testDefaultServiceRateFallback() {
        InvoicePricingService.PricingCalculation calc = pricingService.calculate("Custom Transmission Flush", null);

        assertEquals(new BigDecimal("80.00"), calc.getLaborCharges());
        assertEquals(new BigDecimal("50.00"), calc.getPartsCharges());
        assertEquals(new BigDecimal("0.00"), calc.getAdditionalCharges());
        assertEquals(new BigDecimal("130.00"), calc.getSubtotal());
        assertEquals(new BigDecimal("13.00"), calc.getTaxAmount());
        assertEquals(new BigDecimal("143.00"), calc.getTotalAmount());
    }
}
