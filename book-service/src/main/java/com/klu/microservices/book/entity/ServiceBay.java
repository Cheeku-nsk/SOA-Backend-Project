package com.klu.microservices.book.entity;

import com.klu.microservices.book.entity.enums.BayStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "service_bay")
public class ServiceBay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bay_id")
    private Long bayId;

    @Column(name = "bay_number", nullable = false, unique = true)
    private String bayNumber;

    @Column(name = "bay_type")
    private String bayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BayStatus status;

    // Getters and Setters
    public Long getBayId() {
        return bayId;
    }

    public void setBayId(Long bayId) {
        this.bayId = bayId;
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

    public BayStatus getStatus() {
        return status;
    }

    public void setStatus(BayStatus status) {
        this.status = status;
    }
}
