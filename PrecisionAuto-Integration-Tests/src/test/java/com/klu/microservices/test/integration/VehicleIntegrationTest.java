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

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("PrecisionAuto Care — Vehicle Management Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VehicleIntegrationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. Add a vehicle as an authenticated customer")
    void testAddVehicleAsCustomer() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Vehicle Owner", email, TestData.DEFAULT_PASSWORD);
        String regNumber = TestData.uniqueRegistrationNumber();

        Map<String, Object> req = TestData.createVehicleRequest(regNumber);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(req)
                .when()
                .post(TestConfig.VEHICLES_PREFIX)
                .then()
                .statusCode(201)
                .body("vehicleId", notNullValue())
                .body("registrationNumber", equalTo(regNumber.toUpperCase()))
                .body("make", equalTo(req.get("make")))
                .body("model", equalTo(req.get("model")))
                .body("year", equalTo(req.get("year")))
                .body("customerEmail", equalTo(email));
    }

    @Test
    @Order(2)
    @DisplayName("2. Retrieve customer's vehicles list")
    void testGetCustomerVehicles() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Multi Owner", email, TestData.DEFAULT_PASSWORD);
        String regNumber = TestData.uniqueRegistrationNumber();

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX)
                .then()
                .statusCode(201);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .get(TestConfig.VEHICLES_PREFIX)
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("registrationNumber", hasItem(regNumber.toUpperCase()));
    }

    @Test
    @Order(3)
    @DisplayName("3. Update existing vehicle details as owner")
    void testUpdateVehicleDetails() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Update Owner", email, TestData.DEFAULT_PASSWORD);
        String regNumber = TestData.uniqueRegistrationNumber();

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX);

        Long vehicleId = createRes.jsonPath().getLong("vehicleId");

        Map<String, Object> updateReq = TestData.createVehicleRequest(regNumber);
        updateReq.put("make", "Toyota Updated");
        updateReq.put("model", "Camry Hybrid");
        updateReq.put("year", 2023);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(updateReq)
                .when()
                .put(TestConfig.VEHICLES_PREFIX + "/" + vehicleId)
                .then()
                .statusCode(200)
                .body("vehicleId", equalTo(vehicleId.intValue()))
                .body("make", equalTo("Toyota Updated"))
                .body("model", equalTo("Camry Hybrid"))
                .body("year", equalTo(2023));
    }

    @Test
    @Order(4)
    @DisplayName("4. Delete vehicle without active bookings")
    void testDeleteVehicle() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Delete Owner", email, TestData.DEFAULT_PASSWORD);
        String regNumber = TestData.uniqueRegistrationNumber();

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX);

        Long vehicleId = createRes.jsonPath().getLong("vehicleId");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .when()
                .delete(TestConfig.VEHICLES_PREFIX + "/" + vehicleId)
                .then()
                .statusCode(200)
                .body("message", containsString("successfully removed"))
                .body("vehicleId", equalTo(vehicleId.intValue()));
    }

    @Test
    @Order(5)
    @DisplayName("5. Customer cross-access prevention: Customer B cannot modify or delete Customer A's vehicle")
    void testCustomerCannotAccessAnotherCustomerVehicle() {
        // Customer A registers a vehicle
        String emailA = TestData.uniqueCustomerEmail();
        String tokenA = AuthHelper.registerAndGetCustomerToken("Owner A", emailA, TestData.DEFAULT_PASSWORD);
        String regNumberA = TestData.uniqueRegistrationNumber();

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenA))
                .body(TestData.createVehicleRequest(regNumberA))
                .when()
                .post(TestConfig.VEHICLES_PREFIX);

        Long vehicleIdA = createRes.jsonPath().getLong("vehicleId");

        // Customer B attempts unauthorized update and delete
        String emailB = TestData.uniqueCustomerEmail();
        String tokenB = AuthHelper.registerAndGetCustomerToken("Owner B", emailB, TestData.DEFAULT_PASSWORD);

        Map<String, Object> hackerUpdate = TestData.createVehicleRequest(regNumberA);
        hackerUpdate.put("make", "Hijacked");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenB))
                .body(hackerUpdate)
                .when()
                .put(TestConfig.VEHICLES_PREFIX + "/" + vehicleIdA)
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("permission to modify"));

        given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenB))
                .when()
                .delete(TestConfig.VEHICLES_PREFIX + "/" + vehicleIdA)
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("permission to delete"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Technicians cannot modify customer vehicle ownership or details")
    void testTechnicianCannotModifyVehicle() {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techToken = AuthHelper.registerAndApproveTechnician("Shop Tech", techEmail, TestData.DEFAULT_PASSWORD, adminToken);

        String emailA = TestData.uniqueCustomerEmail();
        String tokenA = AuthHelper.registerAndGetCustomerToken("Owner C", emailA, TestData.DEFAULT_PASSWORD);
        String regNumberA = TestData.uniqueRegistrationNumber();

        Response createRes = given()
                .spec(RestAssuredConfig.authenticatedRequest(tokenA))
                .body(TestData.createVehicleRequest(regNumberA))
                .when()
                .post(TestConfig.VEHICLES_PREFIX);

        Long vehicleId = createRes.jsonPath().getLong("vehicleId");

        Map<String, Object> update = TestData.createVehicleRequest(regNumberA);
        update.put("make", "Tech Modified");

        given()
                .spec(RestAssuredConfig.authenticatedRequest(techToken))
                .body(update)
                .when()
                .put(TestConfig.VEHICLES_PREFIX + "/" + vehicleId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(7)
    @DisplayName("7. Duplicate vehicle registration number rejection")
    void testDuplicateRegistrationNumberRejected() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Dup Owner", email, TestData.DEFAULT_PASSWORD);
        String regNumber = TestData.uniqueRegistrationNumber();

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX)
                .then()
                .statusCode(201);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(TestData.createVehicleRequest(regNumber))
                .when()
                .post(TestConfig.VEHICLES_PREFIX)
                .then()
                .statusCode(400)
                .body("error", equalTo("Validation Error"))
                .body("message", containsString("already registered"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Invalid manufacturing year validation")
    void testInvalidManufacturingYearRejected() {
        String email = TestData.uniqueCustomerEmail();
        String token = AuthHelper.registerAndGetCustomerToken("Year Owner", email, TestData.DEFAULT_PASSWORD);

        Map<String, Object> req = TestData.createVehicleRequest(TestData.uniqueRegistrationNumber());
        req.put("year", 1850); // Prior to 1900

        given()
                .spec(RestAssuredConfig.authenticatedRequest(token))
                .body(req)
                .when()
                .post(TestConfig.VEHICLES_PREFIX)
                .then()
                .statusCode(400);
    }
}
