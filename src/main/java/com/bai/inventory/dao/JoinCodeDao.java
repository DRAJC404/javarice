package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.JoinCode;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JoinCodeDao {

    public int countActiveCodes(int organizationId) {
        String sql = "SELECT COUNT(*) FROM join_codes " +
                     "WHERE organization_id = ? AND used_at IS NULL AND cancelled_at IS NULL AND expires_at > NOW()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JoinCodeDao] Error counting active codes: " + e.getMessage());
        }
        return 0;
    }

    public JoinCode createJoinCode(int organizationId, String codeHash, String role, int createdBy) throws SQLException {
        purgeExpiredCodesOlderThan1Day();

        String sql = "INSERT INTO join_codes (organization_id, code_hash, role, created_by, expires_at) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '10 minutes') " +
                     "RETURNING id, organization_id, code_hash, role, created_by, created_at, expires_at";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            stmt.setString(2, codeHash);
            stmt.setString(3, role);
            stmt.setInt(4, createdBy);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp cat = rs.getTimestamp("created_at");
                    Timestamp exp = rs.getTimestamp("expires_at");
                    return new JoinCode(
                            rs.getInt("id"),
                            rs.getInt("organization_id"),
                            rs.getString("code_hash"),
                            rs.getString("role"),
                            rs.getInt("created_by"),
                            cat != null ? cat.toLocalDateTime() : null,
                            exp != null ? exp.toLocalDateTime() : null,
                            null, null, null
                    );
                }
            }
        }
        throw new SQLException("Failed to generate join code record.");
    }

    public boolean cancelCode(int codeId, int organizationId) throws SQLException {
        String sql = "UPDATE join_codes SET cancelled_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND organization_id = ? AND used_at IS NULL AND cancelled_at IS NULL";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, codeId);
            stmt.setInt(2, organizationId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<JoinCode> findHistoryByOrganization(int organizationId, int limit) {
        List<JoinCode> list = new ArrayList<>();
        String sql = "SELECT jc.id, jc.organization_id, jc.code_hash, jc.role, jc.created_by, " +
                     "u1.full_name AS created_by_name, jc.created_at, jc.expires_at, jc.used_at, " +
                     "jc.used_by, u2.full_name AS used_by_name, jc.cancelled_at " +
                     "FROM join_codes jc " +
                     "LEFT JOIN users u1 ON jc.created_by = u1.id " +
                     "LEFT JOIN users u2 ON jc.used_by = u2.id " +
                     "WHERE jc.organization_id = ? " +
                     "ORDER BY jc.created_at DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp cat = rs.getTimestamp("created_at");
                    Timestamp exp = rs.getTimestamp("expires_at");
                    Timestamp uat = rs.getTimestamp("used_at");
                    Timestamp can = rs.getTimestamp("cancelled_at");
                    int uby = rs.getInt("used_by");
                    Integer usedById = rs.wasNull() ? null : uby;

                    JoinCode jc = new JoinCode(
                            rs.getInt("id"),
                            rs.getInt("organization_id"),
                            rs.getString("code_hash"),
                            rs.getString("role"),
                            rs.getInt("created_by"),
                            cat != null ? cat.toLocalDateTime() : null,
                            exp != null ? exp.toLocalDateTime() : null,
                            uat != null ? uat.toLocalDateTime() : null,
                            usedById,
                            can != null ? can.toLocalDateTime() : null
                    );
                    jc.setCreatedByName(rs.getString("created_by_name"));
                    jc.setUsedByName(rs.getString("used_by_name"));
                    list.add(jc);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JoinCodeDao] Error loading code history: " + e.getMessage());
        }
        return list;
    }

    public int purgeExpiredCodesOlderThan1Day() {
        String sql = "DELETE FROM join_codes WHERE expires_at < NOW() - INTERVAL '1 day'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[JoinCodeDao] Error purging expired codes: " + e.getMessage());
            return 0;
        }
    }

    public int countJoinAttemptsInWindow(String deviceId, int windowMinutes) {
        String sql = "SELECT COUNT(*) FROM join_attempt_rate_limits " +
                     "WHERE device_id = ? AND attempt_time > NOW() - (INTERVAL '1 minute' * ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceId);
            stmt.setInt(2, windowMinutes);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[JoinCodeDao] Error checking DB rate limit: " + e.getMessage());
        }
        return 0;
    }

    public void recordJoinAttempt(String deviceId) {
        String sql = "INSERT INTO join_attempt_rate_limits (device_id, attempt_time) VALUES (?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[JoinCodeDao] Error recording join attempt: " + e.getMessage());
        }
    }
}
