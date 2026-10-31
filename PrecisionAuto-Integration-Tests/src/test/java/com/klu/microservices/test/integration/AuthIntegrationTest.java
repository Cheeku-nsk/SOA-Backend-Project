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

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("PrecisionAuto Care — Authentication & Authorization Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("1. Customer login with valid credentials")
    void testCustomerLoginValidCredentials() {
        String email = TestData.uniqueCustomerEmail();
        String password = TestData.DEFAULT_PASSWORD;
        String name = "Valid Customer";

        AuthHelper.registerCustomer(name, email, password)
                .then()
                .statusCode(200)
                .body("userId", notNullValue())
                .body("role", equalTo("CUSTOMER"))
                .body("status", equalTo("APPROVED"));

        Response loginResponse = AuthHelper.login(email, password, "CUSTOMER");
        loginResponse.then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("token", not(emptyOrNullString()))
                .body("email", equalTo(email))
                .body("role", equalTo("CUSTOMER"))
                .body("status", equalTo("APPROVED"))
                .body("message", equalTo("Login successful"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Customer login with invalid credentials rejection")
    void testCustomerLoginInvalidCredentials() {
        String email = TestData.uniqueCustomerEmail();
        String password = TestData.DEFAULT_PASSWORD;
        AuthHelper.registerCustomer("Test User", email, password);

        Response loginResponse = AuthHelper.login(email, "WrongPassword999!", "CUSTOMER");
        loginResponse.then()
                .statusCode(401)
                .body("error", equalTo("Authentication Failed"))
                .body("message", equalTo("Invalid email or password"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Administrator login with pre-configured admin account")
    void testAdminLoginWithConfiguredAccount() {
        Response response = AuthHelper.login(TestConfig.ADMIN_EMAIL, TestConfig.ADMIN_PASSWORD, "ADMIN");
        response.then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("email", equalTo(TestConfig.ADMIN_EMAIL))
                .body("role", equalTo("ADMIN"))
                .body("status", equalTo("APPROVED"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Approved technician workflow and successful login")
    void testApprovedTechnicianLogin() {
        String adminToken = AuthHelper.getAdminToken();
        String techEmail = TestData.uniqueTechnicianEmail();
        String techPassword = TestData.DEFAULT_PASSWORD;
        String techName = "Certified Technician";

        Response regResponse = AuthHelper.registerTechnician(techName, techEmail, techPassword);
        regResponse.then()
                .statusCode(200)
                .body("userId", notNullValue())
                .body("role", equalTo("TECHNICIAN"))
                .body("status", equalTo("PENDING_APPROVAL"));

        Long techId = regResponse.jsonPath().getLong("userId");

        AuthHelper.approveTechnician(techId, adminToken)
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        Response loginResponse = AuthHelper.login(techEmail, techPassword, "TECHNICIAN");
        loginResponse.then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("role", equalTo("TECHNICIAN"))
                .body("status", equalTo("APPROVED"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Pending technician login is rejected prior to admin approval")
    void testPendingTechnicianLoginRejected() {
        String techEmail = TestData.uniqueTechnicianEmail();
        String techPassword = TestData.DEFAULT_PASSWORD;

        AuthHelper.registerTechnician("Pending Tech", techEmail, techPassword)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING_APPROVAL"));

        Response loginResponse = AuthHelper.login(techEmail, techPassword, "TECHNICIAN");
        loginResponse.then()
                .statusCode(401)
                .body("error", equalTo("Authentication Failed"))
                .body("message", containsString("pending administrator approval"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Missing authentication credentials validation")
    void testMissingCredentialsRejected() {
        Map<String, String> emptyRequest = new HashMap<>();
        emptyRequest.put("email", "");
        emptyRequest.put("password", "");

        given()
                .spec(RestAssuredConfig.baseRequest())
                .body(emptyRequest)
                .when()
                .post(TestConfig.AUTH_PREFIX + "/login")
                .then()
                .statusCode(anyOf(is(400), is(401)));
    }

    @Test
    @Order(7)
    @DisplayName("7. Access to protected microservice endpoints without JWT is rejected by API Gateway")
    void testAccessProtectedEndpointsWithoutToken() {
        String[] protectedEndpoints = {
                TestConfig.VEHICLES_PREFIX,
                TestConfig.BOOKINGS_PREFIX + "/my",
                TestConfig.RECORDS_PREFIX + "/customer",
                TestConfig.BILLING_PREFIX + "/my-invoices",
                TestConfig.AUTH_PREFIX + "/technicians"
        };

        for (String endpoint : protectedEndpoints) {
            given()
                    .spec(RestAssuredConfig.baseRequest())
                    .when()
                    .get(endpoint)
                    .then()
                    .statusCode(401)
                    .body("error", equalTo("Unauthorized"))
                    .body("message", equalTo("Missing or invalid JWT token"));
        }
    }

    @Test
    @Order(8)
    @DisplayName("8. Access to protected endpoints with invalid or malformed JWT is rejected")
    void testAccessWithInvalidJwt() {
        given()
                .spec(RestAssuredConfig.baseRequest())
                .header("Authorization", "Bearer invalid.malformed.jwt.token.string")
                .when()
                .get(TestConfig.VEHICLES_PREFIX)
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid JWT token"));
    }

    @Test
    @Order(9)
    @DisplayName("9. Role protection: public registration as ADMIN is forbidden")
    void testPublicAdminRegistrationForbidden() {
        Map<String, String> adminReg = new HashMap<>();
        adminReg.put("name", "Fake Admin");
        adminReg.put("email", TestData.uniqueEmail("fakeadmin"));
        adminReg.put("password", TestData.DEFAULT_PASSWORD);
        adminReg.put("role", "ADMIN");

        given()
                .spec(RestAssuredConfig.baseRequest())
                .body(adminReg)
                .when()
                .post(TestConfig.AUTH_PREFIX + "/register")
                .then()
                .statusCode(400)
                .body("error", equalTo("Registration Failed"))
                .body("message", equalTo("Public registration as ADMIN is not permitted"));
    }

    @Test
    @Order(10)
    @DisplayName("10. Role protection: customers cannot access administrator technician approval endpoints")
    void testCustomerCannotApproveTechnicians() {
        String customerEmail = TestData.uniqueCustomerEmail();
        String customerToken = AuthHelper.registerAndGetCustomerToken("Customer Only", customerEmail, TestData.DEFAULT_PASSWORD);

        given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .get(TestConfig.AUTH_PREFIX + "/technicians")
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"))
                .body("message", containsString("restricted to administrators"));

        given()
                .spec(RestAssuredConfig.authenticatedRequest(customerToken))
                .when()
                .put(TestConfig.AUTH_PREFIX + "/technicians/1/approve")
                .then()
                .statusCode(403)
                .body("error", equalTo("Forbidden"));
    }
}
