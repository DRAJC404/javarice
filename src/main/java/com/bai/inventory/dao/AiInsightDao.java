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

    private int resolveOrganizationId(int given) {
        if (given > 0) return given;
        com.bai.inventory.model.User current = AuthService.getCurrentUser();
        return (current != null && current.getOrganizationId() > 0) ? current.getOrganizationId() : 1;
    }
}
