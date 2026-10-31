package com.klu.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    private static final List<String> PROTECTED_PREFIXES = List.of(
            "/bookings",
            "/records",
            "/billing",
            "/vehicles",
            "/auth/technicians"
    );

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Allow HTTP OPTIONS pre-flight requests to pass through for CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 2. Check if the route is protected
        boolean isProtected = PROTECTED_PREFIXES.stream().anyMatch(path::startsWith);
        if (!isProtected) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract Authorization header (case-insensitive)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            authHeader = request.getHeader("authorization");
        }
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            sendUnauthorized(response, "Missing or invalid JWT token");
            return;
        }

        String token = authHeader.substring(7).trim();
        Claims claims;
        try {
            // 4. Validate JWT
            claims = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            sendUnauthorized(response, "Missing or invalid JWT token");
            return;
        }

        // 5. Extract claims
        String email = claims.getSubject();
        Object userIdObj = claims.get("userId");
        Long userId = (userIdObj instanceof Number) ? ((Number) userIdObj).longValue() : null;
        String role = claims.get("role", String.class);

        // 6. Wrap request to inject trusted headers and strip spoofable headers
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
            private final Map<String, String> customHeaders = new HashMap<>();

            {
                if (userId != null) customHeaders.put("X-User-Id", String.valueOf(userId));
                if (email != null) customHeaders.put("X-User-Email", email);
                if (role != null) customHeaders.put("X-User-Role", role);
            }

            private String findCustomHeader(String name) {
                if (name == null) return null;
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(name)) {
                        return entry.getValue();
                    }
                }
                return null;
            }

            @Override
            public String getHeader(String name) {
                String customVal = findCustomHeader(name);
                if (customVal != null) {
                    return customVal;
                }
                if (name != null && (name.equalsIgnoreCase("X-User-Id") ||
                    name.equalsIgnoreCase("X-User-Email") ||
                    name.equalsIgnoreCase("X-User-Role"))) {
                    return null;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                String customVal = findCustomHeader(name);
                if (customVal != null) {
                    return Collections.enumeration(Collections.singletonList(customVal));
                }
                if (name != null && (name.equalsIgnoreCase("X-User-Id") ||
                    name.equalsIgnoreCase("X-User-Email") ||
                    name.equalsIgnoreCase("X-User-Role"))) {
                    return Collections.emptyEnumeration();
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Set<String> headerNames = new HashSet<>();
                Enumeration<String> originalHeaderNames = super.getHeaderNames();
                while (originalHeaderNames != null && originalHeaderNames.hasMoreElements()) {
                    String originalName = originalHeaderNames.nextElement();
                    if (!originalName.equalsIgnoreCase("X-User-Id") &&
                        !originalName.equalsIgnoreCase("X-User-Email") &&
                        !originalName.equalsIgnoreCase("X-User-Role")) {
                        headerNames.add(originalName);
                    }
                }
                headerNames.addAll(customHeaders.keySet());
                return Collections.enumeration(headerNames);
            }
        };

        filterChain.doFilter(wrappedRequest, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message));
    }
}
