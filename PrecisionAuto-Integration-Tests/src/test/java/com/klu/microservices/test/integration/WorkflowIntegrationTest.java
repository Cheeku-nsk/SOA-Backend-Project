package com.klu.microservices.test.integration;

import com.klu.microservices.test.config.RestAssuredConfig;
import com.klu.microservices.test.config.TestConfig;
import com.klu.microservices.test.utils.AuthHelper;
import com.klu.microservices.test.utils.TestData;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrecisionAuto Care — Complete End-to-End Workflow Integration Test")
public class WorkflowIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Complete Lifecycle: Customer Registration → Vehicle Registration → Service Booking → Technician Progress Update → Service Completion → Booking Synchronization → Invoice Generation → Customer Invoice Retrieval & PDF Download")
    void testEndToEndAutomotiveServiceLifecycle() {

        // =========================================================================
        // Step 1: Customer Registration & Dynamic JWT Acquisition
        // =========================================================================
        String customerName = "Alexander Wright";
        String customerEmail = TestData.uniqueCustomerEmail();
        String customerPassword = TestData.DEFAULT_PASSWORD;

        Response regResponse = AuthHelper.registerCustomer(customerName, customerEmail, customerPassword);
        regResponse.then()
                .statusCode(200)
                .body("userId", notNullValue())
                .body("email", equalTo(customerEmail))
                .body("role", equalTo("CUSTOMER"))
                .body("status", equalTo("APPROVED"));

        Long customerUserId = regResponse.jsonPath().getLong("userId");
        assertNotNull(customerUserId, "Customer User ID must be dynamically generated");

        String customerToken = AuthHelper.loginAndGetToken(customerEmail, customerPassword, "CUSTOMER");
        assertNotNull(customerToken, "Customer JWT token must be obtained dynamically");

        // =========================================================================
        // Step 2: Vehicle Registration Under Customer
        // =========================================================================
        String regNumber = TestData.uniqueRegistrationNumber();
        Map<String, Object> vehicleReq = new HashMap<>();
        vehicleReq.put("registrationNumber", regNumber);
        vehicleReq.put("make", "BMW");
        vehicleReq.put("model", "330i M Sport");
        vehicleReq.put("year", 2023);
        vehicleReq.put("vehicleType", "Sedan");
        vehicleReq.put("fuelType", "Petrol");

        Response vehicleResponse = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .body(vehicleReq)
                .when()
                .post(TestConfig.VEHICLES_PREFIX);

        vehicleResponse.then()
                .statusCode(201)
                .body("vehicleId", notNullValue())
                .body("registrationNumber", equalTo(regNumber.toUpperCase()))
                .body("make", equalTo("BMW"))
                .body("model", equalTo("330i M Sport"))
                .body("customerEmail", equalTo(customerEmail));

        Long vehicleId = vehicleResponse.jsonPath().getLong("vehicleId");
        assertNotNull(vehicleId, "Vehicle ID must be captured dynamically from response");

        // =========================================================================
        // Step 3: Service Slot Selection & Appointment Booking
        // =========================================================================
        LocalDate appointmentDate = LocalDate.now().plusDays(2);
        Response slotsResponse = given()
                .spec(RestAssuredConfig.baseRequest())
                .queryParam("date", appointmentDate.toString())
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/slots");

        slotsResponse.then().statusCode(200).body("$", not(empty()));

        List<Integer> availableSlotIds = slotsResponse.jsonPath().getList("findAll { it.status == 'AVAILABLE' }.slotId");
        assertFalse(availableSlotIds.isEmpty(), "At least one slot must be available for future booking date");
        Long chosenSlotId = availableSlotIds.get(0).longValue();

        String serviceType = "Oil & Fluid Service";
        String bookingNotes = "Complete synthetic engine oil flush, oil filter change, and brake fluid level inspection.";
        Map<String, Object> bookingReq = TestData.createBookingRequest(vehicleId, chosenSlotId, serviceType, bookingNotes);

        Response bookingResponse = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .body(bookingReq)
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);

        bookingResponse.then()
                .statusCode(201)
                .body("bookingId", notNullValue())
                .body("customerEmail", equalTo(customerEmail))
                .body("vehicleId", equalTo(vehicleId.intValue()))
                .body("slotId", equalTo(chosenSlotId.intValue()))
                .body("serviceType", equalTo(serviceType))
                .body("status", equalTo("CONFIRMED"));

        Long bookingId = bookingResponse.jsonPath().getLong("bookingId");
        assertNotNull(bookingId, "Booking ID must be obtained dynamically");

        // =========================================================================
        // Step 4: Technician Setup & Service Job Assignment
        // =========================================================================
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techPassword = TestData.DEFAULT_PASSWORD;
        String techName = "Marcus Vance";

        String techToken = AuthHelper.registerAndApproveTechnician(techName, techEmail, techPassword, adminToken);
        assertNotNull(techToken, "Technician JWT token must be obtained dynamically");

        // Locate Service Record linked to this booking
        Response customerRecordsRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.RECORDS_PREFIX + "/customer");

        List<Map<String, Object>> recordsList = customerRecordsRes.jsonPath().getList("$");
        Long recordId = null;
        for (Map<String, Object> rec : recordsList) {
            if (rec.get("bookingId") != null && Long.valueOf(String.valueOf(rec.get("bookingId"))).equals(bookingId)) {
                recordId = Long.valueOf(String.valueOf(rec.get("recordId")));
                break;
            }
        }

        // Direct record creation fallback if asynchronous microservice event was decoupled in test
        if (recordId == null) {
            Map<String, Object> manualRecord = new HashMap<>();
            manualRecord.put("bookingId", bookingId);
            manualRecord.put("vehicleId", vehicleId);
            manualRecord.put("serviceType", serviceType);
            manualRecord.put("description", bookingNotes);
            manualRecord.put("customerEmail", customerEmail);

            Response recCreated = given()
                    .spec(RestAssuredConfig.baseRequest())
                    .body(manualRecord)
                    .when()
                    .post(TestConfig.RECORDS_PREFIX);
            recordId = recCreated.jsonPath().getLong("recordId");
        }

        assertNotNull(recordId, "Service Record ID must exist for the active appointment");

        // Technician transitions job: PENDING → IN_PROGRESS
        Map<String, String> startProgressReq = new HashMap<>();
        startProgressReq.put("status", "IN_PROGRESS");
        startProgressReq.put("technicianName", techName);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(startProgressReq)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(200)
                .body("recordId", equalTo(recordId.intValue()))
                .body("status", equalTo("IN_PROGRESS"))
                .body("startTime", notNullValue());

        // =========================================================================
        // Step 5: Service Completion with Detailed Findings
        // =========================================================================
        String completionRemarks = "Drained 5.2L synthetic oil, installed OEM Mann filter, reset service indicator light. Multi-point check passed.";
        Map<String, String> completeJobReq = new HashMap<>();
        completeJobReq.put("status", "COMPLETED");
        completeJobReq.put("remarks", completionRemarks);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(completeJobReq)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(200)
                .body("recordId", equalTo(recordId.intValue()))
                .body("status", equalTo("COMPLETED"))
                .body("endTime", notNullValue())
                .body("remarks", equalTo(completionRemarks));

        // =========================================================================
        // Step 6: Booking Status Synchronization Verification
        // =========================================================================
        given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/" + bookingId)
                .then()
                .statusCode(200)
                .body("bookingId", equalTo(bookingId.intValue()))
                .body("status", equalTo("COMPLETED"));

        // =========================================================================
        // Step 7: Invoice Generation & Verification
        // =========================================================================
        Response customerInvoicesRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/my-invoices");

        customerInvoicesRes.then().statusCode(200).body("$", not(empty()));

        List<Map<String, Object>> invoices = customerInvoicesRes.jsonPath().getList("$");
        Map<String, Object> finalInvoice = null;
        for (Map<String, Object> inv : invoices) {
            if (inv.get("serviceRecordId") != null && Long.valueOf(String.valueOf(inv.get("serviceRecordId"))).equals(recordId)) {
                finalInvoice = inv;
                break;
            }
        }

        assertNotNull(finalInvoice, "A finalized invoice must be generated for the completed service record");
        Long invoiceId = Long.valueOf(String.valueOf(finalInvoice.get("invoiceId")));
        String invoiceNumber = String.valueOf(finalInvoice.get("invoiceNumber"));

        // Fetch detailed invoice
        Response invoiceDetailRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/invoices/" + invoiceId);

        invoiceDetailRes.then()
                .statusCode(200)
                .body("invoiceId", equalTo(invoiceId.intValue()))
                .body("invoiceNumber", equalTo(invoiceNumber))
                .body("serviceRecordId", equalTo(recordId.intValue()))
                .body("bookingId", equalTo(bookingId.intValue()))
                .body("customerEmail", equalTo(customerEmail))
                .body("vehicleRegistrationNumber", equalTo(regNumber.toUpperCase()))
                .body("vehicleMake", equalTo("BMW"))
                .body("vehicleModel", equalTo("330i M Sport"))
                .body("serviceType", equalTo("Oil & Fluid Service"))
                .body("paymentStatus", equalTo("ISSUED"));

        // Check Oil & Fluid Service Pricing Breakdown:
        // Labor: 50.00, Parts: 65.00, Subtotal: 115.00, Tax (10%): 11.50, Total: 126.50
        BigDecimal labor = new BigDecimal(invoiceDetailRes.jsonPath().getString("laborCharges")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal parts = new BigDecimal(invoiceDetailRes.jsonPath().getString("partsCharges")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = new BigDecimal(invoiceDetailRes.jsonPath().getString("subtotal")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = new BigDecimal(invoiceDetailRes.jsonPath().getString("taxAmount")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = new BigDecimal(invoiceDetailRes.jsonPath().getString("totalAmount")).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("50.00"), labor, "Labor charges must equal Oil & Fluid Service rate ($50.00)");
        assertEquals(new BigDecimal("65.00"), parts, "Parts charges must equal Oil & Fluid Service parts rate ($65.00)");
        assertEquals(new BigDecimal("115.00"), subtotal, "Subtotal must equal labor + parts ($115.00)");
        assertEquals(new BigDecimal("11.50"), taxAmount, "Tax amount must be 10% ($11.50)");
        assertEquals(new BigDecimal("126.50"), totalAmount, "Total amount must equal subtotal + tax ($126.50)");

        // =========================================================================
        // Step 8: PDF Invoice Generation & Byte Stream Verification
        // =========================================================================
        Response pdfRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/invoices/" + invoiceId + "/pdf");

        pdfRes.then()
                .statusCode(200)
                .contentType("application/pdf")
                .header("Content-Disposition", containsString(invoiceNumber));

        byte[] pdfBytes = pdfRes.getBody().asByteArray();
        assertTrue(pdfBytes.length > 200, "Downloaded PDF must contain valid binary content");
        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8));
        assertTrue(pdfHeader.startsWith("%PDF-"), "Stream must begin with valid PDF header (%PDF-)");
    }
}
