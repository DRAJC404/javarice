package com.bai.inventory;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.config.EnvConfig;
import com.bai.inventory.dao.AiInsightDao;
import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.model.AiInsight;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AIInsightService;
import com.bai.inventory.service.AIInsightService.InsightResult;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AIInsightServiceTest {

    private static final int TEST_ORG_ID = 888;
    private static User testUser;
    private static AIInsightService service;
    private static AiInsightDao aiDao;

    @BeforeAll
    static void setUpAll() throws SQLException {
        DatabaseConfig.initDataSource();
        aiDao = new AiInsightDao();
        service = new AIInsightService(aiDao, new ProductDao());

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {

            s.executeUpdate("DELETE FROM ai_usage_tracking WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM ai_insights WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM products WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM users WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM organizations WHERE id = " + TEST_ORG_ID);

            s.executeUpdate("INSERT INTO organizations (id, name, slug, is_active) VALUES (" + TEST_ORG_ID + ", 'Test AI Org', 'test-ai-org', true)");
            s.executeUpdate("INSERT INTO users (id, organization_id, email, password_hash, full_name, role_id, approval_status, is_active) " +
                    "VALUES (8881, " + TEST_ORG_ID + ", 'owner@testai.com', 'dummyhash', 'Test Owner', 1, 'APPROVED', true)");

            s.executeUpdate("INSERT INTO products (id, organization_id, sku, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, status) " +
                    "VALUES (88801, " + TEST_ORG_ID + ", 'SKU-AI-1', 'AI Test Product', 'pcs', 10.0, 20.0, 2, 5, 'LOW_STOCK')");
        }

        testUser = new User(8881, TEST_ORG_ID, 1, "Test Owner", "owner@testai.com", "dummyhash", "APPROVED", true);
    }

    @AfterAll
    static void tearDownAll() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bai_pos", "postgres", "");
             Statement s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM ai_usage_tracking WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM ai_insights WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM stock_movements WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM products WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM users WHERE organization_id = " + TEST_ORG_ID);
            s.executeUpdate("DELETE FROM organizations WHERE id = " + TEST_ORG_ID);
        } catch (SQLException ignored) {}
    }

    @Test
    @Order(1)
    @DisplayName("1. EnvConfig masks API keys and never logs full key")
    void testEnvConfigMasking() {
        String masked = EnvConfig.mask("gsk_1234567890abcdefghijklmn");
        assertEquals("gsk_...klmn", masked);
        assertFalse(masked.contains("1234567890"));

        assertEquals("[NOT CONFIGURED]", EnvConfig.mask(null));
        assertEquals("[NOT CONFIGURED]", EnvConfig.mask(""));
        assertEquals("********", EnvConfig.mask("short"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Rule-based fallback triggers when movement data is under 7 days")
    void testRuleBasedFallbackFewerThan7Days() {
        InsightResult result = service.getInsights(testUser);
        assertNotNull(result);
        assertNotNull(result.insights());
        assertFalse(result.isAiGenerated(), "Should be rule-based because test org has < 7 days of movement");
        assertTrue(result.statusNote().contains("at least 7 days") || result.statusNote().contains("rule-based"));

        // Verify rule-based picked up the low stock item
        assertTrue(result.insights().stream().anyMatch(i -> i.getSeverity().equals("WARNING")));
    }

    @Test
    @Order(3)
    @DisplayName("3. Anonymous stats contain no user names, emails, or personal IDs")
    void testAnonymousDataContainsNoPII() {
        String anon = aiDao.getAggregatedAnonymousInventoryData(TEST_ORG_ID);
        assertNotNull(anon);
        assertFalse(anon.contains("owner@testai.com"), "Must not leak user emails");
        assertFalse(anon.contains("Test Owner"), "Must not leak user names");
        assertFalse(anon.contains("8881"), "Must not leak user IDs");
        assertFalse(anon.contains("88801"), "Must not leak product DB IDs");
        assertTrue(anon.contains("AI Test Product"), "Can contain product display name");
        assertTrue(anon.contains("Inventory Status Summary"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Rate limiting restricts AI calls to 20 calls per organization per day")
    void testDailyRateLimiting() {
        // Record 20 calls
        for (int i = 1; i <= 20; i++) {
            boolean allowed = aiDao.recordAiCallAndCheckLimit(TEST_ORG_ID, 20);
            assertTrue(allowed, "Call #" + i + " should be allowed");
        }

        // 21st call should be rejected
        boolean blocked = aiDao.recordAiCallAndCheckLimit(TEST_ORG_ID, 20);
        assertFalse(blocked, "21st call must be blocked by daily rate limit");
    }

    @Test
    @Order(5)
    @DisplayName("5. Ask bAI enforces max length constraint and verified organization")
    void testAskBaiValidation() {
        // Missing user / org
        String unauthAnswer = service.askBai("Hello", null);
        assertTrue(unauthAnswer.contains("Authentication error"), "Must require verified tenant");

        // Question exceeding 200 characters
        String longQuestion = "a".repeat(201);
        String lenAnswer = service.askBai(longQuestion, testUser);
        assertTrue(lenAnswer.contains("maximum 200 characters"), "Must enforce 200 char maximum");

        // Normal question when rate-limited
        String normalAnswer = service.askBai("Which products need restock?", testUser);
        assertNotNull(normalAnswer);
        assertTrue(normalAnswer.contains("Daily AI") || normalAnswer.contains("bAI Assistant"), "Returns rate-limit or offline response");
    }
}
