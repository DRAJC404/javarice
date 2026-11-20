package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditLogDao {

    public void logAction(Connection conn, int organizationId, Integer userId, String action, String details) throws SQLException {
        String sql = "INSERT INTO audit_log (organization_id, user_id, action, details) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            if (userId != null && userId > 0) {
                stmt.setInt(2, userId);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setString(3, action);
            stmt.setString(4, details);
            stmt.executeUpdate();
        }
    }

    public void logAction(int organizationId, Integer userId, String action, String details) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            logAction(conn, organizationId, userId, action, details);
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            System.err.println("[AuditLogDao] Error writing audit log: " + e.getMessage());
        }
    }
}
