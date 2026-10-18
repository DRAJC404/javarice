package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.ReportDao;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.ReportService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportServiceTest {

    private static final int ORG_A_ID = 911;
    private static final int ORG_B_ID = 912;

    private static ReportDao reportDao;
    private static ReportService reportService;

    private static User ownerA;
    private static User employeeA;
    private static User ownerB;

    @BeforeAll
    static void setUpAll() throws SQLException {
        DatabaseConfig.initDataSource();
        reportDao = new ReportDao();
        reportService = new ReportService(reportDao);

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM products WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM categories WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM users WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM organizations WHERE id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");

            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + ORG_A_ID + ", 'Report Org A', 'rep-a', true)");
            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + ORG_B_ID + ", 'Report Org B', 'rep-b', true)");

            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (9111, " + ORG_A_ID + ", 'rep_ownerA@test.com', 'dummy', 'Rep Owner A', 1, 'APPROVED', true)");
            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (9112, " + ORG_A_ID + ", 'rep_empA@test.com', 'dummy', 'Rep Emp A', 3, 'APPROVED', true)");
            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (9121, " + ORG_B_ID + ", 'rep_ownerB@test.com', 'dummy', 'Rep Owner B', 1, 'APPROVED', true)");

            s.executeUpdate("INSERT INTO categories (id, organization_id, name) VALUES (91101, " + ORG_A_ID + ", 'Office')");
            s.executeUpdate("INSERT INTO categories (id, organization_id, name) VALUES (91201, " + ORG_B_ID + ", 'Pantry')");

            // Products for Org A:
            // 1. In stock, healthy: stock 50, low_stock_threshold 10, cost 15.00, selling 25.00
            s.executeUpdate("INSERT INTO products (id, organization_id, category_id, name, sku, barcode, stock_quantity, low_stock_threshold, cost_price, selling_price, unit, is_archived) " +
                    "VALUES (91101, " + ORG_A_ID + ", 91101, 'Stapler', 'STAP-01', 'BAR-STAP', 50, 10, 15.00, 25.00, 'pcs', false)");
            // 2. Low stock: stock 3, low_stock_threshold 5, cost 20.00, selling 40.00
            s.executeUpdate("INSERT INTO products (id, organization_id, category_id, name, sku, barcode, stock_quantity, low_stock_threshold, cost_price, selling_price, unit, is_archived) " +
                    "VALUES (91102, " + ORG_A_ID + ", 91101, 'Binder', 'BIND-01', 'BAR-BIND', 3, 5, 20.00, 40.00, 'pcs', false)");

            // Products for Org B:
            s.executeUpdate("INSERT INTO products (id, organization_id, category_id, name, sku, barcode, stock_quantity, low_stock_threshold, cost_price, selling_price, unit, is_archived) " +
                    "VALUES (91201, " + ORG_B_ID + ", 91201, 'Coffee Beans', 'COF-01', 'BAR-COF', 2, 10, 30.00, 60.00, 'bag', false)");

            // Stock movements for Org A
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_A_ID + ", 'SM-REP-01', 91101, 9111, 'IN', 50, 'OPENING_STOCK', 0, 50, NOW() - INTERVAL '2 days')");
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_A_ID + ", 'SM-REP-02', 91102, 9111, 'OUT', 2, 'DAMAGED', 5, 3, NOW() - INTERVAL '1 day')");

            // Stock movements for Org B
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_B_ID + ", 'SM-REP-03', 91201, 9121, 'IN', 10, 'RESTOCK', 0, 10, NOW() - INTERVAL '1 day')");
        }

        ownerA = new User();
        ownerA.setId(9111);
        ownerA.setOrganizationId(ORG_A_ID);
        ownerA.setRole(Role.OWNER);

        employeeA = new User();
        employeeA.setId(9112);
        employeeA.setOrganizationId(ORG_A_ID);
        employeeA.setRole(Role.EMPLOYEE);

        ownerB = new User();
        ownerB.setId(9121);
        ownerB.setOrganizationId(ORG_B_ID);
        ownerB.setRole(Role.OWNER);
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM products WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM categories WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM users WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM organizations WHERE id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
        }
    }

    @Test
    @Order(1)
    void testTenantIsolationInInventoryReport() {
        List<Product> productsA = reportService.getInventory(ownerA);
        assertEquals(2, productsA.size(), "Org A should see exactly 2 inventory products");
        assertTrue(productsA.stream().anyMatch(p -> p.getName().equals("Stapler")));
        assertTrue(productsA.stream().anyMatch(p -> p.getName().equals("Binder")));
        assertFalse(productsA.stream().anyMatch(p -> p.getName().equals("Coffee Beans")), "Org A must not see Org B products");

        List<Product> productsB = reportService.getInventory(ownerB);
        assertEquals(1, productsB.size(), "Org B should see exactly 1 product");
        assertEquals("Coffee Beans", productsB.get(0).getName());
    }

    @Test
    @Order(2)
    void testRoleMaskingInReports() {
        // Owner sees cost price
        List<Product> ownerInventory = reportService.getInventory(ownerA);
        Product stapler = ownerInventory.stream().filter(p -> p.getName().equals("Stapler")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("15.00"), stapler.getCostPrice(), "Owner should see unmasked cost price");

        // Employee has cost price masked to ZERO
        List<Product> empInventory = reportService.getInventory(employeeA);
        Product empStapler = empInventory.stream().filter(p -> p.getName().equals("Stapler")).findFirst().orElseThrow();
        assertEquals(BigDecimal.ZERO, empStapler.getCostPrice(), "Employee must have cost price masked to 0");
    }

    @Test
    @Order(3)
    void testLowStockReportFiltering() {
        List<Product> lowStockA = reportService.getLowStock(ownerA);
        assertEquals(1, lowStockA.size(), "Only Binder has current_stock (3) <= threshold (5)");
        assertEquals("Binder", lowStockA.get(0).getName());
    }

    @Test
    @Order(4)
    void testStockMovementsDateRangeAndTenancy() {
        LocalDateTime from = LocalDateTime.now().minusDays(5);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        List<StockMovement> movementsA = reportService.getMovements(ownerA, from, to);
        assertEquals(2, movementsA.size(), "Org A should have 2 movements");
        assertTrue(movementsA.stream().allMatch(m -> m.getOrganizationId() == ORG_A_ID));

        List<StockMovement> movementsB = reportService.getMovements(ownerB, from, to);
        assertEquals(1, movementsB.size(), "Org B should have 1 movement");
        assertEquals("Coffee Beans", movementsB.get(0).getProductName());

        // Narrow date range test (movements occurred 1-2 days ago, test 5 to 4 days ago)
        List<StockMovement> oldMovements = reportService.getMovements(ownerA, LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(5));
        assertEquals(0, oldMovements.size(), "No movements should be returned outside the date range");
    }

    @Test
    @Order(5)
    void testExportToCsv() throws IOException {
        File tempCsv = File.createTempFile("test_report_", ".csv");
        tempCsv.deleteOnExit();

        List<String> headers = Arrays.asList("SKU", "Product Name", "Notes");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("SKU-1", "Simple Item", "No issues"),
                Arrays.asList("SKU-2", "Item with, comma", "Item with \"quotes\"")
        );

        reportService.exportToCsv(tempCsv, headers, rows);

        assertTrue(tempCsv.exists());
        assertTrue(tempCsv.length() > 0);

        List<String> lines = Files.readAllLines(tempCsv.toPath());
        assertEquals(3, lines.size());
        assertEquals("\"SKU\",\"Product Name\",\"Notes\"", lines.get(0));
        assertEquals("\"SKU-1\",\"Simple Item\",\"No issues\"", lines.get(1));
        assertEquals("\"SKU-2\",\"Item with, comma\",\"Item with \"\"quotes\"\"\"", lines.get(2));
    }

    @Test
    @Order(6)
    void testExportToPdf() throws Exception {
        File tempPdf = File.createTempFile("test_report_", ".pdf");
        tempPdf.deleteOnExit();

        List<String> headers = Arrays.asList("Date", "Product", "Type", "Qty", "Reason");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("2026-10-18 10:00", "Stapler", "IN", "50", "OPENING_STOCK"),
                Arrays.asList("2026-10-18 11:30", "Binder", "OUT", "2", "DAMAGED")
        );

        reportService.exportToPdf(tempPdf, "Stock Movement Report", "Last 7 Days", headers, rows);

        assertTrue(tempPdf.exists());
        assertTrue(tempPdf.length() > 500, "PDF file should be non-empty with binary headers");

        // Validate PDF magic bytes: %PDF-
        try (FileInputStream fis = new FileInputStream(tempPdf)) {
            byte[] header = new byte[5];
            int read = fis.read(header);
            assertEquals(5, read);
            assertEquals("%PDF-", new String(header));
        }
    }
}
