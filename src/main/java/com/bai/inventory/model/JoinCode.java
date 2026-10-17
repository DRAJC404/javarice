package com.bai.inventory.model;

import java.time.LocalDateTime;

public class JoinCode {
    private int id;
    private int organizationId;
    private String codeHash;
    private String role; 
    private int createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private Integer usedBy;
    private String usedByName;
    private LocalDateTime cancelledAt;

    public JoinCode() {}

    public JoinCode(int id, int organizationId, String codeHash, String role, int createdBy,
                    LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime usedAt,
                    Integer usedBy, LocalDateTime cancelledAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.codeHash = codeHash;
        this.role = role;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.usedBy = usedBy;
        this.cancelledAt = cancelledAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrganizationId() { return organizationId; }
    public void setOrganizationId(int organizationId) { this.organizationId = organizationId; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public Integer getUsedBy() { return usedBy; }
    public void setUsedBy(Integer usedBy) { this.usedBy = usedBy; }

    public String getUsedByName() { return usedByName; }
    public void setUsedByName(String usedByName) { this.usedByName = usedByName; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return !isUsed() && !isCancelled() && !isExpired();
    }

    public String getStatusString() {
        if (isUsed()) return "USED";
        if (isCancelled()) return "CANCELLED";
        if (isExpired()) return "EXPIRED";
        return "PENDING";
    }
}
