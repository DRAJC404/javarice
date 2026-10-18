package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.AiInsight;
import com.bai.inventory.service.AuthService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AiInsightDao {

    public List<AiInsight> findActiveInsights(int limit) {
        return findActiveInsights(limit, resolveOrganizationId(0));
    }

    public List<AiInsight> findActiveInsights(int limit, int organizationId) {
        List<AiInsight> list = new ArrayList<>();
        String sql = "SELECT id, category, title, description, severity, metric_value, action_label, action_command, is_resolved, created_at " +
                     "FROM ai_insights WHERE is_resolved = false AND organization_id = ? ORDER BY id ASC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp cat = rs.getTimestamp("created_at");
                    list.add(new AiInsight(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("severity"),
                            rs.getString("metric_value"),
                            rs.getString("action_label"),
                            rs.getString("action_command"),
                            rs.getBoolean("is_resolved"),
                            cat != null ? cat.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error loading AI insights: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            list.add(new AiInsight(1, "FAST_MOVING", "Fresh Whole Milk 1L deduction velocity peaked at 5.4 units/day",
                    "Velocity over the last 14 days has surged 22%. Stockout will occur in ~1.3 days.", "WARNING",
                    "1.3 days remaining", "Quick Restock", "OPEN_RESTOCK_MODAL", false, null));
            list.add(new AiInsight(2, "LOW_STOCK", "4 products are currently below safety thresholds",
                    "Fresh Whole Milk (7/20), Matcha Tin (6/10), Croissant Box (8/12), and Cold Brew Keg (3/4) need restocking.", "WARNING",
                    "4 Low-Stock", "Review Low Stock", "NAVIGATE_LOW_STOCK", false, null));
            list.add(new AiInsight(3, "RESTOCK_RECOMMENDATION", "Recommended Reorder Batch for Week 43",
                    "Suggested purchase order: Fresh Whole Milk (40 pcs), Ceremonial Matcha (15 tins), Butter Croissant (20 boxes).", "OPPORTUNITY",
                    "₱8,600.00 Batch", "Export Reorder List", "EXPORT_REORDER_PDF", false, null));
        }

        return list;
    }

    public boolean recordAiCallAndCheckLimit(int organizationId, int dailyLimit) {
        int orgId = resolveOrganizationId(organizationId);
        String sql =
                "INSERT INTO ai_usage_tracking (organization_id, usage_date, call_count, last_called_at) " +
                "VALUES (?, CURRENT_DATE, 1, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (organization_id, usage_date) " +
                "DO UPDATE SET call_count = ai_usage_tracking.call_count + 1, last_called_at = CURRENT_TIMESTAMP " +
                "RETURNING call_count";

        try (Connection conn = DatabaseConfig.getConnection(orgId)) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt("call_count");
                        conn.commit();
                        return count <= dailyLimit;
                    }
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error tracking AI usage: " + e.getMessage());
        }
        return false;
    }

    public int getTodayAiCallCount(int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        String sql = "SELECT call_count FROM ai_usage_tracking WHERE organization_id = ? AND usage_date = CURRENT_DATE";
        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("call_count");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error reading AI usage: " + e.getMessage());
        }
        return 0;
    }

    public int getDistinctMovementDaysCount(int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        String sql = "SELECT COUNT(DISTINCT DATE(created_at)) AS day_count FROM stock_movements WHERE organization_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("day_count");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error counting movement days: " + e.getMessage());
        }
        return 0;
    }

    public void cacheAiInsights(int organizationId, List<AiInsight> insights) {
        if (insights == null || insights.isEmpty()) return;
        int orgId = resolveOrganizationId(organizationId);

        String deleteOld = "DELETE FROM ai_insights WHERE organization_id = ? AND is_resolved = false";
        String insertSql = "INSERT INTO ai_insights (organization_id, category, title, description, severity, metric_value, action_label, action_command, is_resolved, created_at, expires_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 hour')";

        try (Connection conn = DatabaseConfig.getConnection(orgId)) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement delStmt = conn.prepareStatement(deleteOld)) {
                    delStmt.setInt(1, orgId);
                    delStmt.executeUpdate();
                }

                try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                    for (AiInsight ins : insights) {
                        insStmt.setInt(1, orgId);
                        insStmt.setString(2, sanitizeCategory(ins.getCategory()));
                        insStmt.setString(3, truncate(ins.getTitle(), 200));
                        insStmt.setString(4, ins.getDescription() != null ? ins.getDescription() : "");
                        insStmt.setString(5, sanitizeSeverity(ins.getSeverity()));
                        insStmt.setString(6, truncate(ins.getMetricValue(), 50));
                        insStmt.setString(7, truncate(ins.getActionLabel(), 80));
                        insStmt.setString(8, truncate(ins.getActionCommand(), 100));
                        insStmt.addBatch();
                    }
                    insStmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error caching AI insights: " + e.getMessage());
        }
    }

    public String getAggregatedAnonymousInventoryData(int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        StringBuilder sb = new StringBuilder();

        String statsSql =
                "SELECT " +
                "  COUNT(*) AS total_products, " +
                "  COUNT(CASE WHEN stock_quantity > low_stock_threshold THEN 1 END) AS in_stock, " +
                "  COUNT(CASE WHEN stock_quantity > 0 AND stock_quantity <= low_stock_threshold THEN 1 END) AS low_stock, " +
                "  COUNT(CASE WHEN stock_quantity <= 0 THEN 1 END) AS out_of_stock " +
                "FROM products WHERE organization_id = ? AND is_archived = false";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(statsSql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    sb.append("Inventory Status Summary:\n")
                      .append("- Total catalog products: ").append(rs.getInt("total_products")).append("\n")
                      .append("- Healthy in stock: ").append(rs.getInt("in_stock")).append("\n")
                      .append("- Low stock alert: ").append(rs.getInt("low_stock")).append("\n")
                      .append("- Completely out of stock: ").append(rs.getInt("out_of_stock")).append("\n\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error reading product aggregations: " + e.getMessage());
        }

        String moveSql =
                "SELECT " +
                "  COALESCE(SUM(CASE WHEN movement_type = 'IN' THEN quantity ELSE 0 END), 0) AS units_in_30d, " +
                "  COALESCE(SUM(CASE WHEN movement_type = 'OUT' THEN quantity ELSE 0 END), 0) AS units_out_30d, " +
                "  COUNT(CASE WHEN movement_type = 'OUT' THEN 1 END) AS deduction_transactions_30d " +
                "FROM stock_movements WHERE organization_id = ? AND created_at >= (CURRENT_TIMESTAMP - interval '30 days')";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(moveSql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    sb.append("30-Day Velocity:\n")
                      .append("- Units received: ").append(rs.getInt("units_in_30d")).append("\n")
                      .append("- Units deducted: ").append(rs.getInt("units_out_30d")).append("\n")
                      .append("- Total deduction events: ").append(rs.getInt("deduction_transactions_30d")).append("\n\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error reading movement aggregations: " + e.getMessage());
        }

        String topCatSql =
                "SELECT COALESCE(c.name, 'Uncategorized') AS cat_name, COALESCE(SUM(sm.quantity), 0) AS qty " +
                "FROM stock_movements sm " +
                "JOIN products p ON sm.product_id = p.id " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE sm.organization_id = ? AND sm.movement_type = 'OUT' AND sm.created_at >= (CURRENT_TIMESTAMP - interval '30 days') " +
                "GROUP BY c.name ORDER BY qty DESC LIMIT 5";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(topCatSql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                sb.append("Top 5 Outflow Categories (30 Days):\n");
                while (rs.next()) {
                    sb.append("- ").append(rs.getString("cat_name")).append(": ").append(rs.getInt("qty")).append(" units\n");
                }
                sb.append("\n");
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error reading top categories: " + e.getMessage());
        }

        String lowStockItemsSql =
                "SELECT name, stock_quantity, low_stock_threshold " +
                "FROM products WHERE organization_id = ? AND stock_quantity <= low_stock_threshold AND is_archived = false " +
                "ORDER BY stock_quantity ASC LIMIT 5";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(lowStockItemsSql)) {
            stmt.setInt(1, orgId);
            try (ResultSet rs = stmt.executeQuery()) {
                sb.append("Current Critical Products:\n");
                while (rs.next()) {
                    sb.append("- ").append(rs.getString("name"))
                      .append(" (Stock: ").append(rs.getInt("stock_quantity"))
                      .append("/").append(rs.getInt("low_stock_threshold")).append(")\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AiInsightDao] Error reading critical products: " + e.getMessage());
        }

        return sb.toString();
    }

    private String sanitizeCategory(String cat) {
        if (cat == null) return "FAST_MOVING";
        String upper = cat.trim().toUpperCase();
        return switch (upper) {
            case "FAST_MOVING", "DEMAND_FORECAST", "LOW_STOCK", "OVERSTOCK",
                 "SLOW_MOVING", "EXPIRY_RISK", "ANOMALY", "RESTOCK_RECOMMENDATION" -> upper;
            default -> "FAST_MOVING";
        };
    }

    private String sanitizeSeverity(String sev) {
        if (sev == null) return "INFO";
        String upper = sev.trim().toUpperCase();
        return switch (upper) {
            case "INFO", "WARNING", "OPPORTUNITY" -> upper;
            default -> "INFO";
        };
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return null;
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }

    private int resolveOrganizationId(int given) {
        if (given > 0) return given;
        com.bai.inventory.model.User current = AuthService.getCurrentUser();
        return (current != null && current.getOrganizationId() > 0) ? current.getOrganizationId() : 1;
    }
}
