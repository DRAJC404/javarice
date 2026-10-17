package com.bai.inventory.view.pages;

import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.dao.StockMovementDao;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.InventoryService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.*;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class InventoryView extends VBox implements Disposable {

    private static final String SVG_REFRESH   = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_HISTORY   = "M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z";
    private static final String SVG_BULK      = "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9h-4v4h-2v-4H9V9h4V5h2v4h4v2z";
    private static final String SVG_SEARCH    = "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
    private static final String SVG_BOX       = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z";
    private static final String SVG_ADD       = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_MINUS     = "M19 13H5v-2h14v2z";
    private static final String SVG_ADJUST    = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";

    private final ProductDao productDao;
    private final StockMovementDao movementDao;
    private final InventoryService inventoryService;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);

    private StatCard totalProductsCard;
    private StatCard inStockCard;
    private StatCard lowStockCard;
    private StatCard outOfStockCard;

    private TextField searchField;
    private CategorySelector categorySelector;
    private ComboBox<String> statusFilterCombo;

    private TableView<Product> productTable;
    private final List<Product> allLoadedProducts = new ArrayList<>();
    private final List<Product> filteredProducts = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 12;

    private Label paginationLabel;
    private Button prevPageBtn;
    private Button nextPageBtn;

    private final boolean isManagerOrOwner;
    private final User currentUser;

    public InventoryView() {
        this.productDao = new ProductDao();
        this.movementDao = new StockMovementDao();
        this.inventoryService = new InventoryService(movementDao);

        this.currentUser = AuthService.getCurrentUser();
        this.isManagerOrOwner = (currentUser != null && currentUser.getRole() != null && currentUser.getRole().isManagerOrAbove());

        getStyleClass().addAll("content-pane", "inventory-root");
        setSpacing(20);

        buildHeader();

        buildKpiRow();

        buildFilterBar();

        buildProductTable();

        buildPaginationBar();

        loadDataAsync();
    }

    private void buildHeader() {
        PageHeader header = new PageHeader(
                "Inventory Control",
                "Track stock counts, set par thresholds, execute fast restocks, and record multi-item deductions."
        );

        Button bulkDeductBtn = new Button("Bulk Deduct");
        bulkDeductBtn.getStyleClass().add("btn-secondary");
        HBox bulkGraphic = createIconLabel(SVG_BULK, "Bulk Deduct");
        bulkDeductBtn.setGraphic(bulkGraphic);
        bulkDeductBtn.setOnAction(e -> openBulkDeductDialog());

        Button historyBtn = new Button("Movement History");
        historyBtn.getStyleClass().add("btn-secondary");
        HBox histGraphic = createIconLabel(SVG_HISTORY, "Movement History");
        historyBtn.setGraphic(histGraphic);
        historyBtn.setOnAction(e -> openMovementHistoryDialog(null));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        HBox refGraphic = createIconLabel(SVG_REFRESH, "Refresh");
        refreshBtn.setGraphic(refGraphic);
        refreshBtn.setOnAction(e -> loadDataAsync());

        header.addActions(bulkDeductBtn, historyBtn, refreshBtn);
        getChildren().add(header);
    }

    private void buildKpiRow() {
        HBox kpiRow = new HBox(16);
        kpiRow.setAlignment(Pos.CENTER_LEFT);

        totalProductsCard = new StatCard("Total Products", "--", "Active in catalog", StatCard.StatType.PRIMARY);
        inStockCard       = new StatCard("In Stock", "--", "Healthy par level", StatCard.StatType.SUCCESS);
        lowStockCard      = new StatCard("Low Stock", "--", "Below par threshold", StatCard.StatType.WARNING);
        outOfStockCard    = new StatCard("Out of Stock", "--", "Immediate restock needed", StatCard.StatType.DANGER);

        kpiRow.getChildren().addAll(totalProductsCard, inStockCard, lowStockCard, outOfStockCard);
        getChildren().add(kpiRow);
    }

    private void buildFilterBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("inventory-filter-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search by product name, SKU, or barcode...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());

        categorySelector = new CategorySelector(CategorySelector.Mode.FILTER);
        categorySelector.setPrefWidth(220);
        categorySelector.selectedCategoryProperty().addListener((obs, oldV, newV) -> applyFilters());

        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.getStyleClass().add("combo-box");
        statusFilterCombo.setPrefWidth(170);
        statusFilterCombo.getItems().addAll("All Statuses", "In Stock", "Low Stock", "Out of Stock");
        statusFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(searchField, categorySelector, statusFilterCombo, spacer);
        getChildren().add(bar);
    }

    private void buildProductTable() {
        productTable = new TableView<>();
        productTable.getStyleClass().add("table-view");
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(productTable, Priority.ALWAYS);

        TableColumn<Product, Product> thumbCol = new TableColumn<>("");
        thumbCol.setPrefWidth(54);
        thumbCol.setMaxWidth(54);
        thumbCol.setResizable(false);
        thumbCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        thumbCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    StackPane box = new StackPane();
                    box.setPrefSize(38, 38);
                    box.setMinSize(38, 38);
                    box.getStyleClass().add("card");
                    box.setPadding(Insets.EMPTY);

                    Image img = AppExecutor.getCachedImage(item.getImagePath(), 40, 40);
                    if (img != null) {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(38);
                        iv.setFitHeight(38);
                        iv.setPreserveRatio(true);
                        box.getChildren().add(iv);
                    } else {
                        SVGPath icon = new SVGPath();
                        icon.setContent(SVG_BOX);
                        icon.getStyleClass().add("dialog-close-icon");
                        box.getChildren().add(icon);
                    }
                    setGraphic(box);
                }
            }
        });

        TableColumn<Product, Product> nameCol = new TableColumn<>("Product & SKU");
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox v = new VBox(2);
                    v.setAlignment(Pos.CENTER_LEFT);

                    Label nameLbl = new Label(item.getName());
                    nameLbl.getStyleClass().add("form-label");

                    String sub = "SKU: " + (item.getSku() != null ? item.getSku() : "N/A");
                    if (item.getBarcode() != null && !item.getBarcode().isBlank()) {
                        sub += " • Barcode: " + item.getBarcode();
                    }
                    Label skuLbl = new Label(sub);
                    skuLbl.getStyleClass().add("form-helper");

                    v.getChildren().addAll(nameLbl, skuLbl);
                    setGraphic(v);
                }
            }
        });

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setPrefWidth(120);
        catCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCategoryName() != null ? p.getValue().getCategoryName() : "General"));
        catCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setGraphic(null);
                } else {
                    setGraphic(Badge.neutral(cat));
                }
            }
        });

        TableColumn<Product, Product> stockCol = new TableColumn<>("Stock Level");
        stockCol.setPrefWidth(160);
        stockCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        stockCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                } else {
                    VBox v = new VBox(4);
                    v.setAlignment(Pos.CENTER_LEFT);

                    HBox countRow = new HBox(6);
                    countRow.setAlignment(Pos.CENTER_LEFT);
                    Label qtyLbl = new Label(p.getStockQuantity() + " " + p.getUnit());
                    qtyLbl.getStyleClass().add("form-label");

                    Label parLbl = new Label("(Min: " + p.getLowStockThreshold() + ")");
                    parLbl.getStyleClass().add("form-helper");
                    countRow.getChildren().addAll(qtyLbl, parLbl);

                    double maxRatioTarget = Math.max(1, p.getLowStockThreshold() * 2);
                    double ratio = Math.min(1.0, (double) p.getStockQuantity() / maxRatioTarget);

                    StackPane track = new StackPane();
                    track.getStyleClass().add("stock-progress-track");
                    track.setAlignment(Pos.CENTER_LEFT);

                    Region bar = new Region();
                    bar.getStyleClass().add("stock-progress-bar");
                    bar.setPrefWidth(100 * ratio);

                    if (p.getStockQuantity() == 0) {
                        bar.getStyleClass().add("stock-progress-out");
                        bar.setPrefWidth(4);
                    } else if (p.getStockQuantity() <= p.getLowStockThreshold()) {
                        bar.getStyleClass().add("stock-progress-low");
                    } else {
                        bar.getStyleClass().add("stock-progress-good");
                    }

                    track.getChildren().add(bar);
                    v.getChildren().addAll(countRow, track);
                    setGraphic(v);
                }
            }
        });

        TableColumn<Product, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(110);
        statusCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) {
                    setGraphic(null);
                } else {
                    Badge b = switch (st.toUpperCase()) {
                        case "IN_STOCK" -> Badge.success("IN STOCK");
                        case "LOW_STOCK" -> Badge.warning("LOW STOCK");
                        case "OUT_OF_STOCK" -> Badge.danger("OUT OF STOCK");
                        default -> Badge.neutral(st.replace("_", " "));
                    };
                    setGraphic(b);
                }
            }
        });

        TableColumn<Product, Product> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(250);
        actionCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                } else {
                    HBox actions = new HBox(6);
                    actions.getStyleClass().add("inventory-table-actions");

                    Button addBtn = new Button("+ Add");
                    addBtn.getStyleClass().addAll("btn-table-action", "btn-table-add");
                    addBtn.setVisible(isManagerOrOwner);
                    addBtn.setManaged(isManagerOrOwner);
                    addBtn.setOnAction(e -> openAddStockDialog(p));

                    Button deductBtn = new Button("- Deduct");
                    deductBtn.getStyleClass().addAll("btn-table-action", "btn-table-deduct");
                    deductBtn.setDisable(p.getStockQuantity() <= 0);
                    deductBtn.setOnAction(e -> openDeductDialog(p));

                    Button adjustBtn = new Button("Adjust");
                    adjustBtn.getStyleClass().addAll("btn-table-action", "btn-table-adjust");
                    adjustBtn.setVisible(isManagerOrOwner);
                    adjustBtn.setManaged(isManagerOrOwner);
                    adjustBtn.setOnAction(e -> openAdjustDialog(p));

                    Button histBtn = new Button("History");
                    histBtn.getStyleClass().addAll("btn-table-action", "btn-table-history");
                    histBtn.setOnAction(e -> openMovementHistoryDialog(p));

                    actions.getChildren().addAll(addBtn, deductBtn, adjustBtn, histBtn);
                    setGraphic(actions);
                }
            }
        });

        productTable.getColumns().addAll(List.of(thumbCol, nameCol, catCol, stockCol, statusCol, actionCol));

        productTable.setPlaceholder(new EmptyState(
                SVG_BOX,
                "No Products Match Filters",
                "Adjust your search keywords or stock status filters to display items."
        ));

        getChildren().add(productTable);
    }

    private void buildPaginationBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("pagination-bar");
        bar.setAlignment(Pos.CENTER_RIGHT);

        paginationLabel = new Label("Showing 0 of 0 products");
        paginationLabel.getStyleClass().add("pagination-label");

        prevPageBtn = new Button("Previous");
        prevPageBtn.getStyleClass().add("pagination-btn");
        prevPageBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderTablePage();
            }
        });

        nextPageBtn = new Button("Next");
        nextPageBtn.getStyleClass().add("pagination-btn");
        nextPageBtn.setOnAction(e -> {
            int maxPages = Math.max(1, (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE));
            if (currentPage < maxPages) {
                currentPage++;
                renderTablePage();
            }
        });

        bar.getChildren().addAll(paginationLabel, prevPageBtn, nextPageBtn);
        getChildren().add(bar);
    }

    private void loadDataAsync() {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;

        AppExecutor.execute(() -> {
            if (isDisposed.get()) return;

            ProductDao.ProductSummaryMetrics metrics = productDao.getSummaryMetrics(orgId);

            List<Category> categories = productDao.findAllCategories(orgId);

            List<Product> products = productDao.findAll("", null, orgId, false);

            Platform.runLater(() -> {
                if (isDisposed.get()) return;

                totalProductsCard.setValue(String.valueOf(metrics.totalProducts()));
                inStockCard.setValue(String.valueOf(metrics.inStockCount()));
                lowStockCard.setValue(String.valueOf(metrics.lowStockCount()));
                outOfStockCard.setValue(String.valueOf(metrics.outOfStockCount()));

                if (categorySelector != null) {
                    categorySelector.refreshCategories(null);
                }

                allLoadedProducts.clear();
                allLoadedProducts.addAll(products);

                applyFilters();
            });
        });
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        Integer catId = (categorySelector != null) ? categorySelector.getSelectedCategoryId() : null;
        String statusFilter = statusFilterCombo.getValue();

        filteredProducts.clear();

        for (Product p : allLoadedProducts) {
            if (catId != null && p.getCategoryId() != catId) {
                continue;
            }

            if (statusFilter != null && !statusFilter.equals("All Statuses")) {
                if ("In Stock".equals(statusFilter) && !"IN_STOCK".equalsIgnoreCase(p.getStatus())) continue;
                if ("Low Stock".equals(statusFilter) && !"LOW_STOCK".equalsIgnoreCase(p.getStatus())) continue;
                if ("Out of Stock".equals(statusFilter) && !"OUT_OF_STOCK".equalsIgnoreCase(p.getStatus())) continue;
            }

            if (!query.isEmpty()) {
                boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(query);
                boolean matchSku = p.getSku() != null && p.getSku().toLowerCase().contains(query);
                boolean matchBarcode = p.getBarcode() != null && p.getBarcode().toLowerCase().contains(query);
                if (!matchName && !matchSku && !matchBarcode) {
                    continue;
                }
            }

            filteredProducts.add(p);
        }

        currentPage = 1;
        renderTablePage();
    }

    private void renderTablePage() {
        int total = filteredProducts.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > maxPages) currentPage = maxPages;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, total);

        List<Product> pageItems = (total > 0 && fromIndex < total)
                ? filteredProducts.subList(fromIndex, toIndex)
                : List.of();

        productTable.setItems(FXCollections.observableArrayList(pageItems));

        paginationLabel.setText(total > 0
                ? "Showing " + (fromIndex + 1) + " - " + toIndex + " of " + total + " products (Page " + currentPage + " of " + maxPages + ")"
                : "No products found");
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= maxPages);
    }

    private void openAddStockDialog(Product product) {
        if (!isManagerOrOwner) {
            ViewNavigator.showToast("Access Denied: Business Owner or Manager role required to add stock.", true);
            return;
        }

        AppDialog dialog = new AppDialog("Add Stock - " + product.getName(), 460);

        Label currentStockNotice = new Label("Current stock: " + product.getStockQuantity() + " " + product.getUnit() +
                                            " (Par threshold: " + product.getLowStockThreshold() + ")");
        currentStockNotice.getStyleClass().add("form-helper");

        TextField qtyField = new TextField();
        qtyField.setPromptText("Enter quantity to add (e.g. 24)");
        FormField qtyFormField = new FormField("Restock Quantity", qtyField, true, "Must be a positive integer.");

        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll("RESTOCK", "OPENING_STOCK", "OTHER");
        reasonCombo.getSelectionModel().select("RESTOCK");
        FormField reasonFormField = new FormField("Movement Reason", reasonCombo, true);

        TextField notesField = new TextField();
        notesField.setPromptText("Optional supplier invoice, batch no, or shipment note");
        FormField notesFormField = new FormField("Notes", notesField, false);

        dialog.setBody(currentStockNotice, qtyFormField, reasonFormField, notesFormField);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        Button submitBtn = new Button("Add Stock");
        submitBtn.getStyleClass().add("btn-dialog-confirm");
        submitBtn.setOnAction(e -> {
            String qtyStr = qtyField.getText().trim();
            if (qtyStr.isEmpty()) {
                qtyFormField.setError("Quantity is required.");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
                if (qty <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                qtyFormField.setError("Enter a valid integer greater than zero.");
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("Adding...");

            AppExecutor.execute(() -> {
                try {
                    StockMovement sm = inventoryService.addStock(
                            product.getId(),
                            qty,
                            reasonCombo.getValue(),
                            notesField.getText().trim(),
                            currentUser
                    );
                    Platform.runLater(() -> {
                        ViewNavigator.hideOverlayDialog();
                        ViewNavigator.showToast("Successfully added " + qty + " units to " + product.getName() + " (Ref: " + sm.getReferenceNo() + ")", false);
                        loadDataAsync();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        submitBtn.setText("Add Stock");
                        qtyFormField.setError(ex.getMessage());
                    });
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, submitBtn);
        dialog.show();
    }

    private void openDeductDialog(Product product) {
        AppDialog dialog = new AppDialog("Deduct Stock - " + product.getName(), 460);

        Label currentStockNotice = new Label("Current available stock: " + product.getStockQuantity() + " " + product.getUnit());
        currentStockNotice.getStyleClass().add("form-helper");

        TextField qtyField = new TextField();
        qtyField.setPromptText("Enter quantity to deduct (max " + product.getStockQuantity() + ")");
        FormField qtyFormField = new FormField("Deduction Quantity", qtyField, true, "Cannot exceed current available stock.");

        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll("SOLD", "USED", "DAMAGED", "EXPIRED");
        reasonCombo.getSelectionModel().select("SOLD");
        FormField reasonFormField = new FormField("Reason", reasonCombo, true);

        dialog.setBody(currentStockNotice, qtyFormField, reasonFormField);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        Button submitBtn = new Button("Deduct Stock");
        submitBtn.getStyleClass().add("btn-dialog-confirm");
        submitBtn.setOnAction(e -> {
            String qtyStr = qtyField.getText().trim();
            if (qtyStr.isEmpty()) {
                qtyFormField.setError("Quantity is required.");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
                if (qty <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                qtyFormField.setError("Enter a valid integer greater than zero.");
                return;
            }

            if (qty > product.getStockQuantity()) {
                qtyFormField.setError("Cannot deduct " + qty + ". Available stock is only " + product.getStockQuantity() + ".");
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("Deducting...");

            AppExecutor.execute(() -> {
                try {
                    StockMovement sm = inventoryService.deductStock(
                            product.getId(),
                            qty,
                            reasonCombo.getValue(),
                            currentUser
                    );
                    Platform.runLater(() -> {
                        ViewNavigator.hideOverlayDialog();
                        ViewNavigator.showUndoToast(
                                "Deducted " + qty + " units of " + product.getName() + " (" + sm.getReason() + ")",
                                () -> handleQuickUndo(sm)
                        );
                        loadDataAsync();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        submitBtn.setText("Deduct Stock");
                        qtyFormField.setError(ex.getMessage());
                    });
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, submitBtn);
        dialog.show();
    }

    private void openAdjustDialog(Product product) {
        if (!isManagerOrOwner) {
            ViewNavigator.showToast("Access Denied: Business Owner or Manager role required to adjust stock.", true);
            return;
        }

        AppDialog dialog = new AppDialog("Audit Stock Adjustment - " + product.getName(), 480);

        Label notice = new Label("System count: " + product.getStockQuantity() + " " + product.getUnit());
        notice.getStyleClass().add("form-helper");

        TextField physicalCountField = new TextField(String.valueOf(product.getStockQuantity()));
        FormField physicalFormField = new FormField("Actual Physical Count", physicalCountField, true, "Enter the exact physical count verified during audit.");

        Label diffLabel = new Label("Difference: 0 units (No variance)");
        diffLabel.getStyleClass().add("form-label");

        physicalCountField.textProperty().addListener((obs, oldV, newV) -> {
            try {
                int physical = Integer.parseInt(newV.trim());
                int diff = physical - product.getStockQuantity();
                if (diff == 0) {
                    diffLabel.setText("Difference: 0 units (No variance)");
                } else if (diff > 0) {
                    diffLabel.setText("Variance: +" + diff + " units (Surplus discovered)");
                } else {
                    diffLabel.setText("Variance: " + diff + " units (Shrinkage / discrepancy)");
                }
            } catch (Exception ignored) {
                diffLabel.setText("Variance: --");
            }
        });

        TextField notesField = new TextField();
        notesField.setPromptText("e.g. End-of-week physical count audit discrepancy");
        FormField notesFormField = new FormField("Audit Notes", notesField, false);

        dialog.setBody(notice, physicalFormField, diffLabel, notesFormField);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        Button submitBtn = new Button("Apply Adjustment");
        submitBtn.getStyleClass().add("btn-dialog-confirm");
        submitBtn.setOnAction(e -> {
            String countStr = physicalCountField.getText().trim();
            int physical;
            try {
                physical = Integer.parseInt(countStr);
                if (physical < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                physicalFormField.setError("Physical count must be a non-negative integer.");
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("Applying...");

            AppExecutor.execute(() -> {
                try {
                    StockMovement sm = inventoryService.adjustStock(
                            product.getId(),
                            physical,
                            notesField.getText().trim(),
                            currentUser
                    );
                    Platform.runLater(() -> {
                        ViewNavigator.hideOverlayDialog();
                        ViewNavigator.showToast("Stock adjusted to " + physical + " units (Ref: " + sm.getReferenceNo() + ")", false);
                        loadDataAsync();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        submitBtn.setText("Apply Adjustment");
                        physicalFormField.setError(ex.getMessage());
                    });
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, submitBtn);
        dialog.show();
    }

    private void openBulkDeductDialog() {
        AppDialog dialog = new AppDialog("Bulk Stock Deduction", 640);

        Label info = new Label("Add items to deduct in a single atomic transaction. All rows will be locked and validated.");
        info.getStyleClass().add("form-helper");

        VBox linesContainer = new VBox(10);
        List<BulkRow> rows = new ArrayList<>();

        Runnable addLine = () -> {
            HBox line = new HBox(8);
            line.setAlignment(Pos.CENTER_LEFT);

            ComboBox<Product> prodCombo = new ComboBox<>();
            prodCombo.getStyleClass().add("combo-box");
            prodCombo.setPrefWidth(220);
            prodCombo.getItems().addAll(allLoadedProducts.stream().filter(p -> p.getStockQuantity() > 0).toList());
            prodCombo.setPromptText("Select product...");

            TextField qtyField = new TextField("1");
            qtyField.getStyleClass().add("text-field");
            qtyField.setPrefWidth(70);

            ComboBox<String> reasonCombo = new ComboBox<>();
            reasonCombo.getStyleClass().add("combo-box");
            reasonCombo.setPrefWidth(120);
            reasonCombo.getItems().addAll("SOLD", "USED", "DAMAGED", "EXPIRED");
            reasonCombo.getSelectionModel().selectFirst();

            Button removeBtn = new Button("✕");
            removeBtn.getStyleClass().add("btn-table-deduct");

            BulkRow rowObj = new BulkRow(line, prodCombo, qtyField, reasonCombo);
            rows.add(rowObj);

            removeBtn.setOnAction(ev -> {
                linesContainer.getChildren().remove(line);
                rows.remove(rowObj);
            });

            line.getChildren().addAll(prodCombo, qtyField, reasonCombo, removeBtn);
            linesContainer.getChildren().add(line);
        };

        addLine.run();
        addLine.run();

        Button addLineBtn = new Button("+ Add Line");
        addLineBtn.getStyleClass().add("btn-secondary");
        addLineBtn.setOnAction(e -> addLine.run());

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setVisible(false);

        dialog.setBody(info, linesContainer, addLineBtn, errorLabel);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        Button submitBtn = new Button("Deduct All");
        submitBtn.getStyleClass().add("btn-dialog-confirm");
        submitBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            List<InventoryService.BulkDeductionItem> items = new ArrayList<>();

            for (BulkRow r : rows) {
                Product p = r.prodCombo.getValue();
                if (p == null) {
                    errorLabel.setText("Please select a product for all lines, or remove empty lines.");
                    errorLabel.setVisible(true);
                    return;
                }
                int q;
                try {
                    q = Integer.parseInt(r.qtyField.getText().trim());
                    if (q <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errorLabel.setText("Quantity must be a positive integer.");
                    errorLabel.setVisible(true);
                    return;
                }
                if (q > p.getStockQuantity()) {
                    errorLabel.setText("Cannot deduct " + q + " of '" + p.getName() + "'. Available: " + p.getStockQuantity());
                    errorLabel.setVisible(true);
                    return;
                }
                items.add(new InventoryService.BulkDeductionItem(p.getId(), q, r.reasonCombo.getValue(), "Bulk deduction"));
            }

            if (items.isEmpty()) {
                errorLabel.setText("At least one deduction item is required.");
                errorLabel.setVisible(true);
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("Deducting...");

            AppExecutor.execute(() -> {
                try {
                    List<StockMovement> saved = inventoryService.bulkDeduct(items, currentUser);
                    Platform.runLater(() -> {
                        ViewNavigator.hideOverlayDialog();
                        ViewNavigator.showToast("Successfully deducted " + saved.size() + " items in batch.", false);
                        loadDataAsync();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        submitBtn.setText("Deduct All");
                        errorLabel.setText(ex.getMessage());
                        errorLabel.setVisible(true);
                    });
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, submitBtn);
        dialog.show();
    }

    private record BulkRow(HBox rowNode, ComboBox<Product> prodCombo, TextField qtyField, ComboBox<String> reasonCombo) {}
   
    private void openMovementHistoryDialog(Product filterProduct) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        String title = filterProduct != null ? "Stock History: " + filterProduct.getName() : "Global Stock Movement History";

        AppDialog dialog = new AppDialog(title, 820);

        HBox histFilters = new HBox(12);
        histFilters.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("ALL", "IN", "OUT", "ADJUSTMENT");
        typeFilter.getSelectionModel().selectFirst();
        typeFilter.getStyleClass().add("combo-box");
        typeFilter.setPrefWidth(140);

        TextField searchMovements = new TextField();
        searchMovements.setPromptText("Search reference no, user, notes...");
        searchMovements.getStyleClass().add("search-field");
        searchMovements.setPrefWidth(260);

        histFilters.getChildren().addAll(new Label("Type:"), typeFilter, searchMovements);

        TableView<StockMovement> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPrefHeight(340);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<StockMovement, String> dateCol = new TableColumn<>("Date / Time");
        dateCol.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getCreatedAt() != null ? m.getValue().getCreatedAt().toString().replace("T", " ") : ""));
        dateCol.setPrefWidth(140);

        TableColumn<StockMovement, String> refCol = new TableColumn<>("Reference");
        refCol.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getReferenceNo()));
        refCol.setPrefWidth(130);

        TableColumn<StockMovement, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getProductName() != null ? m.getValue().getProductName() : "Product #" + m.getValue().getProductId()));
        prodCol.setPrefWidth(150);

        TableColumn<StockMovement, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getMovementType()));
        typeCol.setPrefWidth(80);
        typeCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) setGraphic(null);
                else {
                    Badge b = switch (t.toUpperCase()) {
                        case "IN" -> Badge.success("IN");
                        case "OUT" -> Badge.danger("OUT");
                        case "ADJUSTMENT" -> Badge.info("ADJ");
                        default -> Badge.neutral(t);
                    };
                    setGraphic(b);
                }
            }
        });

        TableColumn<StockMovement, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getReason()));
        reasonCol.setPrefWidth(110);

        TableColumn<StockMovement, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(m -> new SimpleStringProperty(String.valueOf(m.getValue().getQuantity())));
        qtyCol.setPrefWidth(60);

        TableColumn<StockMovement, StockMovement> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        actionCol.setPrefWidth(100);
        actionCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(StockMovement sm, boolean empty) {
                super.updateItem(sm, empty);
                if (empty || sm == null) {
                    setGraphic(null);
                } else {
                    if ("OUT".equalsIgnoreCase(sm.getMovementType())) {
                        boolean isUndone = sm.isUndone();
                        Button undoBtn = new Button(isUndone ? "Undone" : "Undo");
                        undoBtn.getStyleClass().add("btn-table-action");
                        undoBtn.getStyleClass().add(isUndone ? "btn-table-history" : "btn-table-deduct");
                        undoBtn.setDisable(isUndone);

                        undoBtn.setOnAction(e -> {
                            undoBtn.setDisable(true);
                            undoBtn.setText("Undoing...");
                            AppExecutor.execute(() -> {
                                try {
                                    StockMovement res = inventoryService.undoDeduction(sm.getId(), currentUser);
                                    Platform.runLater(() -> {
                                        sm.setUndoneByMovementId(res.getId());
                                        undoBtn.setText("Undone");
                                        ViewNavigator.showToast("Deduction undone successfully (Restored " + sm.getQuantity() + " units)", false);
                                        loadDataAsync();
                                    });
                                } catch (Exception ex) {
                                    Platform.runLater(() -> {
                                        undoBtn.setDisable(false);
                                        undoBtn.setText("Undo");
                                        ViewNavigator.showToast("Cannot undo: " + ex.getMessage(), true);
                                    });
                                }
                            });
                        });
                        setGraphic(undoBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        table.getColumns().addAll(List.of(dateCol, refCol, prodCol, typeCol, reasonCol, qtyCol, actionCol));

        Runnable fetchMovements = () -> {
            String selectedType = typeFilter.getValue();
            String q = searchMovements.getText();
            Integer pId = filterProduct != null ? filterProduct.getId() : null;

            AppExecutor.execute(() -> {
                List<StockMovement> list = movementDao.findMovements(1, 100, pId, selectedType, q, orgId);
                Platform.runLater(() -> table.setItems(FXCollections.observableArrayList(list)));
            });
        };

        typeFilter.valueProperty().addListener((obs, oldV, newV) -> fetchMovements.run());
        searchMovements.textProperty().addListener((obs, oldV, newV) -> fetchMovements.run());

        fetchMovements.run();

        dialog.setBody(histFilters, table);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-dialog-confirm");
        closeBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        dialog.setFooterButtons(closeBtn);
        dialog.show();
    }

    private void handleQuickUndo(StockMovement movement) {
        if (movement == null) return;
        AppExecutor.execute(() -> {
            try {
                StockMovement undoRes = inventoryService.undoDeduction(movement.getId(), currentUser);
                Platform.runLater(() -> {
                    movement.setUndoneByMovementId(undoRes.getId());
                    ViewNavigator.showToast("Restored " + movement.getQuantity() + " units (Undo Ref: " + undoRes.getReferenceNo() + ")", false);
                    loadDataAsync();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> ViewNavigator.showToast("Undo failed: " + ex.getMessage(), true));
            }
        });
    }

    private HBox createIconLabel(String svgPath, String text) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("dialog-close-icon");
        Label lbl = new Label(text);
        box.getChildren().addAll(icon, lbl);
        return box;
    }

    @Override
    public void dispose() {
        isDisposed.set(true);
    }
}
