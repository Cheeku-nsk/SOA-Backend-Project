package com.klu.microservices.auth.controller;

import com.klu.microservices.auth.dto.AuthResponse;
import com.klu.microservices.auth.dto.LoginRequest;
import com.klu.microservices.auth.dto.RegisterRequest;
import com.klu.microservices.auth.dto.UserResponse;
import com.klu.microservices.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Registration Failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication Failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/technicians/pending")
    public ResponseEntity<?> getPendingTechnicians(
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        if (!isAdmin(headerRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Access restricted to administrators"));
        }
        try {
            List<UserResponse> pending = authService.getPendingTechnicians();
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve pending technicians", "message", e.getMessage()));
        }
    }

    @GetMapping("/technicians")
    public ResponseEntity<?> getAllTechnicians(
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        if (!isAdmin(headerRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Access restricted to administrators"));
        }
        try {
            List<UserResponse> technicians = authService.getAllTechnicians();
            return ResponseEntity.ok(technicians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve technicians", "message", e.getMessage()));
        }
    }

    @PutMapping("/technicians/{id}/approve")
    public ResponseEntity<?> approveTechnician(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        if (!isAdmin(headerRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Access restricted to administrators"));
        }
        try {
            UserResponse response = authService.approveTechnician(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Approval Failed", "message", e.getMessage()));
        }
    }

    @PutMapping("/technicians/{id}/reject")
    public ResponseEntity<?> rejectTechnician(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole) {
        if (!isAdmin(headerRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "message", "Access restricted to administrators"));
        }
        try {
            UserResponse response = authService.rejectTechnician(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Rejection Failed", "message", e.getMessage()));
        }
    }

    private boolean isAdmin(String headerRole) {
        if ("ADMIN".equalsIgnoreCase(headerRole)) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        }
        return false;
    }
}
