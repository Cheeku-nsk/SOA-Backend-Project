package com.klu.microservices.bill.service;

import com.klu.microservices.bill.dto.BillingSummaryResponse;
import com.klu.microservices.bill.dto.GenerateInvoiceRequest;
import com.klu.microservices.bill.dto.InvoiceResponse;
import com.klu.microservices.bill.entity.Invoice;
import com.klu.microservices.bill.repository.FakeInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillingServiceTest {

    private FakeInvoiceRepository invoiceRepository;
    private InvoicePricingService pricingService;
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        invoiceRepository = new FakeInvoiceRepository();
        pricingService = new InvoicePricingService();
        ReflectionTestUtils.setField(pricingService, "taxPercentage", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(pricingService, "defaultLabor", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(pricingService, "defaultParts", new BigDecimal("50.00"));

        billingService = new BillingService();
        ReflectionTestUtils.setField(billingService, "invoiceRepository", invoiceRepository);
        ReflectionTestUtils.setField(billingService, "pricingService", pricingService);
        ReflectionTestUtils.setField(billingService, "bookingServiceUrl", "http://localhost:2005/bookings");
    }

    @Test
    @DisplayName("Test 3: Automatic Invoice Generation - creates exactly one invoice with valid calculations")
    void testGenerateInvoiceSuccessfully() {
        GenerateInvoiceRequest req = new GenerateInvoiceRequest();
        req.setServiceRecordId(101L);
        req.setBookingId(202L);
        req.setCustomerId(5L);
        req.setCustomerName("John Doe");
        req.setCustomerEmail("john@example.com");
        req.setVehicleId(12L);
        req.setVehicleRegistrationNumber("KA-01-AB-1234");
        req.setVehicleMake("Toyota");
        req.setVehicleModel("Camry");
        req.setServiceType("General Maintenance");
        req.setTechnicianName("Tech Mike");
        req.setRemarks("All checks passed");
        req.setServiceCompletedAt(LocalDateTime.now());

        InvoiceResponse response = billingService.generateInvoice(req);

        assertNotNull(response);
        assertNotNull(response.getInvoiceId());
        assertTrue(response.getInvoiceNumber().startsWith("INV-"));
        assertEquals(101L, response.getServiceRecordId());
        assertEquals(202L, response.getBookingId());
        assertEquals("john@example.com", response.getCustomerEmail());
        assertEquals("KA-01-AB-1234", response.getVehicleRegistrationNumber());
        assertEquals(new BigDecimal("75.00"), response.getLaborCharges());
        assertEquals(new BigDecimal("45.00"), response.getPartsCharges());
        assertEquals(new BigDecimal("120.00"), response.getSubtotal());
        assertEquals(new BigDecimal("12.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("132.00"), response.getTotalAmount());

        assertEquals(1L, invoiceRepository.count());
    }

    @Test
    @DisplayName("Test 6: Duplicate Prevention - idempotent request returns existing invoice without re-saving")
    void testDuplicateInvoicePrevention() {
        GenerateInvoiceRequest req = new GenerateInvoiceRequest();
        req.setServiceRecordId(101L);
        req.setBookingId(202L);
        req.setCustomerEmail("john@example.com");
        req.setServiceType("General Maintenance");

        // First generation
        InvoiceResponse firstResponse = billingService.generateInvoice(req);
        assertNotNull(firstResponse);
        String initialInvoiceNumber = firstResponse.getInvoiceNumber();

        // Second duplicate request
        InvoiceResponse secondResponse = billingService.generateInvoice(req);
        assertNotNull(secondResponse);
        assertEquals(initialInvoiceNumber, secondResponse.getInvoiceNumber());

        // Confirm database has ONLY ONE invoice
        assertEquals(1L, invoiceRepository.count());
    }

    @Test
    @DisplayName("Test 4: Customer Access - Customer can only view their own invoice; others are forbidden")
    void testCustomerInvoiceAccessControl() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-20261030-1010");
        invoice.setServiceRecordId(303L);
        invoice.setBookingId(404L);
        invoice.setCustomerEmail("alice@example.com");
        invoice.setTotalAmount(new BigDecimal("150.00"));
        invoice.setGeneratedAt(LocalDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        // 1. Owner can access
        InvoiceResponse ownerResponse = billingService.getInvoiceById(saved.getInvoiceId(), "alice@example.com", "CUSTOMER");
        assertNotNull(ownerResponse);
        assertEquals("INV-20261030-1010", ownerResponse.getInvoiceNumber());

        // 2. Admin can access
        InvoiceResponse adminResponse = billingService.getInvoiceById(saved.getInvoiceId(), "admin@precisionauto.com", "ADMIN");
        assertNotNull(adminResponse);

        // 3. Unauthorized other customer is blocked with SecurityException
        SecurityException ex = assertThrows(SecurityException.class, () ->
                billingService.getInvoiceById(saved.getInvoiceId(), "eve@example.com", "CUSTOMER")
        );
        assertTrue(ex.getMessage().contains("Forbidden"));
    }

    @Test
    @DisplayName("Test 5: Admin Access - Admin can view all invoices and actual database metrics")
    void testAdminSummaryAndAllInvoices() {
        Invoice inv1 = new Invoice();
        inv1.setInvoiceNumber("INV-001");
        inv1.setServiceRecordId(1L);
        inv1.setBookingId(1L);
        inv1.setTotalAmount(new BigDecimal("100.00"));
        inv1.setGeneratedAt(LocalDateTime.now().minusHours(2));
        invoiceRepository.save(inv1);

        Invoice inv2 = new Invoice();
        inv2.setInvoiceNumber("INV-002");
        inv2.setServiceRecordId(2L);
        inv2.setBookingId(2L);
        inv2.setTotalAmount(new BigDecimal("200.00"));
        inv2.setGeneratedAt(LocalDateTime.now().minusHours(1));
        invoiceRepository.save(inv2);

        // Admin summary call
        BillingSummaryResponse summary = billingService.getAdminSummary("ADMIN");
        assertNotNull(summary);
        assertEquals(2L, summary.getTotalInvoices());
        assertEquals(new BigDecimal("300.00"), summary.getTotalInvoiceValue());
        assertEquals(new BigDecimal("150.00"), summary.getAverageInvoiceValue());
        assertEquals(2, summary.getRecentInvoices().size());

        List<InvoiceResponse> allInvoices = billingService.getAllInvoices("ADMIN");
        assertEquals(2, allInvoices.size());

        // Non-admin call is forbidden
        assertThrows(SecurityException.class, () -> billingService.getAllInvoices("CUSTOMER"));
        assertThrows(SecurityException.class, () -> billingService.getAdminSummary("CUSTOMER"));
    }
}
