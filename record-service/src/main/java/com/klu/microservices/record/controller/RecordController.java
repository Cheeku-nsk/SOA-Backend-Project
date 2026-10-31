package com.klu.microservices.record.controller;

import com.klu.microservices.record.dto.CreateRecordRequest;
import com.klu.microservices.record.dto.RecordResponse;
import com.klu.microservices.record.dto.UpdateRecordStatusRequest;
import com.klu.microservices.record.service.ServiceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/records")
public class RecordController {

    @Autowired
    private ServiceRecordService recordService;

    @PostMapping
    public ResponseEntity<?> createRecord(@RequestBody CreateRecordRequest request) {
        try {
            RecordResponse response = recordService.createRecord(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Record Creation Failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/customer")
    public ResponseEntity<?> getCustomerRecords(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Missing user credentials"));
        }
        try {
            List<RecordResponse> records = recordService.getRecordsForCustomer(userEmail);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve customer records", "message", e.getMessage()));
        }
    }

    @GetMapping("/technician")
    public ResponseEntity<?> getTechnicianRecords(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        boolean isTechOrAdmin = "TECHNICIAN".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole);
        if (!isTechOrAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Only technicians and admins can access this queue"));
        }
        try {
            List<RecordResponse> records = recordService.getRecordsForTechnician(userEmail, userId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve technician records", "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllRecords(
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Only administrators can view all records"));
        }
        try {
            List<RecordResponse> records = recordService.getAllRecords();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve all records", "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRecordById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        try {
            RecordResponse record = recordService.getRecordById(id, userEmail, userRole);
            return ResponseEntity.ok(record);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateRecordStatusRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        try {
            RecordResponse response = recordService.updateStatus(id, request, userEmail, userId, userRole);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid Status Transition", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Update Failed", "message", e.getMessage()));
        }
    }

    /**
     * Reconciliation endpoint for retrying invoice generation on a completed record
     */
    @PostMapping("/{id}/reconcile-invoice")
    public ResponseEntity<?> reconcileInvoice(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Only administrators can trigger invoice reconciliation"));
        }
        try {
            boolean success = recordService.reconcileInvoice(id);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Invoice generation triggered successfully for record #" + id));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Service Unavailable", "message", "Failed to communicate with billing service"));
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Reconciliation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Reconciliation Failed", "message", e.getMessage()));
        }
    }
}
