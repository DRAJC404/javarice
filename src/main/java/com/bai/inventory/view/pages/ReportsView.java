package com.bai.inventory.view.pages;

import com.bai.inventory.model.Product;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.ReportService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.EmptyState;
import com.bai.inventory.view.component.PageHeader;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
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
    private static final String SVG_PRINT = "M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final NumberFormat PESO_FMT = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    private enum ReportTab {
        MOVEMENTS("Stock Movement"),
        INVENTORY("Inventory Valuation"),
        PRODUCTS("Product Catalog"),
        LOW_STOCK("Low Stock Alerts");

        final String title;
        ReportTab(String title) { this.title = title; }
    }

    private final ReportService reportService;
    private ReportTab activeTab = ReportTab.MOVEMENTS;

    private ComboBox<String> dateRangeCombo;
    private Button exportCsvBtn;
    private Button exportPdfBtn;
    private Button printBtn;
    private ProgressIndicator loadingIndicator;

    private StackPane tableContainer;
    private TableView<StockMovement> movementsTable;
    private TableView<Product> inventoryTable;
    private TableView<Product> productsTable;
    private TableView<Product> lowStockTable;

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
        tableContainer.getChildren().setAll(movementsTable);

        HBox footerBar = buildFooterBar();

        rootBox.getChildren().addAll(header, toolbar, tableContainer, footerBar);
        setCenter(rootBox);
    }

    private Node buildHeader() {
        PageHeader header = new PageHeader(
                "Inventory Reports & Export",
                "Generate, analyze, and export comprehensive stock movement, valuation, and threshold summaries."
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

        printBtn = new Button("Print");
        printBtn.getStyleClass().add("btn-secondary");
        SVGPath printIcon = new SVGPath();
        printIcon.setContent(SVG_PRINT);
        printIcon.getStyleClass().add("sidebar-item-icon");
        printBtn.setGraphic(printIcon);
        printBtn.setOnAction(e -> handlePrint());

        header.addActions(loadingIndicator, exportCsvBtn, exportPdfBtn, printBtn);
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
        rangeLabel.getStyleClass().add("text-label");

        dateRangeCombo = new ComboBox<>();
        dateRangeCombo.getItems().addAll("Today", "Last 7 Days", "Last 30 Days", "Last 90 Days", "All Time");
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
        dateRangeCombo.setDisable(activeTab != ReportTab.MOVEMENTS);
        switch (activeTab) {
            case MOVEMENTS -> tableContainer.getChildren().setAll(movementsTable);
            case INVENTORY -> tableContainer.getChildren().setAll(inventoryTable);
            case PRODUCTS -> tableContainer.getChildren().setAll(productsTable);
            case LOW_STOCK -> tableContainer.getChildren().setAll(lowStockTable);
        }
        loadActiveReport();
    }

    private void buildTables() {
        buildMovementsTable();
        buildInventoryTable();
        buildProductsTable();
        buildLowStockTable();
    }

    private void buildMovementsTable() {
        movementsTable = new TableView<>();
        movementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        movementsTable.setPlaceholder(new EmptyState(SVG_PDF, "No stock movements recorded", "Movement records will appear here as stock deductions and additions occur."));

        TableColumn<StockMovement, String> refCol = new TableColumn<>("Reference");
        refCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReferenceNo()));
        refCol.setPrefWidth(120);

        TableColumn<StockMovement, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(TIME_FMT) : "-"
        ));
        dateCol.setPrefWidth(130);

        TableColumn<StockMovement, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));
        prodCol.setPrefWidth(160);

        TableColumn<StockMovement, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductSku()));
        skuCol.setPrefWidth(100);

        TableColumn<StockMovement, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMovementType()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("IN".equals(item)) badge.getStyleClass().add("badge-success");
                    else if ("OUT".equals(item)) badge.getStyleClass().add("badge-danger");
                    else badge.getStyleClass().add("badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        typeCol.setPrefWidth(80);

        TableColumn<StockMovement, String> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantity())));
        qtyCol.setPrefWidth(80);

        TableColumn<StockMovement, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason()));
        reasonCol.setPrefWidth(110);

        TableColumn<StockMovement, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPreviousStock() + " → " + d.getValue().getNewStock()));
        balanceCol.setPrefWidth(100);

        TableColumn<StockMovement, String> userCol = new TableColumn<>("Handled By");
        userCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName() != null ? d.getValue().getUserName() : "-"));
        userCol.setPrefWidth(120);

        movementsTable.getColumns().addAll(refCol, dateCol, prodCol, skuCol, typeCol, qtyCol, reasonCol, balanceCol, userCol);
    }

    private void buildInventoryTable() {
        inventoryTable = new TableView<>();
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        inventoryTable.setPlaceholder(new EmptyState(SVG_PDF, "No inventory items", "Add products to view current valuation."));

        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSku()));
        skuCol.setPrefWidth(110);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName() != null ? d.getValue().getCategoryName() : "-"));
        catCol.setPrefWidth(130);

        TableColumn<Product, String> stockCol = new TableColumn<>("On Hand");
        stockCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockQuantity() + " " + d.getValue().getUnit()));
        stockCol.setPrefWidth(90);

        TableColumn<Product, String> sellingCol = new TableColumn<>("Price");
        sellingCol.setCellValueFactory(d -> new SimpleStringProperty(PESO_FMT.format(d.getValue().getSellingPrice())));
        sellingCol.setPrefWidth(100);

        TableColumn<Product, String> valCol = new TableColumn<>("Total Value");
        valCol.setCellValueFactory(d -> {
            BigDecimal val = d.getValue().getSellingPrice().multiply(BigDecimal.valueOf(d.getValue().getStockQuantity()));
            return new SimpleStringProperty(PESO_FMT.format(val));
        });
        valCol.setPrefWidth(120);

        TableColumn<Product, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> createStatusCell());
        statusCol.setPrefWidth(100);

        inventoryTable.getColumns().addAll(skuCol, nameCol, catCol, stockCol, sellingCol, valCol, statusCol);
    }

    private void buildProductsTable() {
        productsTable = new TableView<>();
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        productsTable.setPlaceholder(new EmptyState(SVG_PDF, "No products registered", "Create your first product in the Product catalog."));

        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSku()));
        skuCol.setPrefWidth(110);

        TableColumn<Product, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBarcode() != null ? d.getValue().getBarcode() : "-"));
        barcodeCol.setPrefWidth(120);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName() != null ? d.getValue().getCategoryName() : "-"));
        catCol.setPrefWidth(120);

        TableColumn<Product, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnit()));
        unitCol.setPrefWidth(70);

        TableColumn<Product, String> costCol = new TableColumn<>("Cost Price");
        costCol.setCellValueFactory(d -> new SimpleStringProperty(PESO_FMT.format(d.getValue().getCostPrice())));
        costCol.setPrefWidth(100);

        TableColumn<Product, String> sellCol = new TableColumn<>("Selling Price");
        sellCol.setCellValueFactory(d -> new SimpleStringProperty(PESO_FMT.format(d.getValue().getSellingPrice())));
        sellCol.setPrefWidth(100);

        TableColumn<Product, String> threshCol = new TableColumn<>("Low Threshold");
        threshCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getLowStockThreshold())));
        threshCol.setPrefWidth(90);

        TableColumn<Product, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> createStatusCell());
        statusCol.setPrefWidth(100);

        productsTable.getColumns().addAll(skuCol, barcodeCol, nameCol, catCol, unitCol, costCol, sellCol, threshCol, statusCol);
    }

    private void buildLowStockTable() {
        lowStockTable = new TableView<>();
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        lowStockTable.setPlaceholder(new EmptyState(SVG_PDF, "Healthy stock levels", "No products are currently at or below their low-stock threshold."));

        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSku()));
        skuCol.setPrefWidth(110);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName() != null ? d.getValue().getCategoryName() : "-"));
        catCol.setPrefWidth(120);

        TableColumn<Product, String> currentStockCol = new TableColumn<>("Current Stock");
        currentStockCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockQuantity() + " " + d.getValue().getUnit()));
        currentStockCol.setPrefWidth(100);

        TableColumn<Product, String> threshCol = new TableColumn<>("Threshold");
        threshCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getLowStockThreshold())));
        threshCol.setPrefWidth(90);

        TableColumn<Product, String> deficitCol = new TableColumn<>("Units Below Par");
        deficitCol.setCellValueFactory(d -> {
            int deficit = Math.max(0, d.getValue().getLowStockThreshold() - d.getValue().getStockQuantity());
            return new SimpleStringProperty(deficit + " " + d.getValue().getUnit());
        });
        deficitCol.setPrefWidth(110);

        TableColumn<Product, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> createStatusCell());
        statusCol.setPrefWidth(110);

        lowStockTable.getColumns().addAll(skuCol, nameCol, catCol, currentStockCol, threshCol, deficitCol, statusCol);
    }

    private TableCell<Product, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status.replace("_", " "));
                    badge.getStyleClass().add("badge");
                    switch (status) {
                        case "IN_STOCK" -> badge.getStyleClass().add("badge-success");
                        case "LOW_STOCK" -> badge.getStyleClass().add("badge-warning");
                        case "OUT_OF_STOCK" -> badge.getStyleClass().add("badge-danger");
                        default -> badge.getStyleClass().add("badge-neutral");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        };
    }

    private HBox buildFooterBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("card");
        bar.setPadding(new Insets(12, 20, 12, 20));

        summaryMetaLabel = new Label("Loading report statistics...");
        summaryMetaLabel.getStyleClass().add("form-helper");

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
                    case MOVEMENTS -> {
                        List<StockMovement> list = reportService.getMovements(currentUser, dates[0], dates[1]);
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            movementsTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Total Movements: " + list.size() + " records in selected period");
                        });
                    }
                    case INVENTORY -> {
                        List<Product> list = reportService.getInventory(currentUser);
                        BigDecimal totalVal = list.stream()
                                .map(p -> p.getSellingPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        int totalUnits = list.stream().mapToInt(Product::getStockQuantity).sum();
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            inventoryTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Total Products: " + list.size() + " | Units on Hand: " + totalUnits + " | Total Inventory Value: " + PESO_FMT.format(totalVal));
                        });
                    }
                    case PRODUCTS -> {
                        List<Product> list = reportService.getProducts(currentUser);
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            productsTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Active Product Catalog: " + list.size() + " items");
                        });
                    }
                    case LOW_STOCK -> {
                        List<Product> list = reportService.getLowStock(currentUser);
                        Platform.runLater(() -> {
                            if (isDisposed) return;
                            loadingIndicator.setVisible(false);
                            lowStockTable.getItems().setAll(list);
                            summaryMetaLabel.setText("Low Stock / Critical Items: " + list.size() + " products need reordering");
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
            case "Last 90 Days" -> new LocalDateTime[]{today.minusDays(90).atStartOfDay(), today.atTime(23, 59, 59)};
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

    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(getScene().getWindow())) {
            Node printTarget = switch (activeTab) {
                case MOVEMENTS -> movementsTable;
                case INVENTORY -> inventoryTable;
                case PRODUCTS -> productsTable;
                case LOW_STOCK -> lowStockTable;
            };
            boolean success = job.printPage(printTarget);
            if (success) {
                job.endJob();
                ViewNavigator.showToast("Report sent to printer successfully.", false);
            }
        } else {
            ViewNavigator.showToast("Print cancelled or no printer available.", true);
        }
    }

    private List<String> getActiveTableHeaders() {
        return switch (activeTab) {
            case MOVEMENTS -> List.of("Reference", "Date", "Product", "SKU", "Type", "Quantity", "Reason", "Balance", "Handled By");
            case INVENTORY -> List.of("SKU", "Product Name", "Category", "On Hand", "Price", "Total Value", "Status");
            case PRODUCTS -> List.of("SKU", "Barcode", "Product Name", "Category", "Unit", "Cost Price", "Selling Price", "Low Threshold", "Status");
            case LOW_STOCK -> List.of("SKU", "Product Name", "Category", "Current Stock", "Threshold", "Units Below Par", "Status");
        };
    }

    private List<List<String>> getActiveTableRows() {
        List<List<String>> rows = new ArrayList<>();
        switch (activeTab) {
            case MOVEMENTS -> {
                for (StockMovement m : movementsTable.getItems()) {
                    rows.add(List.of(
                            m.getReferenceNo() != null ? m.getReferenceNo() : "",
                            m.getCreatedAt() != null ? m.getCreatedAt().format(TIME_FMT) : "",
                            m.getProductName() != null ? m.getProductName() : "",
                            m.getProductSku() != null ? m.getProductSku() : "",
                            m.getMovementType() != null ? m.getMovementType() : "",
                            String.valueOf(m.getQuantity()),
                            m.getReason() != null ? m.getReason() : "",
                            m.getPreviousStock() + " -> " + m.getNewStock(),
                            m.getUserName() != null ? m.getUserName() : ""
                    ));
                }
            }
            case INVENTORY -> {
                for (Product p : inventoryTable.getItems()) {
                    BigDecimal val = p.getSellingPrice().multiply(BigDecimal.valueOf(p.getStockQuantity()));
                    rows.add(List.of(
                            p.getSku() != null ? p.getSku() : "",
                            p.getName() != null ? p.getName() : "",
                            p.getCategoryName() != null ? p.getCategoryName() : "",
                            p.getStockQuantity() + " " + p.getUnit(),
                            PESO_FMT.format(p.getSellingPrice()),
                            PESO_FMT.format(val),
                            p.getStatus() != null ? p.getStatus() : ""
                    ));
                }
            }
            case PRODUCTS -> {
                for (Product p : productsTable.getItems()) {
                    rows.add(List.of(
                            p.getSku() != null ? p.getSku() : "",
                            p.getBarcode() != null ? p.getBarcode() : "",
                            p.getName() != null ? p.getName() : "",
                            p.getCategoryName() != null ? p.getCategoryName() : "",
                            p.getUnit() != null ? p.getUnit() : "",
                            PESO_FMT.format(p.getCostPrice()),
                            PESO_FMT.format(p.getSellingPrice()),
                            String.valueOf(p.getLowStockThreshold()),
                            p.getStatus() != null ? p.getStatus() : ""
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
                            p.getStockQuantity() + " " + p.getUnit(),
                            String.valueOf(p.getLowStockThreshold()),
                            deficit + " " + p.getUnit(),
                            p.getStatus() != null ? p.getStatus() : ""
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
