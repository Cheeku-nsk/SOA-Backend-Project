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

    private static final List<String> PROTECTED_PREFIXES = List.of("/bookings", "/records", "/billing");

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
  
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Check if the route is protected
        boolean isProtected = PROTECTED_PREFIXES.stream().anyMatch(path::startsWith);
        if (!isProtected) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid JWT token");
            return;
        }

        String token = authHeader.substring(7);
        try {
            // 3. Validate JWT
            Claims claims = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 4. Extract claims
            String email = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            // 5. Wrap request to inject trusted headers and strip spoofable headers
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                private final Map<String, String> customHeaders = new HashMap<>();

                {
                    if (userId != null) customHeaders.put("X-User-Id", String.valueOf(userId));
                    if (email != null) customHeaders.put("X-User-Email", email);
                    if (role != null) customHeaders.put("X-User-Role", role);
                }

                @Override
                public String getHeader(String name) {
                    if (customHeaders.containsKey(name)) {
                        return customHeaders.get(name);
                    }
                    if (name.equalsIgnoreCase("X-User-Id") || 
                        name.equalsIgnoreCase("X-User-Email") || 
                        name.equalsIgnoreCase("X-User-Role")) {
                        return null;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (customHeaders.containsKey(name)) {
                        return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
                    }
                    if (name.equalsIgnoreCase("X-User-Id") || 
                        name.equalsIgnoreCase("X-User-Email") || 
                        name.equalsIgnoreCase("X-User-Role")) {
                        return Collections.emptyEnumeration();
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> headerNames = new HashSet<>();
                    Enumeration<String> originalHeaderNames = super.getHeaderNames();
                    while (originalHeaderNames.hasMoreElements()) {
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

        } catch (Exception e) {
            // Invalid, malformed, or expired token
            sendUnauthorized(response, "Missing or invalid JWT token");
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message));
    }
}
