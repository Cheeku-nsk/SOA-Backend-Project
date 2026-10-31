package com.klu.microservices.test.config;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class TestConfig {

    public static final String BASE_URL = "http://localhost:8080";

    // Application Gateway Service Prefixes
    public static final String AUTH_PREFIX = "/auth";
    public static final String VEHICLES_PREFIX = "/vehicles";
    public static final String BOOKINGS_PREFIX = "/bookings";
    public static final String RECORDS_PREFIX = "/records";
    public static final String BILLING_PREFIX = "/billing";

    // Pre-configured Admin Credentials (from environment or application defaults)
    public static final String ADMIN_EMAIL = System.getenv("ADMIN_EMAIL") != null && !System.getenv("ADMIN_EMAIL").isBlank()
            ? System.getenv("ADMIN_EMAIL").trim()
            : "admin@precisionauto.com";

    public static final String ADMIN_PASSWORD = System.getenv("ADMIN_PASSWORD") != null && !System.getenv("ADMIN_PASSWORD").isBlank()
            ? System.getenv("ADMIN_PASSWORD").trim()
            : "Admin@PrecisionAuto2026!";

    private static Boolean gatewayOnline = null;

    /**
     * Checks if the API Gateway is online and accepting connections at BASE_URL.
     * Caches the result to avoid recurring network connection timeouts across tests.
     */
    public static synchronized boolean isGatewayOnline() {
        if (gatewayOnline != null) {
            return gatewayOnline;
        }
        try {
            URL url = URI.create(BASE_URL + AUTH_PREFIX + "/login").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.connect();
            int responseCode = conn.getResponseCode();
            gatewayOnline = (responseCode > 0);
        } catch (Exception e) {
            gatewayOnline = false;
        }
        return gatewayOnline;
    }

    public static synchronized void resetGatewayStatus() {
        gatewayOnline = null;
    }
}
