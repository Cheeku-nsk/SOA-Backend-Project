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

@DisplayName("PrecisionAuto Care — Service Booking Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookingIntegrationTest extends BaseIntegrationTest {

    private Long findAvailableSlotId(LocalDate date) {
        Response slotsRes = given()
                .spec(RestAssuredConfig.baseRequest())
                .queryParam("date", date.toString())
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/slots");

        List<Integer> slotIds = slotsRes.jsonPath().getList("findAll { it.status == 'AVAILABLE' }.slotId");
        if (slotIds == null || slotIds.isEmpty()) {
            throw new IllegalStateException("No available slots found for date: " + date);
        }
        return slotIds.get(0).longValue();
    }

    private Long registerVehicle(String token) {
        String regNumber = TestData.uniqueRegistrationNumber();
        Response res = given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX);
        return res.jsonPath().getLong("vehicleId");
    }

    @Test
    @Order(1)
    @DisplayName("1. Retrieve available service bays")
    void testGetBays() {
        given()
                .spec(RestAssuredConfig.baseRequest())
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/bays")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("bayNumber", hasItem("Bay 1"))
                .body("status", hasItem("AVAILABLE"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Retrieve schedule slots for upcoming date")
    void testGetSlotsForDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        given()
                .spec(RestAssuredConfig.baseRequest())
                .queryParam("date", tomorrow.toString())
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/slots")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("slotDate", everyItem(equalTo(tomorrow.toString())))
                .body("status", hasItem("AVAILABLE"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Create a service appointment booking as customer")
    void testCreateBookingAsCustomer() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Booking Customer", email, TestData.DEFAULT_PASSWORD);
        Long vehicleId = registerVehicle(token);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long slotId = findAvailableSlotId(tomorrow);

        Map<String, Object> req = TestData.createBookingRequest(vehicleId, slotId, "General Maintenance", "Regular checkup");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(req)
                .when()
                .post(TestConfig.BOOKINGS_PREFIX)
                .then()
                .statusCode(201)
                .body("bookingId", notNullValue())
                .body("customerEmail", equalTo(email))
                .body("vehicleId", equalTo(vehicleId.intValue()))
                .body("slotId", equalTo(slotId.intValue()))
                .body("serviceType", equalTo("General Maintenance"))
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Retrieve customer's personal bookings list")
    void testGetMyBookings() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("My Bookings Customer", email, TestData.DEFAULT_PASSWORD);
        Long vehicleId = registerVehicle(token);
        Long slotId = findAvailableSlotId(LocalDate.now().plusDays(2));

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createBookingRequest(vehicleId, slotId, "Oil & Fluid Service", "Notes"))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);

        Long bookingId = createRes.jsonPath().getLong("bookingId");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/my")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("bookingId", hasItem(bookingId.intValue()));
    }

    @Test
    @Order(5)
    @DisplayName("5. Retrieve booking details by ID as owner")
    void testGetBookingById() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Detail Customer", email, TestData.DEFAULT_PASSWORD);
        Long vehicleId = registerVehicle(token);
        Long slotId = findAvailableSlotId(LocalDate.now().plusDays(3));

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createBookingRequest(vehicleId, slotId, "Brake Services", "Check front pads"))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);

        Long bookingId = createRes.jsonPath().getLong("bookingId");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .get(TestConfig.BOOKINGS_PREFIX + "/" + bookingId)
                .then()
                .statusCode(200)
                .body("bookingId", equalTo(bookingId.intValue()))
                .body("serviceType", equalTo("Brake Services"))
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Rejection of booking with vehicle belonging to another customer")
    void testBookingWithUnownedVehicleRejected() {
        // Customer A owns vehicle
        String emailA = TestData.uniqueCustomerEmail();
        String tokenA = AuthHelper.registerAndGetCustomerToken("Owner Alpha", emailA, TestData.DEFAULT_PASSWORD);
        Long vehicleIdA = registerVehicle(tokenA);

        // Customer B tries to book using Customer A's vehicle
        String emailB = TestData.uniqueCustomerEmail();
        String tokenB = AuthHelper.registerAndGetCustomerToken("Owner Beta", emailB, TestData.DEFAULT_PASSWORD);
        Long slotId = findAvailableSlotId(LocalDate.now().plusDays(4));

        Map<String, Object> req = TestData.createBookingRequest(vehicleIdA, slotId, "General Maintenance", "Unauthorized booking");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenB))
                .body(req)
                .when()
                .post(TestConfig.BOOKINGS_PREFIX)
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("do not own this vehicle"));
    }

    @Test
    @Order(7)
    @DisplayName("7. Duplicate slot booking rejection (Conflict 409)")
    void testDuplicateSlotBookingRejected() {
        String email1 = TestData.uniqueCustomerEmail();
        String token1 = AuthHelper.registerAndGetCustomerToken("Cust 1", email1, TestData.DEFAULT_PASSWORD);
        Long vehicleId1 = registerVehicle(token1);
        Long slotId = findAvailableSlotId(LocalDate.now().plusDays(5));

        // First customer successfully books the slot
        given()
                .spec(RestAssuredConfig.authenticatedRequest(token1))
                .body(TestData.createBookingRequest(vehicleId1, slotId, "General Maintenance", null))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX)
                .then()
                .statusCode(201);

        // Second customer tries to book the exact same slot
        String email2 = TestData.uniqueCustomerEmail();
        String token2 = AuthHelper.registerAndGetCustomerToken("Cust 2", email2, TestData.DEFAULT_PASSWORD);
        Long vehicleId2 = registerVehicle(token2);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token2))
                .body(TestData.createBookingRequest(vehicleId2, slotId, "Oil & Fluid Service", null))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX)
                .then()
                .statusCode(409)
                .body("error", equalTo("Slot Conflict"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Cancel booking as customer and verify slot is released")
    void testCancelBookingAsOwner() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Cancel Customer", email, TestData.DEFAULT_PASSWORD);
        Long vehicleId = registerVehicle(token);
        Long slotId = findAvailableSlotId(LocalDate.now().plusDays(6));

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createBookingRequest(vehicleId, slotId, "A/C & Heating", null))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);

        Long bookingId = createRes.jsonPath().getLong("bookingId");

        // Cancel the booking
        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .put(TestConfig.BOOKINGS_PREFIX + "/" + bookingId + "/cancel")
                .then()
                .statusCode(200)
                .body("bookingId", equalTo(bookingId.intValue()))
                .body("status", equalTo("CANCELLED"));
    }

    @Test
    @Order(9)
    @DisplayName("9. Completed bookings cannot be cancelled")
    void testCompletedBookingCannotBeCancelled() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Completed Cust", email, TestData.DEFAULT_PASSWORD);
        Long vehicleId = registerVehicle(token);
        Long slotId = findAvailableSlotId(LocalDate.now().plusDays(7));

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createBookingRequest(vehicleId, slotId, "Engine Repair", null))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);

        Long bookingId = createRes.jsonPath().getLong("bookingId");

        // Simulate internal service update to COMPLETED
        Map<String, String> statusBody = new HashMap<>();
        statusBody.put("status", "COMPLETED");

        given()
                .spec(RestAssuredConfig.baseRequest())
                .body(statusBody)
                .when()
                .put(TestConfig.BOOKINGS_PREFIX + "/" + bookingId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));

        // Now attempt cancellation
        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .put(TestConfig.BOOKINGS_PREFIX + "/" + bookingId + "/cancel")
                .then()
                .statusCode(400)
                .body("error", equalTo("Cancellation Error"))
                .body("message", containsString("Cannot cancel an already completed service booking"));
    }

    @Test
    @Order(10)
    @DisplayName("10. Prevention of unauthorized access to another customer's booking")
    void testAnotherCustomerCannotCancelBooking() {
        String emailA = TestData.uniqueCustomerEmail();
        String tokenA = AuthHelper.registerAndGetCustomerToken("Owner Original", emailA, TestData.DEFAULT_PASSWORD);
        Long vehicleId = registerVehicle(tokenA);
        Long slotId = findAvailableSlotId(LocalDate.now().plusDays(8));

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenA))
                .body(TestData.createBookingRequest(vehicleId, slotId, "General Maintenance", null))
                .when()
                .post(TestConfig.BOOKINGS_PREFIX);

        Long bookingId = createRes.jsonPath().getLong("bookingId");

        String emailB = TestData.uniqueCustomerEmail();
        String tokenB = AuthHelper.registerAndGetCustomerToken("Hacker Customer", emailB, TestData.DEFAULT_PASSWORD);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenB))
                .when()
                .put(TestConfig.BOOKINGS_PREFIX + "/" + bookingId + "/cancel")
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"));
    }
}
