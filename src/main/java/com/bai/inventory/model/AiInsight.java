package com.bai.inventory.model;

import java.time.LocalDateTime;

public class AiInsight {
    private int id;
    private String category;
    private String title;
    private String description;
    private String severity; 
    private String metricValue;
    private String actionLabel;
    private String actionCommand;
    private boolean resolved;
    private LocalDateTime createdAt;

    public AiInsight() {}

    public AiInsight(int id, String category, String title, String description, String severity,
                     String metricValue, String actionLabel, String actionCommand, boolean resolved, LocalDateTime createdAt) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.metricValue = metricValue;
        this.actionLabel = actionLabel;
        this.actionCommand = actionCommand;
        this.resolved = resolved;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMetricValue() { return metricValue; }
    public void setMetricValue(String metricValue) { this.metricValue = metricValue; }

    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }

    public String getActionCommand() { return actionCommand; }
    public void setActionCommand(String actionCommand) { this.actionCommand = actionCommand; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
