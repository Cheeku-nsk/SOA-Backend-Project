package com.klu.microservices.bill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.klu.microservices.bill.dto.BillingSummaryResponse;
import com.klu.microservices.bill.dto.GenerateInvoiceRequest;
import com.klu.microservices.bill.dto.InvoiceResponse;
import com.klu.microservices.bill.service.BillingService;
import com.klu.microservices.bill.service.InvoicePdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private TestBillingService testBillingService;
    private TestPdfService testPdfService;

    @BeforeEach
    void setUp() {
        testBillingService = new TestBillingService();
        testPdfService = new TestPdfService();

        BillingController controller = new BillingController();
        ReflectionTestUtils.setField(controller, "billingService", testBillingService);
        ReflectionTestUtils.setField(controller, "pdfService", testPdfService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /billing/my-invoices should return invoices for authenticated customer")
    void testGetMyInvoices() throws Exception {
        mockMvc.perform(get("/billing/my-invoices")
                        .header("X-User-Email", "john@example.com")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-20261030-0001"))
                .andExpect(jsonPath("$[0].totalAmount").value(132.00));
    }

    @Test
    @DisplayName("GET /billing/my-invoices without credentials returns 401 Unauthorized")
    void testGetMyInvoicesUnauthorized() throws Exception {
        mockMvc.perform(get("/billing/my-invoices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /billing/internal/invoices/generate is FORBIDDEN for customers and technicians")
    void testDirectCustomerOrTechCallForbidden() throws Exception {
        GenerateInvoiceRequest req = new GenerateInvoiceRequest();
        req.setServiceRecordId(10L);
        req.setBookingId(20L);

        mockMvc.perform(post("/billing/internal/invoices/generate")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/billing/internal/invoices/generate")
                        .header("X-User-Role", "TECHNICIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /billing/internal/invoices/generate succeeds for internal backend invocation")
    void testInternalGenerateInvoiceSucceeds() throws Exception {
        GenerateInvoiceRequest req = new GenerateInvoiceRequest();
        req.setServiceRecordId(10L);
        req.setBookingId(20L);

        mockMvc.perform(post("/billing/internal/invoices/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-20261030-0005"));
    }

    @Test
    @DisplayName("GET /billing/admin/summary returns actual summary metrics for admin")
    void testGetAdminSummary() throws Exception {
        mockMvc.perform(get("/billing/admin/summary")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoices").value(5))
                .andExpect(jsonPath("$.totalInvoiceValue").value(750.00))
                .andExpect(jsonPath("$.averageInvoiceValue").value(150.00));
    }

    @Test
    @DisplayName("GET /billing/admin/summary returns 403 Forbidden for non-admin")
    void testGetAdminSummaryForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/billing/admin/summary")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /billing/invoices/{id}/pdf returns application/pdf byte stream")
    void testDownloadInvoicePdf() throws Exception {
        mockMvc.perform(get("/billing/invoices/1/pdf")
                        .header("X-User-Email", "john@example.com")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"inline\"; filename=\"Invoice-INV-20261030-0001.pdf\""));
    }

    // Lightweight test doubles without mocking library dependencies
    static class TestBillingService extends BillingService {
        @Override
        public List<InvoiceResponse> getMyInvoices(String customerEmail) {
            InvoiceResponse res = new InvoiceResponse();
            res.setInvoiceId(1L);
            res.setInvoiceNumber("INV-20261030-0001");
            res.setCustomerEmail(customerEmail);
            res.setTotalAmount(new BigDecimal("132.00"));
            return List.of(res);
        }

        @Override
        public InvoiceResponse generateInvoice(GenerateInvoiceRequest req) {
            InvoiceResponse res = new InvoiceResponse();
            res.setInvoiceId(5L);
            res.setInvoiceNumber("INV-20261030-0005");
            res.setTotalAmount(new BigDecimal("132.00"));
            return res;
        }

        @Override
        public InvoiceResponse getInvoiceById(Long invoiceId, String userEmail, String userRole) {
            InvoiceResponse res = new InvoiceResponse();
            res.setInvoiceId(invoiceId);
            res.setInvoiceNumber("INV-20261030-0001");
            res.setCustomerEmail("john@example.com");
            res.setGeneratedAt(LocalDateTime.now());
            res.setTotalAmount(new BigDecimal("132.00"));
            return res;
        }

        @Override
        public BillingSummaryResponse getAdminSummary(String userRole) {
            return new BillingSummaryResponse(5L, new BigDecimal("750.00"), new BigDecimal("150.00"), List.of());
        }
    }

    static class TestPdfService extends InvoicePdfService {
        @Override
        public byte[] generatePdf(InvoiceResponse inv) {
            return "%PDF-1.4 sample content %%EOF".getBytes();
        }
    }
}
