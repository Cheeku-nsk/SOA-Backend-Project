package com.klu.microservices.test.utils;

import com.klu.microservices.test.config.RestAssuredConfig;
import com.klu.microservices.test.config.TestConfig;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AuthHelper {

    /**
     * Executes login via POST /auth/login through the API Gateway.
     */
    public static Response login(String email, String password, String role) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        if (role != null && !role.isBlank()) {
            body.put("role", role);
        }

        return given()
                .spec(RestAssuredConfig.baseRequest())
                .body(body)
                .when()
                .post(TestConfig.AUTH_PREFIX + "/login");
    }

    /**
     * Performs login and returns the extracted JWT token.
     */
    public static String loginAndGetToken(String email, String password, String role) {
        Response response = login(email, password, role);
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Login failed with status " + response.getStatusCode() + ": " + response.getBody().asString());
        }
        String token = response.jsonPath().getString("token");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Authentication response did not contain a valid JWT token");
        }
        return token;
    }

    /**
     * Obtains the JWT token for the pre-configured system administrator account.
     */
    public static String getAdminToken() {
        return loginAndGetToken(TestConfig.ADMIN_EMAIL, TestConfig.ADMIN_PASSWORD, "ADMIN");
    }

    /**
     * Registers a new customer via POST /auth/register.
     */
    public static Response registerCustomer(String name, String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("role", "CUSTOMER");

        return given()
                .spec(RestAssuredConfig.baseRequest())
                .body(body)
                .when()
                .post(TestConfig.AUTH_PREFIX + "/register");
    }

    /**
     * Registers a customer and returns the authenticated JWT token.
     */
    public static String registerAndGetCustomerToken(String name, String email, String password) {
        Response regResponse = registerCustomer(name, email, password);
        if (regResponse.getStatusCode() == 200) {
            String token = regResponse.jsonPath().getString("token");
            if (token != null && !token.isBlank()) {
                return token;
            }
        }
        // Fallback to login if registration response didn't include immediate token
        return loginAndGetToken(email, password, "CUSTOMER");
    }

    /**
     * Registers a new technician via POST /auth/register.
     */
    public static Response registerTechnician(String name, String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("role", "TECHNICIAN");

        return given()
                .spec(RestAssuredConfig.baseRequest())
                .body(body)
                .when()
                .post(TestConfig.AUTH_PREFIX + "/register");
    }

    /**
     * Approves a pending technician via admin PUT /auth/technicians/{id}/approve.
     */
    public static Response approveTechnician(Long technicianId, String adminToken) {
        return given()
                .spec(RestAssuredConfig.authenticatedRequest(adminToken))
                .when()
                .put(TestConfig.AUTH_PREFIX + "/technicians/" + technicianId + "/approve");
    }

    /**
     * Full setup helper: registers a technician, uses admin token to approve, and logs in.
     */
    public static String registerAndApproveTechnician(String name, String email, String password, String adminToken) {
        Response regRes = registerTechnician(name, email, password);
        if (regRes.getStatusCode() != 200) {
            throw new IllegalStateException("Technician registration failed: " + regRes.getBody().asString());
        }

        Long techId = regRes.jsonPath().getLong("userId");
        if (techId == null) {
            throw new IllegalStateException("Technician registration response did not return a valid userId");
        }

        Response approveRes = approveTechnician(techId, adminToken);
        if (approveRes.getStatusCode() != 200) {
            throw new IllegalStateException("Technician approval failed: " + approveRes.getBody().asString());
        }

        return loginAndGetToken(email, password, "TECHNICIAN");
    }
}
