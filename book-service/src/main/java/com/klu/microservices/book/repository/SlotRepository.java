package com.klu.microservices.book.repository;

import com.klu.microservices.book.entity.Slot;
import com.klu.microservices.book.entity.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findBySlotDate(LocalDate date);
    List<Slot> findBySlotDateOrderByStartTimeAsc(LocalDate date);
    List<Slot> findByServiceBay_BayIdAndSlotDate(Long bayId, LocalDate date);
    Optional<Slot> findByServiceBay_BayIdAndSlotDateAndStartTime(Long bayId, LocalDate date, LocalTime startTime);
    List<Slot> findBySlotDateAndStatus(LocalDate date, SlotStatus status);
    boolean existsBySlotDate(LocalDate date);
}
