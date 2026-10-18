package com.bai.inventory.service;

import com.bai.inventory.dao.AiInsightDao;
import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.dao.StockMovementDao;
import com.bai.inventory.model.AiInsight;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;

import java.util.List;

public class DashboardService {

    private final ProductDao productDao;
    private final StockMovementDao stockMovementDao;
    private final AiInsightDao aiInsightDao;
    private final AIInsightService aiInsightService;

    public DashboardService() {
        this.productDao = new ProductDao();
        this.stockMovementDao = new StockMovementDao();
        this.aiInsightDao = new AiInsightDao();
        this.aiInsightService = new AIInsightService(this.aiInsightDao, this.productDao);
    }

    public DashboardService(ProductDao productDao, StockMovementDao stockMovementDao, AiInsightDao aiInsightDao) {
        this.productDao = productDao;
        this.stockMovementDao = stockMovementDao;
        this.aiInsightDao = aiInsightDao;
        this.aiInsightService = new AIInsightService(this.aiInsightDao, this.productDao);
    }

    public record DashboardData(
            ProductDao.ProductSummaryMetrics productMetrics,
            int todayDeductions,
            List<StockMovementDao.DailyMovementPoint> movementHistory,
            List<AiInsight> activeInsights,
            List<Product> needsRestockList,
            List<StockMovement> recentMovements
    ) {}

    public DashboardData loadDashboardData() {
        User current = AuthService.getCurrentUser();
        int orgId = (current != null && current.getOrganizationId() > 0) ? current.getOrganizationId() : 1;
        return loadDashboardData(orgId);
    }

    public DashboardData loadDashboardData(int organizationId) {
        ProductDao.ProductSummaryMetrics productMetrics = productDao.getSummaryMetrics(organizationId);
        int todayDeductions = stockMovementDao.getTodayDeductionsCount(organizationId);
        List<StockMovementDao.DailyMovementPoint> movementHistory = stockMovementDao.getDailyMovementHistory(7, organizationId);
        User current = AuthService.getCurrentUser();
        List<AiInsight> activeInsights = aiInsightService.getInsights(current).insights();
        List<Product> needsRestockList = productDao.findNeedsRestock(6, organizationId);
        List<StockMovement> recentMovements = stockMovementDao.findRecentMovements(8, organizationId);

        return new DashboardData(
                productMetrics,
                todayDeductions,
                movementHistory,
                activeInsights,
                needsRestockList,
                recentMovements
        );
    }
}
