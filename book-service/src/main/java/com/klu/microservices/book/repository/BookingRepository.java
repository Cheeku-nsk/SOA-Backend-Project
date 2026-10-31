package com.klu.microservices.book.repository;

import com.klu.microservices.book.entity.Booking;
import com.klu.microservices.book.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomer_CustomerId(Long customerId);
    List<Booking> findByCustomer_Email(String email);
    List<Booking> findByVehicle_VehicleId(Long vehicleId);
    boolean existsByVehicle_VehicleIdAndStatusIn(Long vehicleId, List<BookingStatus> statuses);
    boolean existsBySlot_SlotId(Long slotId);
    List<Booking> findAllByOrderByCreatedAtDesc();
}
