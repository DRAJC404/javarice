package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.service.AuthService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;

public class AnalyticsDao {

    private final NumberFormat pesoFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    public record KpiComparison(
            double currentVal,
            double previousVal,
            double percentChange,
            String unit,
            String formattedCurrent,
            String formattedChange,
            boolean isPositiveGood
    ) {}

    public record AnalyticsKpiSummary(
            KpiComparison inflow,
            KpiComparison outflow,
            KpiComparison netMovement,
            KpiComparison turnoverValue,
            KpiComparison shrinkageRate
    ) {}

    public record DailyMovementTrend(
            String dateLabel,
            int inflowQty,
            int outflowQty
    ) {}

    public record TopProduct(
            int productId,
            String name,
            String sku,
            String category,
            int unitsDeducted,
            BigDecimal turnoverValue,
            int currentStock,
            String status
    ) {}

    public record StockStatusCount(
            int inStock,
            int lowStock,
            int outOfStock
    ) {
        public int total() {
            return inStock + lowStock + outOfStock;
        }
    }

    public record CategoryDeduction(
            String categoryName,
            int unitsDeducted,
            BigDecimal turnoverValue
    ) {}

    public record ReasonDeduction(
            String reason,
            int quantity,
            double percentage
    ) {}

    public record PeakDay(
            String dayName,
            int dayNum,
            int movementCount,
            int totalQty
    ) {}

    public record PeakHour(
            String hourLabel,
            int hourOfDay,
            int movementCount,
            int totalQty
    ) {}

