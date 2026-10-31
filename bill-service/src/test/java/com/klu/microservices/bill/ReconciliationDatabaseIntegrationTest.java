package com.klu.microservices.bill;

import com.klu.microservices.bill.dto.GenerateInvoiceRequest;
import com.klu.microservices.bill.dto.InvoiceResponse;
import com.klu.microservices.bill.entity.Invoice;
import com.klu.microservices.bill.repository.InvoiceRepository;
import com.klu.microservices.bill.service.BillingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/precisionauto_billing",
    "spring.datasource.username=postgres",
    "spring.datasource.password=Koushik@338629"
})
public class ReconciliationDatabaseIntegrationTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void testReconcileAndGenerateInvoiceForCompletedRecord1() {
        // Build the request using exact data from precisionauto_records and precisionauto_booking
        GenerateInvoiceRequest req = new GenerateInvoiceRequest();
        req.setServiceRecordId(1L);
        req.setBookingId(1L);
        req.setCustomerId(1L);
        req.setCustomerName("kousik");
        req.setCustomerEmail("kousik@gmail.com");
        req.setVehicleId(1L);
        req.setVehicleRegistrationNumber("AP-16-H-2356");
        req.setVehicleMake("Toyota");
        req.setVehicleModel("Camry");
        req.setServiceType("General Maintenance");
        req.setTechnicianName("Rahul");
        req.setServiceDescription("Scheduled General Maintenance for Toyota Camry (AP-16-H-2356)");
        req.setRemarks("Completed Successfully");
        req.setServiceCompletedAt(LocalDateTime.parse("2026-10-30T13:41:40.817786"));

        // Generate invoice
        InvoiceResponse response = billingService.generateInvoice(req);

        assertNotNull(response);
        assertNotNull(response.getInvoiceId());
        assertNotNull(response.getInvoiceNumber());
        assertTrue(response.getInvoiceNumber().startsWith("INV-"));
        assertEquals(1L, response.getServiceRecordId());
        assertEquals(1L, response.getBookingId());
        assertEquals("kousik@gmail.com", response.getCustomerEmail());
        assertEquals("AP-16-H-2356", response.getVehicleRegistrationNumber());
        assertEquals("Toyota", response.getVehicleMake());
        assertEquals("Camry", response.getVehicleModel());
        assertEquals("General Maintenance", response.getServiceType());
        assertEquals("Rahul", response.getTechnicianName());

        // Verify monetary calculations
        assertTrue(response.getLaborCharges().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);

        // Verify idempotency: calling generateInvoice again for record #1 returns the same invoice
        InvoiceResponse secondCall = billingService.generateInvoice(req);
        assertEquals(response.getInvoiceId(), secondCall.getInvoiceId());
        assertEquals(response.getInvoiceNumber(), secondCall.getInvoiceNumber());

        // Verify invoice is retrievable by customer email
        List<InvoiceResponse> customerInvoices = billingService.getMyInvoices("kousik@gmail.com");
        assertFalse(customerInvoices.isEmpty());
        assertTrue(customerInvoices.stream().anyMatch(inv -> inv.getServiceRecordId().equals(1L)));

        // Verify database persistence
        Optional<Invoice> persisted = invoiceRepository.findByServiceRecordId(1L);
        assertTrue(persisted.isPresent());
        assertEquals(response.getInvoiceNumber(), persisted.get().getInvoiceNumber());
    }
}
