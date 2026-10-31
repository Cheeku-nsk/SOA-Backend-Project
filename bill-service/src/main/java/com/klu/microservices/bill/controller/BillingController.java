package com.klu.microservices.bill.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klu.microservices.bill.dto.BillingSummaryResponse;
import com.klu.microservices.bill.dto.GenerateInvoiceRequest;
import com.klu.microservices.bill.dto.InvoiceResponse;
import com.klu.microservices.bill.service.BillingService;
import com.klu.microservices.bill.service.InvoicePdfService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/billing")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private InvoicePdfService pdfService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public String getBilling() {
        return "Welcome to Billing Service – Certified Auto Maintenance Invoicing.";
    }

    /**
     * Helper to resolve user email from forwarded header or Authorization Bearer token.
     */
    private String resolveUserEmail(String userEmail, String authHeader) {
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
                    JsonNode node = objectMapper.readTree(payloadBytes);
                    if (node.has("sub") && !node.get("sub").asText().isBlank()) {
                        return node.get("sub").asText().trim();
                    }
                    if (node.has("email") && !node.get("email").asText().isBlank()) {
                        return node.get("email").asText().trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Helper to resolve user role from forwarded header or Authorization Bearer token.
     */
    private String resolveUserRole(String userRole, String authHeader) {
        if (userRole != null && !userRole.isBlank()) {
            return userRole.trim().toUpperCase();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
                    JsonNode node = objectMapper.readTree(payloadBytes);
                    if (node.has("role") && !node.get("role").asText().isBlank()) {
                        return node.get("role").asText().trim().toUpperCase();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Internal endpoint: Triggered by Record Service upon verified service completion.
     * Customers and technicians are strictly forbidden from calling this directly.
     */
    @PostMapping("/internal/invoices/generate")
    public ResponseEntity<?> generateInvoice(
            @Valid @RequestBody GenerateInvoiceRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String effectiveRole = resolveUserRole(userRole, authHeader);
        // Block untrusted direct invocations by customer or technician
        if ("CUSTOMER".equalsIgnoreCase(effectiveRole) || "TECHNICIAN".equalsIgnoreCase(effectiveRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Direct invoice generation is restricted to internal service automation"));
        }

        try {
            InvoiceResponse response = billingService.generateInvoice(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Invoice Generation Failed", "message", e.getMessage()));
        }
    }

    /**
     * Customer endpoint: Returns invoices belonging only to the authenticated customer.
     */
    @GetMapping("/my-invoices")
    public ResponseEntity<?> getMyInvoices(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String effectiveEmail = resolveUserEmail(userEmail, authHeader);
        if (effectiveEmail == null || effectiveEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Missing authenticated customer credentials"));
        }
        try {
            List<InvoiceResponse> invoices = billingService.getMyInvoices(effectiveEmail);
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve invoices", "message", e.getMessage()));
        }
    }

    /**
     * Customer & Admin endpoint: Retrieves a specific invoice.
     * Enforces ownership for customers; allows full access for admins.
     */
    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<?> getInvoiceById(
            @PathVariable Long invoiceId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String effectiveEmail = resolveUserEmail(userEmail, authHeader);
        String effectiveRole = resolveUserRole(userRole, authHeader);
        try {
            InvoiceResponse invoice = billingService.getInvoiceById(invoiceId, effectiveEmail, effectiveRole);
            return ResponseEntity.ok(invoice);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch invoice", "message", e.getMessage()));
        }
    }

    /**
     * PDF generation endpoint: Generates a downloadable / printable PDF for an invoice.
     */
    @GetMapping("/invoices/{invoiceId}/pdf")
    public ResponseEntity<?> downloadInvoicePdf(
            @PathVariable Long invoiceId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String effectiveEmail = resolveUserEmail(userEmail, authHeader);
        String effectiveRole = resolveUserRole(userRole, authHeader);
        try {
            InvoiceResponse invoice = billingService.getInvoiceById(invoiceId, effectiveEmail, effectiveRole);
            byte[] pdfBytes = pdfService.generatePdf(invoice);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "Invoice-" + invoice.getInvoiceNumber() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate PDF", "message", e.getMessage()));
        }
    }

    /**
     * Admin endpoint: Lists all invoices in the system.
     */
    @GetMapping("/admin/invoices")
    public ResponseEntity<?> getAllInvoices(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String effectiveRole = resolveUserRole(userRole, authHeader);
        if (!"ADMIN".equalsIgnoreCase(effectiveRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Only administrators can view all invoices"));
        }
        try {
            List<InvoiceResponse> invoices = billingService.getAllInvoices(effectiveRole);
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch invoices", "message", e.getMessage()));
        }
    }

    /**
     * Admin endpoint: Returns summary metrics aggregated from real database records.
     */
    @GetMapping("/admin/summary")
    public ResponseEntity<?> getAdminSummary(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String effectiveRole = resolveUserRole(userRole, authHeader);
        if (!"ADMIN".equalsIgnoreCase(effectiveRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Only administrators can view billing summary"));
        }
        try {
            BillingSummaryResponse summary = billingService.getAdminSummary(effectiveRole);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate billing summary", "message", e.getMessage()));
        }
    }

    /**
     * Admin endpoint: Retrieves detailed invoice view for administrative oversight.
     */
    @GetMapping("/admin/invoices/{invoiceId}")
    public ResponseEntity<?> getAdminInvoiceById(
            @PathVariable Long invoiceId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String effectiveRole = resolveUserRole(userRole, authHeader);
        if (!"ADMIN".equalsIgnoreCase(effectiveRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Only administrators can view invoice details"));
        }
        try {
            InvoiceResponse invoice = billingService.getInvoiceById(invoiceId, null, effectiveRole);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch invoice", "message", e.getMessage()));
        }
    }
}
