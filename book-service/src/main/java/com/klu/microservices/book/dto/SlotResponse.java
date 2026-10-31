package com.klu.microservices.book.dto;

import com.klu.microservices.book.entity.Slot;
import java.time.LocalDate;
import java.time.LocalTime;

public class SlotResponse {
    private Long slotId;
    private Long bayId;
    private String bayNumber;
    private String bayType;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;

    public SlotResponse() {
    }

    public static SlotResponse fromEntity(Slot s) {
        SlotResponse res = new SlotResponse();
        res.setSlotId(s.getSlotId());
        if (s.getServiceBay() != null) {
            res.setBayId(s.getServiceBay().getBayId());
            res.setBayNumber(s.getServiceBay().getBayNumber());
            res.setBayType(s.getServiceBay().getBayType());
        }
        res.setSlotDate(s.getSlotDate());
        res.setStartTime(s.getStartTime());
        res.setEndTime(s.getEndTime());
        res.setStatus(s.getStatus() != null ? s.getStatus().name() : null);
        return res;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
