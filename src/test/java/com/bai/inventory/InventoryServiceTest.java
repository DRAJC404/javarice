package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.dao.StockMovementDao;
import com.bai.inventory.dao.UserDao;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.InventoryService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryServiceTest {

    private static final int ORG_ID = 1;
    private static InventoryService inventoryService;
    private static ProductDao productDao;
    private static StockMovementDao movementDao;

    private static User testOwner;
    private static User testManager;
    private static User testEmployee;
    private static Product testProdA;
    private static Product testProdB;

    @BeforeAll
    static void setUpAll() throws Exception {
        DatabaseConfig.initDataSource();
        movementDao = new StockMovementDao();
        inventoryService = new InventoryService(movementDao);
        productDao = new ProductDao();

        // Create test users in DB
        try (Connection rootConn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "")) {
            // Owner (Role ID 1)
            try (PreparedStatement s = rootConn.prepareStatement(
                    "INSERT INTO users (organization_id, role_id, full_name, email, password_hash, approval_status, is_active) " +
                    "VALUES (?, 1, 'Test Owner', 'test_inv_owner@bai.ph', '$2a$12$e80yqVbX12z.gR4E/rU2..wH0l9M/Zk2YmQ3e4r5t6y7u8i9o0p1q', 'APPROVED', true) " +
                    "ON CONFLICT (email) DO UPDATE SET is_active = true, approval_status = 'APPROVED' RETURNING id")) {
                s.setInt(1, ORG_ID);
                try (ResultSet rs = s.executeQuery()) {
                    rs.next();
                    testOwner = new User(rs.getInt("id"), ORG_ID, 1, "Test Owner", "test_inv_owner@bai.ph", null, "APPROVED", true);
                }
            }

            // Manager (Role ID 2)
            try (PreparedStatement s = rootConn.prepareStatement(
                    "INSERT INTO users (organization_id, role_id, full_name, email, password_hash, approval_status, is_active) " +
                    "VALUES (?, 2, 'Test Manager', 'test_inv_manager@bai.ph', '$2a$12$e80yqVbX12z.gR4E/rU2..wH0l9M/Zk2YmQ3e4r5t6y7u8i9o0p1q', 'APPROVED', true) " +
                    "ON CONFLICT (email) DO UPDATE SET is_active = true, approval_status = 'APPROVED' RETURNING id")) {
                s.setInt(1, ORG_ID);
                try (ResultSet rs = s.executeQuery()) {
                    rs.next();
                    testManager = new User(rs.getInt("id"), ORG_ID, 2, "Test Manager", "test_inv_manager@bai.ph", null, "APPROVED", true);
                }
            }

            // Employee (Role ID 3)
            try (PreparedStatement s = rootConn.prepareStatement(
                    "INSERT INTO users (organization_id, role_id, full_name, email, password_hash, approval_status, is_active) " +
                    "VALUES (?, 3, 'Test Employee', 'test_inv_emp@bai.ph', '$2a$12$e80yqVbX12z.gR4E/rU2..wH0l9M/Zk2YmQ3e4r5t6y7u8i9o0p1q', 'APPROVED', true) " +
                    "ON CONFLICT (email) DO UPDATE SET is_active = true, approval_status = 'APPROVED' RETURNING id")) {
                s.setInt(1, ORG_ID);
                try (ResultSet rs = s.executeQuery()) {
                    rs.next();
                    testEmployee = new User(rs.getInt("id"), ORG_ID, 3, "Test Employee", "test_inv_emp@bai.ph", null, "APPROVED", true);
                }
            }

            // Test Category
            int catId = 1;

            // Product A: 50 in stock
            try (PreparedStatement s = rootConn.prepareStatement(
                    "INSERT INTO products (organization_id, category_id, sku, barcode, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, status) " +
                    "VALUES (?, ?, 'TEST-INV-A', '990001', 'Test Inv Product A', 'pcs', 10.00, 20.00, 50, 10, 'IN_STOCK') " +
                    "ON CONFLICT (organization_id, sku) DO UPDATE SET stock_quantity = 50 RETURNING id")) {
                s.setInt(1, ORG_ID);
                s.setInt(2, catId);
                try (ResultSet rs = s.executeQuery()) {
                    rs.next();
                    testProdA = new Product();
                    testProdA.setId(rs.getInt("id"));
                    testProdA.setName("Test Inv Product A");
                    testProdA.setStockQuantity(50);
                }
            }

            // Product B: 20 in stock
            try (PreparedStatement s = rootConn.prepareStatement(
                    "INSERT INTO products (organization_id, category_id, sku, barcode, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, status) " +
                    "VALUES (?, ?, 'TEST-INV-B', '990002', 'Test Inv Product B', 'pcs', 15.00, 30.00, 20, 5, 'IN_STOCK') " +
                    "ON CONFLICT (organization_id, sku) DO UPDATE SET stock_quantity = 20 RETURNING id")) {
                s.setInt(1, ORG_ID);
                s.setInt(2, catId);
                try (ResultSet rs = s.executeQuery()) {
                    rs.next();
                    testProdB = new Product();
                    testProdB.setId(rs.getInt("id"));
                    testProdB.setName("Test Inv Product B");
                    testProdB.setStockQuantity(20);
                }
            }
        }
    }

    @Test
    @Order(1)
    void testOwnerAddStockSuccess() throws Exception {
        int initialStock = getProductStock(testProdA.getId());
        StockMovement sm = inventoryService.addStock(testProdA.getId(), 25, "RESTOCK", "Weekly replenishment", testOwner);

        assertNotNull(sm);
        assertEquals("IN", sm.getMovementType());
        assertEquals(25, sm.getQuantity());
        assertEquals(initialStock + 25, getProductStock(testProdA.getId()));
    }

    @Test
    @Order(2)
    void testEmployeeAddStockAccessDenied() {
        assertThrows(InventoryService.AccessDeniedException.class, () -> {
            inventoryService.addStock(testProdA.getId(), 10, "RESTOCK", null, testEmployee);
        });
    }

    @Test
    @Order(3)
    void testManagerAdjustStockSuccess() throws Exception {
        StockMovement sm = inventoryService.adjustStock(testProdB.getId(), 35, "Physical inventory audit count", testManager);

        assertNotNull(sm);
        assertEquals("ADJUSTMENT", sm.getMovementType());
        assertEquals(35, getProductStock(testProdB.getId()));

        // Verify row in stock_counts
        try (Connection conn = DatabaseConfig.getConnection(ORG_ID);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT physical_count, difference, status FROM stock_counts WHERE product_id = ? ORDER BY id DESC LIMIT 1")) {
            stmt.setInt(1, testProdB.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(35, rs.getInt("physical_count"));
                assertEquals("APPLIED", rs.getString("status"));
            }
        }
    }

    @Test
    @Order(4)
    void testEmployeeAdjustStockAccessDenied() {
        assertThrows(InventoryService.AccessDeniedException.class, () -> {
            inventoryService.adjustStock(testProdB.getId(), 10, "Unauthorized adjustment", testEmployee);
        });
    }

    @Test
    @Order(5)
    void testBulkDeductAtomicRollbackOnInsufficient() throws Exception {
        int stockA = getProductStock(testProdA.getId());
        int stockB = getProductStock(testProdB.getId());

        // Prod A has plenty, but Prod B only has 35 and we request 9999
        List<InventoryService.BulkDeductionItem> items = List.of(
                new InventoryService.BulkDeductionItem(testProdA.getId(), 5, "SOLD", "Sale line 1"),
                new InventoryService.BulkDeductionItem(testProdB.getId(), 9999, "SOLD", "Sale line 2")
        );

        assertThrows(InventoryService.InventoryException.class, () -> {
            inventoryService.bulkDeduct(items, testEmployee);
        });

        // Ensure ATOMIC rollback: Prod A must NOT have been deducted!
        assertEquals(stockA, getProductStock(testProdA.getId()), "Prod A stock must be rolled back on bulk deduction error");
        assertEquals(stockB, getProductStock(testProdB.getId()), "Prod B stock must remain unchanged");
    }

    @Test
    @Order(6)
    void testBulkDeductSuccess() throws Exception {
        int stockA = getProductStock(testProdA.getId());
        int stockB = getProductStock(testProdB.getId());

        List<InventoryService.BulkDeductionItem> items = List.of(
                new InventoryService.BulkDeductionItem(testProdA.getId(), 4, "SOLD", "Batch 1"),
                new InventoryService.BulkDeductionItem(testProdB.getId(), 5, "USED", "Batch 2")
        );

        List<StockMovement> saved = inventoryService.bulkDeduct(items, testEmployee);

        assertEquals(2, saved.size());
        assertEquals(stockA - 4, getProductStock(testProdA.getId()));
        assertEquals(stockB - 5, getProductStock(testProdB.getId()));
    }

    @Test
    @Order(7)
    void testDeductionIsFinal() throws Exception {
        int currentStock = getProductStock(testProdA.getId());

        // Perform deduction
        StockMovement deducted = inventoryService.deductStock(testProdA.getId(), 6, "DAMAGED", testEmployee);
        assertEquals(currentStock - 6, getProductStock(testProdA.getId()));
        assertNotNull(deducted);
        assertEquals("OUT", deducted.getMovementType());
        assertEquals("DAMAGED", deducted.getReason());

        // Verify movement record is saved in stock_movements table for audit
        try (Connection conn = DatabaseConfig.getConnection(ORG_ID);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, movement_type, reason, quantity FROM stock_movements WHERE id = ?")) {
            stmt.setInt(1, deducted.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("OUT", rs.getString("movement_type"));
                assertEquals("DAMAGED", rs.getString("reason"));
                assertEquals(6, rs.getInt("quantity"));
            }
        }
    }

    private static int getProductStock(int productId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(ORG_ID);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT stock_quantity FROM products WHERE id = ? AND organization_id = ?")) {
            stmt.setInt(1, productId);
            stmt.setInt(2, ORG_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("stock_quantity");
            }
        }
        return -1;
    }
}
