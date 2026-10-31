package com.klu.microservices.auth.dto;

import com.klu.microservices.auth.entity.ApprovalStatus;
import com.klu.microservices.auth.entity.Role;
import com.klu.microservices.auth.entity.User;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private ApprovalStatus status;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String email, Role role, ApprovalStatus status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public static UserResponse fromUser(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }
}
