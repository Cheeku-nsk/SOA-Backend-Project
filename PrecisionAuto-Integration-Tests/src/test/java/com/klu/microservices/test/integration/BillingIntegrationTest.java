package com.klu.microservices.test.integration;

import com.klu.microservices.test.config.RestAssuredConfig;
import com.klu.microservices.test.config.TestConfig;
import com.klu.microservices.test.utils.AuthHelper;
import com.klu.microservices.test.utils.TestData;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrecisionAuto Care — Billing & Invoicing Integration Tests (Invoice Only)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BillingIntegrationTest extends BaseIntegrationTest {

    private Map<String, Object> createCompletedService(String customerToken, String serviceType, String remarks) {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techToken = AuthHelper.registerAndApproveTechnician("Bill Tech", techEmail, TestData.DEFAULT_PASSWORD, adminToken);

        // 1. Vehicle
        String regNumber = TestData.uniqueRegistrationNumber();
        Response vehRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX);
        Long vehicleId = vehRes.jsonPath().getLong("vehicleId");

        // 2. Slot
        Response slotsRes = given()
                .spec(RestAssuredConfig.baseRequest())
                .queryParam("date", LocalDate.now().plusDays(4).toString())
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/slots");
        List<Integer> slotIds = slotsRes.jsonPath().getList("findAll { it.status == 'AVAILABLE' }.slotId");
        Long slotId = slotIds.get(0).longValue();

        // 3. Booking
        Response bookRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .body(TestData.createBookingRequest(vehicleId, slotId, serviceType, "Pre-invoice service"))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);
        Long bookingId = bookRes.jsonPath().getLong("bookingId");

        // 4. Record
        Response recListRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.RECORDS_PREFIX + "/customer");

        List<Map<String, Object>> records = recListRes.jsonPath().getList("$");
        Long recordId = null;
        for (Map<String, Object> r : records) {
            if (r.get("bookingId") != null && Long.valueOf(String.valueOf(r.get("bookingId"))).equals(bookingId)) {
                recordId = Long.valueOf(String.valueOf(r.get("recordId")));
                break;
            }
        }
        if (recordId == null) {
            Map<String, Object> cr = new HashMap<>();
            cr.put("bookingId", bookingId);
            cr.put("vehicleId", vehicleId);
            cr.put("serviceType", serviceType);
            Response crRes = given().spec(RestAssuredConfig.baseRequest()).body(cr).when().post(TestConfig.RECORDS_PREFIX);
            recordId = crRes.jsonPath().getLong("recordId");
        }

        // 5. In-Progress
        Map<String, String> inProg = new HashMap<>();
        inProg.put("status", "IN_PROGRESS");
        given().spec(RestAssuredConfig.authenticatedRequest(techToken)).body(inProg).when().put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status");

        // 6. Complete
        Map<String, String> complete = new HashMap<>();
        complete.put("status", "COMPLETED");
        complete.put("remarks", remarks != null ? remarks : "Completed maintenance satisfactorily");
        given().spec(RestAssuredConfig.authenticatedRequest(techToken)).body(complete).when().put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status");

        Map<String, Object> res = new HashMap<>();
        res.put("vehicleId", vehicleId);
        res.put("regNumber", regNumber);
        res.put("bookingId", bookingId);
        res.put("recordId", recordId);
        return res;
    }

    @Test
    @Order(1)
    @DisplayName("1. Incomplete service must not generate a final invoice")
    void testIncompleteServiceCannotGenerateInvoice() {
        String adminToken = AuthHelper.getAdminToken();
        String customerEmail = TestData.uniqueCustomerEmail();
        String customerToken = AuthHelper.registerAndGetCustomerToken("Incomplete Cust", customerEmail, TestData.DEFAULT_PASSWORD);

        // Register vehicle & create booking (status PENDING)
        String regNumber = TestData.uniqueRegistrationNumber();
        Response vehRes = given().spec(RestAssuredConfig.authenticatedRequest(customerToken)).body(TestData.createVehicleRequest(regNumber)).when().post(TestConfig.VEHICLES_PREFIX);
        Long vehicleId = vehRes.jsonPath().getLong("vehicleId");

        Response slotsRes = given().spec(RestAssuredConfig.baseRequest()).queryParam("date", LocalDate.now().plusDays(5).toString()).when().get(TestConfig.BOOKINGS_PREFIX + "/slots");
        List<Integer> slotIds = slotsRes.jsonPath().getList("findAll { it.status == 'AVAILABLE' }.slotId");
        Long slotId = slotIds.get(0).longValue();

        Response bookRes = given().spec(RestAssuredConfig.authenticatedRequest(customerToken)).body(TestData.createBookingRequest(vehicleId, slotId, "General Maintenance", null)).when().post(TestConfig.BOOKINGS_PREFIX);
        Long bookingId = bookRes.jsonPath().getLong("bookingId");

        Map<String, Object> cr = new HashMap<>();
        cr.put("bookingId", bookingId);
        cr.put("vehicleId", vehicleId);
        cr.put("serviceType", "General Maintenance");
        Response crRes = given().spec(RestAssuredConfig.baseRequest()).body(cr).when().post(TestConfig.RECORDS_PREFIX);
        Long recordId = crRes.jsonPath().getLong("recordId");

        // Attempt admin reconciliation on PENDING record
        given()
                .spec(RestAssuredConfig.authenticatedRequest(adminToken))
                .when()
                .post(TestConfig.RECORDS_PREFIX + "/" + recordId + "/reconcile-invoice")
                .then()
                .statusCode(400)
                .body("error", equalTo("Reconciliation Error"))
                .body("message", containsString("is not marked COMPLETED"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Direct internal invoice generation endpoint rejects customer and technician access")
    void testDirectInvoiceGenerationRestrictedToInternal() {
        String customerEmail = TestData.uniqueCustomerEmail();
        String customerToken = AuthHelper.registerAndGetCustomerToken("No Gen Cust", customerEmail, TestData.DEFAULT_PASSWORD);

        Map<String, Object> req = new HashMap<>();
        req.put("serviceRecordId", 9999L);
        req.put("bookingId", 9999L);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .body(req)
                .when()
                .post(TestConfig.BILLING_PREFIX + "/internal/invoices/generate")
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("restricted to internal service automation"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Retrieve generated invoice and verify complete itemized billing calculation")
    void testRetrieveInvoiceAndVerifyItemizedCalculation() {
        String customerEmail = TestData.uniqueCustomerEmail();
        String customerToken = AuthHelper.registerAndGetCustomerToken("Invoice Cust", customerEmail, TestData.DEFAULT_PASSWORD);

        String serviceType = "Brake Services";
        Map<String, Object> setup = createCompletedService(customerToken, serviceType, "Replaced ceramic brake pads and resurfaced rotors.");
        Long recordId = (Long) setup.get("recordId");
        Long bookingId = (Long) setup.get("bookingId");
        String regNumber = (String) setup.get("regNumber");

        // Customer retrieves their invoices
        Response invoicesRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/my-invoices");

        invoicesRes.then().statusCode(200).body("$", not(empty()));

        List<Map<String, Object>> invoices = invoicesRes.jsonPath().getList("$");
        Map<String, Object> matchedInvoice = null;
        for (Map<String, Object> inv : invoices) {
            if (inv.get("serviceRecordId") != null && Long.valueOf(String.valueOf(inv.get("serviceRecordId"))).equals(recordId)) {
                matchedInvoice = inv;
                break;
            }
        }

        assertNotNull(matchedInvoice, "Invoice should be generated for completed service record");
        Long invoiceId = Long.valueOf(String.valueOf(matchedInvoice.get("invoiceId")));

        // Fetch detailed invoice by ID
        Response detailRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/invoices/" + invoiceId);

        detailRes.then()
                .statusCode(200)
                .body("invoiceId", equalTo(invoiceId.intValue()))
                .body("invoiceNumber", startsWith("INV-"))
                .body("serviceRecordId", equalTo(recordId.intValue()))
                .body("bookingId", equalTo(bookingId.intValue()))
                .body("customerEmail", equalTo(customerEmail))
                .body("vehicleRegistrationNumber", equalTo(regNumber.toUpperCase()))
                .body("serviceType", equalTo("Brake Services"))
                .body("paymentStatus", equalTo("ISSUED"));

        // Verify itemized monetary values
        BigDecimal labor = new BigDecimal(detailRes.jsonPath().getString("laborCharges")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal parts = new BigDecimal(detailRes.jsonPath().getString("partsCharges")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = new BigDecimal(detailRes.jsonPath().getString("subtotal")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxPercent = new BigDecimal(detailRes.jsonPath().getString("taxPercentage")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = new BigDecimal(detailRes.jsonPath().getString("taxAmount")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = new BigDecimal(detailRes.jsonPath().getString("totalAmount")).setScale(2, RoundingMode.HALF_UP);

        // Brake Services: Labor = 120.00, Parts = 130.00
        assertEquals(new BigDecimal("120.00"), labor, "Labor charges must match Brake Services rate");
        assertEquals(new BigDecimal("130.00"), parts, "Parts charges must match Brake Services rate");
        assertEquals(new BigDecimal("250.00"), subtotal, "Subtotal must equal labor + parts");
        assertEquals(new BigDecimal("10.00"), taxPercent, "Tax rate must be 10.00%");
        assertEquals(new BigDecimal("25.00"), taxAmount, "Tax amount must be 10% of subtotal");
        assertEquals(new BigDecimal("275.00"), totalAmount, "Total amount must equal subtotal + tax");
    }

    @Test
    @Order(4)
    @DisplayName("4. Verify duplicate invoice generation is prevented (Idempotent)")
    void testDuplicateInvoiceGenerationPrevented() {
        String adminToken = AuthHelper.getAdminToken();
        String customerEmail = TestData.uniqueCustomerEmail();
        String customerToken = AuthHelper.registerAndGetCustomerToken("Idempotent Cust", customerEmail, TestData.DEFAULT_PASSWORD);

        Map<String, Object> setup = createCompletedService(customerToken, "Oil & Fluid Service", "Synthetic 5W-30 change");
        Long recordId = (Long) setup.get("recordId");
        Long bookingId = (Long) setup.get("bookingId");

        // Retrieve existing invoice number
        Response invoicesRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/my-invoices");

        String originalInvoiceNumber = invoicesRes.jsonPath().getString("find { it.serviceRecordId == " + recordId + " }.invoiceNumber");
        assertNotNull(originalInvoiceNumber);

        // Admin triggers invoice reconciliation again for the exact same service record
        Response reconcileRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(adminToken))
                .when()
                .post(TestConfig.RECORDS_PREFIX + "/" + recordId + "/reconcile-invoice");

        reconcileRes.then().statusCode(200);

        // Verify only ONE invoice exists for this record and invoice number is preserved
        Response invoicesAfterRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/my-invoices");

        List<String> matchingNumbers = invoicesAfterRes.jsonPath().getList("findAll { it.serviceRecordId == " + recordId + " }.invoiceNumber");
        assertEquals(1, matchingNumbers.size(), "Only one invoice must exist per service record");
        assertEquals(originalInvoiceNumber, matchingNumbers.get(0));
    }

    @Test
    @Order(5)
    @DisplayName("5. Customer cannot access another customer's invoice (Forbidden 403)")
    void testCustomerCannotAccessAnotherCustomerInvoice() {
        String emailA = TestData.uniqueCustomerEmail();
        String tokenA = AuthHelper.registerAndGetCustomerToken("Invoice Alpha", emailA, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setupA = createCompletedService(tokenA, "General Maintenance", "Owner A inspection");
        Long recordIdA = (Long) setupA.get("recordId");

        Response invoicesA = given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenA))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/my-invoices");

        Long invoiceIdA = Long.valueOf(String.valueOf(invoicesA.jsonPath().get("find { it.serviceRecordId == " + recordIdA + " }.invoiceId")));

        // Customer B tries to view Customer A's invoice
        String emailB = TestData.uniqueCustomerEmail();
        String tokenB = AuthHelper.registerAndGetCustomerToken("Invoice Beta", emailB, TestData.DEFAULT_PASSWORD);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenB))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/invoices/" + invoiceIdA)
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("permission to view this invoice"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Generate and download invoice PDF stream")
    void testDownloadInvoicePdf() {
        String customerEmail = TestData.uniqueCustomerEmail();
        String customerToken = AuthHelper.registerAndGetCustomerToken("Pdf Cust", customerEmail, TestData.DEFAULT_PASSWORD);

        Map<String, Object> setup = createCompletedService(customerToken, "Engine Repair", "Timing chain adjusted and spark plugs renewed.");
        Long recordId = (Long) setup.get("recordId");

        Response invoicesRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/my-invoices");

        Long invoiceId = Long.valueOf(String.valueOf(invoicesRes.jsonPath().get("find { it.serviceRecordId == " + recordId + " }.invoiceId")));

        Response pdfResponse = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/invoices/" + invoiceId + "/pdf");

        pdfResponse.then()
                .statusCode(200)
                .contentType("application/pdf")
                .header("Content-Disposition", containsString("inline; filename="))
                .header("Content-Disposition", containsString(".pdf"));

        byte[] pdfBytes = pdfResponse.getBody().asByteArray();
        assertTrue(pdfBytes.length > 100, "PDF byte stream must not be empty");
        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8));
        assertTrue(pdfHeader.startsWith("%PDF-"), "Stream must be a valid PDF format starting with %PDF-");
    }

    @Test
    @Order(7)
    @DisplayName("7. Administrator can retrieve all invoices and billing aggregate metrics")
    void testAdminBillingSummary() {
        String adminToken = AuthHelper.getAdminToken();

        given()
                .spec(RestAssuredConfig.authenticatedRequest(adminToken))
                .when()
                .get(TestConfig.BILLING_PREFIX + "/admin/summary")
                .then()
                .statusCode(200)
                .body("totalInvoices", notNullValue())
                .body("totalRevenue", notNullValue())
                .body("averageInvoiceValue", notNullValue());
    }
}
