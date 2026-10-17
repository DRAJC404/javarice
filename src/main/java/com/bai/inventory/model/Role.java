package com.bai.inventory.model;

public enum Role {
    OWNER(1, "OWNER", "Business Owner"),
    ADMIN(1, "ADMIN", "Administrator"),
    MANAGER(2, "MANAGER", "Inventory Manager"),
    EMPLOYEE(3, "EMPLOYEE", "Store Associate");

    private final int id;
    private final String code;
    private final String displayName;

    Role(int id, String code, String displayName) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOwnerOrAdmin() {
        return this == OWNER || this == ADMIN;
    }

    public boolean isManagerOrAbove() {
        return this == OWNER || this == ADMIN || this == MANAGER;
    }

    public static Role fromId(int id) {
        if (id == 1) return OWNER;
        for (Role r : values()) {
            if (r.id == id) return r;
        }
        return EMPLOYEE;
    }

    public static Role fromCode(String code) {
        if (code == null) return EMPLOYEE;
        if ("ADMIN".equalsIgnoreCase(code) || "OWNER".equalsIgnoreCase(code)) return OWNER;
        for (Role r : values()) {
            if (r.code.equalsIgnoreCase(code)) return r;
        }
        return EMPLOYEE;
    }
}
