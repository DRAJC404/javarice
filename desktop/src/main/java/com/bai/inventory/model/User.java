package com.bai.inventory.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private int organizationId = 1;
    private int roleId;
    private Role role;
    private String fullName;
    private String email;
    private String passwordHash;
    private String rememberTokenHash;
    private LocalDateTime rememberExpiresAt;
    private String approvalStatus = "APPROVED";
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(int id, int roleId, String fullName, String email, String passwordHash, boolean active) {
        this(id, 1, roleId, fullName, email, passwordHash, active ? "APPROVED" : "PENDING", active);
    }

    public User(int id, int roleId, String fullName, String email, String passwordHash, String approvalStatus, boolean active) {
        this(id, 1, roleId, fullName, email, passwordHash, approvalStatus, active);
    }

    public User(int id, int organizationId, int roleId, String fullName, String email, String passwordHash, String approvalStatus, boolean active) {
        this.id = id;
        this.organizationId = organizationId;
        this.roleId = roleId;
        this.role = Role.fromId(roleId);
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.approvalStatus = approvalStatus != null ? approvalStatus : "APPROVED";
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrganizationId() { return organizationId; }
    public void setOrganizationId(int organizationId) { this.organizationId = organizationId; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { 
        this.roleId = roleId; 
        this.role = Role.fromId(roleId);
    }

    public Role getRole() { return role != null ? role : Role.fromId(roleId); }
    public void setRole(Role role) { 
        this.role = role;
        if (role != null) this.roleId = role.getId();
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRememberTokenHash() { return rememberTokenHash; }
    public void setRememberTokenHash(String rememberTokenHash) { this.rememberTokenHash = rememberTokenHash; }

    public LocalDateTime getRememberExpiresAt() { return rememberExpiresAt; }
    public void setRememberExpiresAt(LocalDateTime rememberExpiresAt) { this.rememberExpiresAt = rememberExpiresAt; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
