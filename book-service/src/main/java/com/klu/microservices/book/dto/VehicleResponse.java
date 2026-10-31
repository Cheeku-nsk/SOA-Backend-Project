package com.klu.microservices.book.dto;

import com.klu.microservices.book.entity.Vehicle;
import java.time.LocalDateTime;

public class VehicleResponse {
    private Long vehicleId;
    private Long customerId;
    private String customerEmail;
    private String registrationNumber;
    private String make;
    private String model;
    private Integer year;
    private String vehicleType;
    private String fuelType;
    private LocalDateTime createdAt;

    public VehicleResponse() {
    }

    public static VehicleResponse fromEntity(Vehicle v) {
        VehicleResponse res = new VehicleResponse();
        res.setVehicleId(v.getVehicleId());
        if (v.getCustomer() != null) {
            res.setCustomerId(v.getCustomer().getCustomerId());
            res.setCustomerEmail(v.getCustomer().getEmail());
        }
        res.setRegistrationNumber(v.getRegistrationNumber());
        res.setMake(v.getMake());
        res.setModel(v.getModel());
        res.setYear(v.getYear());
        res.setVehicleType(v.getVehicleType());
        res.setFuelType(v.getFuelType());
        res.setCreatedAt(v.getCreatedAt());
        return res;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
