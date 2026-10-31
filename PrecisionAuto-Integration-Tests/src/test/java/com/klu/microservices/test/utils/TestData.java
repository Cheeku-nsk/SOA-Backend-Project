package com.klu.microservices.test.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TestData {

    private static final AtomicInteger COUNTER = new AtomicInteger(100);

    public static final String DEFAULT_PASSWORD = "Pass@Precision2026!";

    public static final List<String> SUPPORTED_SERVICES = List.of(
            "General Maintenance",
            "Oil & Fluid Service",
            "Brake Services",
            "Electrical Diagnostics",
            "A/C & Heating",
            "Engine Repair"
    );

    // Expected pricing rates from bill-service InvoicePricingService
    public static final Map<String, BigDecimal> LABOR_RATES = new HashMap<>();
    public static final Map<String, BigDecimal> PARTS_RATES = new HashMap<>();
    public static final BigDecimal TAX_PERCENTAGE = new BigDecimal("10.00");

    static {
        LABOR_RATES.put("General Maintenance", new BigDecimal("75.00"));
        PARTS_RATES.put("General Maintenance", new BigDecimal("45.00"));

        LABOR_RATES.put("Oil & Fluid Service", new BigDecimal("50.00"));
        PARTS_RATES.put("Oil & Fluid Service", new BigDecimal("65.00"));

        LABOR_RATES.put("Brake Services", new BigDecimal("120.00"));
        PARTS_RATES.put("Brake Services", new BigDecimal("130.00"));

        LABOR_RATES.put("Electrical Diagnostics", new BigDecimal("95.00"));
        PARTS_RATES.put("Electrical Diagnostics", new BigDecimal("40.00"));

        LABOR_RATES.put("A/C & Heating", new BigDecimal("110.00"));
        PARTS_RATES.put("A/C & Heating", new BigDecimal("85.00"));

        LABOR_RATES.put("Engine Repair", new BigDecimal("250.00"));
        PARTS_RATES.put("Engine Repair", new BigDecimal("180.00"));
    }

    public static String uniqueEmail(String rolePrefix) {
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%d_%s@precisiontest.com", rolePrefix.toLowerCase(), System.currentTimeMillis() % 1000000, shortId);
    }

    public static String uniqueCustomerEmail() {
        return uniqueEmail("customer");
    }

    public static String uniqueTechnicianEmail() {
        return uniqueEmail("technician");
    }

    public static String uniqueRegistrationNumber() {
        int id = COUNTER.incrementAndGet();
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        // Conforms to pattern ^[A-Za-z0-9 -]{4,15}$
        return String.format("KA%02d-%s", id % 99 + 1, suffix);
    }

    public static Map<String, Object> createVehicleRequest(String regNo) {
        Map<String, Object> req = new HashMap<>();
        req.put("registrationNumber", regNo != null ? regNo : uniqueRegistrationNumber());
        req.put("make", "Toyota");
        req.put("model", "Corolla Altis");
        req.put("year", 2022);
        req.put("vehicleType", "Sedan");
        req.put("fuelType", "Petrol");
        return req;
    }

    public static Map<String, Object> createBookingRequest(Long vehicleId, Long slotId, String serviceType, String notes) {
        Map<String, Object> req = new HashMap<>();
        req.put("vehicleId", vehicleId);
        req.put("slotId", slotId);
        req.put("serviceType", serviceType != null ? serviceType : "General Maintenance");
        req.put("notes", notes != null ? notes : "Regular scheduled inspection");
        return req;
    }

    public static Map<String, Object> calculateExpectedInvoice(String serviceType, BigDecimal additionalCharges) {
        BigDecimal labor = LABOR_RATES.getOrDefault(serviceType, new BigDecimal("80.00")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal parts = PARTS_RATES.getOrDefault(serviceType, new BigDecimal("50.00")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal additional = (additionalCharges != null && additionalCharges.compareTo(BigDecimal.ZERO) > 0)
                ? additionalCharges.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = labor.add(parts).add(additional).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = TAX_PERCENTAGE.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> map = new HashMap<>();
        map.put("laborCharges", labor);
        map.put("partsCharges", parts);
        map.put("additionalCharges", additional);
        map.put("subtotal", subtotal);
        map.put("taxPercentage", taxRate);
        map.put("taxAmount", taxAmount);
        map.put("totalAmount", totalAmount);
        return map;
    }
}
