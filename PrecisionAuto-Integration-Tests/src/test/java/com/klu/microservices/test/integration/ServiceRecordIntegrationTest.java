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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("PrecisionAuto Care — Service Record Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceRecordIntegrationTest extends BaseIntegrationTest {

    private Map<String, Object> createBookingAndRecord(String customerToken) {
        // Register vehicle
        String regNumber = TestData.uniqueRegistrationNumber();
        Response vehRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX);
        Long vehicleId = vehRes.jsonPath().getLong("vehicleId");

        // Pick available slot
        Response slotsRes = given()
                .spec(RestAssuredConfig.baseRequest())
                .queryParam("date", LocalDate.now().plusDays(3).toString())
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/slots");

        List<Integer> slotIds = slotsRes.jsonPath().getList("findAll { it.status == 'AVAILABLE' }.slotId");
        Long slotId = slotIds.get(0).longValue();

        // Create booking
        Response bookRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .body(TestData.createBookingRequest(vehicleId, slotId, "General Maintenance", "Record test booking"))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);

        Long bookingId = bookRes.jsonPath().getLong("bookingId");

        // Service Record is automatically created by book-service calling record-service,
        // or can be retrieved via GET /records/customer
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

        // If not automatically triggered in test environment, trigger POST /records
        if (recordId == null) {
            Map<String, Object> createRec = new HashMap<>();
            createRec.put("bookingId", bookingId);
            createRec.put("vehicleId", vehicleId);
            createRec.put("serviceType", "General Maintenance");
            createRec.put("description", "Maintenance job");

            Response crRes = given()
                    .spec(RestAssuredConfig.baseRequest())
                    .body(createRec)
                    .when()
                    .post(TestConfig.RECORDS_PREFIX);
            recordId = crRes.jsonPath().getLong("recordId");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("vehicleId", vehicleId);
        result.put("bookingId", bookingId);
        result.put("recordId", recordId);
        return result;
    }

    @Test
    @Order(1)
    @DisplayName("1. Customer access to their own service records list")
    void testCustomerAccessOwnRecords() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Record Cust", email, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(token);
        Long recordId = (Long) setup.get("recordId");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .get(TestConfig.RECORDS_PREFIX + "/customer")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("recordId", hasItem(recordId.intValue()));
    }

    @Test
    @Order(2)
    @DisplayName("2. Retrieve specific service record by ID as owner")
    void testGetRecordByIdAsOwner() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Record Detail Cust", email, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(token);
        Long recordId = (Long) setup.get("recordId");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .get(TestConfig.RECORDS_PREFIX + "/" + recordId)
                .then()
                .statusCode(200)
                .body("recordId", equalTo(recordId.intValue()))
                .body("status", equalTo("PENDING"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Prevention of cross-customer access to another customer's service records")
    void testCustomerCannotAccessAnotherCustomerRecord() {
        String emailA = TestData.uniqueCustomerEmail();
        String tokenA = AuthHelper.registerAndGetCustomerToken("Cust Alpha", emailA, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(tokenA);
        Long recordIdA = (Long) setup.get("recordId");

        String emailB = TestData.uniqueCustomerEmail();
        String tokenB = AuthHelper.registerAndGetCustomerToken("Cust Beta", emailB, TestData.DEFAULT_PASSWORD);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenB))
                .when()
                .get(TestConfig.RECORDS_PREFIX + "/" + recordIdA)
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("permission to view this record"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Customer cannot update service record status (Forbidden 403)")
    void testCustomerCannotUpdateRecordStatus() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Cust Hacker", email, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(token);
        Long recordId = (Long) setup.get("recordId");

        Map<String, String> statusReq = new HashMap<>();
        statusReq.put("status", "IN_PROGRESS");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(statusReq)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("Only technicians or administrators"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Technician access to pending service queue")
    void testTechnicianAccessPendingQueue() {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techToken = AuthHelper.registerAndApproveTechnician("Queue Tech", techEmail, TestData.DEFAULT_PASSWORD, adminToken);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .when()
                .get(TestConfig.RECORDS_PREFIX + "/technician")
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("6. Technician updates service record to IN_PROGRESS")
    void testTechnicianProgressUpdate() {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techToken = AuthHelper.registerAndApproveTechnician("InProg Tech", techEmail, TestData.DEFAULT_PASSWORD, adminToken);

        String custEmail = TestData.uniqueCustomerEmail();
        String custToken = AuthHelper.registerAndGetCustomerToken("InProg Cust", custEmail, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(custToken);
        Long recordId = (Long) setup.get("recordId");

        Map<String, String> updateReq = new HashMap<>();
        updateReq.put("status", "IN_PROGRESS");
        updateReq.put("technicianName", "Lead Tech Dave");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(updateReq)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(200)
                .body("recordId", equalTo(recordId.intValue()))
                .body("status", equalTo("IN_PROGRESS"))
                .body("startTime", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("7. Lifecycle transition validation: PENDING cannot directly jump to COMPLETED")
    void testInvalidStatusTransitionRejected() {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techToken = AuthHelper.registerAndApproveTechnician("Invalid Tech", techEmail, TestData.DEFAULT_PASSWORD, adminToken);

        String custEmail = TestData.uniqueCustomerEmail();
        String custToken = AuthHelper.registerAndGetCustomerToken("Invalid Cust", custEmail, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(custToken);
        Long recordId = (Long) setup.get("recordId");

        Map<String, String> directCompleteReq = new HashMap<>();
        directCompleteReq.put("status", "COMPLETED");
        directCompleteReq.put("remarks", "Direct jump attempt");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(directCompleteReq)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(400)
                .body("error", equalTo("Invalid Status Transition"))
                .body("message", containsString("must be moved to IN_PROGRESS before it can be completed"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Completing service requires technician remarks")
    void testCompleteServiceRemarksRequired() {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techToken = AuthHelper.registerAndApproveTechnician("Remarks Tech", techEmail, TestData.DEFAULT_PASSWORD, adminToken);

        String custEmail = TestData.uniqueCustomerEmail();
        String custToken = AuthHelper.registerAndGetCustomerToken("Remarks Cust", custEmail, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(custToken);
        Long recordId = (Long) setup.get("recordId");

        // Move to IN_PROGRESS first
        Map<String, String> inProg = new HashMap<>();
        inProg.put("status", "IN_PROGRESS");
        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(inProg)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(200);

        // Attempt COMPLETED without remarks
        Map<String, String> completeWithoutRemarks = new HashMap<>();
        completeWithoutRemarks.put("status", "COMPLETED");
        completeWithoutRemarks.put("remarks", "");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(completeWithoutRemarks)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(400)
                .body("message", containsString("remarks/findings are required"));
    }

    @Test
    @Order(9)
    @DisplayName("9. Completing service and verifying booking status synchronization")
    void testCompleteServiceAndVerifyBookingSynchronization() {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techToken = AuthHelper.registerAndApproveTechnician("Sync Tech", techEmail, TestData.DEFAULT_PASSWORD, adminToken);

        String custEmail = TestData.uniqueCustomerEmail();
        String custToken = AuthHelper.registerAndGetCustomerToken("Sync Cust", custEmail, TestData.DEFAULT_PASSWORD);
        Map<String, Object> setup = createBookingAndRecord(custToken);
        Long recordId = (Long) setup.get("recordId");
        Long bookingId = (Long) setup.get("bookingId");

        // 1. Move to IN_PROGRESS
        Map<String, String> inProg = new HashMap<>();
        inProg.put("status", "IN_PROGRESS");
        inProg.put("technicianName", "Master Tech Rick");
        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(inProg)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(200);

        // 2. Complete with remarks
        Map<String, String> completeReq = new HashMap<>();
        completeReq.put("status", "COMPLETED");
        completeReq.put("remarks", "Performed 40-point vehicle safety inspection, topped up brake fluid, replaced cabin air filter.");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(completeReq)
                .when()
                .put(TestConfig.RECORDS_PREFIX + "/" + recordId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("endTime", notNullValue())
                .body("remarks", containsString("40-point vehicle safety inspection"));

        // 3. Verify booking status synchronization in book-service
        given()
                .spec(RestAssuredConfig.authenticatedRequest(custToken))
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/" + bookingId)
                .then()
                .statusCode(200)
                .body("bookingId", equalTo(bookingId.intValue()))
                .body("status", equalTo("COMPLETED"));
    }
}
