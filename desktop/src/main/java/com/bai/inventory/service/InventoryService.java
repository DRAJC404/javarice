package com.bai.inventory.service;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.StockMovementDao;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class InventoryService {

    private final StockMovementDao movementDao;

    public InventoryService() {
        this.movementDao = new StockMovementDao();
    }

    public InventoryService(StockMovementDao movementDao) {
        this.movementDao = movementDao;
    }

    public StockMovement deductPosSale(int productId, int quantity, User user)
            throws InventoryException, AccessDeniedException, SQLException {
        return deductStock(productId, quantity, "SOLD", user);
    }

    public StockMovement deductStock(int productId, int quantity, String reason, User user)
            throws InventoryException, AccessDeniedException, SQLException {

        if (user == null) {
            throw new AccessDeniedException("User session required.");
        }

        if (user.getRole() == Role.EMPLOYEE) {
            if (!("SOLD".equalsIgnoreCase(reason) || "USED".equalsIgnoreCase(reason) ||
                  "DAMAGED".equalsIgnoreCase(reason) || "EXPIRED".equalsIgnoreCase(reason))) {
                throw new AccessDeniedException("Employees are only permitted to record SOLD, USED, DAMAGED, or EXPIRED deductions.");
            }
        }

        if (quantity <= 0) {
            throw new InventoryException("Deduction quantity must be greater than zero.");
        }

        if (DatabaseConfig.isDevModeEnabled()) {
            StockMovement mock = new StockMovement();
            mock.setId((int) (System.currentTimeMillis() % 100000));
            mock.setOrganizationId(user.getOrganizationId());
            mock.setReferenceNo("SM-DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            mock.setProductId(productId);
            mock.setUserId(user.getId());
            mock.setMovementType("OUT");
            mock.setQuantity(quantity);
            mock.setReason(reason.toUpperCase());
            mock.setPreviousStock(20);
            mock.setNewStock(Math.max(0, 20 - quantity));
            mock.setCreatedAt(LocalDateTime.now());
            return mock;
        }

        int orgId = user.getOrganizationId() > 0 ? user.getOrganizationId() : 1;

        try (Connection conn = DatabaseConfig.getConnection(orgId)) {
            conn.setAutoCommit(false);
            try {
                checkUserActiveInDb(conn, user.getId(), orgId);

                String lockSql = "SELECT id, name, stock_quantity, low_stock_threshold FROM products WHERE id = ? AND organization_id = ? FOR UPDATE";
                int currentStock;
                String productName;

                try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                    lockStmt.setInt(1, productId);
                    lockStmt.setInt(2, orgId);
                    try (ResultSet rs = lockStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new InventoryException("Product not found (ID: " + productId + ")");
                        }
                        currentStock = rs.getInt("stock_quantity");
                        productName = rs.getString("name");
                    }
                }

                if (quantity > currentStock) {
                    throw new InventoryException("Cannot deduct " + quantity + " units. Current stock for '" +
                            productName + "' is only " + currentStock + ".");
                }

                int newStock = currentStock - quantity;

                String updateSql = "UPDATE products SET stock_quantity = ? WHERE id = ? AND organization_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, newStock);
                    updateStmt.setInt(2, productId);
                    updateStmt.setInt(3, orgId);
                    updateStmt.executeUpdate();
                }

                StockMovement movement = new StockMovement(
                        0,
                        orgId,
                        generateRefNo("OUT"),
                        productId,
                        user.getId(),
                        "OUT",
                        quantity,
                        reason.toUpperCase(),
                        currentStock,
                        newStock,
                        "Terminal deduction by " + user.getFullName(),
                        null
                );
                movement.setProductName(productName);

                StockMovement saved = insertMovementWithRetry(conn, movement);

                conn.commit();
                return saved;

            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof InventoryException ie) throw ie;
                if (ex instanceof AccessDeniedException ade) throw ade;
                if (ex instanceof SQLException se) throw se;
                throw new InventoryException("Error performing deduction: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public StockMovement addStock(int productId, int quantity, String reason, String notes, User user)
            throws InventoryException, AccessDeniedException, SQLException {

        if (user == null) {
            throw new AccessDeniedException("User session required.");
        }
        if (quantity <= 0) {
            throw new InventoryException("Restock quantity must be greater than zero.");
        }

        if (DatabaseConfig.isDevModeEnabled()) {
            StockMovement mock = new StockMovement();
            mock.setId((int) (System.currentTimeMillis() % 100000));
            mock.setOrganizationId(user.getOrganizationId());
            mock.setReferenceNo(generateRefNo("IN"));
            mock.setProductId(productId);
            mock.setUserId(user.getId());
            mock.setMovementType("IN");
            mock.setQuantity(quantity);
            mock.setReason(reason != null && !reason.isBlank() ? reason.toUpperCase() : "RESTOCK");
            mock.setPreviousStock(10);
            mock.setNewStock(10 + quantity);
            mock.setNotes(notes);
            mock.setCreatedAt(LocalDateTime.now());
            return mock;
        }

        int orgId = user.getOrganizationId() > 0 ? user.getOrganizationId() : 1;

        try (Connection conn = DatabaseConfig.getConnection(orgId)) {
            conn.setAutoCommit(false);
            try {
                checkUserRoleAndActiveInDb(conn, user.getId(), orgId, true);

                String lockSql = "SELECT id, name, stock_quantity FROM products WHERE id = ? AND organization_id = ? FOR UPDATE";
                int currentStock;
                String productName;
                try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                    lockStmt.setInt(1, productId);
                    lockStmt.setInt(2, orgId);
                    try (ResultSet rs = lockStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new InventoryException("Product not found (ID: " + productId + ")");
                        }
                        currentStock = rs.getInt("stock_quantity");
                        productName = rs.getString("name");
                    }
                }

                int newStock = currentStock + quantity;

                String updateSql = "UPDATE products SET stock_quantity = ? WHERE id = ? AND organization_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, newStock);
                    updateStmt.setInt(2, productId);
                    updateStmt.setInt(3, orgId);
                    updateStmt.executeUpdate();
                }

                String rsn = (reason != null && !reason.isBlank()) ? reason.toUpperCase() : "RESTOCK";
                StockMovement movement = new StockMovement(
                        0,
                        orgId,
                        generateRefNo("IN"),
                        productId,
                        user.getId(),
                        "IN",
                        quantity,
                        rsn,
                        currentStock,
                        newStock,
                        notes,
                        null
                );
                movement.setProductName(productName);
                StockMovement saved = insertMovementWithRetry(conn, movement);

                conn.commit();
                return saved;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof InventoryException ie) throw ie;
                if (ex instanceof AccessDeniedException ade) throw ade;
                if (ex instanceof SQLException se) throw se;
                throw new InventoryException("Error adding stock: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public StockMovement adjustStock(int productId, int physicalCount, String notes, User user)
            throws InventoryException, AccessDeniedException, SQLException {

        if (user == null) {
            throw new AccessDeniedException("User session required.");
        }
        if (physicalCount < 0) {
            throw new InventoryException("Physical count cannot be negative.");
        }

        if (DatabaseConfig.isDevModeEnabled()) {
            StockMovement mock = new StockMovement();
            mock.setId((int) (System.currentTimeMillis() % 100000));
            mock.setOrganizationId(user.getOrganizationId());
            mock.setReferenceNo(generateRefNo("ADJ"));
            mock.setProductId(productId);
            mock.setUserId(user.getId());
            mock.setMovementType("ADJUSTMENT");
            mock.setQuantity(Math.abs(physicalCount - 15));
            mock.setReason("AUDIT_CORRECTION");
            mock.setPreviousStock(15);
            mock.setNewStock(physicalCount);
            mock.setNotes(notes);
            mock.setCreatedAt(LocalDateTime.now());
            return mock;
        }

        int orgId = user.getOrganizationId() > 0 ? user.getOrganizationId() : 1;

        try (Connection conn = DatabaseConfig.getConnection(orgId)) {
            conn.setAutoCommit(false);
            try {
                checkUserRoleAndActiveInDb(conn, user.getId(), orgId, true);

                String lockSql = "SELECT id, name, stock_quantity FROM products WHERE id = ? AND organization_id = ? FOR UPDATE";
                int currentStock;
                String productName;
                try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                    lockStmt.setInt(1, productId);
                    lockStmt.setInt(2, orgId);
                    try (ResultSet rs = lockStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new InventoryException("Product not found (ID: " + productId + ")");
                        }
                        currentStock = rs.getInt("stock_quantity");
                        productName = rs.getString("name");
                    }
                }

                int diff = physicalCount - currentStock;

                String updateSql = "UPDATE products SET stock_quantity = ? WHERE id = ? AND organization_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, physicalCount);
                    updateStmt.setInt(2, productId);
                    updateStmt.setInt(3, orgId);
                    updateStmt.executeUpdate();
                }

                String countSql = "INSERT INTO stock_counts (organization_id, user_id, product_id, system_stock, physical_count, status, notes) " +
                                  "VALUES (?, ?, ?, ?, ?, 'APPLIED', ?)";
                try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                    countStmt.setInt(1, orgId);
                    countStmt.setInt(2, user.getId());
                    countStmt.setInt(3, productId);
                    countStmt.setInt(4, currentStock);
                    countStmt.setInt(5, physicalCount);
                    countStmt.setString(6, notes);
                    countStmt.executeUpdate();
                }

                int qty = Math.abs(diff);
                if (qty == 0) qty = 1; 
                StockMovement movement = new StockMovement(
                        0,
                        orgId,
                        generateRefNo("ADJ"),
                        productId,
                        user.getId(),
                        "ADJUSTMENT",
                        qty,
                        "AUDIT_CORRECTION",
                        currentStock,
                        physicalCount,
                        notes != null && !notes.isBlank() ? notes : "Physical audit adjustment (" + (diff >= 0 ? "+" : "") + diff + ")",
                        null
                );
                movement.setProductName(productName);
                StockMovement saved = insertMovementWithRetry(conn, movement);

                conn.commit();
                return saved;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof InventoryException ie) throw ie;
                if (ex instanceof AccessDeniedException ade) throw ade;
                if (ex instanceof SQLException se) throw se;
                throw new InventoryException("Error adjusting stock: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public record BulkDeductionItem(int productId, int quantity, String reason, String notes) {}

    public List<StockMovement> bulkDeduct(List<BulkDeductionItem> items, User user)
            throws InventoryException, AccessDeniedException, SQLException {

        if (user == null) {
            throw new AccessDeniedException("User session required.");
        }
        if (items == null || items.isEmpty()) {
            throw new InventoryException("Bulk deduction list cannot be empty.");
        }

        for (BulkDeductionItem item : items) {
            if (item.quantity() <= 0) {
                throw new InventoryException("Deduction quantity must be greater than zero for all items.");
            }
            if (user.getRole() == Role.EMPLOYEE) {
                String rsn = item.reason();
                if (!("SOLD".equalsIgnoreCase(rsn) || "USED".equalsIgnoreCase(rsn) ||
                      "DAMAGED".equalsIgnoreCase(rsn) || "EXPIRED".equalsIgnoreCase(rsn))) {
                    throw new AccessDeniedException("Employees are only permitted to record SOLD, USED, DAMAGED, or EXPIRED deductions.");
                }
            }
        }

        if (DatabaseConfig.isDevModeEnabled()) {
            List<StockMovement> list = new ArrayList<>();
            for (BulkDeductionItem item : items) {
                StockMovement mock = new StockMovement();
                mock.setId((int) (System.currentTimeMillis() % 100000));
                mock.setProductId(item.productId());
                mock.setQuantity(item.quantity());
                mock.setMovementType("OUT");
                mock.setReason(item.reason().toUpperCase());
                mock.setCreatedAt(LocalDateTime.now());
                list.add(mock);
            }
            return list;
        }

        int orgId = user.getOrganizationId() > 0 ? user.getOrganizationId() : 1;

        List<BulkDeductionItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparingInt(BulkDeductionItem::productId));

        try (Connection conn = DatabaseConfig.getConnection(orgId)) {
            conn.setAutoCommit(false);
            try {
                checkUserActiveInDb(conn, user.getId(), orgId);

                List<StockMovement> savedMovements = new ArrayList<>();

                for (BulkDeductionItem item : sortedItems) {
                    String lockSql = "SELECT id, name, stock_quantity, is_archived FROM products WHERE id = ? AND organization_id = ? FOR UPDATE";
                    int currentStock;
                    String productName;
                    boolean isArchived;
                    try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                        lockStmt.setInt(1, item.productId());
                        lockStmt.setInt(2, orgId);
                        try (ResultSet rs = lockStmt.executeQuery()) {
                            if (!rs.next()) {
                                throw new InventoryException("Product ID " + item.productId() + " not found.");
                            }
                            currentStock = rs.getInt("stock_quantity");
                            productName = rs.getString("name");
                            isArchived = rs.getBoolean("is_archived");
                        }
                    }

                    if (isArchived) {
                        throw new InventoryException("Cannot deduct from archived product '" + productName + "'.");
                    }

                    if (item.quantity() > currentStock) {
                        throw new InventoryException("Insufficient stock for '" + productName +
                                "'. Available: " + currentStock + ", Requested: " + item.quantity() + ".");
                    }

                    int newStock = currentStock - item.quantity();

                    String updateSql = "UPDATE products SET stock_quantity = ? WHERE id = ? AND organization_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, newStock);
                        updateStmt.setInt(2, item.productId());
                        updateStmt.setInt(3, orgId);
                        updateStmt.executeUpdate();
                    }

                    StockMovement sm = new StockMovement(
                            0,
                            orgId,
                            generateRefNo("OUT"),
                            item.productId(),
                            user.getId(),
                            "OUT",
                            item.quantity(),
                            item.reason().toUpperCase(),
                            currentStock,
                            newStock,
                            item.notes() != null ? item.notes() : "Bulk deduction",
                            null
                    );
                    sm.setProductName(productName);
                    savedMovements.add(insertMovementWithRetry(conn, sm));
                }

                conn.commit();
                return savedMovements;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof InventoryException ie) throw ie;
                if (ex instanceof AccessDeniedException ade) throw ade;
                if (ex instanceof SQLException se) throw se;
                throw new InventoryException("Error performing bulk deduction: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void checkUserActiveInDb(Connection conn, int userId, int organizationId) throws AccessDeniedException, SQLException {
        checkUserRoleAndActiveInDb(conn, userId, organizationId, false);
    }

    private void checkUserRoleAndActiveInDb(Connection conn, int userId, int organizationId, boolean requireManagerOrAbove)
            throws AccessDeniedException, SQLException {
        String sql = "SELECT u.is_active, r.name AS role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.id = ? AND u.organization_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || !rs.getBoolean("is_active")) {
                    throw new AccessDeniedException("User account is inactive or disabled.");
                }
                if (requireManagerOrAbove) {
                    String roleName = rs.getString("role_name");
                    if (!"OWNER".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName) && !"MANAGER".equalsIgnoreCase(roleName)) {
                        throw new AccessDeniedException("Only business owners and inventory managers are authorized to perform this operation.");
                    }
                }
            }
        }
    }

    private StockMovement insertMovementWithRetry(Connection conn, StockMovement movement) throws SQLException {
        java.sql.Savepoint sp = conn.setSavepoint("sp_insert_movement");
        try {
            StockMovement saved = movementDao.insertMovement(conn, movement);
            conn.releaseSavepoint(sp);
            return saved;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                conn.rollback(sp);
                movement.setReferenceNo(generateRefNo(movement.getMovementType()));
                return movementDao.insertMovement(conn, movement);
            }
            throw e;
        }
    }

    private String generateRefNo(String prefix) {
        return "SM-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static class InventoryException extends Exception { public InventoryException(String msg) { super(msg); } }
    public static class AccessDeniedException extends Exception { public AccessDeniedException(String msg) { super(msg); } }
}
