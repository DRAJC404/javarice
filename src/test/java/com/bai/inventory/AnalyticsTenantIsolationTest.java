package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.AnalyticsDao;
import com.bai.inventory.dao.AnalyticsDao.*;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AnalyticsService;
import com.bai.inventory.service.AuthService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsTenantIsolationTest {

    private static final int ORG_A = 777;
    private static final int ORG_B = 778;

    private static AnalyticsDao analyticsDao;
    private static AnalyticsService analyticsService;
    private static User userA;
    private static User userB;

    @BeforeAll
    static void setUpAll() throws SQLException {
        DatabaseConfig.initDataSource();
        analyticsDao = new AnalyticsDao();
        analyticsService = new AnalyticsService(analyticsDao);

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {

            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM products WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM categories WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM users WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM organizations WHERE id IN (" + ORG_A + ", " + ORG_B + ")");

            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + ORG_A + ", 'Tenant Analytics A', 'tenant-a', true)");
            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + ORG_B + ", 'Tenant Analytics B', 'tenant-b', true)");

            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (7771, " + ORG_A + ", 'usera@test.com', 'dummyhash', 'User A', 1, 'APPROVED', true)");
            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (7781, " + ORG_B + ", 'userb@test.com', 'dummyhash', 'User B', 1, 'APPROVED', true)");

            // Categories
            s.executeUpdate("INSERT INTO categories (id, organization_id, name) VALUES (77701, " + ORG_A + ", 'Org A Beverages')");
            s.executeUpdate("INSERT INTO categories (id, organization_id, name) VALUES (77801, " + ORG_B + ", 'Org B Hardware')");

            // Products
            s.executeUpdate("INSERT INTO products (id, organization_id, category_id, sku, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, status) " +
                    "VALUES (777001, " + ORG_A + ", 77701, 'SKU-A-1', 'Product A1', 'pcs', 10.00, 20.00, 50, 10, 'IN_STOCK')");
            s.executeUpdate("INSERT INTO products (id, organization_id, category_id, sku, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, status) " +
                    "VALUES (777002, " + ORG_A + ", 77701, 'SKU-A-2', 'Product A2', 'pcs', 5.00, 15.00, 3, 10, 'LOW_STOCK')");

            s.executeUpdate("INSERT INTO products (id, organization_id, category_id, sku, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, status) " +
                    "VALUES (778001, " + ORG_B + ", 77801, 'SKU-B-1', 'Product B1', 'pcs', 50.00, 100.00, 0, 5, 'OUT_OF_STOCK')");

            // Stock movements for Org A
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_A + ", 'REF-A-IN-1', 777001, 7771, 'IN', 100, 'RESTOCK', 0, 100, CURRENT_TIMESTAMP - interval '2 days')");
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_A + ", 'REF-A-OUT-1', 777001, 7771, 'OUT', 30, 'SOLD', 100, 70, CURRENT_TIMESTAMP - interval '1 day')");
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_A + ", 'REF-A-OUT-2', 777001, 7771, 'OUT', 5, 'DAMAGED', 70, 65, CURRENT_TIMESTAMP - interval '1 day')");

            // Stock movements for Org B
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_B + ", 'REF-B-IN-1', 778001, 7781, 'IN', 500, 'RESTOCK', 0, 500, CURRENT_TIMESTAMP - interval '3 days')");
            s.executeUpdate("INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, created_at) " +
                    "VALUES (" + ORG_B + ", 'REF-B-OUT-1', 778001, 7781, 'OUT', 250, 'SOLD', 500, 250, CURRENT_TIMESTAMP - interval '1 day')");
        }

        userA = new User(7771, ORG_A, 1, "User A", "usera@test.com", "dummyhash", "APPROVED", true);
        userB = new User(7781, ORG_B, 1, "User B", "userb@test.com", "dummyhash", "APPROVED", true);
    }

    @AfterAll
    static void tearDownAll() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM products WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM categories WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM users WHERE organization_id IN (" + ORG_A + ", " + ORG_B + ")");
            s.executeUpdate("DELETE FROM organizations WHERE id IN (" + ORG_A + ", " + ORG_B + ")");
        } catch (SQLException ignored) {}
    }

    @Test
    @Order(1)
    @DisplayName("1. Tenant A KPI summary reflects only Tenant A movements")
    void testTenantAKpiSummaryIsolation() {
        AuthService.setCurrentUser(userA);
        AnalyticsKpiSummary summaryA = analyticsDao.getKpiSummary(30, ORG_A);

        assertEquals(100.0, summaryA.inflow().currentVal(), "Tenant A should have 100 units inflow");
        assertEquals(35.0, summaryA.outflow().currentVal(), "Tenant A should have 35 units outflow (30 sold + 5 damaged)");
        assertEquals(65.0, summaryA.netMovement().currentVal(), "Tenant A net movement should be +65 units");

        // Outflow valuation: 35 units * 20.00 PHP selling price = 700.00
        assertEquals(700.0, summaryA.turnoverValue().currentVal(), 0.01, "Tenant A turnover should be 700.00 PHP");

        // Shrinkage: 5 damaged / 35 total * 100 = 14.3%
        assertEquals(14.3, summaryA.shrinkageRate().currentVal(), 0.1, "Tenant A shrinkage rate should be ~14.3%");
    }

    @Test
    @Order(2)
    @DisplayName("2. Tenant B KPI summary does not leak into Tenant A")
    void testTenantBKpiSummaryIsolation() {
        AuthService.setCurrentUser(userB);
        AnalyticsKpiSummary summaryB = analyticsDao.getKpiSummary(30, ORG_B);

        assertEquals(500.0, summaryB.inflow().currentVal(), "Tenant B should have 500 units inflow");
        assertEquals(250.0, summaryB.outflow().currentVal(), "Tenant B should have 250 units outflow");
        assertEquals(250.0, summaryB.netMovement().currentVal(), "Tenant B net movement should be 250");
    }

    @Test
    @Order(3)
    @DisplayName("3. Top moving products are strictly tenant-scoped")
    void testTopMovingProductsIsolation() {
        AuthService.setCurrentUser(userA);
        List<TopProduct> topA = analyticsDao.getTopMovingProducts(30, 5, ORG_A);

        assertFalse(topA.isEmpty(), "Tenant A should have top moving products");
        assertTrue(topA.stream().allMatch(p -> p.name().startsWith("Product A")), "All top products must belong to Tenant A");
        assertFalse(topA.stream().anyMatch(p -> p.name().startsWith("Product B")), "Tenant B products must NOT appear");

        AuthService.setCurrentUser(userB);
        List<TopProduct> topB = analyticsDao.getTopMovingProducts(30, 5, ORG_B);
        assertTrue(topB.stream().allMatch(p -> p.name().startsWith("Product B")), "All top products must belong to Tenant B");
    }

    @Test
    @Order(4)
    @DisplayName("4. Stock status donut distribution is isolated per tenant")
    void testStockStatusCountIsolation() {
        AuthService.setCurrentUser(userA);
        StockStatusCount statusA = analyticsDao.getStockStatusCount(ORG_A);
        assertEquals(1, statusA.inStock(), "Tenant A has 1 in-stock product");
        assertEquals(1, statusA.lowStock(), "Tenant A has 1 low-stock product");
        assertEquals(0, statusA.outOfStock(), "Tenant A has 0 out-of-stock products");

        AuthService.setCurrentUser(userB);
        StockStatusCount statusB = analyticsDao.getStockStatusCount(ORG_B);
        assertEquals(0, statusB.inStock(), "Tenant B has 0 in-stock products");
        assertEquals(0, statusB.lowStock(), "Tenant B has 0 low-stock products");
        assertEquals(1, statusB.outOfStock(), "Tenant B has 1 out-of-stock product");
    }

    @Test
    @Order(5)
    @DisplayName("5. Deductions by category and reasons are isolated")
    void testCategoryAndReasonDeductionsIsolation() {
        AuthService.setCurrentUser(userA);
        List<CategoryDeduction> catsA = analyticsDao.getDeductionsByCategory(30, 5, ORG_A);
        assertFalse(catsA.isEmpty());
        assertEquals("Org A Beverages", catsA.get(0).categoryName());

        List<ReasonDeduction> reasonsA = analyticsDao.getDeductionReasons(30, ORG_A);
        assertFalse(reasonsA.isEmpty());
        assertTrue(reasonsA.stream().anyMatch(r -> "SOLD".equals(r.reason()) && r.quantity() == 30));
        assertTrue(reasonsA.stream().anyMatch(r -> "DAMAGED".equals(r.reason()) && r.quantity() == 5));
    }
}
