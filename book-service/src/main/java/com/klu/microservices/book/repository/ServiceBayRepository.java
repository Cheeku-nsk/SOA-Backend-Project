package com.klu.microservices.book.repository;

import com.klu.microservices.book.entity.ServiceBay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceBayRepository extends JpaRepository<ServiceBay, Long> {
}
