package com.klu.microservices.auth.repository;

import com.klu.microservices.auth.entity.ApprovalStatus;
import com.klu.microservices.auth.entity.Role;
import com.klu.microservices.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    List<User> findByRole(Role role);
    List<User> findByRoleAndStatus(Role role, ApprovalStatus status);
}
