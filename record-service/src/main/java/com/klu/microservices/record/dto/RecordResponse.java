package com.klu.microservices.record.dto;

import com.klu.microservices.record.entity.ServiceRecord;
import java.time.LocalDateTime;

public class RecordResponse {
    private Long recordId;
    private Long bookingId;
    private Long vehicleId;
    private String serviceType;
    private String description;
    private String mechanic;
    private Long technicianId;
    private String technicianName;
    private String customerEmail;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;

    public RecordResponse() {
    }

    public static RecordResponse fromEntity(ServiceRecord r) {
        RecordResponse res = new RecordResponse();
        res.setRecordId(r.getRecordId());
        res.setBookingId(r.getBookingId());
        res.setVehicleId(r.getVehicleId());
        res.setServiceType(r.getServiceType());
        res.setDescription(r.getDescription());
        res.setMechanic(r.getMechanic());
        res.setTechnicianId(r.getTechnicianId());
        res.setTechnicianName(r.getTechnicianName());
        res.setCustomerEmail(r.getCustomerEmail());
        res.setStartTime(r.getStartTime());
        res.setEndTime(r.getEndTime());
        res.setStatus(r.getStatus() != null ? r.getStatus().name() : null);
        res.setRemarks(r.getRemarks());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMechanic() {
        return mechanic;
    }

    public void setMechanic(String mechanic) {
        this.mechanic = mechanic;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
