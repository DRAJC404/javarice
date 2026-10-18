package com.bai.inventory.view.pages;

import com.bai.inventory.dao.AnalyticsDao.*;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AnalyticsService;
import com.bai.inventory.service.AnalyticsService.AnalyticsData;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.Badge;
import com.bai.inventory.view.component.EmptyState;
import com.bai.inventory.view.component.ErrorBanner;
import com.bai.inventory.view.component.LoadingSkeleton;
import com.bai.inventory.view.component.PageHeader;
import com.bai.inventory.view.navigation.ViewNavigator;
import com.bai.inventory.view.navigation.ViewType;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class AnalyticsView extends StackPane implements Disposable {

    private static final String SVG_REFRESH   = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_ARROW_UP  = "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z";
    private static final String SVG_ARROW_DN  = "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z";
    private static final String SVG_ANALYTICS = "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z";

    private final AnalyticsService analyticsService;
    private final NumberFormat pesoFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final AtomicLong requestId = new AtomicLong(0);

    private volatile boolean isDisposed = false;
    private int currentPeriodDays = 30;

    private ScrollPane scrollPane;
    private VBox contentBox;
    private LoadingSkeleton loadingView;
    private ErrorBanner errorBanner;
    private EmptyState emptyStateView;

    private ComboBox<String> periodCombo;
    private Button refreshBtn;

    public AnalyticsView() {
        this.analyticsService = new AnalyticsService();
        buildUI();
        loadDataAsync();
    }

    private void buildUI() {
        getStyleClass().add("content-pane");

        loadingView = new LoadingSkeleton("Crunching stock movements and tenant metrics...");
        loadingView.setVisible(false);
        loadingView.setManaged(false);

        errorBanner = new ErrorBanner("Unable to calculate inventory analytics metrics.", this::loadDataAsync);
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);

        emptyStateView = new EmptyState(
                SVG_ANALYTICS,
                "No Movement Analytics Yet",
                "Start recording sales, usage, or restocks in the Store Terminal or Inventory page to see velocity trends.",
                "Go to Inventory Control",
                () -> ViewNavigator.navigateTo(ViewType.INVENTORY)
        );
        emptyStateView.setVisible(false);
        emptyStateView.setManaged(false);

        contentBox = new VBox(20);
        contentBox.setPadding(new Insets(12, 16, 40, 16));

        scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        getChildren().addAll(scrollPane, loadingView, emptyStateView);
    }

    private void loadDataAsync() {
        if (isDisposed) return;
        long reqId = requestId.incrementAndGet();

        loadingView.setVisible(true);
        loadingView.setManaged(true);
        emptyStateView.setVisible(false);
        emptyStateView.setManaged(false);
        if (refreshBtn != null) refreshBtn.setDisable(true);

        User user = AuthService.getCurrentUser();
        int days = currentPeriodDays;

        AppExecutor.execute(() -> {
            try {
                AnalyticsData data = analyticsService.getAnalyticsData(days, user);
                if (reqId == requestId.get() && !isDisposed) {
                    Platform.runLater(() -> {
                        loadingView.setVisible(false);
                        loadingView.setManaged(false);
                        if (refreshBtn != null) refreshBtn.setDisable(false);
                        renderAnalytics(data);
                    });
                }
            } catch (Exception ex) {
                if (reqId == requestId.get() && !isDisposed) {
                    Platform.runLater(() -> {
                        loadingView.setVisible(false);
                        loadingView.setManaged(false);
                        if (refreshBtn != null) refreshBtn.setDisable(false);
                        showError(ex.getMessage());
                    });
                }
            }
        });
    }

    private void showError(String msg) {
        contentBox.getChildren().clear();
        errorBanner.setMessage("Failed to load analytics: " + (msg != null ? msg : "Database query error."));
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
        contentBox.getChildren().add(errorBanner);
    }

    private void renderAnalytics(AnalyticsData data) {
        contentBox.getChildren().clear();

        Node header = buildHeader();
        contentBox.getChildren().add(header);

        if (!data.hasData()) {
            emptyStateView.setVisible(true);
            emptyStateView.setManaged(true);
            return;
        }

        Node kpiCards = buildKpiCardsRow(data.kpiSummary());
        Node movementTrend = buildMovementTrendCard(data.movementTrend());
        Node middleRow = buildMiddleRow(data.topProducts(), data.stockStatus());
        Node bottomRow = buildBottomRow(data.categoryDeductions(), data.reasonDeductions());
        Node peakActivityRow = buildPeakActivityRow(data.peakDays(), data.peakHours());

        contentBox.getChildren().addAll(
                kpiCards,
                movementTrend,
                middleRow,
                bottomRow,
                peakActivityRow
        );
    }

    private Node buildHeader() {
        PageHeader header = new PageHeader(
                "Stock Analytics & Velocity",
                "Turnover rates, shrinkage analysis, category outflow, and peak operational hours."
        );

        periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll("Last 7 Days", "Last 30 Days", "Last 90 Days");
        if (currentPeriodDays == 7) periodCombo.setValue("Last 7 Days");
        else if (currentPeriodDays == 90) periodCombo.setValue("Last 90 Days");
        else periodCombo.setValue("Last 30 Days");
        periodCombo.getStyleClass().add("combo-box");
        periodCombo.setPrefWidth(160);

        periodCombo.setOnAction(e -> {
            String val = periodCombo.getValue();
            if ("Last 7 Days".equals(val)) currentPeriodDays = 7;
            else if ("Last 90 Days".equals(val)) currentPeriodDays = 90;
            else currentPeriodDays = 30;
            loadDataAsync();
        });

        refreshBtn = new Button();
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setTooltip(new Tooltip("Refresh Analytics"));
        SVGPath refIcon = new SVGPath();
        refIcon.setContent(SVG_REFRESH);
        refIcon.getStyleClass().add("btn-icon-svg");
        refreshBtn.setGraphic(refIcon);
        refreshBtn.setOnAction(e -> loadDataAsync());

        header.addActions(periodCombo, refreshBtn);
        return header;
    }

    private Node buildKpiCardsRow(AnalyticsKpiSummary kpi) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);

        Node cardIn = createKpiCard("Stock Added (Inflow)", kpi.inflow());
        Node cardOut = createKpiCard("Stock Deducted (Outflow)", kpi.outflow());
        Node cardNet = createKpiCard("Net Stock Movement", kpi.netMovement());
        Node cardVal = createKpiCard("Deduction Valuation", kpi.turnoverValue());
        Node cardShrink = createKpiCard("Shrinkage / Waste Rate", kpi.shrinkageRate());

        HBox.setHgrow(cardIn, Priority.ALWAYS);
        HBox.setHgrow(cardOut, Priority.ALWAYS);
        HBox.setHgrow(cardNet, Priority.ALWAYS);
        HBox.setHgrow(cardVal, Priority.ALWAYS);
        HBox.setHgrow(cardShrink, Priority.ALWAYS);

        row.getChildren().addAll(cardIn, cardOut, cardNet, cardVal, cardShrink);
        return row;
    }

    private Node createKpiCard(String title, KpiComparison comp) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", "stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-card-title");

        Label valLabel = new Label(comp.formattedCurrent());
        valLabel.getStyleClass().add("stat-card-value");

        HBox trendBox = new HBox(4);
        trendBox.setAlignment(Pos.CENTER_LEFT);

        boolean isNeutral = Math.abs(comp.percentChange()) < 0.05;
        boolean isUp = comp.percentChange() > 0;
        boolean isGood = comp.isPositiveGood() ? isUp : !isUp;

        Label pill = new Label();
        SVGPath arrow = new SVGPath();
        if (isNeutral) {
            pill.getStyleClass().addAll("badge", "badge-neutral");
            arrow.setContent("");
        } else if (isGood) {
            pill.getStyleClass().addAll("badge", "badge-success");
            arrow.setContent(isUp ? SVG_ARROW_UP : SVG_ARROW_DN);
            arrow.setStyle("-fx-fill: #065F46;");
        } else {
            pill.getStyleClass().addAll("badge", "badge-danger");
            arrow.setContent(isUp ? SVG_ARROW_UP : SVG_ARROW_DN);
            arrow.setStyle("-fx-fill: #991B1B;");
        }

        if (!arrow.getContent().isEmpty()) {
            pill.setGraphic(arrow);
        }
        pill.setText(comp.formattedChange());
        trendBox.getChildren().add(pill);

        card.getChildren().addAll(titleLabel, valLabel, trendBox);
        return card;
    }

    private Node buildMovementTrendCard(List<DailyMovementTrend> trends) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Daily Stock Movement Trend");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        Label subtitle = new Label("Day-by-day comparison of incoming stock received vs units deducted");
        subtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Quantity (units)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setPrefHeight(270);
        chart.setAnimated(false);
        chart.setLegendSide(Side.TOP);

        XYChart.Series<String, Number> seriesIn = new XYChart.Series<>();
        seriesIn.setName("Stock In (Received)");

        XYChart.Series<String, Number> seriesOut = new XYChart.Series<>();
        seriesOut.setName("Stock Out (Deducted)");

        for (DailyMovementTrend pt : trends) {
            seriesIn.getData().add(new XYChart.Data<>(pt.dateLabel(), pt.inflowQty()));
            seriesOut.getData().add(new XYChart.Data<>(pt.dateLabel(), pt.outflowQty()));
        }

        chart.getData().addAll(seriesIn, seriesOut);

        card.getChildren().addAll(titleBox, chart);
        return card;
    }

    private Node buildMiddleRow(List<TopProduct> topProducts, StockStatusCount stockStatus) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);

        Node topProductsCard = buildTopProductsCard(topProducts);
        Region stockStatusCard = buildStockStatusDonutCard(stockStatus);

        HBox.setHgrow(topProductsCard, Priority.ALWAYS);
        stockStatusCard.setPrefWidth(380);
        stockStatusCard.setMinWidth(340);

        row.getChildren().addAll(topProductsCard, stockStatusCard);
        return row;
    }

    private Node buildTopProductsCard(List<TopProduct> topProducts) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);

        VBox titleBox = new VBox(2);
        Label title = new Label("Top Moving Products");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        Label subtitle = new Label("Products with the highest deduction velocity in this period");
        subtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        TableView<TopProduct> table = new TableView<>();
        table.setPrefHeight(230);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TopProduct, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setMinWidth(150);

        TableColumn<TopProduct, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sku()));
        skuCol.setMinWidth(85);

        TableColumn<TopProduct, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().category()));
        catCol.setMinWidth(95);

        TableColumn<TopProduct, String> qtyCol = new TableColumn<>("Deducted");
        qtyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().unitsDeducted() + " units"));
        qtyCol.setMinWidth(80);

        TableColumn<TopProduct, String> valCol = new TableColumn<>("Turnover Value");
        valCol.setCellValueFactory(data -> new SimpleStringProperty(pesoFormat.format(data.getValue().turnoverValue())));
        valCol.setMinWidth(100);

        TableColumn<TopProduct, Void> statusCol = new TableColumn<>("Status");
        statusCol.setMinWidth(95);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    TopProduct p = getTableRow().getItem();
                    Badge badge = switch (p.status()) {
                        case "IN_STOCK" -> Badge.success("In Stock (" + p.currentStock() + ")");
                        case "LOW_STOCK" -> Badge.warning("Low (" + p.currentStock() + ")");
                        case "OUT_OF_STOCK" -> Badge.danger("Out of Stock");
                        default -> Badge.neutral(p.status());
                    };
                    setGraphic(badge);
                }
            }
        });

        table.getColumns().addAll(nameCol, skuCol, catCol, qtyCol, valCol, statusCol);
        table.getItems().setAll(topProducts);

        card.getChildren().addAll(titleBox, table);
        return card;
    }

    private Region buildStockStatusDonutCard(StockStatusCount stockStatus) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Stock Health Status");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        Label subtitle = new Label("Overall catalog stock level distribution");
        subtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        PieChart pie = new PieChart();
        pie.setAnimated(false);
        pie.setLegendVisible(false);
        pie.setLabelsVisible(true);
        pie.setPrefHeight(180);

        int total = stockStatus.total();
        if (total > 0) {
            PieChart.Data inStockData = new PieChart.Data("In Stock (" + stockStatus.inStock() + ")", stockStatus.inStock());
            PieChart.Data lowStockData = new PieChart.Data("Low Stock (" + stockStatus.lowStock() + ")", stockStatus.lowStock());
            PieChart.Data outStockData = new PieChart.Data("Out of Stock (" + stockStatus.outOfStock() + ")", stockStatus.outOfStock());
            pie.getData().addAll(inStockData, lowStockData, outStockData);
        }

        StackPane donutStack = new StackPane();
        Circle hole = new Circle(44);
        hole.setStyle("-fx-fill: white; -fx-stroke: -border-subtle; -fx-stroke-width: 1px;");

        VBox centerText = new VBox(2);
        centerText.setAlignment(Pos.CENTER);
        Label countLbl = new Label(String.valueOf(total));
        countLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: -text-main;");
        Label labelLbl = new Label("Products");
        labelLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
        centerText.getChildren().addAll(countLbl, labelLbl);

        donutStack.getChildren().addAll(pie, hole, centerText);

        HBox legendRow = new HBox(12);
        legendRow.setAlignment(Pos.CENTER);
        legendRow.getChildren().addAll(
                createStatusLegendItem("In Stock", stockStatus.inStock(), "#10B981"),
                createStatusLegendItem("Low Stock", stockStatus.lowStock(), "#F59E0B"),
                createStatusLegendItem("Out of Stock", stockStatus.outOfStock(), "#EF4444")
        );

        card.getChildren().addAll(titleBox, donutStack, legendRow);
        return card;
    }

    private Node createStatusLegendItem(String label, int count, String colorHex) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER);

        Circle dot = new Circle(4);
        dot.setStyle("-fx-fill: " + colorHex + ";");

        Label text = new Label(label + ": " + count);
        text.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: -text-muted;");

        item.getChildren().addAll(dot, text);
        return item;
    }

    private Node buildBottomRow(List<CategoryDeduction> catDeductions, List<ReasonDeduction> reasonDeductions) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);

        Node catCard = buildCategoryDeductionsCard(catDeductions);
        Node reasonCard = buildReasonDeductionsCard(reasonDeductions);

        HBox.setHgrow(catCard, Priority.ALWAYS);
        HBox.setHgrow(reasonCard, Priority.ALWAYS);

        row.getChildren().addAll(catCard, reasonCard);
        return row;
    }

    private Node buildCategoryDeductionsCard(List<CategoryDeduction> catDeductions) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Deductions by Category");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        Label subtitle = new Label("Total volume deducted per product category");
        subtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Units Deducted");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setPrefHeight(230);
        chart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (CategoryDeduction cd : catDeductions) {
            series.getData().add(new XYChart.Data<>(cd.categoryName(), cd.unitsDeducted()));
        }
        chart.getData().add(series);

        card.getChildren().addAll(titleBox, chart);
        return card;
    }

    private Node buildReasonDeductionsCard(List<ReasonDeduction> reasonDeductions) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Deduction Reasons Breakdown");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        Label subtitle = new Label("Distribution by operational cause (Sold, Used, Waste)");
        subtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        HBox content = new HBox(16);
        content.setAlignment(Pos.CENTER);

        PieChart pie = new PieChart();
        pie.setAnimated(false);
        pie.setLegendVisible(false);
        pie.setLabelsVisible(false);
        pie.setPrefSize(180, 180);
        pie.setMaxSize(180, 180);

        VBox reasonsList = new VBox(8);
        reasonsList.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(reasonsList, Priority.ALWAYS);

        for (ReasonDeduction rd : reasonDeductions) {
            pie.getData().add(new PieChart.Data(rd.reason(), rd.quantity()));

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label badge = Badge.neutral(rd.reason());
            badge.setMinWidth(70);

            ProgressBar bar = new ProgressBar(Math.min(1.0, rd.percentage() / 100.0));
            bar.setPrefWidth(90);
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label pct = new Label(String.format(Locale.US, "%.1f%% (%d)", rd.percentage(), rd.quantity()));
            pct.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: -text-main;");

            row.getChildren().addAll(badge, bar, pct);
            reasonsList.getChildren().add(row);
        }

        content.getChildren().addAll(pie, reasonsList);
        card.getChildren().addAll(titleBox, content);
        return card;
    }

    private Node buildPeakActivityRow(List<PeakDay> peakDays, List<PeakHour> peakHours) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);

        Node daysCard = buildPeakDaysCard(peakDays);
        Node hoursCard = buildPeakHoursCard(peakHours);

        HBox.setHgrow(daysCard, Priority.ALWAYS);
        HBox.setHgrow(hoursCard, Priority.ALWAYS);

        row.getChildren().addAll(daysCard, hoursCard);
        return row;
    }

    private Node buildPeakDaysCard(List<PeakDay> peakDays) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Peak Days of the Week");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        Label subtitle = new Label("Movement activity and volume by weekday");
        subtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Movements");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setPrefHeight(200);
        chart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (PeakDay pd : peakDays) {
            series.getData().add(new XYChart.Data<>(pd.dayName(), pd.movementCount()));
        }
        chart.getData().add(series);

        card.getChildren().addAll(titleBox, chart);
        return card;
    }

    private Node buildPeakHoursCard(List<PeakHour> peakHours) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Peak Operational Hours");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        Label subtitle = new Label("Hourly concentration of inventory deductions (07:00 - 21:00)");
        subtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Transactions");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setPrefHeight(200);
        chart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (PeakHour ph : peakHours) {
            series.getData().add(new XYChart.Data<>(ph.hourLabel(), ph.movementCount()));
        }
        chart.getData().add(series);

        card.getChildren().addAll(titleBox, chart);
        return card;
    }

    @Override
    public void dispose() {
        this.isDisposed = true;
        requestId.incrementAndGet();
    }
}
