package com.klu.microservices.bill.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class InvoicePricingService {

    @Value("${billing.tax.percentage:10.00}")
    private BigDecimal taxPercentage;

    @Value("${billing.pricing.default-labor:80.00}")
    private BigDecimal defaultLabor;

    @Value("${billing.pricing.default-parts:50.00}")
    private BigDecimal defaultParts;

    private static final Map<String, BigDecimal> LABOR_RATES = new HashMap<>();
    private static final Map<String, BigDecimal> PARTS_RATES = new HashMap<>();

    static {
        LABOR_RATES.put("general maintenance", new BigDecimal("75.00"));
        PARTS_RATES.put("general maintenance", new BigDecimal("45.00"));

        LABOR_RATES.put("oil & fluid service", new BigDecimal("50.00"));
        PARTS_RATES.put("oil & fluid service", new BigDecimal("65.00"));

        LABOR_RATES.put("brake services", new BigDecimal("120.00"));
        PARTS_RATES.put("brake services", new BigDecimal("130.00"));

        LABOR_RATES.put("electrical diagnostics", new BigDecimal("95.00"));
        PARTS_RATES.put("electrical diagnostics", new BigDecimal("40.00"));

        LABOR_RATES.put("a/c & heating", new BigDecimal("110.00"));
        PARTS_RATES.put("a/c & heating", new BigDecimal("85.00"));

        LABOR_RATES.put("engine repair", new BigDecimal("250.00"));
        PARTS_RATES.put("engine repair", new BigDecimal("180.00"));
    }

    public BigDecimal getLaborChargeForService(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return defaultLabor;
        }
        String key = serviceType.trim().toLowerCase();
        for (Map.Entry<String, BigDecimal> entry : LABOR_RATES.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return defaultLabor;
    }

    public BigDecimal getPartsChargeForService(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return defaultParts;
        }
        String key = serviceType.trim().toLowerCase();
        for (Map.Entry<String, BigDecimal> entry : PARTS_RATES.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return defaultParts;
    }

    public BigDecimal getTaxPercentage() {
        return taxPercentage != null ? taxPercentage : new BigDecimal("10.00");
    }

    public PricingCalculation calculate(String serviceType, BigDecimal additionalCharges) {
        BigDecimal labor = getLaborChargeForService(serviceType).setScale(2, RoundingMode.HALF_UP);
        BigDecimal parts = getPartsChargeForService(serviceType).setScale(2, RoundingMode.HALF_UP);
        BigDecimal additional = (additionalCharges != null && additionalCharges.compareTo(BigDecimal.ZERO) > 0)
                ? additionalCharges.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = labor.add(parts).add(additional).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = getTaxPercentage().setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(taxRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        return new PricingCalculation(labor, parts, additional, subtotal, taxRate, taxAmount, totalAmount);
    }

    public static class PricingCalculation {
        private final BigDecimal laborCharges;
        private final BigDecimal partsCharges;
        private final BigDecimal additionalCharges;
        private final BigDecimal subtotal;
        private final BigDecimal taxPercentage;
        private final BigDecimal taxAmount;
        private final BigDecimal totalAmount;

        public PricingCalculation(BigDecimal laborCharges, BigDecimal partsCharges, BigDecimal additionalCharges,
                                  BigDecimal subtotal, BigDecimal taxPercentage, BigDecimal taxAmount, BigDecimal totalAmount) {
            this.laborCharges = laborCharges;
            this.partsCharges = partsCharges;
            this.additionalCharges = additionalCharges;
            this.subtotal = subtotal;
            this.taxPercentage = taxPercentage;
            this.taxAmount = taxAmount;
            this.totalAmount = totalAmount;
        }

        public BigDecimal getLaborCharges() {
            return laborCharges;
        }

        public BigDecimal getPartsCharges() {
            return partsCharges;
        }

        public BigDecimal getAdditionalCharges() {
            return additionalCharges;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getTaxPercentage() {
            return taxPercentage;
        }

        public BigDecimal getTaxAmount() {
            return taxAmount;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }
}
