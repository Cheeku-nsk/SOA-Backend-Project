package com.klu.microservices.auth.service;

import com.klu.microservices.auth.dto.AuthResponse;
import com.klu.microservices.auth.dto.LoginRequest;
import com.klu.microservices.auth.dto.RegisterRequest;
import com.klu.microservices.auth.dto.UserResponse;
import com.klu.microservices.auth.entity.ApprovalStatus;
import com.klu.microservices.auth.entity.Role;
import com.klu.microservices.auth.entity.User;
import com.klu.microservices.auth.repository.UserRepository;
import com.klu.microservices.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        Role assignedRole = Role.CUSTOMER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String roleUpper = request.getRole().trim().toUpperCase();
            if (roleUpper.equals("ADMIN")) {
                throw new RuntimeException("Public registration as ADMIN is not permitted");
            } else if (roleUpper.equals("TECHNICIAN")) {
                assignedRole = Role.TECHNICIAN;
            } else if (roleUpper.equals("CUSTOMER")) {
                assignedRole = Role.CUSTOMER;
            } else {
                throw new RuntimeException("Invalid role specified: " + request.getRole());
            }
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(assignedRole);

        if (assignedRole == Role.TECHNICIAN) {
            user.setStatus(ApprovalStatus.PENDING_APPROVAL);
            User savedUser = userRepository.save(user);
            return new AuthResponse(
                    null,
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getEmail(),
                    savedUser.getRole().name(),
                    savedUser.getStatus().name(),
                    "Your technician registration request has been submitted. Please wait for administrator approval."
            );
        } else {
            user.setStatus(ApprovalStatus.APPROVED);
            User savedUser = userRepository.save(user);
            String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().name());
            return new AuthResponse(
                    token,
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getEmail(),
                    savedUser.getRole().name(),
                    savedUser.getStatus().name(),
                    "Customer registration successful"
            );
        }
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Validate requested role against server-stored role
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String requestedRole = request.getRole().trim().toUpperCase();
            if (!requestedRole.equalsIgnoreCase(user.getRole().name())) {
                throw new RuntimeException("Role mismatch: This account is registered as " + user.getRole().name() + ", not " + requestedRole + ".");
            }
        }

        // Validate approval status for technicians
        if (user.getRole() == Role.TECHNICIAN) {
            if (user.getStatus() == ApprovalStatus.PENDING_APPROVAL) {
                throw new RuntimeException("Your technician registration is pending administrator approval.");
            } else if (user.getStatus() == ApprovalStatus.REJECTED) {
                throw new RuntimeException("Your technician registration request was rejected by an administrator.");
            }
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                "Login successful"
        );
    }

    public List<UserResponse> getPendingTechnicians() {
        return userRepository.findByRoleAndStatus(Role.TECHNICIAN, ApprovalStatus.PENDING_APPROVAL)
                .stream()
                .map(UserResponse::fromUser)
                .toList();
    }

    public List<UserResponse> getAllTechnicians() {
        return userRepository.findByRole(Role.TECHNICIAN)
                .stream()
                .map(UserResponse::fromUser)
                .toList();
    }

    public UserResponse approveTechnician(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Technician not found with ID: " + id));
        if (user.getRole() != Role.TECHNICIAN) {
            throw new RuntimeException("User is not a technician");
        }
        user.setStatus(ApprovalStatus.APPROVED);
        User saved = userRepository.save(user);
        return UserResponse.fromUser(saved);
    }

    public UserResponse rejectTechnician(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Technician not found with ID: " + id));
        if (user.getRole() != Role.TECHNICIAN) {
            throw new RuntimeException("User is not a technician");
        }
        user.setStatus(ApprovalStatus.REJECTED);
        User saved = userRepository.save(user);
        return UserResponse.fromUser(saved);
    }
}
