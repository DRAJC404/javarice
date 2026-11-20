package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.ProductHistory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductHistoryDao {

    public void record(int productId, String action, String field, String oldValue, String newValue, Integer userId) {
        String sql = "INSERT INTO product_history (product_id, action, field, old_value, new_value, user_id, date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConfig.getConnection(1);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setString(2, action != null ? action : "UPDATE");
            stmt.setString(3, field);
            stmt.setString(4, oldValue);
            stmt.setString(5, newValue);
            if (userId != null && userId > 0) {
                stmt.setInt(6, userId);
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("[ProductHistoryDao] Error recording history: " + e.getMessage());
        }
    }

    public List<ProductHistory> findByProductId(int productId, String actionFilter, LocalDate fromDate, LocalDate toDate) {
        List<ProductHistory> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT ph.history_id, ph.product_id, ph.action, ph.field, ph.old_value, ph.new_value, " +
                "ph.user_id, u.full_name AS user_name, ph.date " +
                "FROM product_history ph " +
                "LEFT JOIN users u ON ph.user_id = u.id " +
                "WHERE ph.product_id = ?"
        );

        if (actionFilter != null && !actionFilter.isBlank() && !"ALL".equalsIgnoreCase(actionFilter)) {
            sql.append(" AND ph.action = ?");
        }
        if (fromDate != null) {
            sql.append(" AND ph.date >= ?");
        }
        if (toDate != null) {
            sql.append(" AND ph.date <= ?");
        }
        sql.append(" ORDER BY ph.date DESC, ph.history_id DESC");

        try (Connection conn = DatabaseConfig.getConnection(1);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setInt(idx++, productId);
            if (actionFilter != null && !actionFilter.isBlank() && !"ALL".equalsIgnoreCase(actionFilter)) {
                stmt.setString(idx++, actionFilter.trim());
            }
            if (fromDate != null) {
                stmt.setTimestamp(idx++, Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                stmt.setTimestamp(idx++, Timestamp.valueOf(toDate.atTime(23, 59, 59)));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProductHistory h = new ProductHistory();
                    h.setHistoryId(rs.getInt("history_id"));
                    h.setProductId(rs.getInt("product_id"));
                    h.setAction(rs.getString("action"));
                    h.setField(rs.getString("field"));
                    h.setOldValue(rs.getString("old_value"));
                    h.setNewValue(rs.getString("new_value"));
                    int uid = rs.getInt("user_id");
                    h.setUserId(rs.wasNull() ? null : uid);
                    h.setUserName(rs.getString("user_name"));
                    Timestamp ts = rs.getTimestamp("date");
                    if (ts != null) {
                        h.setDate(ts.toLocalDateTime());
                    }
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductHistoryDao] Error fetching history for product " + productId + ": " + e.getMessage());
        }
        return list;
    }
}
