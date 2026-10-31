package com.klu.microservices.record.service;

import com.klu.microservices.record.dto.CreateRecordRequest;
import com.klu.microservices.record.dto.RecordResponse;
import com.klu.microservices.record.dto.UpdateRecordStatusRequest;
import com.klu.microservices.record.entity.ServiceRecord;
import com.klu.microservices.record.entity.enums.ServiceRecordStatus;
import com.klu.microservices.record.repository.ServiceRecordRepository;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServiceRecordService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRecordService.class);

    @Autowired
    private ServiceRecordRepository recordRepository;

    @Value("${booking.service.url:http://localhost:2005/bookings}")
    private String bookingServiceUrl;

    @Value("${billing.service.url:http://localhost:2007/billing/internal/invoices/generate}")
    private String billingServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public RecordResponse createRecord(CreateRecordRequest req) {
        if (req.getBookingId() == null || req.getVehicleId() == null) {
            throw new IllegalArgumentException("Booking ID and Vehicle ID are required to create a service record");
        }

        // Idempotency: Return existing record if already created for this booking
        Optional<ServiceRecord> existing = recordRepository.findByBookingId(req.getBookingId());
        if (existing.isPresent()) {
            return RecordResponse.fromEntity(existing.get());
        }

        ServiceRecord record = new ServiceRecord();
        record.setBookingId(req.getBookingId());
        record.setVehicleId(req.getVehicleId());
        record.setServiceType(req.getServiceType() != null ? req.getServiceType() : "Standard Service");
        record.setDescription(req.getDescription() != null ? req.getDescription() : "Routine maintenance check");
        record.setCustomerEmail(req.getCustomerEmail());
        record.setStatus(ServiceRecordStatus.PENDING);

        ServiceRecord saved = recordRepository.save(record);
        return RecordResponse.fromEntity(saved);
    }

    public List<RecordResponse> getRecordsForCustomer(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email is required");
        }
        return recordRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(RecordResponse::fromEntity)
                .toList();
    }

    public List<RecordResponse> getRecordsForTechnician(String userEmail, Long userId) {
        // Return records assigned to this technician or open pending jobs
        return recordRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(r -> r.getStatus() == ServiceRecordStatus.PENDING ||
                        (r.getTechnicianId() != null && r.getTechnicianId().equals(userId)) ||
                        (r.getMechanic() != null && r.getMechanic().equalsIgnoreCase(userEmail)))
                .map(RecordResponse::fromEntity)
                .toList();
    }

    public List<RecordResponse> getAllRecords() {
        return recordRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(RecordResponse::fromEntity)
                .toList();
    }

    public RecordResponse getRecordById(Long id, String userEmail, String userRole) {
        ServiceRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service record not found with ID: " + id));

        boolean isAdminOrTech = "ADMIN".equalsIgnoreCase(userRole) || "TECHNICIAN".equalsIgnoreCase(userRole);
        boolean isOwner = record.getCustomerEmail() != null && record.getCustomerEmail().equalsIgnoreCase(userEmail);

        if (!isAdminOrTech && !isOwner) {
            throw new SecurityException("Forbidden: You do not have permission to view this record");
        }

        return RecordResponse.fromEntity(record);
    }

    @Transactional
    public RecordResponse updateStatus(Long recordId, UpdateRecordStatusRequest req, String userEmail, Long userId, String userRole) {
        boolean isTech = "TECHNICIAN".equalsIgnoreCase(userRole);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);

        if (!isTech && !isAdmin) {
            throw new SecurityException("Forbidden: Only technicians or administrators can update service records");
        }

        ServiceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Service record not found with ID: " + recordId));

        if (record.getStatus() == ServiceRecordStatus.COMPLETED) {
            throw new IllegalStateException("Service record is already marked COMPLETED and cannot be modified");
        }
        if (record.getStatus() == ServiceRecordStatus.CANCELLED) {
            throw new IllegalStateException("Service record is CANCELLED and cannot be modified");
        }

        String targetStatusStr = req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "";
        ServiceRecordStatus targetStatus;
        try {
            targetStatus = ServiceRecordStatus.valueOf(targetStatusStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + req.getStatus());
        }

        // Validate lifecycle transitions: PENDING -> IN_PROGRESS -> COMPLETED
        if (record.getStatus() == ServiceRecordStatus.PENDING) {
            if (targetStatus != ServiceRecordStatus.IN_PROGRESS && targetStatus != ServiceRecordStatus.CANCELLED) {
                throw new IllegalStateException("A pending job must be moved to IN_PROGRESS before it can be completed");
            }
            if (targetStatus == ServiceRecordStatus.IN_PROGRESS) {
                if (record.getStartTime() == null) {
                    record.setStartTime(LocalDateTime.now());
                }
                record.setTechnicianId(userId);
                record.setMechanic(userEmail);
                if (req.getTechnicianName() != null && !req.getTechnicianName().isBlank()) {
                    record.setTechnicianName(req.getTechnicianName());
                } else {
                    record.setTechnicianName(userEmail);
                }
            }
        } else if (record.getStatus() == ServiceRecordStatus.IN_PROGRESS) {
            if (targetStatus != ServiceRecordStatus.COMPLETED && targetStatus != ServiceRecordStatus.CANCELLED) {
                throw new IllegalStateException("An in-progress job can only be moved to COMPLETED or CANCELLED");
            }
            if (targetStatus == ServiceRecordStatus.COMPLETED) {
                record.setEndTime(LocalDateTime.now());
                if (req.getRemarks() == null || req.getRemarks().isBlank()) {
                    throw new IllegalArgumentException("Completion remarks/findings are required when completing service");
                }
            }
        }

        record.setStatus(targetStatus);
        if (req.getRemarks() != null && !req.getRemarks().isBlank()) {
            record.setRemarks(req.getRemarks().trim());
        }

        // 1. Save updated record state first
        ServiceRecord saved = recordRepository.save(record);

        // 2. Synchronize booking status to keep appointment state updated
        syncBookingStatus(saved.getBookingId(), targetStatus.name());

        // 3. Automatically trigger invoice generation when marked COMPLETED
        if (targetStatus == ServiceRecordStatus.COMPLETED) {
            triggerInvoiceGeneration(saved);
        }

        return RecordResponse.fromEntity(saved);
    }

    public boolean triggerInvoiceGeneration(ServiceRecord record) {
        if (record == null || record.getStatus() != ServiceRecordStatus.COMPLETED) {
            logger.warn("Skipping invoice generation: Record is null or not in COMPLETED status");
            return false;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("serviceRecordId", record.getRecordId());
            payload.put("bookingId", record.getBookingId());
            payload.put("vehicleId", record.getVehicleId());
            payload.put("serviceType", record.getServiceType());
            payload.put("serviceDescription", record.getDescription());
            payload.put("technicianName", record.getTechnicianName() != null ? record.getTechnicianName() : record.getMechanic());
            payload.put("customerEmail", record.getCustomerEmail());
            payload.put("remarks", record.getRemarks());
            payload.put("serviceCompletedAt", record.getEndTime() != null ? record.getEndTime().toString() : LocalDateTime.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "RECORD-SERVICE");
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(billingServiceUrl, requestEntity, String.class);
            logger.info("Successfully initiated invoice generation for service record #{}", record.getRecordId());
            return true;
        } catch (Exception e) {
            // Fail-safe: Record remains preserved even if Billing Service is temporarily unavailable
            logger.error("Failed downstream invoice generation for service record #{}: {}", record.getRecordId(), e.getMessage());
            return false;
        }
    }

    public boolean reconcileInvoice(Long recordId) {
        ServiceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Service record not found with ID: " + recordId));

        if (record.getStatus() != ServiceRecordStatus.COMPLETED) {
            throw new IllegalStateException("Cannot generate invoice: Service record is not marked COMPLETED");
        }

        return triggerInvoiceGeneration(record);
    }

    private void syncBookingStatus(Long bookingId, String status) {
        if (bookingId == null) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "RECORD-SERVICE");
            Map<String, String> body = Map.of("status", status);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(bookingServiceUrl + "/" + bookingId + "/status", org.springframework.http.HttpMethod.PUT, request, String.class);
            logger.info("Synchronized booking #{} status to {}", bookingId, status);
        } catch (Exception e) {
            logger.warn("Could not synchronize booking #{} status with book-service: {}", bookingId, e.getMessage());
        }
    }
}
