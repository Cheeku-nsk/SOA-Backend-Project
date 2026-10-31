package com.klu.microservices.book.service;

import com.klu.microservices.book.dto.BayResponse;
import com.klu.microservices.book.dto.SlotResponse;
import com.klu.microservices.book.entity.ServiceBay;
import com.klu.microservices.book.entity.Slot;
import com.klu.microservices.book.entity.enums.BayStatus;
import com.klu.microservices.book.entity.enums.SlotStatus;
import com.klu.microservices.book.repository.ServiceBayRepository;
import com.klu.microservices.book.repository.SlotRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {

    @Autowired
    private ServiceBayRepository serviceBayRepository;

    @Autowired
    private SlotRepository slotRepository;

    @PostConstruct
    public void initBays() {
        if (serviceBayRepository.count() == 0) {
            List<ServiceBay> bays = List.of(
                    createBay("Bay 1", "Express Maintenance", BayStatus.AVAILABLE),
                    createBay("Bay 2", "Diagnostics & Electrical", BayStatus.AVAILABLE),
                    createBay("Bay 3", "Engine & Heavy Overhaul", BayStatus.AVAILABLE),
                    createBay("Bay 4", "Brake & Hydraulic Alignment", BayStatus.AVAILABLE)
            );
            serviceBayRepository.saveAll(bays);
        }
    }

    private ServiceBay createBay(String number, String type, BayStatus status) {
        ServiceBay bay = new ServiceBay();
        bay.setBayNumber(number);
        bay.setBayType(type);
        bay.setStatus(status);
        return bay;
    }

    public List<BayResponse> getAllBays() {
        return serviceBayRepository.findAll()
                .stream()
                .map(BayResponse::fromEntity)
                .toList();
    }

    @Transactional
    public List<SlotResponse> getSlotsByDate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        // Auto-generate slots for this date if not already generated
        if (!slotRepository.existsBySlotDate(date)) {
            generateSlotsForDate(date);
        }

        return slotRepository.findBySlotDateOrderByStartTimeAsc(date)
                .stream()
                .map(SlotResponse::fromEntity)
                .toList();
    }

    private void generateSlotsForDate(LocalDate date) {
        List<ServiceBay> activeBays = serviceBayRepository.findByStatus(BayStatus.AVAILABLE);
        if (activeBays.isEmpty()) {
            activeBays = serviceBayRepository.findAll();
        }

        List<LocalTime[]> intervals = List.of(
                new LocalTime[]{LocalTime.of(9, 0), LocalTime.of(10, 30)},
                new LocalTime[]{LocalTime.of(10, 30), LocalTime.of(12, 0)},
                new LocalTime[]{LocalTime.of(13, 0), LocalTime.of(14, 30)},
                new LocalTime[]{LocalTime.of(14, 30), LocalTime.of(16, 0)},
                new LocalTime[]{LocalTime.of(16, 0), LocalTime.of(17, 30)}
        );

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Slot> slotsToSave = new ArrayList<>();
        for (ServiceBay bay : activeBays) {
            for (LocalTime[] interval : intervals) {
                Slot slot = new Slot();
                slot.setServiceBay(bay);
                slot.setSlotDate(date);
                slot.setStartTime(interval[0]);
                slot.setEndTime(interval[1]);

                if (date.isBefore(today)) {
                    slot.setStatus(SlotStatus.BLOCKED);
                } else if (date.isEqual(today) && interval[0].isBefore(now)) {
                    slot.setStatus(SlotStatus.BLOCKED);
                } else {
                    slot.setStatus(SlotStatus.AVAILABLE);
                }
                slotsToSave.add(slot);
            }
        }
        slotRepository.saveAll(slotsToSave);
    }
}
