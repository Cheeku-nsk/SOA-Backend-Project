package com.klu.microservices.bill.dto;

import com.klu.microservices.bill.entity.Invoice;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceResponse {

    private Long invoiceId;
    private String invoiceNumber;
    private Long serviceRecordId;
    private Long bookingId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private String vehicleMake;
    private String vehicleModel;
    private String serviceType;
    private String technicianName;
    private String serviceDescription;
    private String remarks;
    private BigDecimal laborCharges;
    private BigDecimal partsCharges;
    private BigDecimal additionalCharges;
    private BigDecimal subtotal;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private LocalDateTime generatedAt;
    private LocalDateTime serviceCompletedAt;

    public InvoiceResponse() {
    }

    public static InvoiceResponse fromEntity(Invoice inv) {
        if (inv == null) return null;
        InvoiceResponse res = new InvoiceResponse();
        res.setInvoiceId(inv.getInvoiceId());
        res.setInvoiceNumber(inv.getInvoiceNumber());
        res.setServiceRecordId(inv.getServiceRecordId());
        res.setBookingId(inv.getBookingId());
        res.setCustomerId(inv.getCustomerId());
        res.setCustomerName(inv.getCustomerName());
        res.setCustomerEmail(inv.getCustomerEmail());
        res.setVehicleId(inv.getVehicleId());
        res.setVehicleRegistrationNumber(inv.getVehicleRegistrationNumber());
        res.setVehicleMake(inv.getVehicleMake());
        res.setVehicleModel(inv.getVehicleModel());
        res.setServiceType(inv.getServiceType());
        res.setTechnicianName(inv.getTechnicianName());
        res.setServiceDescription(inv.getServiceDescription());
        res.setRemarks(inv.getRemarks());
        res.setLaborCharges(inv.getLaborCharges());
        res.setPartsCharges(inv.getPartsCharges());
        res.setAdditionalCharges(inv.getAdditionalCharges() != null ? inv.getAdditionalCharges() : BigDecimal.ZERO);
        res.setSubtotal(inv.getSubtotal());
        res.setTaxPercentage(inv.getTaxPercentage());
        res.setTaxAmount(inv.getTaxAmount());
        res.setTotalAmount(inv.getTotalAmount());
        res.setPaymentStatus(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "ISSUED");
        res.setGeneratedAt(inv.getGeneratedAt());
        res.setServiceCompletedAt(inv.getServiceCompletedAt());
        return res;
    }

    // Getters and Setters
    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Long getServiceRecordId() {
        return serviceRecordId;
    }

    public void setServiceRecordId(Long serviceRecordId) {
        this.serviceRecordId = serviceRecordId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public void setVehicleRegistrationNumber(String vehicleRegistrationNumber) {
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public BigDecimal getLaborCharges() {
        return laborCharges;
    }

    public void setLaborCharges(BigDecimal laborCharges) {
        this.laborCharges = laborCharges;
    }

    public BigDecimal getPartsCharges() {
        return partsCharges;
    }

    public void setPartsCharges(BigDecimal partsCharges) {
        this.partsCharges = partsCharges;
    }

    public BigDecimal getAdditionalCharges() {
        return additionalCharges;
    }

    public void setAdditionalCharges(BigDecimal additionalCharges) {
        this.additionalCharges = additionalCharges;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }

    public void setTaxPercentage(BigDecimal taxPercentage) {
        this.taxPercentage = taxPercentage;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getServiceCompletedAt() {
        return serviceCompletedAt;
    }

    public void setServiceCompletedAt(LocalDateTime serviceCompletedAt) {
        this.serviceCompletedAt = serviceCompletedAt;
    }
}
