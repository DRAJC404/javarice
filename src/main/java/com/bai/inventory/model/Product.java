package com.bai.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    private int id;
    private int organizationId = 1;
    private Integer categoryId;
    private String categoryName;
    private String sku;
    private String barcode;
    private String name;
    private String unit = "pcs";
    private BigDecimal costPrice = BigDecimal.ZERO;
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    private int stockQuantity;
    private int lowStockThreshold = 10;
    private String imagePath;
    private String status = "IN_STOCK";
    private boolean isArchived = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(int id, Integer categoryId, String sku, String barcode, String name, String unit,
                   BigDecimal costPrice, BigDecimal sellingPrice, int stockQuantity,
                   int lowStockThreshold, String imagePath, String status) {
        this(id, 1, categoryId, sku, barcode, name, unit, costPrice, sellingPrice, stockQuantity, lowStockThreshold, imagePath, status, false);
    }

    public Product(int id, int organizationId, Integer categoryId, String sku, String barcode, String name, String unit,
                   BigDecimal costPrice, BigDecimal sellingPrice, int stockQuantity,
                   int lowStockThreshold, String imagePath, String status, boolean isArchived) {
        this.id = id;
        this.organizationId = organizationId;
        this.categoryId = categoryId;
        this.sku = sku;
        this.barcode = barcode;
        this.name = name;
        this.unit = unit;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
        this.lowStockThreshold = lowStockThreshold;
        this.imagePath = imagePath;
        this.status = status;
        this.isArchived = isArchived;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { 
        this.stockQuantity = stockQuantity; 
        updateComputedStatus();
    }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { 
        this.lowStockThreshold = lowStockThreshold; 
        updateComputedStatus();
    }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void updateComputedStatus() {
        if (stockQuantity <= 0) {
            this.status = "OUT_OF_STOCK";
        } else if (stockQuantity <= lowStockThreshold) {
            this.status = "LOW_STOCK";
        } else {
            this.status = "IN_STOCK";
        }
    }

    public boolean isInStock() {
        return !isOutOfStock() && !isLowStock();
    }

    public boolean isOutOfStock() {
        return "OUT_OF_STOCK".equalsIgnoreCase(status) || stockQuantity <= 0;
    }

    public boolean isLowStock() {
        return "LOW_STOCK".equalsIgnoreCase(status) || (stockQuantity > 0 && stockQuantity <= lowStockThreshold);
    }

    public int getOrganizationId() { return organizationId; }
    public void setOrganizationId(int organizationId) { this.organizationId = organizationId; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }
}
