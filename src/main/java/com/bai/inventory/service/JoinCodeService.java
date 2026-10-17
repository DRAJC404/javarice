package com.bai.inventory.service;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.AuditLogDao;
import com.bai.inventory.dao.JoinCodeDao;
import com.bai.inventory.model.JoinCode;
import com.bai.inventory.model.Role;
import com.bai.inventory.util.PasswordUtil;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JoinCodeService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_ACTIVE_CODES_PER_ORG = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final JoinCodeDao joinCodeDao;
    private final AuditLogDao auditLogDao;

    public JoinCodeService() {
        this.joinCodeDao = new JoinCodeDao();
        this.auditLogDao = new AuditLogDao();
    }

    public record GeneratedCodeResult(String rawCodeFormatted, JoinCode joinCode) {}

    public GeneratedCodeResult generateCode(int callerUserId, Role targetRole) throws SQLException, SecurityException, AuthService.ValidationException {
        if (targetRole != Role.EMPLOYEE && targetRole != Role.MANAGER) {
            throw new IllegalArgumentException("Join codes can only be generated for EMPLOYEE or MANAGER. A business can never have a second OWNER.");
        }

        int callerOrgId = requireOwnerCaller(callerUserId);

        int activeCount = joinCodeDao.countActiveCodes(callerOrgId);
        if (activeCount >= MAX_ACTIVE_CODES_PER_ORG) {
            throw new AuthService.ValidationException("Maximum limit of 5 active join codes reached. Please wait for existing codes to be redeemed/expire or cancel an unused code.");
        }

        StringBuilder raw = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int idx = secureRandom.nextInt(ALPHABET.length());
            raw.append(ALPHABET.charAt(idx));
        }
        String rawCode = raw.toString();
        String formattedCode = rawCode.substring(0, 4) + "-" + rawCode.substring(4);

        String codeHash = PasswordUtil.sha256Hex(rawCode);

        JoinCode created = joinCodeDao.createJoinCode(callerOrgId, codeHash, targetRole.name(), callerUserId);

        auditLogDao.logAction(callerOrgId, callerUserId, "CODE_CREATED",
                "Generated " + targetRole.name() + " join code #" + created.getId() + " (expires in 10 minutes)");

        return new GeneratedCodeResult(formattedCode, created);
    }

    public boolean cancelCode(int callerUserId, int codeId) throws SQLException, SecurityException {
        int callerOrgId = requireOwnerCaller(callerUserId);
        boolean cancelled = joinCodeDao.cancelCode(codeId, callerOrgId);
        if (cancelled) {
            auditLogDao.logAction(callerOrgId, callerUserId, "CODE_CANCELLED",
                    "Cancelled pending join code #" + codeId);
        }
        return cancelled;
    }

    public List<JoinCode> listCodeHistory(int callerUserId, int limit) throws SQLException, SecurityException {
        int callerOrgId = requireOwnerCaller(callerUserId);
        return joinCodeDao.findHistoryByOrganization(callerOrgId, limit);
    }
    
    private int requireOwnerCaller(int callerUserId) throws SQLException, SecurityException {
        String sql = "SELECT u.organization_id, r.name, u.approval_status, u.is_active " +
                     "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, callerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SecurityException("Unauthorized: Caller not found.");
                }
                String roleName = rs.getString("name");
                String approval = rs.getString("approval_status");
                boolean active = rs.getBoolean("is_active");
                int orgId = rs.getInt("organization_id");

                boolean isOwner = "OWNER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName);
                if (!isOwner || !"APPROVED".equalsIgnoreCase(approval) || !active) {
                    throw new SecurityException("Unauthorized: Only an active Business Owner can manage join codes.");
                }
                return orgId;
            }
        }
    }
}
