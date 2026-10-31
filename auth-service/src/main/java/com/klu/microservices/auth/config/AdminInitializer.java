package com.klu.microservices.auth.config;

import com.klu.microservices.auth.entity.ApprovalStatus;
import com.klu.microservices.auth.entity.Role;
import com.klu.microservices.auth.entity.User;
import com.klu.microservices.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:${ADMIN_EMAIL:admin@precisionauto.com}}")
    private String adminEmail;

    @Value("${app.admin.password:${ADMIN_PASSWORD:Admin@PrecisionAuto2026!}}")
    private String adminPassword;

    @Value("${app.admin.name:${ADMIN_NAME:System Administrator}}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN) && !userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setStatus(ApprovalStatus.APPROVED);
            userRepository.save(admin);
            logger.info("Default administrator account initialized successfully for email: {}", adminEmail);
        } else {
            logger.info("Administrator account already exists in database.");
        }
    }
}
