package com.bai.inventory.service;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private void requireAdminCaller(Connection conn, int callerUserId) throws SQLException, SecurityException {
        String sql = "SELECT r.name, u.approval_status, u.is_active " +
                     "FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, callerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SecurityException("Unauthorized: Caller account not found in database.");
                }
                String roleName = rs.getString("name");
                String approvalStatus = rs.getString("approval_status");
                boolean isActive = rs.getBoolean("is_active");

                if (!"ADMIN".equalsIgnoreCase(roleName) || !"APPROVED".equalsIgnoreCase(approvalStatus) || !isActive) {
                    throw new SecurityException("Unauthorized: Caller must be an active, approved Administrator.");
                }
            }
        }
    }

    public boolean approveUser(int callerUserId, int targetUserId) throws SQLException, SecurityException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            requireAdminCaller(conn, callerUserId);

            String sql = "UPDATE users SET approval_status = 'APPROVED', updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, targetUserId);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    public boolean rejectUser(int callerUserId, int targetUserId) throws SQLException, SecurityException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            requireAdminCaller(conn, callerUserId);

            String sql = "UPDATE users SET approval_status = 'REJECTED', remember_token_hash = NULL, remember_expires_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, targetUserId);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    public boolean disableUser(int callerUserId, int targetUserId) throws SQLException, SecurityException {
        if (callerUserId == targetUserId) {
            throw new IllegalArgumentException("Administrators cannot disable their own account.");
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            requireAdminCaller(conn, callerUserId);

            String sql = "UPDATE users SET approval_status = 'DISABLED', remember_token_hash = NULL, remember_expires_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, targetUserId);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    public boolean changeUserRole(int callerUserId, int targetUserId, Role newRole) throws SQLException, SecurityException {
        if (newRole == null) {
            throw new IllegalArgumentException("Role cannot be null.");
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            requireAdminCaller(conn, callerUserId);

            String sql = "UPDATE users SET role_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, newRole.getId());
                stmt.setInt(2, targetUserId);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    public boolean resetUserPassword(int callerUserId, int targetUserId, String newRawPassword)
            throws SQLException, SecurityException, AuthService.ValidationException {
        if (newRawPassword == null || newRawPassword.length() < 8 || !newRawPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new AuthService.ValidationException("Password must be at least 8 characters long and contain at least one letter and one number.");
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            requireAdminCaller(conn, callerUserId);

            String hash = PasswordUtil.hash(newRawPassword);
            String sql = "UPDATE users SET password_hash = ?, remember_token_hash = NULL, remember_expires_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, hash);
                stmt.setInt(2, targetUserId);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    public List<User> listAllUsers(int callerUserId) throws SQLException, SecurityException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            requireAdminCaller(conn, callerUserId);

            List<User> list = new ArrayList<>();
            String sql = "SELECT id, role_id, full_name, email, password_hash, approval_status, remember_token_hash, remember_expires_at, is_active " +
                         "FROM users ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User u = new User(
                            rs.getInt("id"),
                            rs.getInt("role_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("approval_status"),
                            rs.getBoolean("is_active")
                    );
                    list.add(u);
                }
            }
            return list;
        }
    }
}
