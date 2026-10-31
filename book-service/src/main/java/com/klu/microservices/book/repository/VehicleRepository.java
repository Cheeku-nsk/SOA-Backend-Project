package com.klu.microservices.book.repository;

import com.klu.microservices.book.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCustomer_CustomerId(Long customerId);
    List<Vehicle> findByCustomer_Email(String email);
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    boolean existsByRegistrationNumber(String registrationNumber);
    boolean existsByRegistrationNumberAndVehicleIdNot(String registrationNumber, Long vehicleId);
}
