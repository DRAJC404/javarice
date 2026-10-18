package com.bai.inventory.view.pages;

import com.bai.inventory.dao.AnalyticsDao.*;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AIInsightService;
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
import com.bai.inventory.view.component.StatCard;
import com.bai.inventory.view.component.StatCard.StatType;
import com.bai.inventory.view.navigation.ViewNavigator;
import com.bai.inventory.view.navigation.ViewType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class AnalyticsView extends StackPane implements Disposable {

    private static final String SVG_REFRESH   = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_ANALYTICS = "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z";
    private static final String SVG_SPARKLE   = "M12 2l2.4 7.2L22 12l-7.6 2.8L12 22l-2.4-7.2L2 12l7.6-2.8z";

    private final AnalyticsService analyticsService;
    private final AIInsightService aiInsightService;
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

    // Ask bAI components
    private TextField askField;
    private Label charCounter;
    private Label askStatusNote;
    private Button askBtn;
    private VBox askResponseBox;
    private Label askResponseText;

    public AnalyticsView() {
        this.analyticsService = new AnalyticsService();
        this.aiInsightService = new AIInsightService();
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
        contentBox.setPadding(new Insets(16, 24, 40, 24));
        contentBox.setMaxWidth(1400);

        scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("scroll-pane");

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

        Node askBaiCard = buildAskBaiCard();
        Node kpiCards = buildKpiCardsRow(data.kpiSummary());
        Node movementTrend = buildMovementTrendCard(data.movementTrend());
        Node middleRow = buildMiddleRow(data.topProducts(), data.stockStatus());
        Node bottomRow = buildBottomRow(data.categoryDeductions(), data.reasonDeductions());
        Node peakActivityRow = buildPeakActivityRow(data.peakDays(), data.peakHours());

        contentBox.getChildren().addAll(
                askBaiCard,
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

    private Node buildAskBaiCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("ask-bai-card");

        BorderPane headerRow = new BorderPane();
        headerRow.getStyleClass().add("ask-bai-header");

        HBox leftHeader = new HBox(8);
        leftHeader.setAlignment(Pos.CENTER_LEFT);

        SVGPath spark = new SVGPath();
        spark.setContent(SVG_SPARKLE);
        spark.getStyleClass().add("dialog-icon-info");

        Label title = new Label("Ask bAI Assistant");
        title.getStyleClass().add("ask-bai-title");

        leftHeader.getChildren().addAll(spark, title);
        headerRow.setLeft(leftHeader);

        askStatusNote = new Label("Ask concise questions about your store's movement data");
        askStatusNote.getStyleClass().add("ask-bai-note");
        headerRow.setRight(askStatusNote);

        HBox inputRow = new HBox(10);
        inputRow.getStyleClass().add("ask-bai-input-row");
        inputRow.setAlignment(Pos.CENTER_LEFT);

        askField = new TextField();
        askField.setPromptText("Ask about inventory velocity (e.g. Which category had the highest deduction?)...");
        askField.getStyleClass().add("ask-bai-field");
        HBox.setHgrow(askField, Priority.ALWAYS);

        charCounter = new Label("0 / 200");
        charCounter.getStyleClass().add("ask-bai-counter");

        askField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 200) {
                askField.setText(oldVal);
                return;
            }
            int len = newVal != null ? newVal.length() : 0;
            charCounter.setText(len + " / 200");
        });

        askBtn = new Button("Ask bAI");
        askBtn.getStyleClass().add("btn-primary");
        askBtn.setOnAction(e -> handleAskBai());
        askField.setOnAction(e -> handleAskBai());

        inputRow.getChildren().addAll(askField, charCounter, askBtn);

        askResponseBox = new VBox(6);
        askResponseBox.getStyleClass().add("ask-bai-response-box");
        askResponseBox.setVisible(false);
        askResponseBox.setManaged(false);

        askResponseText = new Label();
        askResponseText.setWrapText(true);
        askResponseText.getStyleClass().add("ask-bai-response-text");
        askResponseBox.getChildren().add(askResponseText);

        card.getChildren().addAll(headerRow, inputRow, askResponseBox);
        return card;
    }

    private void handleAskBai() {
        if (askField == null || askBtn == null) return;
        String query = askField.getText() != null ? askField.getText().trim() : "";
        if (query.isEmpty()) return;

        askBtn.setDisable(true);
        askBtn.setText("Analyzing...");
        askResponseBox.setVisible(false);
        askResponseBox.setManaged(false);

        User user = AuthService.getCurrentUser();
        AppExecutor.execute(() -> {
            String answer = aiInsightService.askBai(query, user);
            Platform.runLater(() -> {
                askBtn.setDisable(false);
                askBtn.setText("Ask bAI");
                askResponseText.setText(answer);
                askResponseBox.setVisible(true);
                askResponseBox.setManaged(true);
            });
        });
    }

    private Node buildKpiCardsRow(AnalyticsKpiSummary kpi) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);

        StatCard card1 = new StatCard(
                "Total Deductions",
                kpi.totalDeductions().formattedCurrent(),
                kpi.totalDeductions().formattedChange(),
                StatType.PRIMARY
        );

        StatCard card2 = new StatCard(
                "Units Deducted",
                kpi.unitsDeducted().formattedCurrent(),
                kpi.unitsDeducted().formattedChange(),
                StatType.WARNING
        );

        StatCard card3 = new StatCard(
                "Active Products",
                kpi.activeProducts().formattedCurrent(),
                kpi.activeProducts().formattedChange(),
                StatType.INFO
        );

        StatCard card4 = new StatCard(
                "Avg Daily Deduction",
                kpi.avgDailyDeduction().formattedCurrent(),
                kpi.avgDailyDeduction().formattedChange(),
                StatType.SUCCESS
        );

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, card4);
        return row;
    }

    private Node buildMovementTrendCard(List<DailyMovementTrend> trends) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Daily Stock Movement Trend");
        title.getStyleClass().add("analytics-section-title");
        Label subtitle = new Label("Day-by-day comparison of incoming stock received vs units deducted");
        subtitle.getStyleClass().add("analytics-section-sub");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");

        NumberAxis yAxis = createIntegerNumberAxis("Quantity (units)");

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

        Node topProductsCard = buildTopProductsBarCard(topProducts);
        Region stockStatusCard = buildStockStatusDonutCard(stockStatus);

        HBox.setHgrow(topProductsCard, Priority.ALWAYS);
        stockStatusCard.setPrefWidth(380);
        stockStatusCard.setMinWidth(340);

        row.getChildren().addAll(topProductsCard, stockStatusCard);
        return row;
    }

    private Node buildTopProductsBarCard(List<TopProduct> topProducts) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);

        VBox titleBox = new VBox(2);
        Label title = new Label("Top Moving Products");
        title.getStyleClass().add("analytics-section-title");
        Label subtitle = new Label("Products with the highest deduction volume in this period");
        subtitle.getStyleClass().add("analytics-section-sub");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Product");

        NumberAxis yAxis = createIntegerNumberAxis("Units Deducted");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setPrefHeight(230);
        chart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (TopProduct p : topProducts) {
            String label = p.name().length() > 18 ? p.name().substring(0, 15) + "..." : p.name();
            series.getData().add(new XYChart.Data<>(label, p.unitsDeducted()));
        }
        chart.getData().add(series);

        card.getChildren().addAll(titleBox, chart);
        return card;
    }

    private Region buildStockStatusDonutCard(StockStatusCount stockStatus) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Stock Health Status");
        title.getStyleClass().add("analytics-section-title");
        Label subtitle = new Label("Overall catalog stock level distribution");
        subtitle.getStyleClass().add("analytics-section-sub");
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
        hole.getStyleClass().add("donut-hole-circle");

        VBox centerText = new VBox(2);
        centerText.setAlignment(Pos.CENTER);
        Label countLbl = new Label(String.valueOf(total));
        countLbl.getStyleClass().add("donut-center-count");
        Label labelLbl = new Label("Products");
        labelLbl.getStyleClass().add("donut-center-label");
        centerText.getChildren().addAll(countLbl, labelLbl);

        donutStack.getChildren().addAll(pie, hole, centerText);

        HBox legendRow = new HBox(12);
        legendRow.setAlignment(Pos.CENTER);
        legendRow.getChildren().addAll(
                createStatusLegendItem("In Stock", stockStatus.inStock(), "status-dot-in-stock"),
                createStatusLegendItem("Low Stock", stockStatus.lowStock(), "status-dot-low-stock"),
                createStatusLegendItem("Out of Stock", stockStatus.outOfStock(), "status-dot-out-of-stock")
        );

        card.getChildren().addAll(titleBox, donutStack, legendRow);
        return card;
    }

    private Node createStatusLegendItem(String label, int count, String dotClass) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER);

        Circle dot = new Circle(4);
        dot.getStyleClass().add(dotClass);

        Label text = new Label(label + ": " + count);
        text.getStyleClass().add("chart-legend-text");

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
        title.getStyleClass().add("analytics-section-title");
        Label subtitle = new Label("Total volume deducted per product category");
        subtitle.getStyleClass().add("analytics-section-sub");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = createIntegerNumberAxis("Units Deducted");

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
        title.getStyleClass().add("analytics-section-title");
        Label subtitle = new Label("Distribution by operational cause (Sold, Used, Waste)");
        subtitle.getStyleClass().add("analytics-section-sub");
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
            pct.getStyleClass().add("reason-pct-text");

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
        title.getStyleClass().add("analytics-section-title");
        Label subtitle = new Label("Movement activity and volume by weekday");
        subtitle.getStyleClass().add("analytics-section-sub");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = createIntegerNumberAxis("Movements");

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
        title.getStyleClass().add("analytics-section-title");
        Label subtitle = new Label("Hourly concentration of inventory deductions (07:00 - 21:00)");
        subtitle.getStyleClass().add("analytics-section-sub");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = createIntegerNumberAxis("Transactions");

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

    private NumberAxis createIntegerNumberAxis(String label) {
        NumberAxis axis = new NumberAxis();
        if (label != null) axis.setLabel(label);
        axis.setMinorTickVisible(false);
        axis.setTickUnit(1);
        axis.setForceZeroInRange(true);
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                if (object == null) return "";
                if (Math.abs(object.doubleValue() - object.longValue()) < 0.0001) {
                    return String.format(Locale.US, "%,d", object.longValue());
                }
                return "";
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
        return axis;
    }

    @Override
    public void dispose() {
        this.isDisposed = true;
        requestId.incrementAndGet();
    }
}
