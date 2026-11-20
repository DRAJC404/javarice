package com.bai.inventory.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CalendarEvent {

    public static final String TYPE_RESTOCK = "RESTOCK";
    public static final String TYPE_AUDIT = "AUDIT";
    public static final String TYPE_PROMOTION = "PROMOTION";
    public static final String TYPE_EXPIRY = "EXPIRY";
    public static final String TYPE_GENERAL = "GENERAL";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private int id;
    private int organizationId;
    private String title;
    private String eventType = TYPE_GENERAL;
    private LocalDate eventDate;
    private String eventTime;
    private String description;
    private Integer createdBy;
    private String creatorName;
    private LocalDateTime createdAt;

    public CalendarEvent() {
    }

    public CalendarEvent(String title, String eventType, LocalDate eventDate) {
        this.title = title;
        this.eventType = eventType;
        this.eventDate = eventDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEventType() {
        return eventType != null ? eventType : TYPE_GENERAL;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedDate() {
        return eventDate != null ? eventDate.format(DATE_FMT) : "";
    }

    public String getTypeDisplayName() {
        if (eventType == null) return "General";
        return switch (eventType.toUpperCase()) {
            case TYPE_RESTOCK -> "Restock & Delivery";
            case TYPE_AUDIT -> "Inventory Audit";
            case TYPE_PROMOTION -> "Promotion / Sale";
            case TYPE_EXPIRY -> "Expiry Check";
            default -> "General Note";
        };
    }

    public String getTypeBgColor() {
        if (eventType == null) return "#F1F5F9";
        return switch (eventType.toUpperCase()) {
            case TYPE_RESTOCK -> "#ECFDF5";
            case TYPE_AUDIT -> "#F3E8FF";
            case TYPE_PROMOTION -> "#EFF6FF";
            case TYPE_EXPIRY -> "#FEF2F2";
            default -> "#F1F5F9";
        };
    }

    public String getTypeTextColor() {
        if (eventType == null) return "#475569";
        return switch (eventType.toUpperCase()) {
            case TYPE_RESTOCK -> "#065F46";
            case TYPE_AUDIT -> "#6B21A8";
            case TYPE_PROMOTION -> "#1E40AF";
            case TYPE_EXPIRY -> "#991B1B";
            default -> "#334155";
        };
    }

    public String getTypeBorderColor() {
        if (eventType == null) return "#CBD5E1";
        return switch (eventType.toUpperCase()) {
            case TYPE_RESTOCK -> "#A7F3D0";
            case TYPE_AUDIT -> "#E9D5FF";
            case TYPE_PROMOTION -> "#BFDBFE";
            case TYPE_EXPIRY -> "#FECACA";
            default -> "#CBD5E1";
        };
    }
}
