package com.klu.microservices.auth.service;

import com.klu.microservices.auth.dto.AuthResponse;
import com.klu.microservices.auth.dto.LoginRequest;
import com.klu.microservices.auth.dto.RegisterRequest;
import com.klu.microservices.auth.entity.Role;
import com.klu.microservices.auth.entity.User;
import com.klu.microservices.auth.repository.UserRepository;
import com.klu.microservices.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER); // Default role

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().name());

        return new AuthResponse(token, savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name());
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

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
