package com.klu.microservices.book.service;

import com.klu.microservices.book.dto.BookingRequest;
import com.klu.microservices.book.dto.BookingResponse;
import com.klu.microservices.book.entity.Booking;
import com.klu.microservices.book.entity.Slot;
import com.klu.microservices.book.entity.Vehicle;
import com.klu.microservices.book.entity.enums.BayStatus;
import com.klu.microservices.book.entity.enums.BookingStatus;
import com.klu.microservices.book.entity.enums.SlotStatus;
import com.klu.microservices.book.repository.BookingRepository;
import com.klu.microservices.book.repository.SlotRepository;
import com.klu.microservices.book.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Value("${record.service.url:http://localhost:2006/records}")
    private String recordServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final List<String> SUPPORTED_SERVICES = List.of(
            "General Maintenance",
            "Oil & Fluid Service",
            "Brake Services",
            "Electrical Diagnostics",
            "A/C & Heating",
            "Engine Repair"
    );

    @Transactional
    public synchronized BookingResponse createBooking(BookingRequest request, String userEmail, String userName) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new SecurityException("Unauthorized: User email required");
        }

        // 1. Validate Vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + request.getVehicleId()));

        if (vehicle.getCustomer() == null || !userEmail.equalsIgnoreCase(vehicle.getCustomer().getEmail())) {
            throw new SecurityException("Forbidden: You do not own this vehicle");
        }

        // 2. Validate Slot
        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found with ID: " + request.getSlotId()));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Selected slot is no longer available. Please choose another time or bay.");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        if (slot.getSlotDate().isBefore(today)) {
            throw new IllegalArgumentException("Cannot book appointments for past dates.");
        }
        if (slot.getSlotDate().isEqual(today) && slot.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Cannot book appointments for past time slots.");
        }

        if (slot.getServiceBay() != null && slot.getServiceBay().getStatus() != BayStatus.AVAILABLE) {
            throw new IllegalStateException("Service bay is temporarily unavailable due to maintenance.");
        }

        if (bookingRepository.existsBySlot_SlotId(slot.getSlotId())) {
            throw new IllegalStateException("Slot is already associated with an existing booking.");
        }

        // 3. Validate Service Type
        String validatedServiceType = matchSupportedService(request.getServiceType());

        // 4. Mark Slot as BOOKED
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        // 5. Create & Save Booking
        Booking booking = new Booking();
        booking.setCustomer(vehicle.getCustomer());
        booking.setVehicle(vehicle);
        booking.setSlot(slot);
        booking.setServiceType(validatedServiceType);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setNotes(request.getNotes() != null ? request.getNotes().trim() : "");

        Booking savedBooking = bookingRepository.save(booking);

        // 6. Asynchronously or cleanly initiate Service Record in record-service
        notifyRecordService(savedBooking, vehicle, userEmail);

        return BookingResponse.fromEntity(savedBooking);
    }

    public List<BookingResponse> getMyBookings(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new SecurityException("Unauthorized: User email required");
        }
        return bookingRepository.findByCustomer_Email(userEmail)
                .stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    public BookingResponse getBookingById(Long id, String userEmail, String userRole) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));

        boolean isAdminOrTech = "ADMIN".equalsIgnoreCase(userRole) || "TECHNICIAN".equalsIgnoreCase(userRole);
        boolean isOwner = booking.getCustomer() != null && userEmail != null && userEmail.equalsIgnoreCase(booking.getCustomer().getEmail());

        if (!isAdminOrTech && !isOwner) {
            throw new SecurityException("Forbidden: You are not authorized to view this booking");
        }

        return BookingResponse.fromEntity(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, String userEmail, String userRole) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        boolean isOwner = booking.getCustomer() != null && userEmail != null && userEmail.equalsIgnoreCase(booking.getCustomer().getEmail());

        if (!isAdmin && !isOwner) {
            throw new SecurityException("Forbidden: You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel an already completed service booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getSlot() != null) {
            booking.getSlot().setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(booking.getSlot());
        }

        Booking saved = bookingRepository.save(booking);
        return BookingResponse.fromEntity(saved);
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, String statusStr) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));

        BookingStatus status = BookingStatus.valueOf(statusStr.trim().toUpperCase());
        booking.setStatus(status);

        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            if (booking.getSlot() != null) {
                booking.getSlot().setStatus(SlotStatus.AVAILABLE);
                slotRepository.save(booking.getSlot());
            }
        }

        Booking saved = bookingRepository.save(booking);
        return BookingResponse.fromEntity(saved);
    }

    private String matchSupportedService(String input) {
        if (input == null || input.isBlank()) {
            return SUPPORTED_SERVICES.get(0);
        }
        for (String service : SUPPORTED_SERVICES) {
            if (service.equalsIgnoreCase(input.trim())) {
                return service;
            }
        }
        // If not exact match, check fuzzy contains or return input
        for (String service : SUPPORTED_SERVICES) {
            if (service.toLowerCase().contains(input.toLowerCase()) || input.toLowerCase().contains(service.toLowerCase())) {
                return service;
            }
        }
        return input.trim();
    }

    private void notifyRecordService(Booking booking, Vehicle vehicle, String userEmail) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("bookingId", booking.getBookingId());
            payload.put("vehicleId", vehicle.getVehicleId());
            payload.put("serviceType", booking.getServiceType());
            payload.put("description", "Scheduled " + booking.getServiceType() + " for " + vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getRegistrationNumber() + ")");
            payload.put("customerEmail", userEmail);
            payload.put("status", "PENDING");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(recordServiceUrl, entity, String.class);
            logger.info("Service record created downstream for booking ID: {}", booking.getBookingId());
        } catch (Exception e) {
            logger.warn("Downstream record-service creation skipped or failed: {}", e.getMessage());
        }
    }
}