    public AnalyticsKpiSummary getKpiSummary(int periodDays, int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        int days = Math.max(1, periodDays);

        String sql =
                "WITH cur_period AS (" +
                "    SELECT " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'IN' THEN sm.quantity ELSE 0 END), 0) AS in_qty, " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'OUT' THEN sm.quantity ELSE 0 END), 0) AS out_qty, " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'OUT' AND sm.reason IN ('DAMAGED', 'EXPIRED') THEN sm.quantity ELSE 0 END), 0) AS shrink_qty, " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'OUT' THEN sm.quantity * p.selling_price ELSE 0 END), 0.00) AS turn_val " +
                "    FROM stock_movements sm " +
                "    JOIN products p ON sm.product_id = p.id " +
                "    WHERE sm.organization_id = ? AND sm.created_at >= (CURRENT_TIMESTAMP - (? || ' days')::interval)" +
                "), " +
                "prev_period AS (" +
                "    SELECT " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'IN' THEN sm.quantity ELSE 0 END), 0) AS in_qty, " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'OUT' THEN sm.quantity ELSE 0 END), 0) AS out_qty, " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'OUT' AND sm.reason IN ('DAMAGED', 'EXPIRED') THEN sm.quantity ELSE 0 END), 0) AS shrink_qty, " +
                "        COALESCE(SUM(CASE WHEN sm.movement_type = 'OUT' THEN sm.quantity * p.selling_price ELSE 0 END), 0.00) AS turn_val " +
                "    FROM stock_movements sm " +
                "    JOIN products p ON sm.product_id = p.id " +
                "    WHERE sm.organization_id = ? " +
                "      AND sm.created_at >= (CURRENT_TIMESTAMP - ((2 * ?) || ' days')::interval) " +
                "      AND sm.created_at < (CURRENT_TIMESTAMP - (? || ' days')::interval)" +
                ") " +
                "SELECT c.in_qty AS cur_in, c.out_qty AS cur_out, c.shrink_qty AS cur_shrink, c.turn_val AS cur_val, " +
                "       p.in_qty AS prev_in, p.out_qty AS prev_out, p.shrink_qty AS prev_shrink, p.turn_val AS prev_val " +
                "FROM cur_period c, prev_period p";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, days);
            stmt.setInt(3, orgId);
            stmt.setInt(4, days);
            stmt.setInt(5, days);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double curIn = rs.getDouble("cur_in");
                    double prevIn = rs.getDouble("prev_in");
                    double curOut = rs.getDouble("cur_out");
                    double prevOut = rs.getDouble("prev_out");
                    double curShrink = rs.getDouble("cur_shrink");
                    double prevShrink = rs.getDouble("prev_shrink");
                    double curVal = rs.getDouble("cur_val");
                    double prevVal = rs.getDouble("prev_val");

                    double curNet = curIn - curOut;
                    double prevNet = prevIn - prevOut;

                    double curShrinkRate = curOut > 0 ? (curShrink / curOut) * 100.0 : 0.0;
                    double prevShrinkRate = prevOut > 0 ? (prevShrink / prevOut) * 100.0 : 0.0;

                    return new AnalyticsKpiSummary(
                            createComparison(curIn, prevIn, "units", "%,.0f units", true, days),
                            createComparison(curOut, prevOut, "units", "%,.0f units", true, days),
                            createComparison(curNet, prevNet, "units", "%+,.0f units", true, days),
                            createComparison(curVal, prevVal, "PHP", pesoFormat.format(curVal), true, days),
                            createComparison(curShrinkRate, prevShrinkRate, "%", String.format(Locale.US, "%.1f%%", curShrinkRate), false, days)
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting KPI summary: " + e.getMessage());
        }

        if (DatabaseConfig.isDevModeEnabled()) {
            return getDemoKpiSummary(days);
        }
        return getZeroKpiSummary(days);
    }

    public List<DailyMovementTrend> getDailyMovementTrend(int periodDays, int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        int days = Math.max(1, periodDays);
        List<DailyMovementTrend> list = new ArrayList<>();

        String sql =
                "SELECT TO_CHAR(d.day, 'MM/DD') AS date_label, " +
                "       COALESCE(SUM(CASE WHEN sm.movement_type = 'IN' THEN sm.quantity ELSE 0 END), 0) AS in_qty, " +
                "       COALESCE(SUM(CASE WHEN sm.movement_type = 'OUT' THEN sm.quantity ELSE 0 END), 0) AS out_qty " +
                "FROM generate_series(CURRENT_DATE - (? || ' days')::interval, CURRENT_DATE, '1 day'::interval) d(day) " +
                "LEFT JOIN stock_movements sm ON DATE(sm.created_at) = DATE(d.day) AND sm.organization_id = ? " +
                "GROUP BY d.day ORDER BY d.day ASC";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days - 1);
            stmt.setInt(2, orgId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new DailyMovementTrend(
                            rs.getString("date_label"),
                            rs.getInt("in_qty"),
                            rs.getInt("out_qty")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting daily movement trend: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            return getDemoDailyTrend(days);
        }
        return list;
    }

    public List<TopProduct> getTopMovingProducts(int periodDays, int limit, int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        int days = Math.max(1, periodDays);
        List<TopProduct> list = new ArrayList<>();

        String sql =
                "SELECT p.id, p.name, p.sku, COALESCE(c.name, 'Uncategorized') AS category_name, " +
                "       p.stock_quantity, p.status, " +
                "       COALESCE(SUM(sm.quantity), 0) AS units_deducted, " +
                "       COALESCE(SUM(sm.quantity * p.selling_price), 0.00) AS turnover_val " +
                "FROM stock_movements sm " +
                "JOIN products p ON sm.product_id = p.id " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE sm.organization_id = ? " +
                "  AND sm.movement_type = 'OUT' " +
                "  AND sm.created_at >= (CURRENT_TIMESTAMP - (? || ' days')::interval) " +
                "GROUP BY p.id, p.name, p.sku, c.name, p.stock_quantity, p.status " +
                "ORDER BY units_deducted DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, days);
            stmt.setInt(3, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopProduct(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("sku"),
                            rs.getString("category_name"),
                            rs.getInt("units_deducted"),
                            rs.getBigDecimal("turnover_val"),
                            rs.getInt("stock_quantity"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting top moving products: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            return getDemoTopProducts();
        }
        return list;
    }

    public StockStatusCount getStockStatusCount(int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        String sql =
                "SELECT " +
                "    COALESCE(SUM(CASE WHEN status = 'IN_STOCK' THEN 1 ELSE 0 END), 0) AS in_stock, " +
                "    COALESCE(SUM(CASE WHEN status = 'LOW_STOCK' THEN 1 ELSE 0 END), 0) AS low_stock, " +
                "    COALESCE(SUM(CASE WHEN status = 'OUT_OF_STOCK' THEN 1 ELSE 0 END), 0) AS out_stock " +
                "FROM products " +
                "WHERE organization_id = ? AND is_archived = false";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StockStatusCount(
                            rs.getInt("in_stock"),
                            rs.getInt("low_stock"),
                            rs.getInt("out_stock")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting stock status count: " + e.getMessage());
        }

        if (DatabaseConfig.isDevModeEnabled()) {
            return new StockStatusCount(24, 7, 2);
        }
        return new StockStatusCount(0, 0, 0);
    }

    public List<CategoryDeduction> getDeductionsByCategory(int periodDays, int limit, int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        int days = Math.max(1, periodDays);
        List<CategoryDeduction> list = new ArrayList<>();

        String sql =
                "SELECT COALESCE(c.name, 'Uncategorized') AS cat_name, " +
                "       COALESCE(SUM(sm.quantity), 0) AS units_deducted, " +
                "       COALESCE(SUM(sm.quantity * p.selling_price), 0.00) AS turnover_val " +
                "FROM stock_movements sm " +
                "JOIN products p ON sm.product_id = p.id " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE sm.organization_id = ? " +
                "  AND sm.movement_type = 'OUT' " +
                "  AND sm.created_at >= (CURRENT_TIMESTAMP - (? || ' days')::interval) " +
                "GROUP BY c.name " +
                "ORDER BY units_deducted DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, days);
            stmt.setInt(3, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new CategoryDeduction(
                            rs.getString("cat_name"),
                            rs.getInt("units_deducted"),
                            rs.getBigDecimal("turnover_val")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting deductions by category: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            return getDemoCategoryDeductions();
        }
        return list;
    }

    public List<ReasonDeduction> getDeductionReasons(int periodDays, int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        int days = Math.max(1, periodDays);
        List<ReasonDeduction> list = new ArrayList<>();

        String sql =
                "WITH totals AS (" +
                "    SELECT COALESCE(SUM(quantity), 0) AS grand_total " +
                "    FROM stock_movements " +
                "    WHERE organization_id = ? AND movement_type = 'OUT' " +
                "      AND created_at >= (CURRENT_TIMESTAMP - (? || ' days')::interval)" +
                ") " +
                "SELECT sm.reason, SUM(sm.quantity) AS reason_qty, " +
                "       ROUND((SUM(sm.quantity)::numeric / NULLIF((SELECT grand_total FROM totals), 0)::numeric) * 100, 1) AS pct " +
                "FROM stock_movements sm " +
                "WHERE sm.organization_id = ? AND sm.movement_type = 'OUT' " +
                "  AND sm.created_at >= (CURRENT_TIMESTAMP - (? || ' days')::interval) " +
                "GROUP BY sm.reason " +
                "ORDER BY reason_qty DESC";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, days);
            stmt.setInt(3, orgId);
            stmt.setInt(4, days);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReasonDeduction(
                            rs.getString("reason"),
                            rs.getInt("reason_qty"),
                            rs.getDouble("pct")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting deduction reasons: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            return getDemoReasonDeductions();
        }
        return list;
    }

    public List<PeakDay> getPeakDays(int periodDays, int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        int days = Math.max(1, periodDays);
        Map<Integer, PeakDay> map = new LinkedHashMap<>();
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 1; i <= 7; i++) {
            map.put(i, new PeakDay(dayNames[i - 1], i, 0, 0));
        }

        String sql =
                "SELECT EXTRACT(ISODOW FROM created_at)::int AS day_num, " +
                "       COUNT(*) AS move_count, " +
                "       COALESCE(SUM(quantity), 0) AS total_qty " +
                "FROM stock_movements " +
                "WHERE organization_id = ? " +
                "  AND created_at >= (CURRENT_TIMESTAMP - (? || ' days')::interval) " +
                "GROUP BY day_num " +
                "ORDER BY day_num ASC";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, days);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int dayNum = rs.getInt("day_num");
                    if (dayNum >= 1 && dayNum <= 7) {
                        map.put(dayNum, new PeakDay(
                                dayNames[dayNum - 1],
                                dayNum,
                                rs.getInt("move_count"),
                                rs.getInt("total_qty")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting peak days: " + e.getMessage());
        }

        return new ArrayList<>(map.values());
    }

    public List<PeakHour> getPeakHours(int periodDays, int organizationId) {
        int orgId = resolveOrganizationId(organizationId);
        int days = Math.max(1, periodDays);
        Map<Integer, PeakHour> map = new TreeMap<>();
        for (int h = 7; h <= 21; h++) {
            String label = String.format("%02d:00", h);
            map.put(h, new PeakHour(label, h, 0, 0));
        }

        String sql =
                "SELECT EXTRACT(HOUR FROM created_at)::int AS hour_val, " +
                "       COUNT(*) AS move_count, " +
                "       COALESCE(SUM(quantity), 0) AS total_qty " +
                "FROM stock_movements " +
                "WHERE organization_id = ? " +
                "  AND created_at >= (CURRENT_TIMESTAMP - (? || ' days')::interval) " +
                "GROUP BY hour_val " +
                "ORDER BY hour_val ASC";

        try (Connection conn = DatabaseConfig.getConnection(orgId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orgId);
            stmt.setInt(2, days);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int h = rs.getInt("hour_val");
                    if (map.containsKey(h)) {
                        map.put(h, new PeakHour(
                                String.format("%02d:00", h),
                                h,
                                rs.getInt("move_count"),
                                rs.getInt("total_qty")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[AnalyticsDao] Error getting peak hours: " + e.getMessage());
        }

        return new ArrayList<>(map.values());
    }

    private KpiComparison createComparison(double current, double previous, String unit, String formatStr, boolean isPositiveGood, int days) {
        double pct;
        if (previous == 0.0) {
            pct = current > 0 ? 100.0 : 0.0;
        } else {
            pct = ((current - previous) / previous) * 100.0;
        }

        String formattedCurrent;
        if ("PHP".equals(unit)) {
            formattedCurrent = formatStr;
        } else if ("%".equals(unit)) {
            formattedCurrent = formatStr;
        } else {
            formattedCurrent = String.format(Locale.US, formatStr, current);
        }

        String sign = pct > 0 ? "+" : "";
        String formattedChange = String.format(Locale.US, "%s%.1f%% vs prev %dd", sign, pct, days);

        return new KpiComparison(current, previous, pct, unit, formattedCurrent, formattedChange, isPositiveGood);
    }

    private AnalyticsKpiSummary getDemoKpiSummary(int days) {
        return new AnalyticsKpiSummary(
                createComparison(480, 420, "units", "%,.0f units", true, days),
                createComparison(360, 310, "units", "%,.0f units", true, days),
                createComparison(120, 110, "units", "%+,.0f units", true, days),
                createComparison(84500.0, 72000.0, "PHP", pesoFormat.format(84500.0), true, days),
                createComparison(2.8, 3.4, "%", "2.8%", false, days)
        );
    }

    private AnalyticsKpiSummary getZeroKpiSummary(int days) {
        return new AnalyticsKpiSummary(
                createComparison(0, 0, "units", "0 units", true, days),
                createComparison(0, 0, "units", "0 units", true, days),
                createComparison(0, 0, "units", "0 units", true, days),
                createComparison(0, 0, "PHP", pesoFormat.format(0), true, days),
                createComparison(0, 0, "%", "0.0%", false, days)
        );
    }

    private List<DailyMovementTrend> getDemoDailyTrend(int days) {
        List<DailyMovementTrend> list = new ArrayList<>();
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd");
        int count = Math.min(days, 14);
        int[] inArr = {30, 0, 45, 10, 0, 50, 20, 0, 40, 15, 0, 60, 25, 10};
        int[] outArr = {22, 18, 25, 30, 28, 35, 40, 24, 29, 33, 27, 42, 38, 26};
        for (int i = count - 1; i >= 0; i--) {
            int idx = (count - 1 - i) % inArr.length;
            list.add(new DailyMovementTrend(now.minusDays(i).format(fmt), inArr[idx], outArr[idx]));
        }
        return list;
    }

    private List<TopProduct> getDemoTopProducts() {
        return List.of(
                new TopProduct(1, "Barako Arabica Beans 1kg", "BEV-001", "Beverages", 84, new BigDecimal("54600.00"), 42, "IN_STOCK"),
                new TopProduct(8, "Barista Oat Milk 1L", "DAI-002", "Fresh & Dairy", 62, new BigDecimal("10850.00"), 35, "IN_STOCK"),
                new TopProduct(5, "Special Ensaymada (4pk)", "BAK-002", "Bakery", 48, new BigDecimal("13440.00"), 18, "IN_STOCK"),
                new TopProduct(10, "Eco Paper Cups 12oz (50s)", "SUP-001", "Store Supplies", 38, new BigDecimal("5320.00"), 55, "IN_STOCK"),
                new TopProduct(4, "Butter Croissant (Box of 6)", "BAK-001", "Bakery", 28, new BigDecimal("8960.00"), 8, "LOW_STOCK")
        );
    }

    private List<CategoryDeduction> getDemoCategoryDeductions() {
        return List.of(
                new CategoryDeduction("Beverages", 125, new BigDecimal("68500.00")),
                new CategoryDeduction("Bakery", 98, new BigDecimal("24500.00")),
                new CategoryDeduction("Fresh & Dairy", 72, new BigDecimal("12600.00")),
                new CategoryDeduction("Store Supplies", 45, new BigDecimal("6300.00")),
                new CategoryDeduction("Packaged Snacks", 20, new BigDecimal("3700.00"))
        );
    }

    private List<ReasonDeduction> getDemoReasonDeductions() {
        return List.of(
                new ReasonDeduction("SOLD", 295, 81.9),
                new ReasonDeduction("USED", 45, 12.5),
                new ReasonDeduction("DAMAGED", 12, 3.3),
                new ReasonDeduction("EXPIRED", 8, 2.3)
        );
    }

    private int resolveOrganizationId(int given) {
        if (given > 0) return given;
        com.bai.inventory.model.User current = AuthService.getCurrentUser();
        return (current != null && current.getOrganizationId() > 0) ? current.getOrganizationId() : 1;
    }
}
