package com.bai.inventory.model;

import com.bai.inventory.util.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductSubmission {

    public record FieldDiff(String fieldName, String label, String oldValue, String newValue) {}

    private int id;
    private int organizationId;
    private Integer productId;
    private int submittedBy;
    private String submitterName;
    private String submitterRole;
    private String submissionType; 
    private String status;         
    private String rejectionReason;
    private Integer reviewedBy;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    private String name;
    private Integer categoryId;
    private String categoryName;
    private String sku;
    private String barcode;
    private BigDecimal costPrice = BigDecimal.ZERO;
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    private int stockQuantity;
    private int lowStockThreshold = 10;
    private String imagePath;

    private String oldName;
    private Integer oldCategoryId;
    private String oldCategoryName;
    private String oldSku;
    private String oldBarcode;
    private BigDecimal oldCostPrice = BigDecimal.ZERO;
    private BigDecimal oldSellingPrice = BigDecimal.ZERO;
    private int oldStockQuantity;
    private int oldLowStockThreshold = 10;
    private String oldImagePath;

    public ProductSubmission() {
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public List<FieldDiff> getFieldDifferences() {
        List<FieldDiff> diffs = new ArrayList<>();
        if ("CREATE".equalsIgnoreCase(submissionType)) {
            diffs.add(new FieldDiff("name", "Product Name", "—", name));
            diffs.add(new FieldDiff("sku", "SKU", "—", sku));
            if (barcode != null && !barcode.isBlank()) {
                diffs.add(new FieldDiff("barcode", "Barcode", "—", barcode));
            }
            diffs.add(new FieldDiff("category", "Category", "—", categoryName != null ? categoryName : "Category #" + categoryId));
            diffs.add(new FieldDiff("sellingPrice", "Selling Price", "—", String.format("₱%,.2f", sellingPrice)));
            diffs.add(new FieldDiff("costPrice", "Cost Price", "—", String.format("₱%,.2f", costPrice)));
            diffs.add(new FieldDiff("stockQuantity", "Initial Stock", "—", stockQuantity + " " + Constants.UNIT));
            diffs.add(new FieldDiff("lowStockThreshold", "Low-Stock Level", "—", lowStockThreshold + " " + Constants.UNIT));
            return diffs;
        }

        if (name != null && !name.equals(oldName)) {
            diffs.add(new FieldDiff("name", "Product Name", oldName != null ? oldName : "—", name));
        }
        if (categoryId != null && !categoryId.equals(oldCategoryId)) {
            diffs.add(new FieldDiff("category", "Category",
                    oldCategoryName != null ? oldCategoryName : "ID " + oldCategoryId,
                    categoryName != null ? categoryName : "ID " + categoryId));
        }
        if (sku != null && !sku.equals(oldSku)) {
            diffs.add(new FieldDiff("sku", "SKU", oldSku != null ? oldSku : "—", sku));
        }
        if (barcode != null && !barcode.equals(oldBarcode)) {
            diffs.add(new FieldDiff("barcode", "Barcode", oldBarcode != null ? oldBarcode : "—", barcode));
        }
        if (sellingPrice != null && oldSellingPrice != null && sellingPrice.compareTo(oldSellingPrice) != 0) {
            diffs.add(new FieldDiff("sellingPrice", "Selling Price",
                    String.format("₱%,.2f", oldSellingPrice),
                    String.format("₱%,.2f", sellingPrice)));
        }
        if (costPrice != null && oldCostPrice != null && costPrice.compareTo(oldCostPrice) != 0) {
            diffs.add(new FieldDiff("costPrice", "Cost Price",
                    String.format("₱%,.2f", oldCostPrice),
                    String.format("₱%,.2f", costPrice)));
        }
        if (stockQuantity != oldStockQuantity) {
            diffs.add(new FieldDiff("stockQuantity", "Stock Quantity",
                    oldStockQuantity + " " + Constants.UNIT,
                    stockQuantity + " " + Constants.UNIT));
        }
        if (lowStockThreshold != oldLowStockThreshold) {
            diffs.add(new FieldDiff("lowStockThreshold", "Low-Stock Level",
                    oldLowStockThreshold + " " + Constants.UNIT,
                    lowStockThreshold + " " + Constants.UNIT));
        }
        if (imagePath != null && !imagePath.equals(oldImagePath)) {
            diffs.add(new FieldDiff("imagePath", "Product Image", "Previous Image", "New Image"));
        }

        if (diffs.isEmpty()) {
            diffs.add(new FieldDiff("info", "Update", "No visible field differences", "No change"));
        }
        return diffs;
    }

    public Product toProduct() {
        Product p = new Product();
        if (productId != null) p.setId(productId);
        p.setOrganizationId(organizationId);
        p.setCategoryId(categoryId);
        p.setCategoryName(categoryName);
        p.setSku(sku);
        p.setBarcode(barcode);
        p.setName(name);
        p.setCostPrice(costPrice != null ? costPrice : BigDecimal.ZERO);
        p.setSellingPrice(sellingPrice != null ? sellingPrice : BigDecimal.ZERO);
        p.setStockQuantity(stockQuantity);
        p.setLowStockThreshold(lowStockThreshold);
        p.setImagePath(imagePath);
        p.setApprovalStatus(status != null ? status : "PENDING");
        return p;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrganizationId() { return organizationId; }
    public void setOrganizationId(int organizationId) { this.organizationId = organizationId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public int getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(int submittedBy) { this.submittedBy = submittedBy; }

    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }

    public String getSubmitterRole() { return submitterRole; }
    public void setSubmitterRole(String submitterRole) { this.submitterRole = submitterRole; }

    public String getSubmissionType() { return submissionType; }
    public void setSubmissionType(String submissionType) { this.submissionType = submissionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getOldName() { return oldName; }
    public void setOldName(String oldName) { this.oldName = oldName; }

    public Integer getOldCategoryId() { return oldCategoryId; }
    public void setOldCategoryId(Integer oldCategoryId) { this.oldCategoryId = oldCategoryId; }

    public String getOldCategoryName() { return oldCategoryName; }
    public void setOldCategoryName(String oldCategoryName) { this.oldCategoryName = oldCategoryName; }

    public String getOldSku() { return oldSku; }
    public void setOldSku(String oldSku) { this.oldSku = oldSku; }

    public String getOldBarcode() { return oldBarcode; }
    public void setOldBarcode(String oldBarcode) { this.oldBarcode = oldBarcode; }

    public BigDecimal getOldCostPrice() { return oldCostPrice; }
    public void setOldCostPrice(BigDecimal oldCostPrice) { this.oldCostPrice = oldCostPrice; }

    public BigDecimal getOldSellingPrice() { return oldSellingPrice; }
    public void setOldSellingPrice(BigDecimal oldSellingPrice) { this.oldSellingPrice = oldSellingPrice; }

    public int getOldStockQuantity() { return oldStockQuantity; }
    public void setOldStockQuantity(int oldStockQuantity) { this.oldStockQuantity = oldStockQuantity; }

    public int getOldLowStockThreshold() { return oldLowStockThreshold; }
    public void setOldLowStockThreshold(int oldLowStockThreshold) { this.oldLowStockThreshold = oldLowStockThreshold; }

    public String getOldImagePath() { return oldImagePath; }
    public void setOldImagePath(String oldImagePath) { this.oldImagePath = oldImagePath; }
}
