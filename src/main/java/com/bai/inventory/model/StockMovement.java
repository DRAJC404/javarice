package com.bai.inventory.model;

import java.time.LocalDateTime;

public class StockMovement {
    private int id;
    private int organizationId;
    private String referenceNo;
    private int productId;
    private String productName;
    private String productSku;
    private int userId;
    private String userName;
    private String movementType; 
    private int quantity;
    private String reason;       
    private int previousStock;
    private int newStock;
    private Integer undoneByMovementId;
    private String notes;
    private LocalDateTime createdAt;

    public StockMovement() {}

    public StockMovement(int id, String referenceNo, int productId, int userId,
                         String movementType, int quantity, String reason,
                         int previousStock, int newStock, String notes, LocalDateTime createdAt) {
        this(id, 1, referenceNo, productId, userId, movementType, quantity, reason, previousStock, newStock, notes, createdAt);
    }

    public StockMovement(int id, int organizationId, String referenceNo, int productId, int userId,
                         String movementType, int quantity, String reason,
                         int previousStock, int newStock, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.referenceNo = referenceNo;
        this.productId = productId;
        this.userId = userId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.reason = reason;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrganizationId() { return organizationId; }
    public void setOrganizationId(int organizationId) { this.organizationId = organizationId; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getPreviousStock() { return previousStock; }
    public void setPreviousStock(int previousStock) { this.previousStock = previousStock; }

    public int getNewStock() { return newStock; }
    public void setNewStock(int newStock) { this.newStock = newStock; }

    public Integer getUndoneByMovementId() { return undoneByMovementId; }
    public void setUndoneByMovementId(Integer undoneByMovementId) { this.undoneByMovementId = undoneByMovementId; }

    public boolean isUndone() { return undoneByMovementId != null; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
