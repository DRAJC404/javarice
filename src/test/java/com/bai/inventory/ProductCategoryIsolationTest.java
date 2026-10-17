package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.ProductService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductCategoryIsolationTest {

    private static final int ORG_TEST_ID = 888;
    private static final int ORG_OTHER_ID = 1;

    private static ProductDao productDao;
    private static ProductService productService;
    private static User testOwner;

    @BeforeAll
    static void setUpAll() throws SQLException {
        DatabaseConfig.initDataSource();
        productDao = new ProductDao();
        productService = new ProductService(productDao);

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM products WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM categories WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM users WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM organizations WHERE id = " + ORG_TEST_ID);

            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + ORG_TEST_ID + ", 'Zero Category Corp', 'zero-cat', true)");
            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (8881, " + ORG_TEST_ID + ", 'owner888@test.com', 'dummyhash', 'Test Owner 888', 1, 'APPROVED', true)");
        }

        testOwner = new User(8881, ORG_TEST_ID, 1, "Test Owner 888", "owner888@test.com", "dummyhash", "APPROVED", true);
    }

    @AfterAll
    static void tearDownAll() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM products WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM categories WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM users WHERE organization_id = " + ORG_TEST_ID);
            s.executeUpdate("DELETE FROM organizations WHERE id = " + ORG_TEST_ID);
        } catch (SQLException ignored) {}
    }

    @Test
    @Order(1)
    @DisplayName("1. A new business with zero categories can add a product with no category (null)")
    void testNewBusinessWithZeroCategoriesCanAddProductWithNoCategory() throws Exception {
        AuthService.setCurrentUser(testOwner);

        List<Category> cats = productService.getCategories(testOwner);
        assertTrue(cats.isEmpty(), "New business should have zero categories initially");

        Product p = new Product();
        p.setOrganizationId(ORG_TEST_ID);
        p.setName("Uncategorized Coffee Cup");
        p.setCategoryId(null);
        p.setSku("UNCAT-001");
        p.setBarcode("123456789012");
        p.setUnit("pcs");
        p.setCostPrice(new BigDecimal("25.00"));
        p.setSellingPrice(new BigDecimal("50.00"));
        p.setStockQuantity(10);
        p.setLowStockThreshold(5);

        Product created = productService.createProduct(p, testOwner);

        assertNotNull(created, "Product should be created successfully without category");
        assertTrue(created.getId() > 0, "Created product should have a positive ID");
        assertNull(created.getCategoryId(), "Created product category_id must be null");

        try (Connection conn = DatabaseConfig.getConnection(ORG_TEST_ID);
             PreparedStatement stmt = conn.prepareStatement("SELECT category_id FROM products WHERE id = ? AND organization_id = ?")) {
            stmt.setInt(1, created.getId());
            stmt.setInt(2, ORG_TEST_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Product record must exist in DB");
                assertNull(rs.getObject("category_id"), "DB column category_id must be SQL NULL");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("2. Business can add a product with a newly created category")
    void testBusinessCanAddProductWithNewlyCreatedCategory() throws Exception {
        AuthService.setCurrentUser(testOwner);

        Category newCat = productService.createCategory("House Specials", "Custom house blends", testOwner);
        assertNotNull(newCat, "Category should be created");
        assertTrue(newCat.getId() > 0, "Category should have a positive ID");

        Product p = new Product();
        p.setOrganizationId(ORG_TEST_ID);
        p.setName("House Signature Blend");
        p.setCategoryId(newCat.getId());
        p.setSku("HOUSE-001");
        p.setBarcode("987654321098");
        p.setUnit("bag");
        p.setCostPrice(new BigDecimal("150.00"));
        p.setSellingPrice(new BigDecimal("300.00"));
        p.setStockQuantity(20);
        p.setLowStockThreshold(5);

        Product created = productService.createProduct(p, testOwner);

        assertNotNull(created, "Product with category should be created successfully");
        assertEquals(newCat.getId(), created.getCategoryId(), "Created product category_id should match new category");

        try (Connection conn = DatabaseConfig.getConnection(ORG_TEST_ID);
             PreparedStatement stmt = conn.prepareStatement("SELECT category_id FROM products WHERE id = ? AND organization_id = ?")) {
            stmt.setInt(1, created.getId());
            stmt.setInt(2, ORG_TEST_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(newCat.getId(), rs.getInt("category_id"));
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Using another business's category id is rejected by ProductService validation")
    void testUsingAnotherBusinessCategoryIsRejectedByService() throws SQLException {
        AuthService.setCurrentUser(testOwner);

        int foreignCategoryId = 1;
        try (Connection conn = DatabaseConfig.getConnection(ORG_OTHER_ID);
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM categories WHERE organization_id = ? LIMIT 1")) {
            stmt.setInt(1, ORG_OTHER_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    foreignCategoryId = rs.getInt("id");
                }
            }
        }

        Product p = new Product();
        p.setOrganizationId(ORG_TEST_ID);
        p.setName("Foreign Category Product");
        p.setCategoryId(foreignCategoryId);
        p.setSku("FOREIGN-001");
        p.setUnit("pcs");
        p.setCostPrice(new BigDecimal("10.00"));
        p.setSellingPrice(new BigDecimal("20.00"));

        ProductService.ValidationException ex = assertThrows(
                ProductService.ValidationException.class,
                () -> productService.createProduct(p, testOwner),
                "Should reject foreign category ID with friendly validation message"
        );

        assertEquals("Selected category no longer exists.", ex.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("4. Using another business's category id is rejected by composite foreign key fk_products_org_category")
    void testUsingAnotherBusinessCategoryIsRejectedByDatabaseConstraint() {
        SQLException ex = assertThrows(SQLException.class, () -> {
            try (Connection conn = DatabaseConfig.getConnection(ORG_TEST_ID);
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO products (organization_id, category_id, sku, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold) " +
                         "VALUES (?, 1, 'RAW-FAIL', 'Raw Failure Item', 'pcs', 10, 20, 5, 2)")) {
                stmt.setInt(1, ORG_TEST_ID);
                stmt.executeUpdate();
            }
        });

        assertEquals("23503", ex.getSQLState(), "Database must reject mismatched organization_id / category_id with SQLState 23503");
        assertTrue(ex.getMessage().contains("fk_products_org_category") || ex.getMessage().contains("foreign key constraint"),
                "Constraint fk_products_org_category should be violated");
    }
}
