package com.bai.inventory.view.pages;

import com.bai.inventory.model.AiInsight;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.DashboardService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.util.Constants;
import com.bai.inventory.util.ProductImageManager;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardView extends StackPane implements Disposable {

    private static final String SVG_REFRESH    = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_PACKAGE    = "M20 7l-8-4-8 4 8 4 8-4zm0 6l-8 4-8-4m16 4l-8 4-8-4";
    private static final String SVG_WARNING    = "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z";
    private static final String SVG_X_CIRCLE   = "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z";
    private static final String SVG_TREND_DOWN = "M16 18l2.29-2.29-4.88-4.88-4 4L2 7.41 3.41 6l6 6 4-4 6.3 6.29L22 12v6z";
    private static final String SVG_WALLET     = "M21 18v1c0 1.1-.9 2-2 2H5c-1.11 0-2-.9-2-2V5c0-1.1.89-2 2-2h14c1.1 0 2 .9 2 2v1h-9c-1.11 0-2 .9-2 2v8c0 1.1.89 2 2 2h9zm-9-2h10V8H12v8zm4-2.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z";
    private static final String SVG_SPARKLE    = "M12 2L9.5 8.5 3 11l6.5 2.5L12 20l2.5-6.5L21 11l-6.5-2.5z";
    private static final String SVG_ARROW_UP   = "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z";
    private static final String SVG_ARROW_DN   = "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z";

    private final DashboardService dashboardService;
    private final NumberFormat pesoFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    private ScrollPane contentScroll;
    private VBox contentBox;
    private VBox loadingPane;
    private VBox errorPane;
    private Label errorLabel;
    private Button refreshBtn;

    private volatile boolean isDisposed = false;

    public DashboardView() {
        this.dashboardService = new DashboardService();
        buildUI();
        loadDashboardDataAsync();
    }

    private void buildUI() {
        getStyleClass().add("content-pane");

        loadingPane = new VBox(16);
        loadingPane.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(44, 44);
        Label loadingText = new Label("Analyzing real-time stock levels & insights...");
        loadingText.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-muted; -fx-font-weight: 600;");
        loadingPane.getChildren().addAll(spinner, loadingText);

        errorPane = new VBox(16);
        errorPane.setAlignment(Pos.CENTER);
        errorPane.setVisible(false);
        errorPane.setManaged(false);

        Label errIcon = new Label("⚠️");
        errIcon.setStyle("-fx-font-size: 32px;");
        errorLabel = new Label("Failed to load inventory dashboard data.");
        errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -status-danger; -fx-font-weight: 600;");
        Button retryBtn = new Button("Retry");
        retryBtn.getStyleClass().add("btn-dialog-confirm");
        retryBtn.setOnAction(e -> loadDashboardDataAsync());
        errorPane.getChildren().addAll(errIcon, errorLabel, retryBtn);

        contentBox = new VBox(24);
        contentBox.setPadding(new Insets(32));
        contentBox.setMaxWidth(1400);

        StackPane centerWrapper = new StackPane(contentBox);
        centerWrapper.setAlignment(Pos.TOP_CENTER);

        contentScroll = new ScrollPane(centerWrapper);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.getStyleClass().add("scroll-pane");
        contentScroll.setVisible(false);
        contentScroll.setManaged(false);

        getChildren().addAll(loadingPane, errorPane, contentScroll);
    }

    private void loadDashboardDataAsync() {
        if (isDisposed) return;

        showLoading();

        AppExecutor.execute(() -> {
            try {
                DashboardService.DashboardData data = dashboardService.loadDashboardData();
                Platform.runLater(() -> {
                    if (isDisposed) return;
                    renderDashboard(data);
                    showContent();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (isDisposed) return;
                    showError(ex.getMessage());
                });
            }
        });
    }

    private void renderDashboard(DashboardService.DashboardData data) {
        contentBox.getChildren().clear();

        Node headerNode = buildHeader();

        Node statsRow = buildStatCards(data);

        HBox midSection = new HBox(20);
        midSection.setAlignment(Pos.TOP_LEFT);

        Region chartCard = (Region) buildChartCard(data);
        Region insightsCard = (Region) buildAiInsightsCard(data);

        HBox.setHgrow(chartCard, Priority.ALWAYS);
        HBox.setHgrow(insightsCard, Priority.ALWAYS);
        chartCard.setPrefWidth(550);
        insightsCard.setPrefWidth(420);

        midSection.getChildren().addAll(chartCard, insightsCard);

        HBox bottomSection = new HBox(20);
        bottomSection.setAlignment(Pos.TOP_LEFT);

        Region restockCard = (Region) buildNeedsRestockCard(data);
        Region recentCard = (Region) buildRecentMovementsCard(data);

        HBox.setHgrow(restockCard, Priority.ALWAYS);
        HBox.setHgrow(recentCard, Priority.ALWAYS);
        restockCard.setPrefWidth(480);
        recentCard.setPrefWidth(480);

        contentBox.getChildren().add(headerNode);

        User currentUser = AuthService.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == com.bai.inventory.model.Role.OWNER && data.staffCount() == 0) {
            contentBox.getChildren().add(buildInviteTeamBanner());
        }

        contentBox.getChildren().addAll(statsRow, midSection, bottomSection);
    }

    private Node buildInviteTeamBanner() {
        BorderPane banner = new BorderPane();
        banner.getStyleClass().add("join-code-banner");

        VBox textCol = new VBox(2);
        Label title = new Label("Invite your team");
        title.getStyleClass().add("join-code-banner-title");

        Label desc = new Label("Add staff with a one-time code that expires in 10 minutes");
        desc.getStyleClass().add("join-code-banner-sub");
        textCol.getChildren().addAll(title, desc);

        Button generateBtn = new Button("Generate join code");
        generateBtn.getStyleClass().add("btn-primary");
        generateBtn.setOnAction(e -> com.bai.inventory.view.dialog.JoinCodeDialogHelper.showAddTeamMemberModal(this::loadDashboardDataAsync));

        banner.setLeft(textCol);
        banner.setRight(generateBtn);
        BorderPane.setAlignment(generateBtn, Pos.CENTER_RIGHT);
        return banner;
    }

    private Node buildHeader() {
        BorderPane header = new BorderPane();
        header.getStyleClass().add("page-header");

        User user = AuthService.getCurrentUser();
        String name = (user != null && user.getFullName() != null) ? user.getFullName() : "User";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));

        Label greetingLabel = new Label("Welcome back, " + name + ". " + dateStr);
        greetingLabel.getStyleClass().add("page-title");
        greetingLabel.setWrapText(true);
        header.setLeft(greetingLabel);

        refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        SVGPath refIcon = new SVGPath();
        refIcon.setContent(SVG_REFRESH);
        refIcon.getStyleClass().add("dialog-close-icon");
        refreshBtn.setGraphic(refIcon);
        refreshBtn.setOnAction(e -> loadDashboardDataAsync());

        header.setRight(refreshBtn);
        return header;
    }

    private Node buildStatCards(DashboardService.DashboardData data) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        var pm = data.productMetrics();
        String valFormatted = pesoFormat.format(pm.totalValuation());

        row.getChildren().addAll(
                createStatCard("TOTAL PRODUCTS", String.valueOf(pm.totalProducts()), "Active retail catalog SKUs", "stat-icon", SVG_PACKAGE),
                createStatCard("LOW STOCK ALERTS", String.valueOf(pm.lowStockCount()), "Below configured par levels", "stat-icon-warning", SVG_WARNING),
                createStatCard("OUT OF STOCK", String.valueOf(pm.outOfStockCount()), "Critically depleted items", "stat-icon-danger", SVG_X_CIRCLE),
                createStatCard("DEDUCTIONS TODAY", String.valueOf(data.todayDeductions()), "Units sold, used or write-off", "stat-icon", SVG_TREND_DOWN),
                createStatCard("TOTAL VALUATION", valFormatted, "Total retail stock value", "stat-icon", SVG_WALLET)
        );

        return row;
    }

    private Node createStatCard(String label, String value, String subtext, String iconStyleClass, String svgPath) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        StackPane tile = new StackPane();
        tile.getStyleClass().add("stat-icon-tile");

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().addAll("stat-tile-icon", iconStyleClass);
        tile.getChildren().add(icon);

        top.getChildren().addAll(lbl, tile);

        Label val = new Label(value);
        val.getStyleClass().add("stat-value");

        Label sub = new Label(subtext);
        sub.getStyleClass().add("stat-subtext");

        card.getChildren().addAll(top, val, sub);
        return card;
    }

    private String formatItems(int count) {
        return count + " " + (count == 1 ? "item" : "items");
    }

    private String formatPcs(int count, String unit) {
        String u = (unit != null && !unit.isBlank()) ? unit : "pc";
        if ("pcs".equalsIgnoreCase(u) || "pc".equalsIgnoreCase(u)) {
            return count + " " + (count == 1 ? "pc" : "pcs");
        }
        return count + " " + (count == 1 ? u : u + "s");
    }

    private Node buildChartCard(DashboardService.DashboardData data) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        BorderPane header = new BorderPane();

        Label title = new Label("Stock Movement Trends (Past 7 Days)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        header.setLeft(title);

        HBox legends = new HBox(12);
        legends.setAlignment(Pos.CENTER_RIGHT);

        Label legendIn = new Label("■ Stock IN (Restock)");
        legendIn.getStyleClass().add("legend-in-text");

        Label legendOut = new Label("■ Stock OUT (Deduction)");
        legendOut.getStyleClass().add("legend-out-text");

        legends.getChildren().addAll(legendIn, legendOut);
        header.setRight(legends);

        boolean allZero = true;
        for (var point : data.movementHistory()) {
            if (point.inQty() > 0 || point.outQty() > 0) {
                allZero = false;
                break;
            }
        }

        if (allZero || data.movementHistory().isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.getStyleClass().add("chart-empty-box");
            Label emptyTitle = new Label("No stock movement yet");
            emptyTitle.getStyleClass().add("chart-empty-text");
            Label emptySub = new Label("Stock IN and stock OUT deductions will appear here over the past 7 days.");
            emptySub.getStyleClass().add("chart-empty-subtext");
            emptyBox.getChildren().addAll(emptyTitle, emptySub);
            card.getChildren().addAll(header, emptyBox);
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(null);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(null);
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(250);
        barChart.setMinHeight(250);

        XYChart.Series<String, Number> seriesIn = new XYChart.Series<>();
        seriesIn.setName("Stock IN");

        XYChart.Series<String, Number> seriesOut = new XYChart.Series<>();
        seriesOut.setName("Stock OUT");

        for (var point : data.movementHistory()) {
            seriesIn.getData().add(new XYChart.Data<>(point.dateLabel(), point.inQty()));
            seriesOut.getData().add(new XYChart.Data<>(point.dateLabel(), point.outQty()));
        }

        barChart.getData().addAll(seriesIn, seriesOut);

        card.getChildren().addAll(header, barChart);
        return card;
    }

    private Node buildAiInsightsCard(DashboardService.DashboardData data) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        BorderPane header = new BorderPane();

        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath sparkle = new SVGPath();
        sparkle.setContent(SVG_SPARKLE);
        sparkle.setStyle("-fx-fill: -primary;");

        Label title = new Label("AI Insights");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        titleBox.getChildren().addAll(sparkle, title);
        header.setLeft(titleBox);

        int activeCount = data.activeInsights().size();
        Label badge = new Label(activeCount + " Active");
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -primary; -fx-background-color: -primary-light; -fx-padding: 3 8; -fx-background-radius: 6;");
        if (activeCount == 0) {
            badge.setVisible(false);
            badge.setManaged(false);
        }
        header.setRight(badge);

        VBox list = new VBox(10);
        if (data.activeInsights().isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.getStyleClass().add("insights-empty-box");

            SVGPath emptyIcon = new SVGPath();
            emptyIcon.setContent(SVG_SPARKLE);
            emptyIcon.getStyleClass().add("insights-empty-icon");

            Label emptyTitle = new Label("No insights yet");
            emptyTitle.getStyleClass().add("insights-empty-title");

            Label emptySub = new Label("Insights appear after a week of stock movement.");
            emptySub.getStyleClass().add("insights-empty-sub");

            emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptySub);
            list.getChildren().add(emptyBox);
        } else {
            for (AiInsight insight : data.activeInsights()) {
                list.getChildren().add(createInsightItem(insight));
            }
        }

        card.getChildren().addAll(header, list);
        return card;
    }

    private Node createInsightItem(AiInsight insight) {
        VBox item = new VBox(6);
        item.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-padding: 12; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 10;");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String catClean = insight.getCategory().replace("_", " ");
        Label catLabel = new Label(catClean);
        catLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #475569; -fx-background-color: #E2E8F0; -fx-padding: 2 6; -fx-background-radius: 4;");

        Label sevBadge = new Label(insight.getSeverity());
        if ("WARNING".equalsIgnoreCase(insight.getSeverity())) {
            sevBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #92400E; -fx-background-color: #FEF3C7; -fx-padding: 2 6; -fx-background-radius: 4;");
        } else if ("OPPORTUNITY".equalsIgnoreCase(insight.getSeverity())) {
            sevBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #065F46; -fx-background-color: #D1FAE5; -fx-padding: 2 6; -fx-background-radius: 4;");
        } else {
            sevBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #1E40AF; -fx-background-color: #DBEAFE; -fx-padding: 2 6; -fx-background-radius: 4;");
        }

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label metric = new Label(insight.getMetricValue() != null ? insight.getMetricValue() : "");
        metric.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        topRow.getChildren().addAll(catLabel, sevBadge, sp, metric);

        Label title = new Label(insight.getTitle());
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        Label desc = new Label(insight.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");

        HBox botRow = new HBox(8);
        botRow.setAlignment(Pos.CENTER_RIGHT);
        if (insight.getActionLabel() != null && !insight.getActionLabel().isEmpty()) {
            Button actionBtn = new Button(insight.getActionLabel());
            actionBtn.setStyle("-fx-background-color: white; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -primary; -fx-padding: 4 10; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> handleInsightAction(insight));
            botRow.getChildren().add(actionBtn);
        }

        item.getChildren().addAll(topRow, title, desc, botRow);
        return item;
    }

    private void handleInsightAction(AiInsight insight) {
        ViewNavigator.showOverlayDialog(
                insight.getTitle(),
                insight.getDescription() + "\n\nMetric: " + insight.getMetricValue(),
                ViewNavigator.OverlayType.INFO,
                null
        );
    }

    private Node buildNeedsRestockCard(DashboardService.DashboardData data) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        BorderPane header = new BorderPane();

        Label title = new Label("Needs Restock Immediately");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        header.setLeft(title);

        Label countBadge = new Label(formatItems(data.needsRestockList().size()));
        countBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -status-warning; -fx-background-color: -status-warning-bg; -fx-padding: 3 8; -fx-background-radius: 6;");
        header.setRight(countBadge);

        VBox list = new VBox(8);
        if (data.needsRestockList().isEmpty()) {
            Label empty = new Label("All product par levels are sufficient. No restocks required.");
            empty.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 13px; -fx-padding: 24 0;");
            list.getChildren().add(empty);
        } else {
            for (Product p : data.needsRestockList()) {
                list.getChildren().add(createRestockRow(p));
            }
        }

        card.getChildren().addAll(header, list);
        return card;
    }

    private Node createRestockRow(Product p) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-item-row");

        StackPane thumbBox = new StackPane();
        thumbBox.getStyleClass().add("item-thumb-box");

        boolean hasThumb = false;
        if (p.getImagePath() != null) {
            javafx.scene.image.Image cached = ProductImageManager.getImage(p.getImagePath(), p.getOrganizationId(), 36, 36);
            if (cached != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(cached);
                iv.setFitWidth(36);
                iv.setFitHeight(36);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                thumbBox.getChildren().add(iv);
                hasThumb = true;
            }
        }
        if (!hasThumb) {
            SVGPath boxIcon = new SVGPath();
            boxIcon.setContent(SVG_PACKAGE);
            boxIcon.getStyleClass().add("item-thumb-icon");
            thumbBox.getChildren().add(boxIcon);
        }

        VBox info = new VBox(2);
        Label name = new Label(p.getName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -text-main;");
        name.setWrapText(true);

        Label sku = new Label("SKU: " + p.getSku() + " • " + (p.getCategoryName() != null ? p.getCategoryName() : "General"));
        sku.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
        sku.setWrapText(true);
        info.getChildren().addAll(name, sku);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox countBox = new VBox(2);
        countBox.setAlignment(Pos.CENTER_RIGHT);

        String countText = p.getStockQuantity() + " / " + p.getLowStockThreshold() + " " + Constants.UNIT;
        Label countLbl = new Label(countText);
        countLbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: " + (p.getStockQuantity() == 0 ? "#EF4444" : "#F59E0B") + ";");

        Label statusLbl = new Label(p.getStatus().replace("_", " "));
        statusLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: -text-muted;");

        countBox.getChildren().addAll(countLbl, statusLbl);

        Button restockBtn = new Button("Restock");
        restockBtn.getStyleClass().add("btn-restock-action");
        restockBtn.setOnAction(e -> {
            ViewNavigator.showOverlayDialog(
                    "Restock Reorder Notice",
                    "Product: " + p.getName() + " (" + p.getSku() + ")\n"
                            + "Current Stock: " + p.getStockQuantity() + " " + Constants.UNIT + "\n"
                            + "Par Level Target: " + p.getLowStockThreshold() + " " + Constants.UNIT + "\n\n"
                            + "Recommended Reorder: " + Math.max(10, p.getLowStockThreshold() * 2 - p.getStockQuantity()) + " " + Constants.UNIT + ".",
                    ViewNavigator.OverlayType.INFO,
                    null
            );
        });

        row.getChildren().addAll(thumbBox, info, countBox, restockBtn);
        return row;
    }

    private Node buildRecentMovementsCard(DashboardService.DashboardData data) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        BorderPane header = new BorderPane();

        Label title = new Label("Recent Stock Movements");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        header.setLeft(title);

        Label subtitle = new Label("Live Audit Stream");
        subtitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -text-muted;");
        header.setRight(subtitle);

        VBox list = new VBox(8);
        if (data.recentMovements().isEmpty()) {
            Label empty = new Label("No inventory movements recorded yet.");
            empty.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 13px; -fx-padding: 24 0;");
            list.getChildren().add(empty);
        } else {
            for (StockMovement m : data.recentMovements()) {
                list.getChildren().add(createMovementRow(m));
            }
        }

        card.getChildren().addAll(header, list);
        return card;
    }

    private Node createMovementRow(StockMovement m) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-item-row");

        boolean isOut = "OUT".equalsIgnoreCase(m.getMovementType());
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("item-thumb-box");

        SVGPath icon = new SVGPath();
        icon.setContent(isOut ? SVG_ARROW_DN : SVG_ARROW_UP);
        icon.setStyle("-fx-fill: " + (isOut ? "#EF4444" : "#10B981") + ";");
        iconBox.getChildren().add(icon);

        VBox info = new VBox(2);
        Label name = new Label(m.getProductName() != null ? m.getProductName() : "Product #" + m.getProductId());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -text-main;");
        name.setWrapText(true);

        String timeStr = (m.getCreatedAt() != null)
                ? m.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                : "Recent";
        Label details = new Label(m.getReferenceNo() + " • " + timeStr + " • By " + (m.getUserName() != null ? m.getUserName() : "User"));
        details.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
        details.setWrapText(true);

        info.getChildren().addAll(name, details);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label reasonBadge = new Label(m.getReason());
        reasonBadge.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: -text-muted; -fx-background-color: #E2E8F0; -fx-padding: 3 8; -fx-background-radius: 4;");

        String delta = (isOut ? "-" : "+") + m.getQuantity() + " " + Constants.UNIT;
        Label qtyLbl = new Label(delta);
        qtyLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + (isOut ? "#EF4444" : "#10B981") + ";");

        row.getChildren().addAll(iconBox, info, reasonBadge, qtyLbl);
        return row;
    }

    private void showLoading() {
        loadingPane.setVisible(true);
        loadingPane.setManaged(true);
        errorPane.setVisible(false);
        errorPane.setManaged(false);
        contentScroll.setVisible(false);
        contentScroll.setManaged(false);
        if (refreshBtn != null) refreshBtn.setDisable(true);
    }

    private void showContent() {
        loadingPane.setVisible(false);
        loadingPane.setManaged(false);
        errorPane.setVisible(false);
        errorPane.setManaged(false);
        contentScroll.setVisible(true);
        contentScroll.setManaged(true);
        if (refreshBtn != null) refreshBtn.setDisable(false);
    }

    private void showError(String msg) {
        loadingPane.setVisible(false);
        loadingPane.setManaged(false);
        errorPane.setVisible(true);
        errorPane.setManaged(true);
        contentScroll.setVisible(false);
        contentScroll.setManaged(false);
        errorLabel.setText("Error loading dashboard: " + msg);
        if (refreshBtn != null) refreshBtn.setDisable(false);
    }

    @Override
    public void dispose() {
        this.isDisposed = true;
    }
}
