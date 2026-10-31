package com.klu.microservices.record.repository;

import com.klu.microservices.record.entity.ServiceRecord;
import com.klu.microservices.record.entity.enums.ServiceRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    List<ServiceRecord> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
    List<ServiceRecord> findByMechanicOrderByCreatedAtDesc(String mechanic);
    List<ServiceRecord> findByStatusOrderByCreatedAtDesc(ServiceRecordStatus status);
    List<ServiceRecord> findAllByOrderByCreatedAtDesc();
    Optional<ServiceRecord> findByBookingId(Long bookingId);
}
