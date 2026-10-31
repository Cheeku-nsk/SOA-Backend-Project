package com.klu.microservices.bill;

import com.klu.microservices.bill.dto.InvoiceResponse;
import com.klu.microservices.bill.service.BillingService;
import com.klu.microservices.bill.service.InvoicePdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/precisionauto_billing",
    "spring.datasource.username=postgres",
    "spring.datasource.password=Koushik@338629"
})
public class GeneratePdfTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private InvoicePdfService pdfService;

    @Test
    void exportPdfForInspection() throws Exception {
        InvoiceResponse invoice = billingService.getMyInvoices("kousik@gmail.com").get(0);
        byte[] pdfBytes = pdfService.generatePdf(invoice);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        Files.createDirectories(Paths.get("C:/Users/DELL/.gemini/antigravity/brain/ce2044a5-e30a-4105-aebe-f7726e15ae68/scratch"));
        try (FileOutputStream fos = new FileOutputStream("C:/Users/DELL/.gemini/antigravity/brain/ce2044a5-e30a-4105-aebe-f7726e15ae68/scratch/server_generated_invoice.pdf")) {
            fos.write(pdfBytes);
        }
        System.out.println("Exported single-page PDF bytes: " + pdfBytes.length);

        // Test long multi-page invoice
        InvoiceResponse longInvoice = new InvoiceResponse();
        longInvoice.setInvoiceId(999L);
        longInvoice.setInvoiceNumber("INV-20261031-LONG");
        longInvoice.setBookingId(1L);
        longInvoice.setServiceRecordId(1L);
        longInvoice.setCustomerName("kousik");
        longInvoice.setCustomerEmail("kousik@gmail.com");
        longInvoice.setVehicleMake("Toyota");
        longInvoice.setVehicleModel("Camry");
        longInvoice.setVehicleRegistrationNumber("AP-16-H-2356");
        longInvoice.setServiceType("Complete Major Overhaul & Multi-Point Inspection");
        longInvoice.setTechnicianName("Rahul - Lead Master Diagnostic Specialist");
        longInvoice.setLaborCharges(invoice.getLaborCharges());
        longInvoice.setPartsCharges(invoice.getPartsCharges());
        longInvoice.setAdditionalCharges(invoice.getAdditionalCharges());
        longInvoice.setSubtotal(invoice.getSubtotal());
        longInvoice.setTaxPercentage(invoice.getTaxPercentage());
        longInvoice.setTaxAmount(invoice.getTaxAmount());
        longInvoice.setTotalAmount(invoice.getTotalAmount());
        longInvoice.setPaymentStatus("ISSUED");
        longInvoice.setGeneratedAt(invoice.getGeneratedAt());
        longInvoice.setServiceCompletedAt(invoice.getServiceCompletedAt());
        longInvoice.setRemarks("Comprehensive multi-system diagnostic completed. Verified brake rotor runout within 0.002 mm tolerance. "
                + "Cooling system pressure test held steady at 15 psi for 30 minutes without pressure decay. "
                + "Transmission fluid flush executed with certified low-viscosity synthetic fluid. "
                + "All suspension bushings, ball joints, and tie rod ends inspected and confirmed in optimal condition. "
                + "ECU error codes scanned and cleared with zero pending DTCs. Road tested for 12 miles with exemplary dynamic stability.");

        byte[] longPdfBytes = pdfService.generatePdf(longInvoice);
        assertNotNull(longPdfBytes);
        assertTrue(longPdfBytes.length > 500);

        try (FileOutputStream fos = new FileOutputStream("C:/Users/DELL/.gemini/antigravity/brain/ce2044a5-e30a-4105-aebe-f7726e15ae68/scratch/server_generated_long_invoice.pdf")) {
            fos.write(longPdfBytes);
        }
        System.out.println("Exported multi-page PDF bytes: " + longPdfBytes.length);
    }
}
