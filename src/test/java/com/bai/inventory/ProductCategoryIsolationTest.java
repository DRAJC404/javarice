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
    @DisplayName("1. ProductService requires category, but database column category_id remains nullable")
    void testNewBusinessWithZeroCategoriesRequiresCategoryInService() throws Exception {
        AuthService.setCurrentUser(testOwner);

        List<Category> cats = productService.getCategories(testOwner);
        assertTrue(cats.isEmpty(), "New business should have zero categories initially");

        Product p = new Product();
        p.setOrganizationId(ORG_TEST_ID);
        p.setName("Uncategorized Coffee Cup");
        p.setCategoryId(null);
        p.setImagePath("/images/cup.png");
        p.setSku("UNCAT-001");
        p.setBarcode("123456789012");
        p.setUnit("pcs");
        p.setCostPrice(new BigDecimal("25.00"));
        p.setSellingPrice(new BigDecimal("50.00"));
        p.setStockQuantity(10);
        p.setLowStockThreshold(5);

        // ProductService must reject null category
        ProductService.ValidationException ex = assertThrows(
                ProductService.ValidationException.class,
                () -> productService.createProduct(p, testOwner),
                "ProductService should reject product without category"
        );
        assertTrue(ex.hasFieldError("category"));

        // Database level column category_id must be nullable
        try (Connection conn = DatabaseConfig.getConnection(ORG_TEST_ID);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO products (organization_id, name, category_id, sku, barcode, unit, cost_price, selling_price, stock_quantity, low_stock_threshold) " +
                     "VALUES (?, 'DB Null Category Cup', NULL, 'DB-NULL-001', '123456789012', 'pcs', 25, 50, 10, 5) RETURNING id")) {
            stmt.setInt(1, ORG_TEST_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Database must allow null category_id directly");
                int newId = rs.getInt(1);
                assertTrue(newId > 0);
            }
            conn.commit();
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
        p.setImagePath("/images/blend.png");
        p.setSku("HOUSE-001");
        p.setBarcode("987654321098");
        p.setUnit("pcs");
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
        p.setImagePath("/images/foreign.png");
        p.setSku("FOREIGN-001");
        p.setBarcode("987654321999");
        p.setUnit("pcs");
        p.setCostPrice(new BigDecimal("10.00"));
        p.setSellingPrice(new BigDecimal("20.00"));
        p.setStockQuantity(5);
        p.setLowStockThreshold(2);

        ProductService.ValidationException ex = assertThrows(
                ProductService.ValidationException.class,
                () -> productService.createProduct(p, testOwner),
                "Should reject foreign category ID with friendly validation message"
        );

        assertTrue(ex.hasFieldError("category"), "Error must be on category field");
        assertTrue(ex.getMessage().contains("category"), "Message should mention category");
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

    @Test
    @Order(5)
    @DisplayName("5. Category creation validates empty name, max 60 chars, and case-insensitive duplicates")
    void testCategoryCreationValidation() {
        AuthService.setCurrentUser(testOwner);

        // Empty / whitespace
        ProductService.ValidationException exEmpty = assertThrows(
                ProductService.ValidationException.class,
                () -> productService.createCategory("   ", "Desc", testOwner)
        );
        assertEquals("Please enter a category name.", exEmpty.getMessage());

        // Max 60 chars
        String tooLongName = "A".repeat(61);
        ProductService.ValidationException exLong = assertThrows(
                ProductService.ValidationException.class,
                () -> productService.createCategory(tooLongName, "Desc", testOwner)
        );
        assertEquals("Category name must be 60 characters or fewer.", exLong.getMessage());

        // Case-insensitive duplicate ("house specials" exists from test 2 "House Specials")
        ProductService.ValidationException exDup = assertThrows(
                ProductService.ValidationException.class,
                () -> productService.createCategory("  house specials  ", "Another desc", testOwner)
        );
        assertEquals("A category with this name already exists.", exDup.getMessage());
    }
}
