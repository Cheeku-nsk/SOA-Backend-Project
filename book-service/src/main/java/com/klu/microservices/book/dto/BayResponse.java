package com.klu.microservices.book.dto;

import com.klu.microservices.book.entity.ServiceBay;

public class BayResponse {
    private Long bayId;
    private String bayNumber;
    private String bayType;
    private String status;

    public BayResponse() {
    }

    public static BayResponse fromEntity(ServiceBay bay) {
        BayResponse res = new BayResponse();
        res.setBayId(bay.getBayId());
        res.setBayNumber(bay.getBayNumber());
        res.setBayType(bay.getBayType());
        res.setStatus(bay.getStatus() != null ? bay.getStatus().name() : null);
        return res;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
