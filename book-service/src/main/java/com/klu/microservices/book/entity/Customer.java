package com.klu.microservices.book.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // mappedBy specifies the field in the Vehicle entity
    // We can just define the relationship or leave it out if we want unidirectional.
    // The prompt says "Customer -> Vehicle", let's make it bidirectional if needed, or unidirectional.
    // Given the prompt: "Customer 1 ----- N Vehicle", "Customer 1 ----- N Booking", we can add these if we want, but it's not strictly required unless we need it for business logic. Usually it's better to just have the ManyToOne on the other side.
    // I will stick to simple properties and let Vehicle/Booking own the relationship, which is standard.

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
