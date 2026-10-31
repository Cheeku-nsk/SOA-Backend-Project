package com.klu.microservices.book.service;

import com.klu.microservices.book.dto.VehicleRequest;
import com.klu.microservices.book.dto.VehicleResponse;
import com.klu.microservices.book.entity.Customer;
import com.klu.microservices.book.entity.Vehicle;
import com.klu.microservices.book.entity.enums.BookingStatus;
import com.klu.microservices.book.repository.BookingRepository;
import com.klu.microservices.book.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private BookingRepository bookingRepository;

    public List<VehicleResponse> getVehiclesForCustomer(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Customer email must be provided");
        }
        return vehicleRepository.findByCustomer_Email(email)
                .stream()
                .map(VehicleResponse::fromEntity)
                .toList();
    }

    @Transactional
    public VehicleResponse addVehicle(VehicleRequest request, String userEmail, String userName) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new SecurityException("Unauthorized: missing authenticated user identity");
        }

        String regNumber = request.getRegistrationNumber().trim().toUpperCase();

        if (vehicleRepository.existsByRegistrationNumber(regNumber)) {
            throw new IllegalArgumentException("A vehicle with registration number '" + regNumber + "' is already registered");
        }

        int currentYear = LocalDate.now().getYear();
        if (request.getYear() < 1900 || request.getYear() > currentYear + 1) {
            throw new IllegalArgumentException("Manufacturing year must be between 1900 and " + (currentYear + 1));
        }

        Customer customer = customerService.getOrCreateCustomer(userEmail, userName);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);
        vehicle.setRegistrationNumber(regNumber);
        vehicle.setMake(request.getMake().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setYear(request.getYear());
        vehicle.setVehicleType(request.getVehicleType() != null ? request.getVehicleType().trim() : "Sedan");
        vehicle.setFuelType(request.getFuelType() != null ? request.getFuelType().trim() : "Petrol");

        Vehicle saved = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(saved);
    }

    @Transactional
    public VehicleResponse updateVehicle(Long vehicleId, VehicleRequest request, String userEmail) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        if (vehicle.getCustomer() == null || !userEmail.equalsIgnoreCase(vehicle.getCustomer().getEmail())) {
            throw new SecurityException("Forbidden: You do not have permission to modify this vehicle");
        }

        String regNumber = request.getRegistrationNumber().trim().toUpperCase();
        if (vehicleRepository.existsByRegistrationNumberAndVehicleIdNot(regNumber, vehicleId)) {
            throw new IllegalArgumentException("Registration number '" + regNumber + "' is already in use by another vehicle");
        }

        int currentYear = LocalDate.now().getYear();
        if (request.getYear() < 1900 || request.getYear() > currentYear + 1) {
            throw new IllegalArgumentException("Manufacturing year must be between 1900 and " + (currentYear + 1));
        }

        vehicle.setRegistrationNumber(regNumber);
        vehicle.setMake(request.getMake().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setYear(request.getYear());
        if (request.getVehicleType() != null) {
            vehicle.setVehicleType(request.getVehicleType().trim());
        }
        if (request.getFuelType() != null) {
            vehicle.setFuelType(request.getFuelType().trim());
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteVehicle(Long vehicleId, String userEmail) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        if (vehicle.getCustomer() == null || !userEmail.equalsIgnoreCase(vehicle.getCustomer().getEmail())) {
            throw new SecurityException("Forbidden: You do not have permission to delete this vehicle");
        }

        boolean hasActiveBookings = bookingRepository.existsByVehicle_VehicleIdAndStatusIn(
                vehicleId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS)
        );

        if (hasActiveBookings) {
            throw new IllegalStateException("Cannot delete vehicle because it has active or upcoming bookings");
        }

        vehicleRepository.delete(vehicle);
    }
}
