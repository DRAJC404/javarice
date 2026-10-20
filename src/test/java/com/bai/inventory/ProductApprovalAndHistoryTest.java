package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.dao.ProductHistoryDao;
import com.bai.inventory.dao.ProductSubmissionDao;
import com.bai.inventory.model.*;
import com.bai.inventory.service.ProductService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductApprovalAndHistoryTest {

    private static final int TEST_ORG_ID = 888;
    private static ProductDao productDao;
    private static ProductHistoryDao historyDao;
    private static ProductSubmissionDao submissionDao;
    private static ProductService productService;

    private static User managerUser;
    private static User employeeUser;
    private static int categoryId;

    @BeforeAll
    static void setUpAll() throws SQLException {
        DatabaseConfig.initDataSource();
        productDao = new ProductDao();
        historyDao = new ProductHistoryDao();
        submissionDao = new ProductSubmissionDao();
        productService = new ProductService(productDao, historyDao, submissionDao);

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM product_submissions WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM product_history WHERE product_id IN (SELECT id FROM products WHERE organization_id = " + TEST_ORG_ID + ")");
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM products WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM categories WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM users WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM organizations WHERE id = " + TEST_ORG_ID);

            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + TEST_ORG_ID + ", 'Approval Org', 'app-org', true)");

            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (8801, " + TEST_ORG_ID + ", 'mgr88@test.com', 'hash', 'Test Manager', 2, 'APPROVED', true)");

            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (8802, " + TEST_ORG_ID + ", 'emp88@test.com', 'hash', 'Test Employee', 3, 'APPROVED', true)");

            s.executeUpdate("INSERT INTO categories (id, organization_id, name, description) " +
                    "VALUES (8810, " + TEST_ORG_ID + ", 'Test Category', 'Desc')");
        }

        managerUser = new User();
        managerUser.setId(8801);
        managerUser.setOrganizationId(TEST_ORG_ID);
        managerUser.setFullName("Test Manager");
        managerUser.setRole(Role.MANAGER);

        employeeUser = new User();
        employeeUser.setId(8802);
        employeeUser.setOrganizationId(TEST_ORG_ID);
        employeeUser.setFullName("Test Employee");
        employeeUser.setRole(Role.EMPLOYEE);

        categoryId = 8810;
    }

    @Test
    @Order(1)
    void testProductProfitCalculation() {
        Product p = new Product();
        p.setCostPrice(new BigDecimal("15.50"));
        p.setSellingPrice(new BigDecimal("25.00"));

        BigDecimal unitProfit = p.calculateProfit();
        assertEquals(new BigDecimal("9.50"), unitProfit);

        BigDecimal totalProfit = p.calculateTotalProfit(10);
        assertEquals(new BigDecimal("95.00"), totalProfit);

        assertEquals("₱9.50", p.getFormattedProfit());
    }

    @Test
    @Order(2)
    void testEmployeeCreateProductCreatesPendingSubmission() throws Exception {
        Product p = new Product();
        p.setName("Employee Coffee");
        p.setCategoryId(categoryId);
        p.setSku("EMP-COF-001");
        p.setBarcode("8901234567891");
        p.setSellingPrice(new BigDecimal("50.00"));
        p.setCostPrice(new BigDecimal("30.00"));
        p.setStockQuantity(20);
        p.setLowStockThreshold(5);
        p.setImagePath("demo:coffee.png");

        Product created = productService.createProduct(p, employeeUser);
        assertNotNull(created);
        assertEquals("PENDING", created.getApprovalStatus());

        List<ProductSubmission> pending = productService.getPendingSubmissions(managerUser);
        assertFalse(pending.isEmpty());
        assertTrue(pending.stream().anyMatch(sub -> "EMP-COF-001".equals(sub.getSku())));

        // Verify it is NOT returned in active products for sell screen
        List<Product> activeProds = productDao.findAll(TEST_ORG_ID);
        assertFalse(activeProds.stream().anyMatch(prod -> "EMP-COF-001".equals(prod.getSku())));
    }

    @Test
    @Order(3)
    void testSelfApprovalIsBlocked() {
        List<ProductSubmission> pending = productService.getPendingSubmissions(managerUser);
        ProductSubmission sub = pending.stream().filter(s -> "EMP-COF-001".equals(s.getSku())).findFirst().orElseThrow();

        // Employee cannot approve their own submission
        assertThrows(ProductService.AccessDeniedException.class, () -> {
            productService.approveSubmission(sub.getId(), employeeUser);
        });
    }

    @Test
    @Order(4)
    void testManagerApprovesSubmission() throws Exception {
        List<ProductSubmission> pending = productService.getPendingSubmissions(managerUser);
        ProductSubmission sub = pending.stream().filter(s -> "EMP-COF-001".equals(s.getSku())).findFirst().orElseThrow();

        boolean approved = productService.approveSubmission(sub.getId(), managerUser);
        assertTrue(approved);

        // Product should now be APPROVED in database and visible
        List<Product> activeProds = productDao.findAll(TEST_ORG_ID);
        assertTrue(activeProds.stream().anyMatch(prod -> "EMP-COF-001".equals(prod.getSku())));

        // History record should exist
        List<ProductHistory> history = productService.getProductHistory(sub.getProductId(), "APPROVE", null, null);
        assertFalse(history.isEmpty());
        assertEquals("APPROVE", history.get(0).getAction());
        assertEquals("APPROVED", history.get(0).getNewValue());
    }

    @Test
    @Order(5)
    void testEmployeeEditCreatesPendingUpdateSubmission() throws Exception {
        List<Product> activeProds = productDao.findAll(TEST_ORG_ID);
        Product prod = activeProds.stream().filter(p -> "EMP-COF-001".equals(p.getSku())).findFirst().orElseThrow();

        // Employee changes price from 50 to 65
        Product editReq = new Product();
        editReq.setId(prod.getId());
        editReq.setName(prod.getName());
        editReq.setCategoryId(prod.getCategoryId());
        editReq.setSku(prod.getSku());
        editReq.setBarcode(prod.getBarcode());
        editReq.setCostPrice(prod.getCostPrice());
        editReq.setSellingPrice(new BigDecimal("65.00"));
        editReq.setStockQuantity(prod.getStockQuantity());
        editReq.setLowStockThreshold(prod.getLowStockThreshold());
        editReq.setImagePath(prod.getImagePath());

        boolean submitted = productService.updateProduct(editReq, employeeUser);
        assertTrue(submitted);

        // Live product in database should STILL have old price (50.00)
        Optional<Product> live = productDao.findById(prod.getId(), TEST_ORG_ID);
        assertTrue(live.isPresent());
        assertEquals(0, new BigDecimal("50.00").compareTo(live.get().getSellingPrice()));

        // Pending submission has diff
        List<ProductSubmission> pending = productService.getPendingSubmissions(managerUser);
        ProductSubmission updateSub = pending.stream().filter(s -> s.getProductId() != null && s.getProductId() == prod.getId()).findFirst().orElseThrow();
        assertEquals("UPDATE", updateSub.getSubmissionType());
        assertFalse(updateSub.getFieldDifferences().isEmpty());
    }

    @Test
    @Order(6)
    void testManagerRejectsEditKeepsOldValues() throws Exception {
        List<Product> activeProds = productDao.findAll(TEST_ORG_ID);
        Product prod = activeProds.stream().filter(p -> "EMP-COF-001".equals(p.getSku())).findFirst().orElseThrow();

        List<ProductSubmission> pending = productService.getPendingSubmissions(managerUser);
        ProductSubmission updateSub = pending.stream().filter(s -> s.getProductId() != null && s.getProductId() == prod.getId()).findFirst().orElseThrow();

        boolean rejected = productService.rejectSubmission(updateSub.getId(), "Price increase is too high.", managerUser);
        assertTrue(rejected);

        // Live product still keeps old price (50.00)
        Optional<Product> live = productDao.findById(prod.getId(), TEST_ORG_ID);
        assertTrue(live.isPresent());
        assertEquals(0, new BigDecimal("50.00").compareTo(live.get().getSellingPrice()));

        // Submissions for employee reflects rejection
        List<ProductSubmission> mySubs = productService.getMySubmissions(employeeUser);
        ProductSubmission rejSub = mySubs.stream().filter(s -> s.getId() == updateSub.getId()).findFirst().orElseThrow();
        assertEquals("REJECTED", rejSub.getStatus());
        assertEquals("Price increase is too high.", rejSub.getRejectionReason());
    }

    @Test
    @Order(7)
    void testSoftDeleteProduct() throws Exception {
        List<Product> activeProds = productDao.findAll(TEST_ORG_ID);
        Product prod = activeProds.stream().filter(p -> "EMP-COF-001".equals(p.getSku())).findFirst().orElseThrow();

        boolean deleted = productService.softDeleteProduct(prod.getId(), managerUser);
        assertTrue(deleted);

        // Product should now be hidden from findAll and findById
        List<Product> afterDelete = productDao.findAll(TEST_ORG_ID);
        assertFalse(afterDelete.stream().anyMatch(p -> p.getId() == prod.getId()));

        Optional<Product> lookup = productDao.findById(prod.getId(), TEST_ORG_ID);
        assertTrue(lookup.isEmpty());

        // History entry for DELETE is preserved
        List<ProductHistory> deleteHist = productService.getProductHistory(prod.getId(), "DELETE", null, null);
        assertFalse(deleteHist.isEmpty());
        assertEquals("DELETE", deleteHist.get(0).getAction());
    }

    @Test
    @Order(8)
    void testProductPerformanceMetricsCalculation() {
        ProductPerformance perf = new ProductPerformance(
                101, "Test Item", "SKU-101", "Beverages",
                25, 5, new BigDecimal("100.00"), new BigDecimal("60.00"), 1
        );

        assertEquals(new BigDecimal("2500.00"), perf.getRevenue());
        assertEquals(new BigDecimal("1000.00"), perf.getProfit()); // (100 - 60) * 25
        assertTrue(perf.getTurnoverRate() > 0);
        // Stock left is 5, so status tag is "Needs Attention" (critically low stock)
        assertEquals("Needs Attention", perf.getStatusTag());
        assertTrue(perf.isNeedsAttention());
    }
}
