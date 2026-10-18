package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final String SEED_BCRYPT = "$2a$12$s0Hn2VYxfAcBRGor2.vDauP3/SA1niIuG64FYo8tKBk3C9As1F3Qi";

    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT id, organization_id, role_id, full_name, email, password_hash, approval_status, remember_token_hash, remember_expires_at, is_active FROM fn_find_user_by_email(?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] DB Error finding user by email: " + e.getMessage());
        }

        if (DatabaseConfig.isDevModeEnabled()) {
            String cleanEmail = email.trim().toLowerCase();
            if ("admin@gmail.com".equals(cleanEmail)) {
                return Optional.of(new User(1, 1, Role.OWNER.getId(), "Store Administrator (Owner)", "admin@gmail.com", SEED_BCRYPT, "APPROVED", true));
            } else if ("manager@bai.com".equals(cleanEmail)) {
                return Optional.of(new User(2, 1, Role.MANAGER.getId(), "Jane Santos (Manager)", "manager@bai.com", SEED_BCRYPT, "APPROVED", true));
            } else if ("employee@bai.com".equals(cleanEmail)) {
                return Optional.of(new User(3, 1, Role.EMPLOYEE.getId(), "Alex Cruz (Associate)", "employee@bai.com", SEED_BCRYPT, "APPROVED", true));
            }
        }

        return Optional.empty();
    }

    public Optional<User> findByRememberTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT id, organization_id, role_id, full_name, email, password_hash, approval_status, remember_token_hash, remember_expires_at, is_active FROM fn_find_user_by_remember_token(?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tokenHash.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] DB Error finding user by remember token: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean isEmailTaken(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String sql = "SELECT fn_email_exists(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Error checking if email exists: " + e.getMessage());
        }
        return false;
    }

    public boolean updateRememberToken(int userId, String tokenHash, LocalDateTime expiresAt) {
        String sql = "UPDATE users SET remember_token_hash = ?, remember_expires_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tokenHash);
            stmt.setTimestamp(2, expiresAt != null ? Timestamp.valueOf(expiresAt) : null);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Error updating remember token hash: " + e.getMessage());
            return false;
        }
    }

    public boolean clearRememberToken(int userId) {
        String sql = "UPDATE users SET remember_token_hash = NULL, remember_expires_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Error clearing remember token: " + e.getMessage());
            return false;
        }
    }

    public User registerOwner(String businessName, String fullName, String email, String passwordHash) throws SQLException {
        String insertOrgSql = "INSERT INTO organizations (name, slug, is_active) VALUES (?, ?, true) RETURNING id";
        String insertUserSql = "INSERT INTO users (organization_id, role_id, full_name, email, password_hash, approval_status, is_active) " +
                               "VALUES (?, 1, ?, ?, ?, 'APPROVED', true) " +
                               "RETURNING id, organization_id, role_id, full_name, email, password_hash, approval_status, remember_token_hash, remember_expires_at, is_active";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orgId;
                String slug = businessName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
                try (PreparedStatement orgStmt = conn.prepareStatement(insertOrgSql)) {
                    orgStmt.setString(1, businessName.trim());
                    orgStmt.setString(2, slug);
                    try (ResultSet rs = orgStmt.executeQuery()) {
                        if (rs.next()) {
                            orgId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to create organization record.");
                        }
                    }
                }

                DatabaseConfig.setTenantContext(conn, orgId);

                User user;
                try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql)) {
                    userStmt.setInt(1, orgId);
                    userStmt.setString(2, fullName.trim());
                    userStmt.setString(3, email.trim().toLowerCase());
                    userStmt.setString(4, passwordHash);
                    try (ResultSet rs = userStmt.executeQuery()) {
                        if (rs.next()) {
                            user = mapResultSetToUser(rs);
                        } else {
                            throw new SQLException("Failed to create owner user record.");
                        }
                    }
                }

                String auditSql = "INSERT INTO audit_log (organization_id, user_id, action, details) VALUES (?, ?, 'OWNER_REGISTERED', ?)";
                try (PreparedStatement auditStmt = conn.prepareStatement(auditSql)) {
                    auditStmt.setInt(1, orgId);
                    auditStmt.setInt(2, user.getId());
                    auditStmt.setString(3, "Business owner account registered for " + businessName);
                    auditStmt.executeUpdate();
                }

                conn.commit();
                return user;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public User redeemJoinCode(String codeHash, String fullName, String email, String passwordHash)
            throws SQLException, com.bai.inventory.service.AuthService.ValidationException {
        String selectCodeSql = "SELECT id, organization_id, role, org_active FROM fn_lock_active_join_code(?)";

        String checkEmailSql = "SELECT fn_email_exists(?)";

        String insertUserSql = "INSERT INTO users (organization_id, role_id, full_name, email, password_hash, approval_status, is_active) " +
                               "VALUES (?, ?, ?, ?, ?, 'APPROVED', true) " +
                               "RETURNING id, organization_id, role_id, full_name, email, password_hash, approval_status, remember_token_hash, remember_expires_at, is_active";

        String markUsedSql = "UPDATE join_codes SET used_at = CURRENT_TIMESTAMP, used_by = ? WHERE id = ? AND organization_id = ?";
        String auditSql = "INSERT INTO audit_log (organization_id, user_id, action, details) VALUES (?, ?, 'CODE_REDEEMED', ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int codeId;
                int orgId;
                String roleStr;

                try (PreparedStatement codeStmt = conn.prepareStatement(selectCodeSql)) {
                    codeStmt.setString(1, codeHash);
                    try (ResultSet rs = codeStmt.executeQuery()) {
                        if (!rs.next() || !rs.getBoolean("org_active")) {
                            throw new com.bai.inventory.service.AuthService.ValidationException("Invalid or expired code.");
                        }
                        codeId = rs.getInt("id");
                        orgId = rs.getInt("organization_id");
                        roleStr = rs.getString("role");
                    }
                }

                DatabaseConfig.setTenantContext(conn, orgId);

                try (PreparedStatement emailStmt = conn.prepareStatement(checkEmailSql)) {
                    emailStmt.setString(1, email.trim().toLowerCase());
                    try (ResultSet rs = emailStmt.executeQuery()) {
                        if (rs.next() && rs.getBoolean(1)) {
                            throw new com.bai.inventory.service.AuthService.ValidationException("An account with this email address already exists.");
                        }
                    }
                }

                Role role = Role.fromCode(roleStr);
                User newUser;
                try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql)) {
                    userStmt.setInt(1, orgId);
                    userStmt.setInt(2, role.getId());
                    userStmt.setString(3, fullName.trim());
                    userStmt.setString(4, email.trim().toLowerCase());
                    userStmt.setString(5, passwordHash);
                    try (ResultSet rs = userStmt.executeQuery()) {
                        if (rs.next()) {
                            newUser = mapResultSetToUser(rs);
                        } else {
                            throw new SQLException("Failed to create user record.");
                        }
                    }
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(markUsedSql)) {
                    updateStmt.setInt(1, newUser.getId());
                    updateStmt.setInt(2, codeId);
                    updateStmt.setInt(3, orgId);
                    updateStmt.executeUpdate();
                }

                try (PreparedStatement auditStmt = conn.prepareStatement(auditSql)) {
                    auditStmt.setInt(1, orgId);
                    auditStmt.setInt(2, newUser.getId());
                    auditStmt.setString(3, "Staff member " + fullName + " joined as " + roleStr);
                    auditStmt.executeUpdate();
                }

                conn.commit();
                return newUser;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof com.bai.inventory.service.AuthService.ValidationException ve) {
                    throw ve;
                }
                throw new SQLException("Database error during code redemption: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int countStaffMembers(int organizationId) {
        String sql = "SELECT COUNT(*) FROM users WHERE organization_id = ? AND role_id IN (2, 3) AND is_active = true";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Error counting staff: " + e.getMessage());
        }
        return 0;
    }

    public List<User> findByOrganization(int organizationId) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, organization_id, role_id, full_name, email, password_hash, approval_status, remember_token_hash, remember_expires_at, is_active " +
                     "FROM users WHERE organization_id = ? ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] DB Error listing users for org: " + e.getMessage());
        }
        return list;
    }

    public boolean updateUserRoleOrStatus(int targetUserId, int organizationId, Integer newRoleId, String newApprovalStatus, int callerUserId)
            throws SQLException, com.bai.inventory.service.AuthService.ValidationException {
        String selectTargetSql = "SELECT role_id, approval_status, is_active FROM users WHERE id = ? AND organization_id = ? FOR UPDATE";
        String countActiveOwnersSql = "SELECT COUNT(*) FROM users WHERE organization_id = ? AND role_id = 1 AND approval_status = 'APPROVED' AND is_active = true FOR UPDATE";
        String updateUserSql = "UPDATE users SET role_id = COALESCE(?, role_id), approval_status = COALESCE(?, approval_status), updated_at = CURRENT_TIMESTAMP WHERE id = ? AND organization_id = ?";
        String auditSql = "INSERT INTO audit_log (organization_id, user_id, action, details) VALUES (?, ?, 'USER_STATUS_UPDATED', ?)";

        try (Connection conn = DatabaseConfig.getConnection(organizationId)) {
            conn.setAutoCommit(false);
            try {
                int currentRoleId;
                String currentStatus;
                boolean currentActive;

                try (PreparedStatement stmt = conn.prepareStatement(selectTargetSql)) {
                    stmt.setInt(1, targetUserId);
                    stmt.setInt(2, organizationId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new com.bai.inventory.service.AuthService.ValidationException("User not found in organization.");
                        }
                        currentRoleId = rs.getInt("role_id");
                        currentStatus = rs.getString("approval_status");
                        currentActive = rs.getBoolean("is_active");
                    }
                }

                boolean wasActiveOwner = (currentRoleId == 1 && "APPROVED".equalsIgnoreCase(currentStatus) && currentActive);
                boolean willRemainOwner = (newRoleId == null || newRoleId == 1);
                boolean willRemainApproved = (newApprovalStatus == null || "APPROVED".equalsIgnoreCase(newApprovalStatus));

                if (wasActiveOwner && (!willRemainOwner || !willRemainApproved)) {
                    try (PreparedStatement countStmt = conn.prepareStatement(countActiveOwnersSql)) {
                        countStmt.setInt(1, organizationId);
                        try (ResultSet rs = countStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) <= 1) {
                                throw new com.bai.inventory.service.AuthService.ValidationException(
                                        "Operation denied: Every business must have at least one active Business Owner."
                                );
                            }
                        }
                    }
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateUserSql)) {
                    if (newRoleId != null) updateStmt.setInt(1, newRoleId);
                    else updateStmt.setNull(1, Types.INTEGER);
                    if (newApprovalStatus != null) updateStmt.setString(2, newApprovalStatus);
                    else updateStmt.setNull(2, Types.VARCHAR);
                    updateStmt.setInt(3, targetUserId);
                    updateStmt.setInt(4, organizationId);
                    updateStmt.executeUpdate();
                }

                try (PreparedStatement auditStmt = conn.prepareStatement(auditSql)) {
                    auditStmt.setInt(1, organizationId);
                    auditStmt.setInt(2, callerUserId);
                    auditStmt.setString(3, "Updated user #" + targetUserId + ": role=" + newRoleId + ", status=" + newApprovalStatus);
                    auditStmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof com.bai.inventory.service.AuthService.ValidationException ve) throw ve;
                throw new SQLException("Error updating user status: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getInt("organization_id"),
                rs.getInt("role_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("approval_status"),
                rs.getBoolean("is_active")
        );
        user.setRememberTokenHash(rs.getString("remember_token_hash"));
        Timestamp exp = rs.getTimestamp("remember_expires_at");
        if (exp != null) {
            user.setRememberExpiresAt(exp.toLocalDateTime());
        }
        return user;
    }
}
