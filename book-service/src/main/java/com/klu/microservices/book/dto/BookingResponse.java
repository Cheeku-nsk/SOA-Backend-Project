package com.klu.microservices.book.dto;

import com.klu.microservices.book.entity.Booking;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingResponse {
    private Long bookingId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long vehicleId;
    private String vehicleRegistration;
    private String vehicleMakeModel;
    private Long slotId;
    private String bayNumber;
    private String bayType;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String serviceType;
    private String status;
    private String notes;
    private LocalDateTime createdAt;

    public BookingResponse() {
    }

    public static BookingResponse fromEntity(Booking b) {
        BookingResponse res = new BookingResponse();
        res.setBookingId(b.getBookingId());
        if (b.getCustomer() != null) {
            res.setCustomerId(b.getCustomer().getCustomerId());
            res.setCustomerName(b.getCustomer().getName());
            res.setCustomerEmail(b.getCustomer().getEmail());
        }
        if (b.getVehicle() != null) {
            res.setVehicleId(b.getVehicle().getVehicleId());
            res.setVehicleRegistration(b.getVehicle().getRegistrationNumber());
            res.setVehicleMakeModel(b.getVehicle().getMake() + " " + b.getVehicle().getModel());
        }
        if (b.getSlot() != null) {
            res.setSlotId(b.getSlot().getSlotId());
            res.setSlotDate(b.getSlot().getSlotDate());
            res.setStartTime(b.getSlot().getStartTime());
            res.setEndTime(b.getSlot().getEndTime());
            if (b.getSlot().getServiceBay() != null) {
                res.setBayNumber(b.getSlot().getServiceBay().getBayNumber());
                res.setBayType(b.getSlot().getServiceBay().getBayType());
            }
        }
        res.setServiceType(b.getServiceType());
        res.setStatus(b.getStatus() != null ? b.getStatus().name() : null);
        res.setNotes(b.getNotes());
        res.setCreatedAt(b.getCreatedAt());
        return res;
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

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public void setVehicleRegistration(String vehicleRegistration) {
        this.vehicleRegistration = vehicleRegistration;
    }

    public String getVehicleMakeModel() {
        return vehicleMakeModel;
    }

    public void setVehicleMakeModel(String vehicleMakeModel) {
        this.vehicleMakeModel = vehicleMakeModel;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getBayNumber() {
        return bayNumber;
    }

    public void setBayNumber(String bayNumber) {
        this.bayNumber = bayNumber;
    }

    public String getBayType() {
        return bayType;
    }

    public void setBayType(String bayType) {
        this.bayType = bayType;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
