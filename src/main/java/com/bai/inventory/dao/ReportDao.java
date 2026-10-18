package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.StockMovement;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {

    public List<StockMovement> getStockMovementsReport(int orgId, LocalDateTime from, LocalDateTime to) {
        List<StockMovement> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT sm.id, sm.organization_id, sm.reference_no, sm.product_id, p.name AS product_name, p.sku AS product_sku, " +
                "sm.user_id, u.full_name AS user_name, sm.movement_type, sm.quantity, sm.reason, " +
                "sm.previous_stock, sm.new_stock, sm.undone_by_movement_id, sm.notes, sm.created_at " +
                "FROM stock_movements sm " +
                "JOIN products p ON sm.product_id = p.id " +
                "JOIN users u ON sm.user_id = u.id " +
                "WHERE sm.organization_id = ?"
        );

        if (from != null) {
            sql.append(" AND sm.created_at >= ?");
        }
        if (to != null) {
            sql.append(" AND sm.created_at <= ?");
        }
        sql.append(" ORDER BY sm.created_at DESC");

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setInt(idx++, orgId);
            if (from != null) {
                stmt.setTimestamp(idx++, Timestamp.valueOf(from));
            }
            if (to != null) {
                stmt.setTimestamp(idx++, Timestamp.valueOf(to));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setOrganizationId(rs.getInt("organization_id"));
                    sm.setReferenceNo(rs.getString("reference_no"));
                    sm.setProductId(rs.getInt("product_id"));
                    sm.setProductName(rs.getString("product_name"));
                    sm.setProductSku(rs.getString("product_sku"));
                    sm.setUserId(rs.getInt("user_id"));
                    sm.setUserName(rs.getString("user_name"));
                    sm.setMovementType(rs.getString("movement_type"));
                    sm.setQuantity(rs.getInt("quantity"));
                    sm.setReason(rs.getString("reason"));
                    sm.setPreviousStock(rs.getInt("previous_stock"));
                    sm.setNewStock(rs.getInt("new_stock"));
                    int undone = rs.getInt("undone_by_movement_id");
                    sm.setUndoneByMovementId(rs.wasNull() ? null : undone);
                    sm.setNotes(rs.getString("notes"));
                    Timestamp cat = rs.getTimestamp("created_at");
                    if (cat != null) sm.setCreatedAt(cat.toLocalDateTime());
                    list.add(sm);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReportDao] Error querying stock movements report: " + e.getMessage());
        }
        return list;
    }

    public List<Product> getInventoryReport(int orgId) {
        String sql = "SELECT p.id, p.organization_id, p.category_id, c.name AS category_name, p.sku, p.barcode, " +
                     "p.name, p.unit, p.cost_price, p.selling_price, p.stock_quantity, p.low_stock_threshold, " +
                     "p.image_path, p.status, p.is_archived, p.created_at, p.updated_at " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.organization_id = ? AND p.is_archived = false " +
                     "ORDER BY p.name ASC";
        return queryProducts(orgId, sql);
    }

    public List<Product> getProductCatalogReport(int orgId) {
        String sql = "SELECT p.id, p.organization_id, p.category_id, c.name AS category_name, p.sku, p.barcode, " +
                     "p.name, p.unit, p.cost_price, p.selling_price, p.stock_quantity, p.low_stock_threshold, " +
                     "p.image_path, p.status, p.is_archived, p.created_at, p.updated_at " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.organization_id = ? " +
                     "ORDER BY p.name ASC";
        return queryProducts(orgId, sql);
    }

    public List<Product> getLowStockReport(int orgId) {
        String sql = "SELECT p.id, p.organization_id, p.category_id, c.name AS category_name, p.sku, p.barcode, " +
                     "p.name, p.unit, p.cost_price, p.selling_price, p.stock_quantity, p.low_stock_threshold, " +
                     "p.image_path, p.status, p.is_archived, p.created_at, p.updated_at " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.organization_id = ? AND p.is_archived = false AND p.stock_quantity <= p.low_stock_threshold " +
                     "ORDER BY p.stock_quantity ASC, p.name ASC";
        return queryProducts(orgId, sql);
    }

    private List<Product> queryProducts(int orgId, String sql) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getInt("id"));
                    p.setOrganizationId(rs.getInt("organization_id"));
                    int catId = rs.getInt("category_id");
                    p.setCategoryId(rs.wasNull() ? null : catId);
                    p.setCategoryName(rs.getString("category_name"));
                    p.setSku(rs.getString("sku"));
                    p.setBarcode(rs.getString("barcode"));
                    p.setName(rs.getString("name"));
                    p.setUnit(rs.getString("unit"));
                    p.setCostPrice(rs.getBigDecimal("cost_price"));
                    p.setSellingPrice(rs.getBigDecimal("selling_price"));
                    p.setStockQuantity(rs.getInt("stock_quantity"));
                    p.setLowStockThreshold(rs.getInt("low_stock_threshold"));
                    p.setImagePath(rs.getString("image_path"));
                    p.setStatus(rs.getString("status"));
                    p.setArchived(rs.getBoolean("is_archived"));
                    Timestamp cat = rs.getTimestamp("created_at");
                    if (cat != null) p.setCreatedAt(cat.toLocalDateTime());
                    Timestamp uat = rs.getTimestamp("updated_at");
                    if (uat != null) p.setUpdatedAt(uat.toLocalDateTime());
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReportDao] Error querying products report: " + e.getMessage());
        }
        return list;
    }
}
