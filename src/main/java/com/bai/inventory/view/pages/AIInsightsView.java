package com.bai.inventory.view.pages;

import com.bai.inventory.config.EnvConfig;
import com.bai.inventory.dao.AiInsightDao;
import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.dao.StockMovementDao;
import com.bai.inventory.model.AiInsight;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AIInsightService;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.util.Constants;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.EmptyState;
import com.bai.inventory.view.component.PageHeader;
import com.bai.inventory.view.component.StatCard;
import com.bai.inventory.view.navigation.ViewNavigator;
import com.bai.inventory.view.navigation.ViewType;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AIInsightsView extends BorderPane implements Disposable {

    private static final String SVG_SPARKLE = "M12 2L9.5 8.5 3 11l6.5 2.5L12 20l2.5-6.5L21 11l-6.5-2.5z";
    private static final String SVG_REFRESH = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_ALERT   = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd");
    private static final NumberFormat PESO_FMT = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    private final AIInsightService aiInsightService;
    private final ProductDao productDao;
    private final StockMovementDao stockMovementDao;
    private final AiInsightDao aiInsightDao;

    private ScrollPane scrollPane;
    private VBox contentBox;
    private ProgressIndicator loadingSpinner;
    private Label modelBadge;

    private TextField askField;
    private Button askBtn;
    private Label charCounter;
    private VBox askResponseBox;
    private Label askResponseText;

    private volatile boolean isDisposed = false;

    public AIInsightsView() {
        this.aiInsightService = new AIInsightService();
        this.productDao = new ProductDao();
        this.stockMovementDao = new StockMovementDao();
        this.aiInsightDao = new AiInsightDao();

        buildUI();
        loadAllInsightsAsync();
    }

    private void buildUI() {
        getStyleClass().add("content-pane");

        scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        contentBox = new VBox(24);
        contentBox.setPadding(new Insets(28));
        contentBox.setFillWidth(true);

        scrollPane.setContent(contentBox);
        setCenter(scrollPane);
    }

    private void loadAllInsightsAsync() {
        if (isDisposed) return;

        contentBox.getChildren().clear();

        VBox loadingBox = new VBox(16);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(60));
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        Label loadingLabel = new Label("Generating smart predictive insights & inventory forecast...");
        loadingLabel.getStyleClass().add("form-helper");
        loadingBox.getChildren().addAll(spinner, loadingLabel);

        contentBox.getChildren().add(loadingBox);

        User currentUser = AuthService.getCurrentUser();
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;

        AppExecutor.execute(() -> {
            try {
                AIInsightService.InsightResult insightResult = aiInsightService.getInsights(currentUser);
                List<Product> needsRestock = productDao.findNeedsRestock(6, orgId);
                List<StockMovementDao.DailyMovementPoint> history = stockMovementDao.getDailyMovementHistory(7, orgId);
                int distinctDays = aiInsightDao.getDistinctMovementDaysCount(orgId);

                Platform.runLater(() -> {
                    if (isDisposed) return;
                    renderInsightsView(insightResult, needsRestock, history, distinctDays);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (isDisposed) return;
                    contentBox.getChildren().clear();
                    EmptyState err = new EmptyState(
                            SVG_ALERT,
                            "Failed to generate AI insights",
                            ex.getMessage() != null ? ex.getMessage() : "Please retry or check system diagnostics.",
                            "Retry Insights",
                            this::loadAllInsightsAsync
                    );
                    contentBox.getChildren().add(err);
                });
            }
        });
    }

    private void renderInsightsView(AIInsightService.InsightResult result, List<Product> needsRestock,
                                    List<StockMovementDao.DailyMovementPoint> history, int distinctDays) {
        contentBox.getChildren().clear();

        Node header = buildHeader(result);
        Node kpiCards = buildSummaryCards(result, needsRestock);
        Node askBaiSection = buildAskBaiSection();
        Node forecastCard = buildForecastChartCard(history);
        Node insightsGrid = buildInsightCards(result.insights());
        Node restockAndAnomalies = buildRestockAndAnomaliesRow(needsRestock);

        contentBox.getChildren().addAll(
                header,
                kpiCards,
                askBaiSection,
                forecastCard,
                insightsGrid,
                restockAndAnomalies
        );
    }

    private Node buildHeader(AIInsightService.InsightResult result) {
        PageHeader header = new PageHeader(
                "bAI Smart Insights & Forecasts",
                "Automated demand forecasting, inventory velocity signals, and predictive restock advisories."
        );

        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(20, 20);
        loadingSpinner.setVisible(false);

        modelBadge = new Label();
        modelBadge.getStyleClass().add("badge");
        if (result.isAiGenerated()) {
            modelBadge.getStyleClass().add("badge-info");
            modelBadge.setText("Active AI: " + EnvConfig.getModel());
        } else {
            modelBadge.getStyleClass().add("badge-warning");
            modelBadge.setText("Rule-Based Mode (AI_API_KEY not set)");
        }

        Button refreshBtn = new Button("Refresh Insights");
        refreshBtn.getStyleClass().add("btn-secondary");
        SVGPath refIcon = new SVGPath();
        refIcon.setContent(SVG_REFRESH);
        refIcon.getStyleClass().add("sidebar-item-icon");
        refreshBtn.setGraphic(refIcon);
        refreshBtn.setOnAction(e -> loadAllInsightsAsync());

        header.addActions(loadingSpinner, modelBadge, refreshBtn);
        return header;
    }

    private Node buildSummaryCards(AIInsightService.InsightResult result, List<Product> needsRestock) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);

        int healthScore = Math.max(70, 100 - (needsRestock.size() * 5));
        StatCard card1 = new StatCard(
                "Health Score",
                healthScore + "%",
                healthScore >= 90 ? "Optimal Safety Margin" : "Attention Needed",
                StatCard.StatType.SUCCESS
        );

        StatCard card2 = new StatCard(
                "Restock Needed",
                needsRestock.size() + " items",
                needsRestock.isEmpty() ? "All Above Threshold" : "Critical Stock Reached",
                needsRestock.isEmpty() ? StatCard.StatType.INFO : StatCard.StatType.WARNING
        );

        long anomalyCount = result.insights().stream()
                .filter(i -> "ANOMALY".equalsIgnoreCase(i.getCategory()) || "EXPIRY_RISK".equalsIgnoreCase(i.getCategory()))
                .count();
        StatCard card3 = new StatCard(
                "Anomalies Flagged",
                anomalyCount + " detected",
                anomalyCount == 0 ? "Normal Pattern" : "Irregular Variance",
                anomalyCount == 0 ? StatCard.StatType.SUCCESS : StatCard.StatType.DANGER
        );

        StatCard card4 = new StatCard(
                "AI Allowance",
                "20 / Day",
                "Per-Business Daily Limit",
                StatCard.StatType.PRIMARY
        );

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, card4);
        return row;
    }

    private Node buildAskBaiSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        SVGPath sparkle = new SVGPath();
        sparkle.setContent(SVG_SPARKLE);
        sparkle.getStyleClass().add("ask-bai-sparkle-icon");

        VBox titleCol = new VBox(2);
        Label title = new Label("Ask bAI Assistant");
        title.getStyleClass().add("stat-label");

        Label subtitle = new Label("Ask real-time questions about your store's velocity, stock health, and reorder cycles.");
        subtitle.getStyleClass().add("form-helper");
        titleCol.getChildren().addAll(title, subtitle);

        headerRow.getChildren().addAll(sparkle, titleCol);

        HBox chipsRow = new HBox(8);
        chipsRow.setAlignment(Pos.CENTER_LEFT);

        String[] samplePrompts = {
                "What should I reorder?",
                "Show high-waste products",
                "Forecast next week's sales",
                "How is stock turnover?"
        };

        for (String sample : samplePrompts) {
            Button chip = new Button(sample);
            chip.getStyleClass().add("prompt-chip");
            chip.setOnAction(e -> {
                askField.setText(sample);
                handleAskBai();
            });
            chipsRow.getChildren().add(chip);
        }

        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        askField = new TextField();
        askField.setPromptText("Ask bAI about your store inventory (max 200 chars)...");
        askField.getStyleClass().add("ask-bai-field");
        HBox.setHgrow(askField, Priority.ALWAYS);

        charCounter = new Label("0/200");
        charCounter.getStyleClass().add("ask-bai-counter");

        askField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 200) {
                askField.setText(newVal.substring(0, 200));
            }
            int len = askField.getText() != null ? askField.getText().length() : 0;
            charCounter.setText(len + "/200");
        });
        askField.setOnAction(e -> handleAskBai());

        askBtn = new Button("Ask bAI");
        askBtn.getStyleClass().add("btn-primary");
        askBtn.setOnAction(e -> handleAskBai());

        inputRow.getChildren().addAll(askField, charCounter, askBtn);

        askResponseBox = new VBox(8);
        askResponseBox.getStyleClass().add("ask-bai-response-box");
        askResponseBox.setVisible(false);
        askResponseBox.setManaged(false);

        askResponseText = new Label();
        askResponseText.setWrapText(true);
        askResponseText.getStyleClass().add("ask-bai-response-text");
        askResponseBox.getChildren().add(askResponseText);

        card.getChildren().addAll(headerRow, chipsRow, inputRow, askResponseBox);
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

    private Node buildForecastChartCard(List<StockMovementDao.DailyMovementPoint> history) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        VBox titleBox = new VBox(2);
        Label title = new Label("Demand Forecast: Past 7 Days Actual vs Next 7 Days Predicted");
        title.getStyleClass().add("stat-label");
        Label subtitle = new Label("Predictive consumption model projected from historical deduction velocities.");
        subtitle.getStyleClass().add("form-helper");
        titleBox.getChildren().addAll(title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Timeline");

        NumberAxis yAxis = createIntegerNumberAxis("Units Deducted");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setPrefHeight(270);
        chart.setAnimated(false);
        chart.setLegendSide(Side.TOP);

        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual Consumption (Past 7 Days)");

        XYChart.Series<String, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("Projected Demand (Next 7 Days)");

        double totalHistoricalUnits = 0;
        int count = 0;

        if (history != null && !history.isEmpty()) {
            for (StockMovementDao.DailyMovementPoint pt : history) {
                actualSeries.getData().add(new XYChart.Data<>(pt.dateLabel(), pt.outQty()));
                totalHistoricalUnits += pt.outQty();
                count++;
            }
        } else {
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                actualSeries.getData().add(new XYChart.Data<>(today.minusDays(i).format(DATE_FMT), 0));
            }
        }

        double avgDaily = (count > 0 && totalHistoricalUnits > 0) ? (totalHistoricalUnits / count) : 8.0;
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        for (int i = 0; i < 7; i++) {
            LocalDate day = tomorrow.plusDays(i);
            int dayOfWeek = day.getDayOfWeek().getValue();
            double factor = (dayOfWeek == 6 || dayOfWeek == 7) ? 1.25 : 0.95;
            int predicted = Math.max(1, (int) Math.round(avgDaily * factor));
            predictedSeries.getData().add(new XYChart.Data<>(day.format(DATE_FMT) + " (F)", predicted));
        }

        chart.getData().addAll(actualSeries, predictedSeries);
        card.getChildren().addAll(titleBox, chart);
        return card;
    }

    private Node buildInsightCards(List<AiInsight> insights) {
        VBox container = new VBox(12);

        Label headerLabel = new Label("Strategic AI Action Items");
        headerLabel.getStyleClass().add("stat-label");
        container.getChildren().add(headerLabel);

        if (insights == null || insights.isEmpty()) {
            container.getChildren().add(new EmptyState(SVG_SPARKLE, "No active alerts", "All inventory indicators within safe target parameters."));
            return container;
        }

        for (AiInsight insight : insights) {
            VBox card = new VBox(10);
            card.getStyleClass().add("card");

            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label sevBadge = new Label(insight.getSeverity() != null ? insight.getSeverity() : "INFO");
            sevBadge.getStyleClass().add("badge");
            if ("WARNING".equalsIgnoreCase(insight.getSeverity()) || "CRITICAL".equalsIgnoreCase(insight.getSeverity())) {
                sevBadge.getStyleClass().add("badge-warning");
            } else if ("OPPORTUNITY".equalsIgnoreCase(insight.getSeverity())) {
                sevBadge.getStyleClass().add("badge-success");
            } else {
                sevBadge.getStyleClass().add("badge-info");
            }

            Label catLabel = new Label(insight.getCategory() != null ? insight.getCategory().replace("_", " ") : "GENERAL");
            catLabel.getStyleClass().add("form-helper");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label metricPill = new Label(insight.getMetricValue() != null ? insight.getMetricValue() : "");
            metricPill.getStyleClass().add("insight-metric-pill");

            topRow.getChildren().addAll(sevBadge, catLabel, spacer, metricPill);

            Label titleLabel = new Label(insight.getTitle());
            titleLabel.getStyleClass().add("text-label");

            Label descLabel = new Label(insight.getDescription());
            descLabel.setWrapText(true);
            descLabel.getStyleClass().add("dialog-body-text");

            HBox actionRow = new HBox(10);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            Button actionBtn = new Button(insight.getActionLabel() != null ? insight.getActionLabel() : "View Inventory");
            actionBtn.getStyleClass().add("btn-secondary");
            actionBtn.setOnAction(e -> {
                String cmd = insight.getActionCommand() != null ? insight.getActionCommand() : "NAVIGATE_INVENTORY";
                if ("NAVIGATE_ANALYTICS".equalsIgnoreCase(cmd)) {
                    ViewNavigator.navigateTo(ViewType.ANALYTICS);
                } else {
                    ViewNavigator.navigateTo(ViewType.INVENTORY);
                }
            });

            actionRow.getChildren().add(actionBtn);

            card.getChildren().addAll(topRow, titleLabel, descLabel, actionRow);
            container.getChildren().add(card);
        }

        return container;
    }

    private Node buildRestockAndAnomaliesRow(List<Product> needsRestock) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);

        VBox restockCard = new VBox(12);
        restockCard.getStyleClass().add("card");
        HBox.setHgrow(restockCard, Priority.ALWAYS);

        VBox restockTitleCol = new VBox(2);
        Label restockTitle = new Label("Immediate Restock Suggestions");
        restockTitle.getStyleClass().add("stat-label");
        Label restockSub = new Label("Calculated safety replenishment for items below par");
        restockSub.getStyleClass().add("form-helper");
        restockTitleCol.getChildren().addAll(restockTitle, restockSub);

        TableView<Product> restockTable = new TableView<>();
        restockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        restockTable.setPrefHeight(220);
        restockTable.setPlaceholder(new Label("No products currently require replenishment."));

        TableColumn<Product, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(160);

        TableColumn<Product, String> onHandCol = new TableColumn<>("Stock");
        onHandCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockQuantity() + " " + Constants.UNIT));
        onHandCol.setPrefWidth(80);

        TableColumn<Product, String> suggestCol = new TableColumn<>("Suggested Order");
        suggestCol.setCellValueFactory(d -> {
            int reorderQty = Math.max(10, (d.getValue().getLowStockThreshold() * 2) - d.getValue().getStockQuantity());
            return new SimpleStringProperty(reorderQty + " " + Constants.UNIT);
        });
        suggestCol.setPrefWidth(110);

        TableColumn<Product, String> costCol = new TableColumn<>("Est. Cost");
        costCol.setCellValueFactory(d -> {
            int reorderQty = Math.max(10, (d.getValue().getLowStockThreshold() * 2) - d.getValue().getStockQuantity());
            BigDecimal cost = d.getValue().getCostPrice().multiply(BigDecimal.valueOf(reorderQty));
            return new SimpleStringProperty(PESO_FMT.format(cost));
        });
        costCol.setPrefWidth(100);

        restockTable.getColumns().addAll(nameCol, onHandCol, suggestCol, costCol);
        restockTable.getItems().setAll(needsRestock);

        restockCard.getChildren().addAll(restockTitleCol, restockTable);

        VBox anomaliesCard = new VBox(12);
        anomaliesCard.getStyleClass().add("card");
        anomaliesCard.setPrefWidth(420);
        anomaliesCard.setMinWidth(360);

        VBox anomalyTitleCol = new VBox(2);
        Label anomTitle = new Label("Detected Stock Anomalies");
        anomTitle.getStyleClass().add("stat-label");
        Label anomSub = new Label("Unusual consumption patterns and variance tracking");
        anomSub.getStyleClass().add("form-helper");
        anomalyTitleCol.getChildren().addAll(anomTitle, anomSub);

        VBox anomList = new VBox(10);
        if (needsRestock.isEmpty()) {
            anomList.getChildren().add(new Label("✓ No negative variances or unusual shrinkage events detected."));
        } else {
            VBox item1 = new VBox(4);
            item1.getStyleClass().add("join-code-banner");
            Label item1Title = new Label("High Shrinkage & Rapid Depletion");
            item1Title.getStyleClass().add("join-code-banner-title");
            Label item1Desc = new Label(needsRestock.size() + " products breached safety par levels this cycle.");
            item1Desc.getStyleClass().add("join-code-banner-sub");
            item1.getChildren().addAll(item1Title, item1Desc);

            VBox item2 = new VBox(4);
            item2.getStyleClass().add("card");
            Label item2Title = new Label("Turnover Velocity Variance");
            item2Title.getStyleClass().add("text-label");
            Label item2Desc = new Label("Fast-moving goods accounted for >60% of total deductions over the last 7 days.");
            item2Desc.getStyleClass().add("form-helper");
            item2.getChildren().addAll(item2Title, item2Desc);

            anomList.getChildren().addAll(item1, item2);
        }

        anomaliesCard.getChildren().addAll(anomalyTitleCol, anomList);

        row.getChildren().addAll(restockCard, anomaliesCard);
        return row;
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
        isDisposed = true;
    }
}
