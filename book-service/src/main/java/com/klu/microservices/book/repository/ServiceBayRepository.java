package com.klu.microservices.book.repository;

import com.klu.microservices.book.entity.ServiceBay;
import com.klu.microservices.book.entity.enums.BayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceBayRepository extends JpaRepository<ServiceBay, Long> {
    List<ServiceBay> findByStatus(BayStatus status);
    Optional<ServiceBay> findByBayNumber(String bayNumber);
    boolean existsByBayNumber(String bayNumber);
}
