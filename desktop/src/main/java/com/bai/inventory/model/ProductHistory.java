package com.bai.inventory.model;

import java.time.LocalDateTime;

public class ProductHistory {
    private int historyId;
    private int productId;
    private String action;
    private String field;
    private String oldValue;
    private String newValue;
    private Integer userId;
    private String userName;
    private LocalDateTime date;

    public ProductHistory() {
        this.date = LocalDateTime.now();
    }

    public ProductHistory(int historyId, int productId, String action, String field,
                          String oldValue, String newValue, Integer userId, LocalDateTime date) {
        this.historyId = historyId;
        this.productId = productId;
        this.action = action;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.userId = userId;
        this.date = date != null ? date : LocalDateTime.now();
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
