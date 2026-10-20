package com.bai.inventory.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ProductPerformance {
    private int productId;
    private String productName;
    private String sku;
    private String categoryName;
    private int unitsSold;
    private int stockLeft;
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    private BigDecimal costPrice = BigDecimal.ZERO;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal profit = BigDecimal.ZERO;
    private double turnoverRate = 0.0;
    private int damagedOrExpiredUnits;
    private String statusTag; 
    private String attentionReason;

    public ProductPerformance() {}

    public ProductPerformance(int productId, String productName, String sku, String categoryName,
                              int unitsSold, int stockLeft, BigDecimal sellingPrice, BigDecimal costPrice,
                              int damagedOrExpiredUnits) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.categoryName = categoryName;
        this.unitsSold = unitsSold;
        this.stockLeft = stockLeft;
        this.sellingPrice = sellingPrice != null ? sellingPrice : BigDecimal.ZERO;
        this.costPrice = costPrice != null ? costPrice : BigDecimal.ZERO;
        this.damagedOrExpiredUnits = damagedOrExpiredUnits;
        calculateMetrics();
    }

    public void calculateMetrics() {
        this.revenue = sellingPrice.multiply(BigDecimal.valueOf(unitsSold));
        BigDecimal unitProfit = sellingPrice.subtract(costPrice);
        this.profit = unitProfit.multiply(BigDecimal.valueOf(unitsSold));

        int totalUnitsHandled = unitsSold + stockLeft;
        if (totalUnitsHandled > 0) {
            this.turnoverRate = ((double) unitsSold / totalUnitsHandled) * 100.0;
        } else {
            this.turnoverRate = 0.0;
        }

        if (stockLeft <= 5 || (damagedOrExpiredUnits > 0 && damagedOrExpiredUnits >= Math.max(1, unitsSold / 4))) {
            this.statusTag = "Needs Attention";
            if (stockLeft <= 0) {
                this.attentionReason = "Out of stock";
            } else if (stockLeft <= 5) {
                this.attentionReason = "Critically low stock (" + stockLeft + " left)";
            } else {
                this.attentionReason = "High damage/loss (" + damagedOrExpiredUnits + " units)";
            }
        } else if (unitsSold == 0 && stockLeft >= 20) {
            this.statusTag = "Needs Attention";
            this.attentionReason = "Slow moving (0 sold)";
        } else if (unitsSold >= 15 || turnoverRate >= 60.0) {
            this.statusTag = "Top Seller";
            this.attentionReason = "High demand";
        } else {
            this.statusTag = "Normal";
            this.attentionReason = "Stable";
        }
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public int getUnitsSold() { return unitsSold; }
    public void setUnitsSold(int unitsSold) { 
        this.unitsSold = unitsSold; 
        calculateMetrics();
    }

    public int getStockLeft() { return stockLeft; }
    public void setStockLeft(int stockLeft) { 
        this.stockLeft = stockLeft; 
        calculateMetrics();
    }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { 
        this.sellingPrice = sellingPrice; 
        calculateMetrics();
    }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { 
        this.costPrice = costPrice; 
        calculateMetrics();
    }

    public BigDecimal getRevenue() { return revenue; }
    public BigDecimal getProfit() { return profit; }
    public double getTurnoverRate() { return turnoverRate; }

    public String getFormattedTurnover() {
        return String.format("%.1f%%", turnoverRate);
    }

    public String getFormattedProfit() {
        return String.format("₱%,.2f", profit);
    }

    public String getFormattedRevenue() {
        return String.format("₱%,.2f", revenue);
    }

    public int getDamagedOrExpiredUnits() { return damagedOrExpiredUnits; }
    public void setDamagedOrExpiredUnits(int damagedOrExpiredUnits) { 
        this.damagedOrExpiredUnits = damagedOrExpiredUnits; 
        calculateMetrics();
    }

    public String getStatusTag() { return statusTag; }
    public void setStatusTag(String statusTag) { this.statusTag = statusTag; }

    public String getAttentionReason() { return attentionReason; }
    public void setAttentionReason(String attentionReason) { this.attentionReason = attentionReason; }

    public boolean isNeedsAttention() {
        return "Needs Attention".equalsIgnoreCase(statusTag);
    }
}
