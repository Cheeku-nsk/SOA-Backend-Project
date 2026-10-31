package com.klu.microservices.bill.service;

import com.klu.microservices.bill.dto.BillingSummaryResponse;
import com.klu.microservices.bill.dto.GenerateInvoiceRequest;
import com.klu.microservices.bill.dto.InvoiceResponse;
import com.klu.microservices.bill.entity.Invoice;
import com.klu.microservices.bill.entity.enums.PaymentStatus;
import com.klu.microservices.bill.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class BillingService {

    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoicePricingService pricingService;

    @Value("${booking.service.url:http://localhost:2005/bookings}")
    private String bookingServiceUrl;

    @Value("${record.service.url:http://localhost:2006/records}")
    private String recordServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public InvoiceResponse generateInvoice(GenerateInvoiceRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Invoice generation request cannot be null");
        }
        if (req.getServiceRecordId() == null) {
            throw new IllegalArgumentException("Service record ID is required for invoice generation");
        }
        if (req.getBookingId() == null) {
            throw new IllegalArgumentException("Booking ID is required for invoice generation");
        }

        // 1. Idempotency Check: Return existing invoice if already generated
        Optional<Invoice> existing = invoiceRepository.findByServiceRecordId(req.getServiceRecordId());
        if (existing.isPresent()) {
            logger.info("Idempotent check hit: Invoice already exists for service record ID {}", req.getServiceRecordId());
            return InvoiceResponse.fromEntity(existing.get());
        }

        // 2. Enrich missing customer/vehicle data from book-service if needed
        enrichFromBookingService(req);

        // 3. Calculate itemized charges and total amount
        InvoicePricingService.PricingCalculation calc = pricingService.calculate(
                req.getServiceType(),
                req.getAdditionalCharges()
        );

        // 4. Generate unique human-readable invoice number
        String invoiceNumber = generateUniqueInvoiceNumber();

        // 5. Construct and populate Invoice entity
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setServiceRecordId(req.getServiceRecordId());
        invoice.setBookingId(req.getBookingId());
        invoice.setCustomerId(req.getCustomerId());
        invoice.setCustomerName(req.getCustomerName() != null ? req.getCustomerName() : "Valued Customer");
        invoice.setCustomerEmail(req.getCustomerEmail());
        invoice.setVehicleId(req.getVehicleId());
        invoice.setVehicleRegistrationNumber(req.getVehicleRegistrationNumber() != null ? req.getVehicleRegistrationNumber() : "UNREGISTERED");
        invoice.setVehicleMake(req.getVehicleMake() != null ? req.getVehicleMake() : "Vehicle");
        invoice.setVehicleModel(req.getVehicleModel() != null ? req.getVehicleModel() : "");
        invoice.setServiceType(req.getServiceType() != null ? req.getServiceType() : "General Maintenance");
        invoice.setTechnicianName(req.getTechnicianName() != null ? req.getTechnicianName() : "PrecisionAuto Service Team");
        invoice.setServiceDescription(req.getServiceDescription());
        invoice.setRemarks(req.getRemarks());

        invoice.setLaborCharges(calc.getLaborCharges());
        invoice.setPartsCharges(calc.getPartsCharges());
        invoice.setAdditionalCharges(calc.getAdditionalCharges());
        invoice.setSubtotal(calc.getSubtotal());
        invoice.setTaxPercentage(calc.getTaxPercentage());
        invoice.setTaxAmount(calc.getTaxAmount());
        invoice.setTotalAmount(calc.getTotalAmount());

        invoice.setPaymentStatus(PaymentStatus.ISSUED);
        invoice.setGeneratedAt(LocalDateTime.now());
        invoice.setServiceCompletedAt(req.getServiceCompletedAt() != null ? req.getServiceCompletedAt() : LocalDateTime.now());
        invoice.setInvoiceDate(invoice.getGeneratedAt());

        try {
            Invoice saved = invoiceRepository.save(invoice);
            logger.info("Invoice {} generated successfully for service record #{}", saved.getInvoiceNumber(), saved.getServiceRecordId());
            return InvoiceResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException dive) {
            logger.warn("Concurrency detected for service record #{}, retrieving saved invoice", req.getServiceRecordId());
            return invoiceRepository.findByServiceRecordId(req.getServiceRecordId())
                    .map(InvoiceResponse::fromEntity)
                    .orElseThrow(() -> new IllegalStateException("Failed to generate or retrieve invoice: " + dive.getMessage()));
        }
    }

    public List<InvoiceResponse> getMyInvoices(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email is required");
        }

        // Automatic safe reconciliation: Check if customer has unbilled completed records
        tryReconcileCustomerRecords(customerEmail.trim());

        return invoiceRepository.findByCustomerEmailIgnoreCaseOrderByGeneratedAtDesc(customerEmail.trim())
                .stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    public InvoiceResponse getInvoiceById(Long invoiceId, String userEmail, String userRole) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID cannot be null");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        boolean isOwner = invoice.getCustomerEmail() != null && userEmail != null
                && invoice.getCustomerEmail().equalsIgnoreCase(userEmail.trim());

        if (!isAdmin && !isOwner) {
            throw new SecurityException("Forbidden: You do not have permission to view this invoice");
        }

        return InvoiceResponse.fromEntity(invoice);
    }

    public List<InvoiceResponse> getAllInvoices(String userRole) {
        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            throw new SecurityException("Forbidden: Administrator access required to view all invoices");
        }

        // Automatic reconciliation for admins
        tryReconcileAllCompletedRecords();

        return invoiceRepository.findAllByOrderByGeneratedAtDesc()
                .stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    public BillingSummaryResponse getAdminSummary(String userRole) {
        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            throw new SecurityException("Forbidden: Administrator access required to view billing metrics");
        }

        tryReconcileAllCompletedRecords();

        long totalCount = invoiceRepository.count();
        BigDecimal totalValue = invoiceRepository.sumTotalAmount();
        BigDecimal avgValue = invoiceRepository.avgTotalAmount();
        List<InvoiceResponse> recent = invoiceRepository.findTop5ByOrderByGeneratedAtDesc()
                .stream()
                .map(InvoiceResponse::fromEntity)
                .toList();

        return new BillingSummaryResponse(totalCount, totalValue, avgValue, recent);
    }

    public void tryReconcileCustomerRecords(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Email", customerEmail.trim());
            headers.set("X-User-Role", "ADMIN");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = recordServiceUrl + "/customer";
            @SuppressWarnings("rawtypes")
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map<?, ?> recordMap) {
                        String status = String.valueOf(recordMap.get("status"));
                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            Long recordId = Long.valueOf(String.valueOf(recordMap.get("recordId")));
                            if (invoiceRepository.findByServiceRecordId(recordId).isEmpty()) {
                                logger.info("Reconciling unbilled completed service record #{} for {}", recordId, customerEmail);
                                GenerateInvoiceRequest req = new GenerateInvoiceRequest();
                                req.setServiceRecordId(recordId);
                                if (recordMap.get("bookingId") != null) {
                                    req.setBookingId(Long.valueOf(String.valueOf(recordMap.get("bookingId"))));
                                }
                                if (recordMap.get("vehicleId") != null) {
                                    req.setVehicleId(Long.valueOf(String.valueOf(recordMap.get("vehicleId"))));
                                }
                                req.setCustomerEmail(customerEmail.trim());
                                req.setServiceType(recordMap.get("serviceType") != null ? String.valueOf(recordMap.get("serviceType")) : "General Maintenance");
                                req.setServiceDescription(recordMap.get("description") != null ? String.valueOf(recordMap.get("description")) : null);
                                req.setTechnicianName(recordMap.get("technicianName") != null ? String.valueOf(recordMap.get("technicianName")) : null);
                                req.setRemarks(recordMap.get("remarks") != null ? String.valueOf(recordMap.get("remarks")) : null);
                                if (recordMap.get("endTime") != null) {
                                    try {
                                        req.setServiceCompletedAt(LocalDateTime.parse(String.valueOf(recordMap.get("endTime"))));
                                    } catch (Exception ignored) {
                                        req.setServiceCompletedAt(LocalDateTime.now());
                                    }
                                }
                                generateInvoice(req);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Customer invoice reconciliation skipped: {}", e.getMessage());
        }
    }

    public void tryReconcileAllCompletedRecords() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = recordServiceUrl;
            @SuppressWarnings("rawtypes")
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map<?, ?> recordMap) {
                        String status = String.valueOf(recordMap.get("status"));
                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            Long recordId = Long.valueOf(String.valueOf(recordMap.get("recordId")));
                            if (invoiceRepository.findByServiceRecordId(recordId).isEmpty()) {
                                logger.info("Reconciling unbilled completed service record #{}", recordId);
                                GenerateInvoiceRequest req = new GenerateInvoiceRequest();
                                req.setServiceRecordId(recordId);
                                if (recordMap.get("bookingId") != null) {
                                    req.setBookingId(Long.valueOf(String.valueOf(recordMap.get("bookingId"))));
                                }
                                if (recordMap.get("vehicleId") != null) {
                                    req.setVehicleId(Long.valueOf(String.valueOf(recordMap.get("vehicleId"))));
                                }
                                if (recordMap.get("customerEmail") != null) {
                                    req.setCustomerEmail(String.valueOf(recordMap.get("customerEmail")));
                                }
                                req.setServiceType(recordMap.get("serviceType") != null ? String.valueOf(recordMap.get("serviceType")) : "General Maintenance");
                                req.setServiceDescription(recordMap.get("description") != null ? String.valueOf(recordMap.get("description")) : null);
                                req.setTechnicianName(recordMap.get("technicianName") != null ? String.valueOf(recordMap.get("technicianName")) : null);
                                req.setRemarks(recordMap.get("remarks") != null ? String.valueOf(recordMap.get("remarks")) : null);
                                if (recordMap.get("endTime") != null) {
                                    try {
                                        req.setServiceCompletedAt(LocalDateTime.parse(String.valueOf(recordMap.get("endTime"))));
                                    } catch (Exception ignored) {
                                        req.setServiceCompletedAt(LocalDateTime.now());
                                    }
                                }
                                generateInvoice(req);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Global invoice reconciliation skipped: {}", e.getMessage());
        }
    }

    private void enrichFromBookingService(GenerateInvoiceRequest req) {
        if (req.getBookingId() == null) return;
        boolean needsCustomer = req.getCustomerEmail() == null || req.getCustomerName() == null;
        boolean needsVehicle = req.getVehicleRegistrationNumber() == null || req.getVehicleMake() == null;

        if (!needsCustomer && !needsVehicle) {
            return;
        }

        try {
            String url = bookingServiceUrl + "/internal/" + req.getBookingId();
            @SuppressWarnings("unchecked")
            Map<String, Object> booking = restTemplate.getForObject(url, Map.class);
            if (booking != null) {
                if (req.getCustomerEmail() == null && booking.get("customerEmail") != null) {
                    req.setCustomerEmail(String.valueOf(booking.get("customerEmail")));
                }
                if (req.getCustomerName() == null && booking.get("customerName") != null) {
                    req.setCustomerName(String.valueOf(booking.get("customerName")));
                }
                if (req.getCustomerId() == null && booking.get("customerId") != null) {
                    req.setCustomerId(Long.valueOf(String.valueOf(booking.get("customerId"))));
                }
                if (req.getVehicleRegistrationNumber() == null && booking.get("vehicleRegistration") != null) {
                    req.setVehicleRegistrationNumber(String.valueOf(booking.get("vehicleRegistration")));
                }
                if (req.getVehicleId() == null && booking.get("vehicleId") != null) {
                    req.setVehicleId(Long.valueOf(String.valueOf(booking.get("vehicleId"))));
                }
                if (req.getServiceType() == null && booking.get("serviceType") != null) {
                    req.setServiceType(String.valueOf(booking.get("serviceType")));
                }
                if (req.getVehicleMake() == null && booking.get("vehicleMakeModel") != null) {
                    String mm = String.valueOf(booking.get("vehicleMakeModel"));
                    String[] parts = mm.split(" ", 2);
                    req.setVehicleMake(parts[0]);
                    if (parts.length > 1) {
                        req.setVehicleModel(parts[1]);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not enrich booking #{} from book-service: {}", req.getBookingId(), e.getMessage());
        }
    }

    private synchronized String generateUniqueInvoiceNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            int seq = 1000 + random.nextInt(9000);
            String candidate = "INV-" + datePrefix + "-" + seq;
            if (invoiceRepository.findByInvoiceNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "INV-" + datePrefix + "-" + System.currentTimeMillis() % 100000;
    }
}
