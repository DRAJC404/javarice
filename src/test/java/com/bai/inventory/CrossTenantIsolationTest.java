package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.AiInsightDao;
import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.dao.StockMovementDao;
import com.bai.inventory.dao.UserDao;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrossTenantIsolationTest {

    private static final int ORG_A_ID = 1;
    private static final int ORG_B_ID = 2;

    private static ProductDao productDao;
    private static StockMovementDao stockMovementDao;
    private static AiInsightDao aiInsightDao;
    private static UserDao userDao;
    private static AuthService authService;

    @BeforeAll
    static void setUpAll() throws SQLException {
        DatabaseConfig.initDataSource();
        productDao = new ProductDao();
        stockMovementDao = new StockMovementDao();
        aiInsightDao = new AiInsightDao();
        userDao = new UserDao();
        authService = new AuthService(userDao);

        // Ensure Org B exists in organizations table
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO organizations (id, name, slug, is_active) " +
                     "VALUES (2, 'bAI Retail Cebu Store', 'bai-cebu', true) " +
                     "ON CONFLICT (id) DO NOTHING")) {
            stmt.executeUpdate();
        }

        cleanupTestTenants();
    }

    @AfterAll
    static void tearDownAll() {
        cleanupTestTenants();
    }

    private static void cleanupTestTenants() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id >= 2");
            s.executeUpdate("DELETE FROM products WHERE organization_id >= 2");
            s.executeUpdate("DELETE FROM categories WHERE organization_id >= 2");
            s.executeUpdate("DELETE FROM audit_log WHERE organization_id >= 2");
            s.executeUpdate("DELETE FROM users WHERE organization_id >= 2");
            s.executeUpdate("DELETE FROM organizations WHERE id >= 3");
        } catch (SQLException ignored) {}
    }

    @Test
    @Order(1)
    @DisplayName("No tenant context returns ZERO rows across all tenant tables")
    void testNoContextReturnsNoRows() throws SQLException {
        AuthService.setCurrentUser(null);
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Verify products
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM products");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "No context must return 0 products under RLS");
            }

            // Verify users
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "No context must return 0 users under RLS");
            }

            // Verify stock_movements
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM stock_movements");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "No context must return 0 movements under RLS");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Create business A, create business B, log in to each, confirm each sees only its own data")
    void testCreateBusinessAAndBAndLoginIsolation() throws Exception {
        AuthService.setCurrentUser(null);

        // Register Business A
        String emailA = "owner_a_" + System.currentTimeMillis() + "@alphamart.com";
        User ownerA = authService.registerBusinessOwner("Alpha Supermarket", "Alice Alpha", emailA, "Password123", "Password123", true);
        assertNotNull(ownerA);
        assertTrue(ownerA.getOrganizationId() > 0);
        int orgA = ownerA.getOrganizationId();

        // Register Business B
        String emailB = "owner_b_" + System.currentTimeMillis() + "@betastores.com";
        User ownerB = authService.registerBusinessOwner("Beta Provisions", "Bob Beta", emailB, "Password123", "Password123", true);
        assertNotNull(ownerB);
        assertTrue(ownerB.getOrganizationId() > 0);
        int orgB = ownerB.getOrganizationId();
        assertNotEquals(orgA, orgB, "Business A and Business B must have distinct organization IDs");

        // Log in to Business A
        User loginA = authService.authenticate(emailA, "Password123", false);
        assertNotNull(loginA);
        assertEquals(orgA, loginA.getOrganizationId());

        // Create a product for Business A
        Category catA;
        try (Connection conn = DatabaseConfig.getConnection(orgA)) {
            String catSql = "INSERT INTO categories (organization_id, name, description) VALUES (?, 'A-Cat', 'Desc A') RETURNING id";
            try (PreparedStatement cs = conn.prepareStatement(catSql)) {
                cs.setInt(1, orgA);
                try (ResultSet rs = cs.executeQuery()) {
                    assertTrue(rs.next());
                    catA = new Category(rs.getInt(1), "A-Cat", "Desc A");
                }
            }
            conn.commit();
        }

        Product prodA = new Product();
        prodA.setCategoryId(catA.getId());
        prodA.setSku("SKU-ALPHA-01");
        prodA.setName("Alpha Instant Coffee");
        prodA.setUnit("pack");
        prodA.setCostPrice(new BigDecimal("50.00"));
        prodA.setSellingPrice(new BigDecimal("85.00"));
        prodA.setStockQuantity(100);
        prodA.setLowStockThreshold(15);
        Product createdA = productDao.createProduct(prodA, orgA, loginA.getId());
        assertNotNull(createdA);

        // Log in to Business B
        User loginB = authService.authenticate(emailB, "Password123", false);
        assertNotNull(loginB);
        assertEquals(orgB, loginB.getOrganizationId());

        // Confirm Business B sees ZERO products from Business A
        List<Product> bProducts = productDao.findAll("", null, orgB);
        assertTrue(bProducts.isEmpty(), "Business B must see 0 products (Business A's product must be hidden)");

        // Business A sees its product
        List<Product> aProducts = productDao.findAll("", null, orgA);
        assertEquals(1, aProducts.size());
        assertEquals("SKU-ALPHA-01", aProducts.get(0).getSku());
    }

    @Test
    @Order(3)
    @DisplayName("Cross-tenant INSERT with another org_id is BLOCKED by PostgreSQL RLS")
    void testInsertWithAnotherOrgIdBlocked() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(ORG_A_ID)) {
            // Connected under Org A, attempt to insert a product explicitly setting organization_id = 2 (Org B)
            String attackSql = "INSERT INTO products (organization_id, category_id, sku, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold) " +
                               "VALUES (2, 1, 'ATTACK-SKU', 'Malicious Product', 'pcs', 10, 20, 5, 2)";

            try (PreparedStatement stmt = conn.prepareStatement(attackSql)) {
                assertThrows(SQLException.class, stmt::executeUpdate,
                        "PostgreSQL RLS must throw SQLException when inserting a row with another organization_id");
            }
            conn.rollback();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Cross-tenant UPDATE attack is BLOCKED by PostgreSQL RLS (0 rows affected)")
    void testCrossTenantUpdateAttackBlocked() throws SQLException {
        // Attempt to update Org A's product (id = 1) from an Org B session
        try (Connection conn = DatabaseConfig.getConnection(ORG_B_ID);
             PreparedStatement stmt = conn.prepareStatement("UPDATE products SET stock_quantity = 9999 WHERE id = 1")) {
            int rowsAffected = stmt.executeUpdate();
            assertEquals(0, rowsAffected, "PostgreSQL RLS must prevent Org B from updating Org A's product row");
            conn.commit();
        }

        // Verify product 1 in Org A was NOT modified
        try (Connection conn = DatabaseConfig.getConnection(ORG_A_ID);
             PreparedStatement stmt = conn.prepareStatement("SELECT stock_quantity FROM products WHERE id = 1")) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Org A product 1 must be visible under Org A session");
                assertNotEquals(9999, rs.getInt("stock_quantity"), "Org A product stock must remain intact");
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Cross-tenant DELETE attack is BLOCKED by PostgreSQL RLS (0 rows affected)")
    void testCrossTenantDeleteAttackBlocked() throws SQLException {
        // Attempt to delete Org A's product (id = 1) from an Org B session
        try (Connection conn = DatabaseConfig.getConnection(ORG_B_ID);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM products WHERE id = 1")) {
            int rowsAffected = stmt.executeUpdate();
            assertEquals(0, rowsAffected, "PostgreSQL RLS must prevent Org B from deleting Org A's product row");
            conn.commit();
        }

        // Verify product 1 in Org A still exists
        try (Connection conn = DatabaseConfig.getConnection(ORG_A_ID);
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE id = 1")) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Org A product must still exist");
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Pooled-connection reuse: context is always reset and does not leak between connections")
    void testPooledConnectionReuseNoLeak() throws SQLException {
        AuthService.setCurrentUser(null);

        // 1. Borrow connection with Org A context (in transaction)
        try (Connection conn1 = DatabaseConfig.getConnection(ORG_A_ID)) {
            try (PreparedStatement ps = conn1.prepareStatement("SELECT current_setting('app.current_organization_id', true)");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("1", rs.getString(1));
            }
        } // conn1 returned to pool here

        // 2. Borrow a connection without specifying tenant context
        try (Connection conn2 = DatabaseConfig.getConnection()) {
            // Verify context is empty/reset
            try (PreparedStatement ps = conn2.prepareStatement("SELECT current_setting('app.current_organization_id', true)");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String val = rs.getString(1);
                assertTrue(val == null || val.isEmpty(), "Context must be empty/reset on fresh connection borrow");
            }

            // Verify query returns 0 rows
            try (PreparedStatement ps = conn2.prepareStatement("SELECT count(*) FROM products");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Clean pooled connection must see 0 products under RLS");
            }
        }
    }
}
