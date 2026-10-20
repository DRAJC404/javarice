package com.bai.inventory.view.pages;

import com.bai.inventory.model.Product;
import com.bai.inventory.model.ProductPerformance;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.ReportService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.util.Constants;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.EmptyState;
import com.bai.inventory.view.component.PageHeader;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsView extends BorderPane implements Disposable {

    private static final String SVG_PDF   = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z";
    private static final String SVG_CSV   = "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final NumberFormat PESO_FMT = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    public enum ReportTab {
        INVENTORY("Inventory Summary"),
        MOVEMENTS("Stock Movement"),
        LOW_STOCK("Low Stock"),
        PERFORMANCE("Product Performance"),
        PROFIT("Profit Summary");

        public final String title;
        ReportTab(String title) { this.title = title; }
    }

    private final ReportService reportService;
    private ReportTab activeTab = ReportTab.INVENTORY;

    private ComboBox<String> dateRangeCombo;
    private Button exportCsvBtn;
    private Button exportPdfBtn;
    private ProgressIndicator loadingIndicator;

    private StackPane tableContainer;
    private TableView<Product> inventoryTable;
    private TableView<StockMovement> movementsTable;
    private TableView<Product> lowStockTable;
    private TableView<ProductPerformance> performanceTable;
    private TableView<ProductPerformance> profitTable;

    private Label summaryMetaLabel;
    private volatile boolean isDisposed = false;

    public ReportsView() {
        this.reportService = new ReportService();
        buildUI();
        loadActiveReport();
    }

    private void buildUI() {
        getStyleClass().add("content-pane");

        VBox rootBox = new VBox(20);
        rootBox.setPadding(new Insets(28));
        rootBox.setFillWidth(true);

        Node header = buildHeader();
        Node toolbar = buildToolbar();

        tableContainer = new StackPane();
        tableContainer.getStyleClass().add("card");
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        buildTables();
        tableContainer.getChildren().setAll(inventoryTable);

        HBox footerBar = buildFooterBar();

        rootBox.getChildren().addAll(header, toolbar, tableContainer, footerBar);
        setCenter(rootBox);
    }

    private Node buildHeader() {
        PageHeader header = new PageHeader(
                "Management Reports & Analytics",
                "Generate, analyze, and export comprehensive stock, performance, and profit valuation reports."
        );

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(22, 22);
        loadingIndicator.setVisible(false);

        exportCsvBtn = new Button("Export CSV");
        exportCsvBtn.getStyleClass().add("btn-secondary");
        SVGPath csvIcon = new SVGPath();
        csvIcon.setContent(SVG_CSV);
        csvIcon.getStyleClass().add("sidebar-item-icon");
        exportCsvBtn.setGraphic(csvIcon);
        exportCsvBtn.setOnAction(e -> handleExportCsv());

        exportPdfBtn = new Button("Export PDF");
        exportPdfBtn.getStyleClass().add("btn-primary");
        SVGPath pdfIcon = new SVGPath();
        pdfIcon.setContent(SVG_PDF);
        pdfIcon.getStyleClass().add("sidebar-item-icon");
        exportPdfBtn.setGraphic(pdfIcon);
        exportPdfBtn.setOnAction(e -> handleExportPdf());

        header.addActions(loadingIndicator, exportCsvBtn, exportPdfBtn);
        return header;
    }

    private Node buildToolbar() {
        BorderPane toolbar = new BorderPane();
        toolbar.getStyleClass().add("action-bar");

        HBox tabBox = new HBox(8);
        tabBox.setAlignment(Pos.CENTER_LEFT);

        for (ReportTab tab : ReportTab.values()) {
            Button tabBtn = new Button(tab.title);
            tabBtn.getStyleClass().add(tab == activeTab ? "btn-primary" : "btn-secondary");
            tabBtn.setOnAction(e -> {
                activeTab = tab;
                updateTabButtons(tabBox);
                switchTab();
            });
            tabBox.getChildren().add(tabBtn);
        }

        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_RIGHT);

        Label rangeLabel = new Label("Date Period:");
        rangeLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -text-primary;");

        dateRangeCombo = new ComboBox<>();
        dateRangeCombo.getItems().addAll("Today", "Last 7 Days", "Last 30 Days", "This Month", "All Time");
        dateRangeCombo.setValue("Last 30 Days");
        dateRangeCombo.getStyleClass().add("filter-combo");
        dateRangeCombo.setOnAction(e -> loadActiveReport());

        filterBox.getChildren().addAll(rangeLabel, dateRangeCombo);

        toolbar.setLeft(tabBox);
        toolbar.setRight(filterBox);
        return toolbar;
    }

    private void updateTabButtons(HBox tabBox) {
        int i = 0;
        for (Node node : tabBox.getChildren()) {
            if (node instanceof Button btn) {
                ReportTab tab = ReportTab.values()[i++];
                btn.getStyleClass().removeAll("btn-primary", "btn-secondary");
                btn.getStyleClass().add(tab == activeTab ? "btn-primary" : "btn-secondary");
            }
        }
    }

    private void switchTab() {
        switch (activeTab) {
            case INVENTORY -> tableContainer.getChildren().setAll(inventoryTable);
            case MOVEMENTS -> tableContainer.getChildren().setAll(movementsTable);
            case LOW_STOCK -> tableContainer.getChildren().setAll(lowStockTable);
            case PERFORMANCE -> tableContainer.getChildren().setAll(performanceTable);
            case PROFIT -> tableContainer.getChildren().setAll(profitTable);
        }
        loadActiveReport();
    }

    private void buildTables() {
        buildInventoryTable();
        buildMovementsTable();
        buildLowStockTable();
        buildPerformanceTable();
        buildProfitTable();
    }

    private void buildInventoryTable() {
        inventoryTable = new TableView<>();
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        inventoryTable.setPlaceholder(new EmptyState(SVG_PDF, "No inventory items found", "Your catalog currently has no products recorded."));

        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSku()));

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName() != null ? d.getValue().getCategoryName() : "—"));

        TableColumn<Product, String> qtyCol = new TableColumn<>("On Hand");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockQuantity() + " " + Constants.UNIT));

        TableColumn<Product, String> costCol = new TableColumn<>("Unit Cost");
        costCol.setCellValueFactory(d -> new SimpleStringProperty(PESO_FMT.format(d.getValue().getCostPrice())));

        TableColumn<Product, String> priceCol = new TableColumn<>("Selling Price");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(PESO_FMT.format(d.getValue().getSellingPrice())));

        TableColumn<Product, String> totalValCol = new TableColumn<>("Total Valuation");
        totalValCol.setCellValueFactory(d -> {
            BigDecimal val = d.getValue().getSellingPrice().multiply(BigDecimal.valueOf(d.getValue().getStockQuantity()));
            return new SimpleStringProperty(PESO_FMT.format(val));
        });

        TableColumn<Product, String> profitUnitCol = new TableColumn<>("Unit Profit");
        profitUnitCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedProfit()));

        inventoryTable.getColumns().addAll(List.of(skuCol, nameCol, catCol, qtyCol, costCol, priceCol, totalValCol, profitUnitCol));
    }

    private void buildMovementsTable() {
        movementsTable = new TableView<>();
        movementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        movementsTable.setPlaceholder(new EmptyState(SVG_PDF, "No stock movements recorded", "Movement records will appear here as stock deductions and additions occur."));

        TableColumn<StockMovement, String> refCol = new TableColumn<>("Reference");
        refCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReferenceNo()));

        TableColumn<StockMovement, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(TIME_FMT) : "-"
        ));

        TableColumn<StockMovement, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));

        TableColumn<StockMovement, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMovementType()));

        TableColumn<StockMovement, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getQuantity() + " " + Constants.UNIT));

        TableColumn<StockMovement, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason() != null ? d.getValue().getReason() : "—"));

        TableColumn<StockMovement, String> balCol = new TableColumn<>("Balance");
        balCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPreviousStock() + " ➔ " + d.getValue().getNewStock()));

        TableColumn<StockMovement, String> userCol = new TableColumn<>("Handled By");
        userCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName() != null ? d.getValue().getUserName() : "System"));

        movementsTable.getColumns().addAll(List.of(refCol, dateCol, prodCol, typeCol, qtyCol, reasonCol, balCol, userCol));
    }

    private void buildLowStockTable() {
        lowStockTable = new TableView<>();
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        lowStockTable.setPlaceholder(new EmptyState(SVG_PDF, "Inventory Levels Healthy", "No products are currently under minimum par threshold levels."));

        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSku()));

        TableColumn<Product, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName() != null ? d.getValue().getCategoryName() : "—"));

        TableColumn<Product, String> currentStockCol = new TableColumn<>("Current Stock");
        currentStockCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockQuantity() + " " + Constants.UNIT));

        TableColumn<Product, String> threshCol = new TableColumn<>("Threshold");
        threshCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLowStockThreshold() + " " + Constants.UNIT));

        TableColumn<Product, String> deficitCol = new TableColumn<>("Deficit");
        deficitCol.setCellValueFactory(d -> {
            int deficit = Math.max(0, d.getValue().getLowStockThreshold() - d.getValue().getStockQuantity());
            return new SimpleStringProperty(deficit + " " + Constants.UNIT);
        });

        TableColumn<Product, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        lowStockTable.getColumns().addAll(List.of(skuCol, nameCol, catCol, currentStockCol, threshCol, deficitCol, statusCol));
    }

    private void buildPerformanceTable() {
        performanceTable = new TableView<>();
        performanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        performanceTable.setPlaceholder(new EmptyState(SVG_PDF, "No sales data found", "No sales or stock transactions recorded for this period."));

        TableColumn<ProductPerformance, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));

        TableColumn<ProductPerformance, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName() != null ? d.getValue().getCategoryName() : "—"));

        TableColumn<ProductPerformance, String> soldCol = new TableColumn<>("Units Sold");
        soldCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnitsSold() + " " + Constants.UNIT));

        TableColumn<ProductPerformance, String> stockCol = new TableColumn<>("Stock Left");
        stockCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockLeft() + " " + Constants.UNIT));

        TableColumn<ProductPerformance, String> turnCol = new TableColumn<>("Turnover Rate");
        turnCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTurnover()));

        TableColumn<ProductPerformance, String> revCol = new TableColumn<>("Revenue");
        revCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedRevenue()));

        TableColumn<ProductPerformance, String> profitCol = new TableColumn<>("Profit");
        profitCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedProfit()));

        TableColumn<ProductPerformance, String> tagCol = new TableColumn<>("Evaluation");
        tagCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatusTag()));

        performanceTable.getColumns().addAll(List.of(nameCol, catCol, soldCol, stockCol, turnCol, revCol, profitCol, tagCol));
    }

    private void buildProfitTable() {
        profitTable = new TableView<>();
        profitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        profitTable.setPlaceholder(new EmptyState(SVG_PDF, "No profit data found", "No product sales recorded in the selected date range."));

        TableColumn<ProductPerformance, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));

        TableColumn<ProductPerformance, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName() != null ? d.getValue().getCategoryName() : "—"));

        TableColumn<ProductPerformance, String> soldCol = new TableColumn<>("Units Sold");
        soldCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnitsSold() + " " + Constants.UNIT));

        TableColumn<ProductPerformance, String> sellCol = new TableColumn<>("Selling Price");
        sellCol.setCellValueFactory(d -> new SimpleStringProperty(PESO_FMT.format(d.getValue().getSellingPrice())));

        TableColumn<ProductPerformance, String> costCol = new TableColumn<>("Unit Cost");
        costCol.setCellValueFactory(d -> new SimpleStringProperty(PESO_FMT.format(d.getValue().getCostPrice())));

        TableColumn<ProductPerformance, String> unitProfitCol = new TableColumn<>("Unit Profit");
        unitProfitCol.setCellValueFactory(d -> {
            BigDecimal up = d.getValue().getSellingPrice().subtract(d.getValue().getCostPrice());
            return new SimpleStringProperty(PESO_FMT.format(up));
        });

        TableColumn<ProductPerformance, String> revCol = new TableColumn<>("Total Revenue");
        revCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedRevenue()));

        TableColumn<ProductPerformance, String> profitCol = new TableColumn<>("Total Profit");
        profitCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedProfit()));

        TableColumn<ProductPerformance, String> marginCol = new TableColumn<>("Margin %");
        marginCol.setCellValueFactory(d -> {
            if (d.getValue().getRevenue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = d.getValue().getProfit()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(d.getValue().getRevenue(), 1, RoundingMode.HALF_UP);
                return new SimpleStringProperty(margin + "%");
            }
            return new SimpleStringProperty("0.0%");
        });

        profitTable.getColumns().addAll(List.of(nameCol, catCol, soldCol, sellCol, costCol, unitProfitCol, revCol, profitCol, marginCol));
    }

    private HBox buildFooterBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("card");
        bar.setPadding(new Insets(12, 20, 12, 20));

        summaryMetaLabel = new Label("Loading report statistics...");
        summaryMetaLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: -text-primary; -fx-font-size: 13px;");

        bar.getChildren().add(summaryMetaLabel);
        return bar;
    }

    private void loadActiveReport() {
        if (isDisposed) return;
        loadingIndicator.setVisible(true);

        User currentUser = AuthService.getCurrentUser();
        LocalDateTime[] dates = resolveDateRange();

        AppExecutor.execute(() -> {
            try {
                switch (activeTab) {
                    case INVENTORY -> {
                        List<Product> list = reportService.getInventory(currentUser);
                        BigDecimal totalVal = list.stream()
                                .map(p -> p.getSellingPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalCost = list.stream()
                                .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        int totalUnits = list.stream().mapToInt(Product::getStockQuantity).sum();
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            inventoryTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Total Products: " + list.size() + " | Units on Hand: " + totalUnits +
                                    " " + Constants.UNIT + " | Cost Basis: " + PESO_FMT.format(totalCost) +
                                    " | Total Valuation: " + PESO_FMT.format(totalVal) +
                                    " | Potential Profit: " + PESO_FMT.format(totalVal.subtract(totalCost)));
                        });
                    }
                    case MOVEMENTS -> {
                        List<StockMovement> list = reportService.getMovements(currentUser, dates[0], dates[1]);
                        int totalQty = list.stream().mapToInt(StockMovement::getQuantity).sum();
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            movementsTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Total Transactions: " + list.size() + " | Units Moved: " + totalQty + " " + Constants.UNIT);
                        });
                    }
                    case LOW_STOCK -> {
                        List<Product> list = reportService.getLowStock(currentUser);
                        int totalDeficit = list.stream().mapToInt(p -> Math.max(0, p.getLowStockThreshold() - p.getStockQuantity())).sum();
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            lowStockTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Low Stock Items: " + list.size() + " products | Total Deficit: " + totalDeficit + " " + Constants.UNIT);
                        });
                    }
                    case PERFORMANCE -> {
                        List<ProductPerformance> list = reportService.getProductPerformance(currentUser, dates[0], dates[1]);
                        int totalSold = list.stream().mapToInt(ProductPerformance::getUnitsSold).sum();
                        BigDecimal totalRev = list.stream().map(ProductPerformance::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalProf = list.stream().map(ProductPerformance::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            performanceTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Units Sold: " + totalSold + " " + Constants.UNIT +
                                    " | Total Revenue: " + PESO_FMT.format(totalRev) +
                                    " | Total Profit: " + PESO_FMT.format(totalProf));
                        });
                    }
                    case PROFIT -> {
                        List<ProductPerformance> list = reportService.getProductPerformance(currentUser, dates[0], dates[1]);
                        int totalSold = list.stream().mapToInt(ProductPerformance::getUnitsSold).sum();
                        BigDecimal totalRev = list.stream().map(ProductPerformance::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalProf = list.stream().map(ProductPerformance::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
                        String margin = totalRev.compareTo(BigDecimal.ZERO) > 0 ?
                                totalProf.multiply(BigDecimal.valueOf(100)).divide(totalRev, 1, RoundingMode.HALF_UP) + "%" : "0.0%";
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            profitTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Products Sold: " + list.size() + " | Units: " + totalSold +
                                    " " + Constants.UNIT + " | Total Revenue: " + PESO_FMT.format(totalRev) +
                                    " | Total Profit: " + PESO_FMT.format(totalProf) + " | Overall Margin: " + margin);
                        });
                    }
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (isDisposed) return;
                    loadingIndicator.setVisible(false);
                    ViewNavigator.showToast("Failed to load report: " + ex.getMessage(), true);
                });
            }
        });
    }

    private LocalDateTime[] resolveDateRange() {
        String val = dateRangeCombo != null && dateRangeCombo.getValue() != null ? dateRangeCombo.getValue() : "Last 30 Days";
        LocalDate today = LocalDate.now();
        return switch (val) {
            case "Today" -> new LocalDateTime[]{today.atStartOfDay(), today.atTime(23, 59, 59)};
            case "Last 7 Days" -> new LocalDateTime[]{today.minusDays(7).atStartOfDay(), today.atTime(23, 59, 59)};
            case "Last 30 Days" -> new LocalDateTime[]{today.minusDays(30).atStartOfDay(), today.atTime(23, 59, 59)};
            case "This Month" -> new LocalDateTime[]{today.withDayOfMonth(1).atStartOfDay(), today.atTime(23, 59, 59)};
            default -> new LocalDateTime[]{null, null};
        };
    }

    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + activeTab.title + " to CSV");
        chooser.setInitialFileName("bai_" + activeTab.name().toLowerCase() + "_report.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        List<String> headers = getActiveTableHeaders();
        List<List<String>> rows = getActiveTableRows();

        AppExecutor.execute(() -> {
            try {
                reportService.exportToCsv(file, headers, rows);
                Platform.runLater(() -> ViewNavigator.showToast("Report exported successfully to " + file.getName(), false));
            } catch (Exception ex) {
                Platform.runLater(() -> ViewNavigator.showToast("Failed to export CSV: " + ex.getMessage(), true));
            }
        });
    }

    private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + activeTab.title + " to PDF");
        chooser.setInitialFileName("bai_" + activeTab.name().toLowerCase() + "_report.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        List<String> headers = getActiveTableHeaders();
        List<List<String>> rows = getActiveTableRows();
        String subtitle = "Period: " + (dateRangeCombo != null ? dateRangeCombo.getValue() : "All");

        AppExecutor.execute(() -> {
            try {
                reportService.exportToPdf(file, activeTab.title, subtitle, headers, rows);
                Platform.runLater(() -> ViewNavigator.showToast("PDF report saved successfully to " + file.getName(), false));
            } catch (Exception ex) {
                Platform.runLater(() -> ViewNavigator.showToast("Failed to export PDF: " + ex.getMessage(), true));
            }
        });
    }

    private List<String> getActiveTableHeaders() {
        return switch (activeTab) {
            case INVENTORY -> List.of("SKU", "Product Name", "Category", "On Hand", "Unit Cost", "Selling Price", "Total Valuation", "Unit Profit");
            case MOVEMENTS -> List.of("Reference", "Date", "Product", "Type", "Quantity", "Reason", "Balance", "Handled By");
            case LOW_STOCK -> List.of("SKU", "Product Name", "Category", "Current Stock", "Threshold", "Deficit", "Status");
            case PERFORMANCE -> List.of("Product Name", "Category", "Units Sold", "Stock Left", "Turnover Rate", "Revenue", "Profit", "Evaluation");
            case PROFIT -> List.of("Product Name", "Category", "Units Sold", "Selling Price", "Unit Cost", "Unit Profit", "Total Revenue", "Total Profit", "Margin %");
        };
    }

    private List<List<String>> getActiveTableRows() {
        List<List<String>> rows = new ArrayList<>();
        switch (activeTab) {
            case INVENTORY -> {
                for (Product p : inventoryTable.getItems()) {
                    BigDecimal val = p.getSellingPrice().multiply(BigDecimal.valueOf(p.getStockQuantity()));
                    rows.add(List.of(
                            p.getSku() != null ? p.getSku() : "",
                            p.getName() != null ? p.getName() : "",
                            p.getCategoryName() != null ? p.getCategoryName() : "",
                            p.getStockQuantity() + " " + Constants.UNIT,
                            PESO_FMT.format(p.getCostPrice()),
                            PESO_FMT.format(p.getSellingPrice()),
                            PESO_FMT.format(val),
                            p.getFormattedProfit()
                    ));
                }
            }
            case MOVEMENTS -> {
                for (StockMovement m : movementsTable.getItems()) {
                    rows.add(List.of(
                            m.getReferenceNo() != null ? m.getReferenceNo() : "",
                            m.getCreatedAt() != null ? m.getCreatedAt().format(TIME_FMT) : "",
                            m.getProductName() != null ? m.getProductName() : "",
                            m.getMovementType() != null ? m.getMovementType() : "",
                            m.getQuantity() + " " + Constants.UNIT,
                            m.getReason() != null ? m.getReason() : "",
                            m.getPreviousStock() + " -> " + m.getNewStock(),
                            m.getUserName() != null ? m.getUserName() : ""
                    ));
                }
            }
            case LOW_STOCK -> {
                for (Product p : lowStockTable.getItems()) {
                    int deficit = Math.max(0, p.getLowStockThreshold() - p.getStockQuantity());
                    rows.add(List.of(
                            p.getSku() != null ? p.getSku() : "",
                            p.getName() != null ? p.getName() : "",
                            p.getCategoryName() != null ? p.getCategoryName() : "",
                            p.getStockQuantity() + " " + Constants.UNIT,
                            p.getLowStockThreshold() + " " + Constants.UNIT,
                            deficit + " " + Constants.UNIT,
                            p.getStatus() != null ? p.getStatus() : ""
                    ));
                }
            }
            case PERFORMANCE -> {
                for (ProductPerformance p : performanceTable.getItems()) {
                    rows.add(List.of(
                            p.getProductName(),
                            p.getCategoryName() != null ? p.getCategoryName() : "—",
                            p.getUnitsSold() + " " + Constants.UNIT,
                            p.getStockLeft() + " " + Constants.UNIT,
                            p.getFormattedTurnover(),
                            p.getFormattedRevenue(),
                            p.getFormattedProfit(),
                            p.getStatusTag()
                    ));
                }
            }
            case PROFIT -> {
                for (ProductPerformance p : profitTable.getItems()) {
                    BigDecimal up = p.getSellingPrice().subtract(p.getCostPrice());
                    String margin = p.getRevenue().compareTo(BigDecimal.ZERO) > 0 ?
                            p.getProfit().multiply(BigDecimal.valueOf(100)).divide(p.getRevenue(), 1, RoundingMode.HALF_UP) + "%" : "0.0%";
                    rows.add(List.of(
                            p.getProductName(),
                            p.getCategoryName() != null ? p.getCategoryName() : "—",
                            p.getUnitsSold() + " " + Constants.UNIT,
                            PESO_FMT.format(p.getSellingPrice()),
                            PESO_FMT.format(p.getCostPrice()),
                            PESO_FMT.format(up),
                            p.getFormattedRevenue(),
                            p.getFormattedProfit(),
                            margin
                    ));
                }
            }
        }
        return rows;
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }
}
