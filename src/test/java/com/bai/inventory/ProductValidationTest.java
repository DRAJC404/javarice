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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductValidationTest {

    private static final int ORG_A_ID = 901;
    private static final int ORG_B_ID = 902;

    private static ProductDao productDao;
    private static ProductService productService;
    private static User ownerA;
    private static User ownerB;
    private static int catAId;
    private static int catBId;

    @BeforeAll
    static void setUpAll() throws SQLException {
        DatabaseConfig.initDataSource();
        productDao = new ProductDao();
        productService = new ProductService(productDao);

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM products WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM categories WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM users WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM organizations WHERE id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");

            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + ORG_A_ID + ", 'Val Org A', 'val-a', true)");
            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + ORG_B_ID + ", 'Val Org B', 'val-b', true)");

            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (9011, " + ORG_A_ID + ", 'ownerA@test.com', 'dummy', 'Owner A', 1, 'APPROVED', true)");
            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (9021, " + ORG_B_ID + ", 'ownerB@test.com', 'dummy', 'Owner B', 1, 'APPROVED', true)");

            s.executeUpdate("INSERT INTO categories (id, organization_id, name) VALUES (90101, " + ORG_A_ID + ", 'Beverages')");
            s.executeUpdate("INSERT INTO categories (id, organization_id, name) VALUES (90201, " + ORG_B_ID + ", 'Snacks')");
        }

        ownerA = new User(9011, ORG_A_ID, 1, "Owner A", "ownerA@test.com", "dummy", "APPROVED", true);
        ownerB = new User(9021, ORG_B_ID, 1, "Owner B", "ownerB@test.com", "dummy", "APPROVED", true);
        catAId = 90101;
        catBId = 90201;
    }

    @AfterAll
    static void tearDownAll() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM products WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM categories WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM users WHERE organization_id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
            s.executeUpdate("DELETE FROM organizations WHERE id IN (" + ORG_A_ID + ", " + ORG_B_ID + ")");
        } catch (SQLException ignored) {}
    }

    private Product createBaseProduct() {
        Product p = new Product();
        p.setOrganizationId(ORG_A_ID);
        p.setName("Valid Product");
        p.setCategoryId(catAId);
        p.setImagePath("/images/product.png");
        p.setUnit("pcs");
        p.setStockQuantity(25);
        p.setCostPrice(new BigDecimal("10.00"));
        p.setSellingPrice(new BigDecimal("15.50"));
        p.setLowStockThreshold(5);
        p.setSku("VAL-SKU-001");
        p.setBarcode("VAL-BAR-001");
        return p;
    }

    @Test
    @Order(1)
    @DisplayName("Reject missing picture")
    void testRejectMissingPicture() {
        Product p = createBaseProduct();
        p.setImagePath(null);
        ProductService.ValidationException ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("picture"));

        p.setImagePath("   ");
        ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("picture"));
    }

    @Test
    @Order(2)
    @DisplayName("Reject missing or too long name")
    void testRejectInvalidName() {
        Product p = createBaseProduct();
        p.setName("");
        ProductService.ValidationException ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("name"));

        p.setName("A".repeat(121));
        ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("name"));
    }

    @Test
    @Order(3)
    @DisplayName("Reject missing category or category of another business")
    void testRejectInvalidCategory() {
        Product p = createBaseProduct();
        p.setCategoryId(null);
        ProductService.ValidationException ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("category"));

        // Use Org B's category for Org A's product
        p.setCategoryId(catBId);
        ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("category"));
    }

    @Test
    @Order(4)
    @DisplayName("Unit is always forced to pcs")
    void testUnitAlwaysForcedToPcs() throws Exception {
        Product p = createBaseProduct();
        p.setUnit("box");
        assertEquals("pcs", p.getUnit());
        Product created = productService.createProduct(p, ownerA);
        assertEquals("pcs", created.getUnit());
    }

    @Test
    @Order(5)
    @DisplayName("Reject negative starting quantity")
    void testRejectNegativeStartingQuantity() {
        Product p = createBaseProduct();
        p.setStockQuantity(-1);
        ProductService.ValidationException ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("startingQuantity"));
    }

    @Test
    @Order(6)
    @DisplayName("Reject negative selling or cost price, or more than 2 decimals")
    void testRejectInvalidPrices() {
        Product p = createBaseProduct();
        p.setSellingPrice(new BigDecimal("-1.00"));
        ProductService.ValidationException ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("sellingPrice"));

        p.setSellingPrice(new BigDecimal("10.555"));
        ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("sellingPrice"));

        p.setSellingPrice(new BigDecimal("15.50"));
        p.setCostPrice(new BigDecimal("-0.50"));
        ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("costPrice"));

        p.setCostPrice(new BigDecimal("10.999"));
        ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("costPrice"));
    }

    @Test
    @Order(7)
    @DisplayName("Reject negative low-stock threshold")
    void testRejectNegativeLowStock() {
        Product p = createBaseProduct();
        p.setLowStockThreshold(-1);
        ProductService.ValidationException ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p, ownerA));
        assertTrue(ex.hasFieldError("lowStockThreshold"));
    }

    @Test
    @Order(8)
    @DisplayName("Reject missing SKU or barcode, or barcode < 4 chars")
    void testRejectInvalidSkuBarcode() {
        Product p1 = createBaseProduct();
        p1.setSku("");
        ProductService.ValidationException ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p1, ownerA));
        assertTrue(ex.hasFieldError("sku"));

        Product p2 = createBaseProduct();
        p2.setBarcode("123"); // < 4 chars
        ex = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(p2, ownerA));
        assertTrue(ex.hasFieldError("barcode"));
    }

    @Test
    @Order(9)
    @DisplayName("Valid product saves and generates OPENING_STOCK movement")
    void testValidProductSavesWithOpeningStockMovement() throws Exception {
        Product p = createBaseProduct();
        p.setSku("VAL-PROD-SUCCESS");
        p.setBarcode("BAR-SUCCESS-999");
        p.setStockQuantity(30);

        Product created = productService.createProduct(p, ownerA);
        assertNotNull(created);
        assertTrue(created.getId() > 0);

        // Verify opening stock movement exists
        try (Connection conn = DatabaseConfig.getConnection(ORG_A_ID);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT movement_type, reason, quantity, previous_stock, new_stock FROM stock_movements WHERE product_id = ? AND organization_id = ?")) {
            stmt.setInt(1, created.getId());
            stmt.setInt(2, ORG_A_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Must have a stock movement record");
                assertEquals("OPENING_STOCK", rs.getString("reason"));
                assertEquals("IN", rs.getString("movement_type"));
                assertEquals(30, rs.getInt("quantity"));
                assertEquals(0, rs.getInt("previous_stock"));
                assertEquals(30, rs.getInt("new_stock"));
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("Reject duplicate SKU and Barcode within the same business")
    void testRejectDuplicateSkuAndBarcode() throws Exception {
        Product p1 = createBaseProduct();
        p1.setSku("DUP-SKU-1");
        p1.setBarcode("DUP-BAR-1");
        p1.setStockQuantity(5);
        Product created1 = productService.createProduct(p1, ownerA);
        assertNotNull(created1);

        // Try adding another product with the same SKU
        Product pDuplicateSku = createBaseProduct();
        pDuplicateSku.setSku("DUP-SKU-1");
        pDuplicateSku.setBarcode("UNIQUE-BAR-2");
        ProductService.ValidationException exSku = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(pDuplicateSku, ownerA));
        assertTrue(exSku.hasFieldError("sku"));

        // Try adding another product with the same Barcode
        Product pDuplicateBarcode = createBaseProduct();
        pDuplicateBarcode.setSku("UNIQUE-SKU-2");
        pDuplicateBarcode.setBarcode("DUP-BAR-1");
        ProductService.ValidationException exBar = assertThrows(ProductService.ValidationException.class,
                () -> productService.createProduct(pDuplicateBarcode, ownerA));
        assertTrue(exBar.hasFieldError("barcode"));

        // Different business can use the same SKU and Barcode (tenant isolation)
        Product pOrgB = createBaseProduct();
        pOrgB.setOrganizationId(ORG_B_ID);
        pOrgB.setCategoryId(catBId);
        pOrgB.setSku("DUP-SKU-1");
        pOrgB.setBarcode("DUP-BAR-1");
        Product createdOrgB = productService.createProduct(pOrgB, ownerB);
        assertNotNull(createdOrgB);
        assertTrue(createdOrgB.getId() > 0);
    }
}
