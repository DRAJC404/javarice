package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.ProductSubmission;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductSubmissionDao {

    public ProductSubmission createSubmission(ProductSubmission sub) throws SQLException {
        String sql = "INSERT INTO product_submissions (" +
                "organization_id, product_id, submitted_by, submission_type, status, " +
                "name, category_id, sku, barcode, cost_price, selling_price, stock_quantity, low_stock_threshold, image_path, " +
                "old_name, old_category_id, old_sku, old_barcode, old_cost_price, old_selling_price, old_stock_quantity, old_low_stock_threshold, old_image_path, " +
                "created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) RETURNING id";

        try (Connection conn = DatabaseConfig.getConnection(sub.getOrganizationId());
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sub.getOrganizationId());
            if (sub.getProductId() != null) {
                stmt.setInt(2, sub.getProductId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, sub.getSubmittedBy());
            stmt.setString(4, sub.getSubmissionType());
            stmt.setString(5, sub.getStatus() != null ? sub.getStatus() : "PENDING");

            stmt.setString(6, sub.getName());
            if (sub.getCategoryId() != null) {
                stmt.setInt(7, sub.getCategoryId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            stmt.setString(8, sub.getSku());
            stmt.setString(9, sub.getBarcode());
            stmt.setBigDecimal(10, sub.getCostPrice() != null ? sub.getCostPrice() : BigDecimal.ZERO);
            stmt.setBigDecimal(11, sub.getSellingPrice() != null ? sub.getSellingPrice() : BigDecimal.ZERO);
            stmt.setInt(12, sub.getStockQuantity());
            stmt.setInt(13, sub.getLowStockThreshold());
            stmt.setString(14, sub.getImagePath());

            stmt.setString(15, sub.getOldName());
            if (sub.getOldCategoryId() != null) {
                stmt.setInt(16, sub.getOldCategoryId());
            } else {
                stmt.setNull(16, Types.INTEGER);
            }
            stmt.setString(17, sub.getOldSku());
            stmt.setString(18, sub.getOldBarcode());
            if (sub.getOldCostPrice() != null) {
                stmt.setBigDecimal(19, sub.getOldCostPrice());
            } else {
                stmt.setNull(19, Types.NUMERIC);
            }
            if (sub.getOldSellingPrice() != null) {
                stmt.setBigDecimal(20, sub.getOldSellingPrice());
            } else {
                stmt.setNull(20, Types.NUMERIC);
            }
            stmt.setInt(21, sub.getOldStockQuantity());
            stmt.setInt(22, sub.getOldLowStockThreshold());
            stmt.setString(23, sub.getOldImagePath());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    sub.setId(rs.getInt("id"));
                    conn.commit();
                    return sub;
                }
            }
        }
        throw new SQLException("Failed to create product submission.");
    }

    public List<ProductSubmission> findPendingSubmissions(int organizationId) {
        String sql = "SELECT ps.*, u.full_name AS submitter_name, r.name AS submitter_role, " +
                "c.name AS category_name, oc.name AS old_category_name " +
                "FROM product_submissions ps " +
                "JOIN users u ON ps.submitted_by = u.id " +
                "LEFT JOIN roles r ON u.role_id = r.id " +
                "LEFT JOIN categories c ON ps.category_id = c.id " +
                "LEFT JOIN categories oc ON ps.old_category_id = oc.id " +
                "WHERE ps.organization_id = ? AND ps.status = 'PENDING' " +
                "ORDER BY ps.created_at ASC";
        return querySubmissions(organizationId, sql, stmt -> stmt.setInt(1, organizationId));
    }

    public List<ProductSubmission> findSubmissionsByUser(int userId, int organizationId) {
        String sql = "SELECT ps.*, u.full_name AS submitter_name, r.name AS submitter_role, " +
                "c.name AS category_name, oc.name AS old_category_name, " +
                "rev.full_name AS reviewer_name " +
                "FROM product_submissions ps " +
                "JOIN users u ON ps.submitted_by = u.id " +
                "LEFT JOIN roles r ON u.role_id = r.id " +
                "LEFT JOIN categories c ON ps.category_id = c.id " +
                "LEFT JOIN categories oc ON ps.old_category_id = oc.id " +
                "LEFT JOIN users rev ON ps.reviewed_by = rev.id " +
                "WHERE ps.organization_id = ? AND ps.submitted_by = ? " +
                "ORDER BY ps.created_at DESC";
        return querySubmissions(organizationId, sql, stmt -> {
            stmt.setInt(1, organizationId);
            stmt.setInt(2, userId);
        });
    }

    public Optional<ProductSubmission> findById(int id, int organizationId) {
        String sql = "SELECT ps.*, u.full_name AS submitter_name, r.name AS submitter_role, " +
                "c.name AS category_name, oc.name AS old_category_name, " +
                "rev.full_name AS reviewer_name " +
                "FROM product_submissions ps " +
                "JOIN users u ON ps.submitted_by = u.id " +
                "LEFT JOIN roles r ON u.role_id = r.id " +
                "LEFT JOIN categories c ON ps.category_id = c.id " +
                "LEFT JOIN categories oc ON ps.old_category_id = oc.id " +
                "LEFT JOIN users rev ON ps.reviewed_by = rev.id " +
                "WHERE ps.id = ? AND ps.organization_id = ?";
        List<ProductSubmission> list = querySubmissions(organizationId, sql, stmt -> {
            stmt.setInt(1, id);
            stmt.setInt(2, organizationId);
        });
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int countPending(int organizationId) {
        String sql = "SELECT COUNT(*) FROM product_submissions WHERE organization_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductSubmissionDao] Error counting pending: " + e.getMessage());
        }
        return 0;
    }

    public boolean updateStatus(int submissionId, String status, String rejectionReason, int reviewerId, int organizationId) throws SQLException {
        String sql = "UPDATE product_submissions SET status = ?, rejection_reason = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND organization_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, rejectionReason);
            stmt.setInt(3, reviewerId);
            stmt.setInt(4, submissionId);
            stmt.setInt(5, organizationId);
            int rows = stmt.executeUpdate();
            conn.commit();
            return rows > 0;
        }
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement stmt) throws SQLException;
    }

    private List<ProductSubmission> querySubmissions(int organizationId, String sql, ParamSetter setter) {
        List<ProductSubmission> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setter.set(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProductSubmission sub = new ProductSubmission();
                    sub.setId(rs.getInt("id"));
                    sub.setOrganizationId(rs.getInt("organization_id"));
                    int pId = rs.getInt("product_id");
                    sub.setProductId(rs.wasNull() ? null : pId);
                    sub.setSubmittedBy(rs.getInt("submitted_by"));
                    sub.setSubmitterName(rs.getString("submitter_name"));
                    sub.setSubmitterRole(rs.getString("submitter_role"));
                    sub.setSubmissionType(rs.getString("submission_type"));
                    sub.setStatus(rs.getString("status"));
                    sub.setRejectionReason(rs.getString("rejection_reason"));
                    int rId = rs.getInt("reviewed_by");
                    sub.setReviewedBy(rs.wasNull() ? null : rId);
                    try {
                        sub.setReviewerName(rs.getString("reviewer_name"));
                    } catch (SQLException ignored) {}
                    Timestamp rat = rs.getTimestamp("reviewed_at");
                    if (rat != null) sub.setReviewedAt(rat.toLocalDateTime());
                    Timestamp cat = rs.getTimestamp("created_at");
                    if (cat != null) sub.setCreatedAt(cat.toLocalDateTime());

                    sub.setName(rs.getString("name"));
                    int cId = rs.getInt("category_id");
                    sub.setCategoryId(rs.wasNull() ? null : cId);
                    sub.setCategoryName(rs.getString("category_name"));
                    sub.setSku(rs.getString("sku"));
                    sub.setBarcode(rs.getString("barcode"));
                    sub.setCostPrice(rs.getBigDecimal("cost_price"));
                    sub.setSellingPrice(rs.getBigDecimal("selling_price"));
                    sub.setStockQuantity(rs.getInt("stock_quantity"));
                    sub.setLowStockThreshold(rs.getInt("low_stock_threshold"));
                    sub.setImagePath(rs.getString("image_path"));

                    sub.setOldName(rs.getString("old_name"));
                    int ocId = rs.getInt("old_category_id");
                    sub.setOldCategoryId(rs.wasNull() ? null : ocId);
                    try {
                        sub.setOldCategoryName(rs.getString("old_category_name"));
                    } catch (SQLException ignored) {}
                    sub.setOldSku(rs.getString("old_sku"));
                    sub.setOldBarcode(rs.getString("old_barcode"));
                    sub.setOldCostPrice(rs.getBigDecimal("old_cost_price"));
                    sub.setOldSellingPrice(rs.getBigDecimal("old_selling_price"));
                    sub.setOldStockQuantity(rs.getInt("old_stock_quantity"));
                    sub.setOldLowStockThreshold(rs.getInt("old_low_stock_threshold"));
                    sub.setOldImagePath(rs.getString("old_image_path"));

                    list.add(sub);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductSubmissionDao] Error querying submissions: " + e.getMessage());
        }
        return list;
    }
}
