package com.klu.microservices.record.repository;

import com.klu.microservices.record.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
}
