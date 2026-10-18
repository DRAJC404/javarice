package com.bai.inventory.service;

import com.bai.inventory.dao.AnalyticsDao;
import com.bai.inventory.dao.AnalyticsDao.*;
import com.bai.inventory.model.User;

import java.util.List;

public class AnalyticsService {

    private final AnalyticsDao analyticsDao;

    public AnalyticsService() {
        this.analyticsDao = new AnalyticsDao();
    }

    public AnalyticsService(AnalyticsDao analyticsDao) {
        this.analyticsDao = analyticsDao;
    }

    public record AnalyticsData(
            int periodDays,
            AnalyticsKpiSummary kpiSummary,
            List<DailyMovementTrend> movementTrend,
            List<TopProduct> topProducts,
            StockStatusCount stockStatus,
            List<CategoryDeduction> categoryDeductions,
            List<ReasonDeduction> reasonDeductions,
            List<PeakDay> peakDays,
            List<PeakHour> peakHours
    ) {
        public boolean hasData() {
            boolean hasTrend = movementTrend != null && movementTrend.stream().anyMatch(p -> p.inflowQty() > 0 || p.outflowQty() > 0);
            boolean hasTop = topProducts != null && !topProducts.isEmpty();
            boolean hasStatus = stockStatus != null && stockStatus.total() > 0;
            return hasTrend || hasTop || hasStatus;
        }
    }

    public AnalyticsData getAnalyticsData(int periodDays, User currentUser) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        return getAnalyticsData(periodDays, orgId);
    }

    public AnalyticsData getAnalyticsData(int periodDays, int organizationId) {
        AnalyticsKpiSummary kpiSummary = analyticsDao.getKpiSummary(periodDays, organizationId);
        List<DailyMovementTrend> movementTrend = analyticsDao.getDailyMovementTrend(periodDays, organizationId);
        List<TopProduct> topProducts = analyticsDao.getTopMovingProducts(periodDays, 6, organizationId);
        StockStatusCount stockStatus = analyticsDao.getStockStatusCount(organizationId);
        List<CategoryDeduction> categoryDeductions = analyticsDao.getDeductionsByCategory(periodDays, 6, organizationId);
        List<ReasonDeduction> reasonDeductions = analyticsDao.getDeductionReasons(periodDays, organizationId);
        List<PeakDay> peakDays = analyticsDao.getPeakDays(periodDays, organizationId);
        List<PeakHour> peakHours = analyticsDao.getPeakHours(periodDays, organizationId);

        return new AnalyticsData(
                periodDays,
                kpiSummary,
                movementTrend,
                topProducts,
                stockStatus,
                categoryDeductions,
                reasonDeductions,
                peakDays,
                peakHours
        );
    }
}
