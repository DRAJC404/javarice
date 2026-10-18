package com.bai.inventory.view.pages;

import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.ProductService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.AppDialog;
import com.bai.inventory.view.component.CategorySelector;
import com.bai.inventory.view.component.FormField;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ProductCatalogView extends BorderPane implements Disposable {

    private final ProductService productService;
    private final ProductDao productDao;
    private final User currentUser;
    private final boolean isEmployee;

    private TextField searchField;
    private CategorySelector categoryFilterSelector;
    private ComboBox<String> statusFilterBox;
    private CheckBox showArchivedCheck;
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));
    private final AtomicLong searchRequestId = new AtomicLong(0);

    private Label totalProductsVal;
    private Label inStockVal;
    private Label lowStockVal;
    private Label outOfStockVal;
    private Label totalValuationVal;

    private TableView<Product> productTable;
    private List<Product> cachedProducts = new ArrayList<>();

    private static final String SVG_BOX = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z";
    private static final String SVG_SEARCH = "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
    private static final String SVG_PLUS = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_REFRESH = "M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";

    public ProductCatalogView() {
        this.productService = new ProductService();
        this.productDao = new ProductDao();
        this.currentUser = AuthService.getCurrentUser();
        this.isEmployee = (currentUser != null && currentUser.getRole() == Role.EMPLOYEE);

        buildUI();
        loadInitialData();
    }

    private void buildUI() {
        getStyleClass().add("catalog-root");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 32, 32, 32));
        VBox.setVgrow(content, Priority.ALWAYS);

        Node header = buildHeader();

        Node kpiCards = buildKpiCards();

        Node filterBar = buildFilterBar();

        Node tableNode = buildProductTable();
        VBox.setVgrow(tableNode, Priority.ALWAYS);

        content.getChildren().addAll(header, kpiCards, filterBar, tableNode);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");

        setCenter(scrollPane);

        searchDebounce.setOnFinished(e -> reloadProductsAsync());
    }

    private Node buildHeader() {
        BorderPane header = new BorderPane();
        header.setPadding(new Insets(0, 0, 4, 0));

        VBox titleBox = new VBox(4);
        Label title = new Label("Product Catalog");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        Label subtitle = new Label("Manage store items, Philippine Peso (₱) pricing, barcodes, stock levels, and par thresholds");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-muted;");

        titleBox.getChildren().addAll(title, subtitle);
        header.setLeft(titleBox);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button refreshBtn = new Button();
        refreshBtn.getStyleClass().addAll("btn-dialog-cancel");
        SVGPath refIcon = new SVGPath();
        refIcon.setContent(SVG_REFRESH);
        refIcon.setStyle("-fx-fill: -text-muted;");
        refreshBtn.setGraphic(refIcon);
        refreshBtn.setTooltip(new Tooltip("Refresh catalog"));
        refreshBtn.setOnAction(e -> loadInitialData());

        actions.getChildren().add(refreshBtn);
        header.setRight(actions);
        return header;
    }

    private Node buildKpiCards() {
        HBox kpiContainer = new HBox(16);
        kpiContainer.setAlignment(Pos.CENTER_LEFT);

        totalProductsVal = new Label("0");
        inStockVal = new Label("0");
        lowStockVal = new Label("0");
        outOfStockVal = new Label("0");
        totalValuationVal = new Label("₱0.00");

        kpiContainer.getChildren().addAll(
                createKpiCard("TOTAL PRODUCTS", totalProductsVal, "#3B82F6"),
                createKpiCard("IN STOCK", inStockVal, "#10B981"),
                createKpiCard("LOW STOCK", lowStockVal, "#F59E0B"),
                createKpiCard("OUT OF STOCK", outOfStockVal, "#EF4444"),
                createKpiCard("TOTAL VALUATION (₱)", totalValuationVal, "#6366F1")
        );

        return kpiContainer;
    }

    private Node createKpiCard(String title, Label valLabel, String accentColor) {
        VBox card = new VBox(6);
        card.getStyleClass().add("catalog-kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("catalog-kpi-title");

        valLabel.getStyleClass().add("catalog-kpi-val");
        valLabel.setStyle("-fx-text-fill: " + accentColor + ";");

        card.getChildren().addAll(titleLabel, valLabel);
        return card;
    }

    private Node buildFilterBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 0, 4, 0));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: -border-subtle; -fx-border-radius: 8; -fx-padding: 6 12;");
        searchBox.setMinWidth(260);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent(SVG_SEARCH);
        searchIcon.setStyle("-fx-fill: -text-muted;");

        searchField = new TextField();
        searchField.setPromptText("Search name, SKU or barcode");
        searchField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2; -fx-font-size: 13px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            searchDebounce.playFromStart();
        });

        searchBox.getChildren().addAll(searchIcon, searchField);

        categoryFilterSelector = new CategorySelector(CategorySelector.Mode.FILTER);
        categoryFilterSelector.setMinWidth(180);
        categoryFilterSelector.setPrefWidth(200);
        categoryFilterSelector.selectedCategoryProperty().addListener((obs, oldV, newV) -> reloadProductsAsync());

        statusFilterBox = new ComboBox<>();
        statusFilterBox.getItems().addAll("All Statuses", "In Stock", "Low Stock", "Out of Stock", "Discontinued");
        statusFilterBox.getSelectionModel().selectFirst();
        statusFilterBox.getStyleClass().add("combo-box");
        statusFilterBox.setMinWidth(140);
        statusFilterBox.setPrefWidth(150);
        statusFilterBox.valueProperty().addListener((obs, oldV, newV) -> reloadProductsAsync());

        bar.getChildren().addAll(searchBox, categoryFilterSelector, statusFilterBox);

        if (!isEmployee) {
            Button addBtn = new Button("Add Product");
            addBtn.getStyleClass().add("btn-dialog-confirm");
            SVGPath plusIcon = new SVGPath();
            plusIcon.setContent(SVG_PLUS);
            plusIcon.setStyle("-fx-fill: white;");
            addBtn.setGraphic(plusIcon);
            addBtn.setOnAction(e -> openAddProductModal());
            bar.getChildren().add(addBtn);
        }

        return bar;
    }

    private Node buildProductTable() {
        productTable = new TableView<>();
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        productTable.setPlaceholder(new Label("No products found in catalog."));
        productTable.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: -border-subtle; -fx-border-radius: 10;");

        TableColumn<Product, Product> imgCol = new TableColumn<>("");
        imgCol.setPrefWidth(55);
        imgCol.setMaxWidth(55);
        imgCol.setMinWidth(55);
        imgCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        imgCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            private final StackPane placeholder = new StackPane();
            {
                iv.setFitWidth(36);
                iv.setFitHeight(36);
                iv.setPreserveRatio(true);
                placeholder.setPrefSize(36, 36);
                placeholder.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 6;");
                SVGPath boxIcon = new SVGPath();
                boxIcon.setContent(SVG_BOX);
                boxIcon.setStyle("-fx-fill: #94A3B8;");
                placeholder.getChildren().add(boxIcon);
            }
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else if (item.getImagePath() != null && !item.getImagePath().trim().isEmpty()) {
                    Image cached = AppExecutor.getCachedImage(item.getImagePath(), 40, 40);
                    if (cached != null) {
                        iv.setImage(cached);
                        setGraphic(iv);
                    } else {
                        setGraphic(placeholder);
                    }
                } else {
                    setGraphic(placeholder);
                }
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<Product, Product> skuCol = new TableColumn<>("SKU / Barcode");
        skuCol.setMinWidth(130);
        skuCol.setPrefWidth(140);
        skuCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        skuCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    Label sku = new Label(item.getSku());
                    sku.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");
                    String bc = item.getBarcode() != null && !item.getBarcode().isEmpty() ? item.getBarcode() : "—";
                    Label barcode = new Label(bc);
                    barcode.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                    box.getChildren().addAll(sku, barcode);
                    setGraphic(box);
                }
            }
        });

        TableColumn<Product, Product> nameCol = new TableColumn<>("Product");
        nameCol.setMinWidth(180);
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(3);
                    Label name = new Label(item.getName());
                    name.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");
                    name.setWrapText(true);
                    name.setMaxWidth(Double.MAX_VALUE);
                    String catName = item.getCategoryName() != null ? item.getCategoryName() : "General";
                    Label cat = new Label(catName);
                    cat.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 1 6; -fx-background-radius: 4;");
                    box.getChildren().addAll(name, cat);
                    setGraphic(box);
                }
            }
        });

        TableColumn<Product, Product> statusCol = new TableColumn<>("Status");
        statusCol.setMinWidth(120);
        statusCol.setPrefWidth(130);
        statusCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    if (item.isArchived()) {
                        badge.setText("ARCHIVED");
                        badge.getStyleClass().add("badge-status-discontinued");
                    } else if (item.isOutOfStock()) {
                        badge.setText("OUT OF STOCK");
                        badge.getStyleClass().add("badge-status-out-of-stock");
                    } else if (item.isLowStock()) {
                        badge.setText("LOW STOCK");
                        badge.getStyleClass().add("badge-status-low-stock");
                    } else {
                        badge.setText("IN STOCK");
                        badge.getStyleClass().add("badge-status-in-stock");
                    }
                    badge.setMinWidth(Label.USE_PREF_SIZE);
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<Product, Product> qtyCol = new TableColumn<>("Stock Level");
        qtyCol.setMinWidth(110);
        qtyCol.setPrefWidth(120);
        qtyCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    String unit = item.getUnit() != null ? item.getUnit() : "pcs";
                    Label qty = new Label(item.getStockQuantity() + " " + unit);
                    qty.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");
                    Label par = new Label("Par threshold: " + item.getLowStockThreshold());
                    par.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
                    box.getChildren().addAll(qty, par);
                    setGraphic(box);
                }
            }
        });

        TableColumn<Product, String> sellCol = new TableColumn<>("Selling Price");
        sellCol.setMinWidth(130);
        sellCol.setPrefWidth(140);
        sellCol.setCellValueFactory(data -> {
            BigDecimal price = data.getValue().getSellingPrice();
            return new SimpleStringProperty(formatPeso(price));
        });
        sellCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 700;");

        TableColumn<Product, String> costCol = new TableColumn<>("Cost Price");
        costCol.setMinWidth(110);
        costCol.setPrefWidth(120);
        costCol.setCellValueFactory(data -> {
            if (isEmployee) {
                return new SimpleStringProperty("—");
            }
            BigDecimal price = data.getValue().getCostPrice();
            return new SimpleStringProperty(formatPeso(price));
        });
        costCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-text-fill: -text-muted;");

        TableColumn<Product, Product> actionCol = new TableColumn<>("Actions");
        actionCol.setMinWidth(150);
        actionCol.setPrefWidth(160);
        actionCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button archiveBtn = new Button("Archive");
            private final HBox actionBox = new HBox(8, editBtn, archiveBtn);

            {
                editBtn.getStyleClass().add("btn-action-edit");
                editBtn.setMinWidth(Button.USE_PREF_SIZE);
                archiveBtn.getStyleClass().add("btn-action-archive");
                archiveBtn.setMinWidth(Button.USE_PREF_SIZE);
                actionBox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || isEmployee) {
                    setGraphic(null);
                } else {
                    if (item.isArchived()) {
                        archiveBtn.setText("Restore");
                        archiveBtn.getStyleClass().setAll("btn-action-restore");
                        archiveBtn.setOnAction(e -> handleRestoreProduct(item));
                    } else {
                        archiveBtn.setText("Archive");
                        archiveBtn.getStyleClass().setAll("btn-action-archive");
                        archiveBtn.setOnAction(e -> handleArchiveProduct(item));
                    }

                    editBtn.setOnAction(e -> openEditProductModal(item));
                    setGraphic(actionBox);
                }
            }
        });

        productTable.getColumns().addAll(
                imgCol, skuCol, nameCol, statusCol, qtyCol, sellCol, costCol, actionCol
        );

        return productTable;
    }

    private void loadInitialData() {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;

        AppExecutor.execute(() -> {
            try {
                ProductDao.ProductSummaryMetrics metrics = productDao.getSummaryMetrics(orgId);

                Platform.runLater(() -> {
                    if (categoryFilterSelector != null) {
                        categoryFilterSelector.refreshCategories(null);
                    }
                    updateKpiMetrics(metrics);
                });
            } catch (Exception ex) {
                System.err.println("[ProductCatalogView] Error loading initial metrics: " + ex.getMessage());
            }
        });

        reloadProductsAsync();
    }

    private void reloadProductsAsync() {
        long reqId = searchRequestId.incrementAndGet();
        String query = (searchField != null && searchField.getText() != null) ? searchField.getText().trim() : "";
        Integer catId = (categoryFilterSelector != null) ? categoryFilterSelector.getSelectedCategoryId() : null;
        String statusVal = (statusFilterBox != null && statusFilterBox.getValue() != null) ? statusFilterBox.getValue() : "All Statuses";
        boolean showArchived = "Discontinued".equalsIgnoreCase(statusVal);

        AppExecutor.execute(() -> {
            try {
                List<Product> products = productService.getProducts(query, catId, currentUser, showArchived);
                if ("In Stock".equalsIgnoreCase(statusVal)) {
                    products = products.stream().filter(Product::isInStock).toList();
                } else if ("Low Stock".equalsIgnoreCase(statusVal)) {
                    products = products.stream().filter(Product::isLowStock).toList();
                } else if ("Out of Stock".equalsIgnoreCase(statusVal)) {
                    products = products.stream().filter(Product::isOutOfStock).toList();
                }

                final List<Product> finalProducts = products;
                if (reqId == searchRequestId.get()) {
                    Platform.runLater(() -> {
                        cachedProducts = finalProducts;
                        productTable.getItems().setAll(finalProducts);
                    });
                }
            } catch (Exception ex) {
                System.err.println("[ProductCatalogView] Error fetching products: " + ex.getMessage());
            }
        });
    }

    private void updateKpiMetrics(ProductDao.ProductSummaryMetrics metrics) {
        if (metrics == null) return;
        totalProductsVal.setText(String.valueOf(metrics.totalProducts()));
        lowStockVal.setText(String.valueOf(metrics.lowStockCount()));
        outOfStockVal.setText(String.valueOf(metrics.outOfStockCount()));
        int inStock = Math.max(0, metrics.totalProducts() - metrics.lowStockCount() - metrics.outOfStockCount());
        inStockVal.setText(String.valueOf(inStock));
        totalValuationVal.setText(formatPeso(metrics.totalValuation()));
    }

    private void openAddProductModal() {
        AppDialog dialog = new AppDialog("Add New Product", 560);

        final SimpleObjectProperty<String> selectedImagePath = new SimpleObjectProperty<>(null);
        StackPane imgPreviewBox = new StackPane();
        imgPreviewBox.setPrefSize(70, 70);
        imgPreviewBox.setMinSize(70, 70);
        imgPreviewBox.setMaxSize(70, 70);
        imgPreviewBox.getStyleClass().add("item-thumb-box");

        SVGPath defaultBoxIcon = new SVGPath();
        defaultBoxIcon.setContent(SVG_BOX);
        defaultBoxIcon.getStyleClass().add("sidebar-item-icon");
        imgPreviewBox.getChildren().add(defaultBoxIcon);

        ImageView previewImgView = new ImageView();
        previewImgView.setFitWidth(66);
        previewImgView.setFitHeight(66);
        previewImgView.setPreserveRatio(true);

        VBox imgDetails = new VBox(6);
        imgDetails.setAlignment(Pos.CENTER_LEFT);

        Button uploadImgBtn = new Button("Upload image");
        uploadImgBtn.getStyleClass().add("btn-dialog-cancel");
        uploadImgBtn.setMinWidth(Button.USE_PREF_SIZE);

        Label imgHint = new Label("PNG/JPG/GIF, max 5 MB");
        imgHint.getStyleClass().add("form-helper");

        FormField imgField = new FormField("Product picture", new HBox(14, imgPreviewBox, imgDetails), true);

        uploadImgBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Product Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File file = chooser.showOpenDialog(ViewNavigator.getPrimaryStage());
            if (file != null) {
                if (file.length() > 5 * 1024 * 1024) {
                    imgField.setError("Image file size exceeds 5 MB limit.");
                    return;
                }
                if (!ProductService.isValidProductImage(file.getAbsolutePath())) {
                    imgField.setError("Selected file is not a valid decodable image.");
                    return;
                }
                imgField.clearError();
                selectedImagePath.set(file.getAbsolutePath());
                previewImgView.setImage(new Image(file.toURI().toString(), 70, 70, true, true));
                imgPreviewBox.getChildren().setAll(previewImgView);
                imgHint.setText(file.getName());
            }
        });

        imgDetails.getChildren().addAll(uploadImgBtn, imgHint);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Barako Coffee Beans 1kg");
        nameField.setMaxWidth(Double.MAX_VALUE);
        FormField nameFormField = new FormField("Product name", nameField, true);

        CategorySelector categorySelector = new CategorySelector(CategorySelector.Mode.FORM);
        categorySelector.setMaxWidth(Double.MAX_VALUE);
        FormField categoryFormField = new FormField("Category", categorySelector, true);

        ComboBox<String> unitBox = new ComboBox<>();
        unitBox.getItems().addAll("pcs", "bag", "box", "keg", "tin", "carton", "sleeve", "pack", "kg", "g", "L");
        unitBox.getSelectionModel().select("pcs");
        unitBox.setMaxWidth(Double.MAX_VALUE);
        FormField unitFormField = new FormField("Unit", unitBox, true);
        HBox.setHgrow(unitFormField, Priority.ALWAYS);

        TextField thresholdField = new TextField("10");
        thresholdField.setMaxWidth(Double.MAX_VALUE);
        FormField thresholdFormField = new FormField("Low-stock level", thresholdField, true);
        HBox.setHgrow(thresholdFormField, Priority.ALWAYS);

        HBox unitThresholdRow = new HBox(12, unitFormField, thresholdFormField);

        TextField qtyField = new TextField("0");
        qtyField.setMaxWidth(Double.MAX_VALUE);
        FormField qtyFormField = new FormField("Starting quantity", qtyField, true, "Recorded as opening stock movement");
        HBox.setHgrow(qtyFormField, Priority.ALWAYS);

        TextField priceField = new TextField("0.00");
        priceField.setMaxWidth(Double.MAX_VALUE);
        FormField priceFormField = new FormField("Selling price (₱)", priceField, true);
        HBox.setHgrow(priceFormField, Priority.ALWAYS);

        HBox qtyPriceRow = new HBox(12, qtyFormField, priceFormField);

        TextField costField = new TextField("0.00");
        costField.setMaxWidth(Double.MAX_VALUE);
        FormField costFormField = new FormField("Cost price (₱)", costField, true);
        HBox.setHgrow(costFormField, Priority.ALWAYS);

        // Warning line for selling price < cost price
        Label priceWarningLabel = new Label("⚠️ Warning: Selling price is lower than cost price (selling at a loss).");
        priceWarningLabel.getStyleClass().add("form-warning-line");
        priceWarningLabel.setVisible(false);
        priceWarningLabel.setManaged(false);

        Runnable checkPriceWarning = () -> {
            try {
                BigDecimal sp = new BigDecimal(priceField.getText().trim());
                BigDecimal cp = new BigDecimal(costField.getText().trim());
                boolean isLoss = sp.compareTo(cp) < 0 && cp.compareTo(BigDecimal.ZERO) > 0;
                priceWarningLabel.setVisible(isLoss);
                priceWarningLabel.setManaged(isLoss);
            } catch (Exception ignored) {
                priceWarningLabel.setVisible(false);
                priceWarningLabel.setManaged(false);
            }
        };
        priceField.textProperty().addListener((obs, o, n) -> checkPriceWarning.run());
        costField.textProperty().addListener((obs, o, n) -> checkPriceWarning.run());

        TextField skuField = new TextField();
        skuField.setPromptText("e.g. BEV-102948");
        skuField.setMaxWidth(Double.MAX_VALUE);
        Button generateSkuBtn = new Button("Generate");
        generateSkuBtn.getStyleClass().add("btn-dialog-cancel");
        generateSkuBtn.setMinWidth(Button.USE_PREF_SIZE);
        generateSkuBtn.setOnAction(e -> {
            CategorySelector.CategoryItem cat = categorySelector.getSelectedCategory();
            String catName = cat != null ? cat.name() : null;
            skuField.setText(productService.generateUniqueSku(currentUser, catName));
        });
        HBox skuInputRow = new HBox(6, skuField, generateSkuBtn);
        HBox.setHgrow(skuField, Priority.ALWAYS);
        FormField skuFormField = new FormField("SKU", skuInputRow, true);
        HBox.setHgrow(skuFormField, Priority.ALWAYS);

        TextField barcodeField = new TextField();
        barcodeField.setPromptText("Barcode / UPC (4-64 digits)");
        barcodeField.setMaxWidth(Double.MAX_VALUE);
        Button generateBarcodeBtn = new Button("Generate");
        generateBarcodeBtn.getStyleClass().add("btn-dialog-cancel");
        generateBarcodeBtn.setMinWidth(Button.USE_PREF_SIZE);
        generateBarcodeBtn.setOnAction(e -> barcodeField.setText(productService.generateUniqueBarcode(currentUser)));
        HBox barcodeInputRow = new HBox(6, barcodeField, generateBarcodeBtn);
        HBox.setHgrow(barcodeField, Priority.ALWAYS);
        FormField barcodeFormField = new FormField("Barcode", barcodeInputRow, true);
        HBox.setHgrow(barcodeFormField, Priority.ALWAYS);

        HBox costSkuRow = new HBox(12, costFormField, skuFormField);
        HBox barcodeRow = new HBox(12, barcodeFormField);

        // Auto-generate initial SKU & Barcode
        skuField.setText(productService.generateUniqueSku(currentUser, null));
        barcodeField.setText(productService.generateUniqueBarcode(currentUser));

        dialog.addBody(imgField, nameFormField, categoryFormField, unitThresholdRow, qtyPriceRow, priceWarningLabel, costSkuRow, barcodeRow);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        Button saveBtn = new Button("Save Product");
        saveBtn.getStyleClass().add("btn-dialog-confirm");
        saveBtn.setOnAction(e -> {
            // Clear previous errors
            imgField.clearError();
            nameFormField.clearError();
            categoryFormField.clearError();
            unitFormField.clearError();
            qtyFormField.clearError();
            priceFormField.clearError();
            costFormField.clearError();
            skuFormField.clearError();
            barcodeFormField.clearError();
            thresholdFormField.clearError();

            boolean hasError = false;
            FormField firstInvalid = null;

            if (selectedImagePath.get() == null || selectedImagePath.get().trim().isEmpty()) {
                imgField.setError("Product picture is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = imgField;
            }

            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            if (name.isEmpty()) {
                nameFormField.setError("Product name is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = nameFormField;
            } else if (name.length() > 120) {
                nameFormField.setError("Product name cannot exceed 120 characters.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = nameFormField;
            }

            Integer catId = categorySelector.getSelectedCategoryId();
            if (catId == null || catId <= 0) {
                categoryFormField.setError("Category is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = categoryFormField;
            }

            String unit = unitBox.getValue();
            if (unit == null || unit.trim().isEmpty()) {
                unitFormField.setError("Unit is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = unitFormField;
            }

            int qty = 0;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
                if (qty < 0) {
                    qtyFormField.setError("Starting quantity must be 0 or greater.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = qtyFormField;
                }
            } catch (Exception ex) {
                qtyFormField.setError("Starting quantity must be a whole number.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = qtyFormField;
            }

            int threshold = 10;
            try {
                threshold = Integer.parseInt(thresholdField.getText().trim());
                if (threshold < 0) {
                    thresholdFormField.setError("Low-stock level must be 0 or greater.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = thresholdFormField;
                }
            } catch (Exception ex) {
                thresholdFormField.setError("Low-stock level must be a whole number.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = thresholdFormField;
            }

            BigDecimal price = null;
            try {
                price = new BigDecimal(priceField.getText().trim());
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    priceFormField.setError("Selling price must be 0 or greater.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = priceFormField;
                } else if (price.scale() > 2) {
                    priceFormField.setError("Selling price cannot have more than 2 decimal places.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = priceFormField;
                }
            } catch (Exception ex) {
                priceFormField.setError("Selling price must be a valid number.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = priceFormField;
            }

            BigDecimal cost = null;
            try {
                cost = new BigDecimal(costField.getText().trim());
                if (cost.compareTo(BigDecimal.ZERO) < 0) {
                    costFormField.setError("Cost price must be 0 or greater.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = costFormField;
                } else if (cost.scale() > 2) {
                    costFormField.setError("Cost price cannot have more than 2 decimal places.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = costFormField;
                }
            } catch (Exception ex) {
                costFormField.setError("Cost price must be a valid number.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = costFormField;
            }

            String sku = skuField.getText() != null ? skuField.getText().trim() : "";
            if (sku.isEmpty()) {
                skuFormField.setError("SKU is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = skuFormField;
            } else if (sku.length() > 64) {
                skuFormField.setError("SKU cannot exceed 64 characters.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = skuFormField;
            }

            String barcode = barcodeField.getText() != null ? barcodeField.getText().trim() : "";
            if (barcode.isEmpty()) {
                barcodeFormField.setError("Barcode is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = barcodeFormField;
            } else if (barcode.length() < 4 || barcode.length() > 64) {
                barcodeFormField.setError("Barcode must be between 4 and 64 characters.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = barcodeFormField;
            }

            if (hasError && firstInvalid != null) {
                firstInvalid.requestFocusOnInput();
                return;
            }

            Product p = new Product();
            p.setName(name);
            p.setCategoryId(catId);
            p.setUnit(unit);
            p.setSku(sku);
            p.setBarcode(barcode);
            p.setCostPrice(cost);
            p.setSellingPrice(price);
            p.setStockQuantity(qty);
            p.setLowStockThreshold(threshold);
            p.setImagePath(selectedImagePath.get());

            saveBtn.setDisable(true);
            saveBtn.setText("Saving...");
            AppExecutor.execute(() -> {
                try {
                    productService.createProduct(p, currentUser);
                    Platform.runLater(() -> {
                        ViewNavigator.hideOverlayDialog();
                        ViewNavigator.showToast("Product '" + name + "' added successfully.", false);
                        loadInitialData();
                    });
                } catch (ProductService.ValidationException ve) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Save Product");
                        FormField firstErr = null;
                        for (ProductService.FieldError fe : ve.getErrors()) {
                            switch (fe.field()) {
                                case "picture" -> { imgField.setError(fe.message()); if (firstErr == null) firstErr = imgField; }
                                case "name" -> { nameFormField.setError(fe.message()); if (firstErr == null) firstErr = nameFormField; }
                                case "category" -> { categoryFormField.setError(fe.message()); if (firstErr == null) firstErr = categoryFormField; }
                                case "unit" -> { unitFormField.setError(fe.message()); if (firstErr == null) firstErr = unitFormField; }
                                case "startingQuantity" -> { qtyFormField.setError(fe.message()); if (firstErr == null) firstErr = qtyFormField; }
                                case "sellingPrice" -> { priceFormField.setError(fe.message()); if (firstErr == null) firstErr = priceFormField; }
                                case "costPrice" -> { costFormField.setError(fe.message()); if (firstErr == null) firstErr = costFormField; }
                                case "sku" -> { skuFormField.setError(fe.message()); if (firstErr == null) firstErr = skuFormField; }
                                case "barcode" -> { barcodeFormField.setError(fe.message()); if (firstErr == null) firstErr = barcodeFormField; }
                                case "lowStockThreshold" -> { thresholdFormField.setError(fe.message()); if (firstErr == null) firstErr = thresholdFormField; }
                                default -> ViewNavigator.showToast(fe.message(), true);
                            }
                        }
                        if (firstErr != null) firstErr.requestFocusOnInput();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Save Product");
                        ViewNavigator.showToast("Unable to save product. Please check your inputs and try again.", true);
                    });
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, saveBtn);
        dialog.show();
    }

    private void openEditProductModal(Product product) {
        AppDialog dialog = new AppDialog("Edit Product: " + product.getName(), 560);

        final SimpleObjectProperty<String> selectedImagePath = new SimpleObjectProperty<>(product.getImagePath());
        StackPane imgPreviewBox = new StackPane();
        imgPreviewBox.setPrefSize(70, 70);
        imgPreviewBox.setMinSize(70, 70);
        imgPreviewBox.setMaxSize(70, 70);
        imgPreviewBox.getStyleClass().add("item-thumb-box");

        ImageView previewImgView = new ImageView();
        previewImgView.setFitWidth(66);
        previewImgView.setFitHeight(66);
        previewImgView.setPreserveRatio(true);

        if (product.getImagePath() != null) {
            Image cached = AppExecutor.getCachedImage(product.getImagePath(), 70, 70);
            if (cached != null) {
                previewImgView.setImage(cached);
                imgPreviewBox.getChildren().setAll(previewImgView);
            } else {
                SVGPath defaultBoxIcon = new SVGPath();
                defaultBoxIcon.setContent(SVG_BOX);
                defaultBoxIcon.getStyleClass().add("sidebar-item-icon");
                imgPreviewBox.getChildren().add(defaultBoxIcon);
            }
        } else {
            SVGPath defaultBoxIcon = new SVGPath();
            defaultBoxIcon.setContent(SVG_BOX);
            defaultBoxIcon.getStyleClass().add("sidebar-item-icon");
            imgPreviewBox.getChildren().add(defaultBoxIcon);
        }

        VBox imgDetails = new VBox(6);
        imgDetails.setAlignment(Pos.CENTER_LEFT);

        Button uploadImgBtn = new Button("Change image");
        uploadImgBtn.getStyleClass().add("btn-dialog-cancel");
        uploadImgBtn.setMinWidth(Button.USE_PREF_SIZE);

        String initialFileName = product.getImagePath() != null ? new File(product.getImagePath()).getName() : "PNG/JPG/GIF, max 5 MB";
        Label imgHint = new Label(initialFileName);
        imgHint.getStyleClass().add("form-helper");

        FormField imgField = new FormField("Product picture", new HBox(14, imgPreviewBox, imgDetails), true);

        uploadImgBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Product Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File file = chooser.showOpenDialog(ViewNavigator.getPrimaryStage());
            if (file != null) {
                if (file.length() > 5 * 1024 * 1024) {
                    imgField.setError("Image file size exceeds 5 MB limit.");
                    return;
                }
                if (!ProductService.isValidProductImage(file.getAbsolutePath())) {
                    imgField.setError("Selected file is not a valid decodable image.");
                    return;
                }
                imgField.clearError();
                selectedImagePath.set(file.getAbsolutePath());
                previewImgView.setImage(new Image(file.toURI().toString(), 70, 70, true, true));
                imgPreviewBox.getChildren().setAll(previewImgView);
                imgHint.setText(file.getName());
            }
        });

        imgDetails.getChildren().addAll(uploadImgBtn, imgHint);

        TextField nameField = new TextField(product.getName());
        nameField.setMaxWidth(Double.MAX_VALUE);
        FormField nameFormField = new FormField("Product name", nameField, true);

        CategorySelector categorySelector = new CategorySelector(CategorySelector.Mode.FORM);
        categorySelector.setMaxWidth(Double.MAX_VALUE);
        categorySelector.setSelectedCategoryId(product.getCategoryId());
        FormField categoryFormField = new FormField("Category", categorySelector, true);

        ComboBox<String> unitBox = new ComboBox<>();
        unitBox.getItems().addAll("pcs", "bag", "box", "keg", "tin", "carton", "sleeve", "pack", "kg", "g", "L");
        unitBox.getSelectionModel().select(product.getUnit() != null ? product.getUnit() : "pcs");
        unitBox.setMaxWidth(Double.MAX_VALUE);
        FormField unitFormField = new FormField("Unit", unitBox, true);
        HBox.setHgrow(unitFormField, Priority.ALWAYS);

        TextField thresholdField = new TextField(String.valueOf(product.getLowStockThreshold()));
        thresholdField.setMaxWidth(Double.MAX_VALUE);
        FormField thresholdFormField = new FormField("Low-stock level", thresholdField, true);
        HBox.setHgrow(thresholdFormField, Priority.ALWAYS);

        HBox unitThresholdRow = new HBox(12, unitFormField, thresholdFormField);

        String currentStockText = product.getStockQuantity() + " " + (product.getUnit() != null ? product.getUnit() : "pcs");
        TextField stockField = new TextField(currentStockText);
        stockField.setEditable(false);
        stockField.setDisable(true);
        stockField.setMaxWidth(Double.MAX_VALUE);
        FormField stockFormField = new FormField("Starting quantity", stockField, true, "Starting quantity is locked after creation");
        HBox.setHgrow(stockFormField, Priority.ALWAYS);

        TextField priceField = new TextField(product.getSellingPrice() != null ? product.getSellingPrice().toPlainString() : "0.00");
        priceField.setMaxWidth(Double.MAX_VALUE);
        FormField priceFormField = new FormField("Selling price (₱)", priceField, true);
        HBox.setHgrow(priceFormField, Priority.ALWAYS);

        HBox stockPriceRow = new HBox(12, stockFormField, priceFormField);

        TextField costField = new TextField(product.getCostPrice() != null ? product.getCostPrice().toPlainString() : "0.00");
        costField.setMaxWidth(Double.MAX_VALUE);
        FormField costFormField = new FormField("Cost price (₱)", costField, true);
        HBox.setHgrow(costFormField, Priority.ALWAYS);

        // Warning line for selling price < cost price
        Label priceWarningLabel = new Label("⚠️ Warning: Selling price is lower than cost price (selling at a loss).");
        priceWarningLabel.getStyleClass().add("form-warning-line");
        priceWarningLabel.setVisible(false);
        priceWarningLabel.setManaged(false);

        Runnable checkPriceWarning = () -> {
            try {
                BigDecimal sp = new BigDecimal(priceField.getText().trim());
                BigDecimal cp = new BigDecimal(costField.getText().trim());
                boolean isLoss = sp.compareTo(cp) < 0 && cp.compareTo(BigDecimal.ZERO) > 0;
                priceWarningLabel.setVisible(isLoss);
                priceWarningLabel.setManaged(isLoss);
            } catch (Exception ignored) {
                priceWarningLabel.setVisible(false);
                priceWarningLabel.setManaged(false);
            }
        };
        priceField.textProperty().addListener((obs, o, n) -> checkPriceWarning.run());
        costField.textProperty().addListener((obs, o, n) -> checkPriceWarning.run());
        checkPriceWarning.run();

        TextField skuField = new TextField(product.getSku());
        skuField.setMaxWidth(Double.MAX_VALUE);
        Button generateSkuBtn = new Button("Generate");
        generateSkuBtn.getStyleClass().add("btn-dialog-cancel");
        generateSkuBtn.setMinWidth(Button.USE_PREF_SIZE);
        generateSkuBtn.setOnAction(e -> {
            CategorySelector.CategoryItem cat = categorySelector.getSelectedCategory();
            String catName = cat != null ? cat.name() : null;
            skuField.setText(productService.generateUniqueSku(currentUser, catName));
        });
        HBox skuInputRow = new HBox(6, skuField, generateSkuBtn);
        HBox.setHgrow(skuField, Priority.ALWAYS);
        FormField skuFormField = new FormField("SKU", skuInputRow, true);
        HBox.setHgrow(skuFormField, Priority.ALWAYS);

        TextField barcodeField = new TextField(product.getBarcode() != null ? product.getBarcode() : "");
        barcodeField.setMaxWidth(Double.MAX_VALUE);
        Button generateBarcodeBtn = new Button("Generate");
        generateBarcodeBtn.getStyleClass().add("btn-dialog-cancel");
        generateBarcodeBtn.setMinWidth(Button.USE_PREF_SIZE);
        generateBarcodeBtn.setOnAction(e -> barcodeField.setText(productService.generateUniqueBarcode(currentUser)));
        HBox barcodeInputRow = new HBox(6, barcodeField, generateBarcodeBtn);
        HBox.setHgrow(barcodeField, Priority.ALWAYS);
        FormField barcodeFormField = new FormField("Barcode", barcodeInputRow, true);
        HBox.setHgrow(barcodeFormField, Priority.ALWAYS);

        HBox costSkuRow = new HBox(12, costFormField, skuFormField);
        HBox barcodeRow = new HBox(12, barcodeFormField);

        dialog.addBody(imgField, nameFormField, categoryFormField, unitThresholdRow, stockPriceRow, priceWarningLabel, costSkuRow, barcodeRow);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        Button saveBtn = new Button("Update Product");
        saveBtn.getStyleClass().add("btn-dialog-confirm");
        saveBtn.setOnAction(e -> {
            // Clear previous errors
            imgField.clearError();
            nameFormField.clearError();
            categoryFormField.clearError();
            unitFormField.clearError();
            priceFormField.clearError();
            costFormField.clearError();
            skuFormField.clearError();
            barcodeFormField.clearError();
            thresholdFormField.clearError();

            boolean hasError = false;
            FormField firstInvalid = null;

            if (selectedImagePath.get() == null || selectedImagePath.get().trim().isEmpty()) {
                imgField.setError("Product picture is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = imgField;
            }

            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            if (name.isEmpty()) {
                nameFormField.setError("Product name is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = nameFormField;
            } else if (name.length() > 120) {
                nameFormField.setError("Product name cannot exceed 120 characters.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = nameFormField;
            }

            Integer catId = categorySelector.getSelectedCategoryId();
            if (catId == null || catId <= 0) {
                categoryFormField.setError("Category is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = categoryFormField;
            }

            String unit = unitBox.getValue();
            if (unit == null || unit.trim().isEmpty()) {
                unitFormField.setError("Unit is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = unitFormField;
            }

            int threshold = 10;
            try {
                threshold = Integer.parseInt(thresholdField.getText().trim());
                if (threshold < 0) {
                    thresholdFormField.setError("Low-stock level must be 0 or greater.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = thresholdFormField;
                }
            } catch (Exception ex) {
                thresholdFormField.setError("Low-stock level must be a whole number.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = thresholdFormField;
            }

            BigDecimal price = null;
            try {
                price = new BigDecimal(priceField.getText().trim());
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    priceFormField.setError("Selling price must be 0 or greater.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = priceFormField;
                } else if (price.scale() > 2) {
                    priceFormField.setError("Selling price cannot have more than 2 decimal places.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = priceFormField;
                }
            } catch (Exception ex) {
                priceFormField.setError("Selling price must be a valid number.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = priceFormField;
            }

            BigDecimal cost = null;
            try {
                cost = new BigDecimal(costField.getText().trim());
                if (cost.compareTo(BigDecimal.ZERO) < 0) {
                    costFormField.setError("Cost price must be 0 or greater.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = costFormField;
                } else if (cost.scale() > 2) {
                    costFormField.setError("Cost price cannot have more than 2 decimal places.");
                    hasError = true;
                    if (firstInvalid == null) firstInvalid = costFormField;
                }
            } catch (Exception ex) {
                costFormField.setError("Cost price must be a valid number.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = costFormField;
            }

            String sku = skuField.getText() != null ? skuField.getText().trim() : "";
            if (sku.isEmpty()) {
                skuFormField.setError("SKU is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = skuFormField;
            } else if (sku.length() > 64) {
                skuFormField.setError("SKU cannot exceed 64 characters.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = skuFormField;
            }

            String barcode = barcodeField.getText() != null ? barcodeField.getText().trim() : "";
            if (barcode.isEmpty()) {
                barcodeFormField.setError("Barcode is required.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = barcodeFormField;
            } else if (barcode.length() < 4 || barcode.length() > 64) {
                barcodeFormField.setError("Barcode must be between 4 and 64 characters.");
                hasError = true;
                if (firstInvalid == null) firstInvalid = barcodeFormField;
            }

            if (hasError && firstInvalid != null) {
                firstInvalid.requestFocusOnInput();
                return;
            }

            product.setName(name);
            product.setCategoryId(catId);
            product.setUnit(unit);
            product.setSku(sku);
            product.setBarcode(barcode);
            product.setCostPrice(cost);
            product.setSellingPrice(price);
            product.setLowStockThreshold(threshold);
            product.setImagePath(selectedImagePath.get());

            saveBtn.setDisable(true);
            saveBtn.setText("Updating...");
            AppExecutor.execute(() -> {
                try {
                    productService.updateProduct(product, currentUser);
                    Platform.runLater(() -> {
                        ViewNavigator.hideOverlayDialog();
                        ViewNavigator.showToast("Product '" + name + "' updated.", false);
                        loadInitialData();
                    });
                } catch (ProductService.ValidationException ve) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Update Product");
                        FormField firstErr = null;
                        for (ProductService.FieldError fe : ve.getErrors()) {
                            switch (fe.field()) {
                                case "picture" -> { imgField.setError(fe.message()); if (firstErr == null) firstErr = imgField; }
                                case "name" -> { nameFormField.setError(fe.message()); if (firstErr == null) firstErr = nameFormField; }
                                case "category" -> { categoryFormField.setError(fe.message()); if (firstErr == null) firstErr = categoryFormField; }
                                case "unit" -> { unitFormField.setError(fe.message()); if (firstErr == null) firstErr = unitFormField; }
                                case "sellingPrice" -> { priceFormField.setError(fe.message()); if (firstErr == null) firstErr = priceFormField; }
                                case "costPrice" -> { costFormField.setError(fe.message()); if (firstErr == null) firstErr = costFormField; }
                                case "sku" -> { skuFormField.setError(fe.message()); if (firstErr == null) firstErr = skuFormField; }
                                case "barcode" -> { barcodeFormField.setError(fe.message()); if (firstErr == null) firstErr = barcodeFormField; }
                                case "lowStockThreshold" -> { thresholdFormField.setError(fe.message()); if (firstErr == null) firstErr = thresholdFormField; }
                                default -> ViewNavigator.showToast(fe.message(), true);
                            }
                        }
                        if (firstErr != null) firstErr.requestFocusOnInput();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Update Product");
                        ViewNavigator.showToast("Unable to update product. Please check your inputs and try again.", true);
                    });
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, saveBtn);
        dialog.show();
    }

    private void handleArchiveProduct(Product product) {
        ViewNavigator.showConfirmDialog(
                "Archive Product",
                "Archive '" + product.getName() + "'? This will hide the item from the Store Terminal and mark it as Discontinued.",
                "Archive Product",
                () -> {
                    AppExecutor.execute(() -> {
                        try {
                            productService.archiveProduct(product.getId(), currentUser);
                            Platform.runLater(() -> {
                                ViewNavigator.showToast("Product '" + product.getName() + "' archived.", false);
                                loadInitialData();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> ViewNavigator.showToast("Error archiving: " + ex.getMessage(), true));
                        }
                    });
                }
        );
    }

    private void handleRestoreProduct(Product product) {
        ViewNavigator.showConfirmDialog(
                "Restore Product",
                "Restore '" + product.getName() + "' back to active store inventory?",
                "Restore Product",
                () -> {
                    AppExecutor.execute(() -> {
                        try {
                            productService.unarchiveProduct(product.getId(), currentUser);
                            Platform.runLater(() -> {
                                ViewNavigator.showToast("Product '" + product.getName() + "' restored to catalog.", false);
                                loadInitialData();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> ViewNavigator.showToast("Error restoring: " + ex.getMessage(), true));
                        }
                    });
                }
        );
    }

    private String generateRandomSku() {
        return "PRD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String formatPeso(BigDecimal amount) {
        if (amount == null) return "₱0.00";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "₱" + nf.format(amount);
    }

    @Override
    public void dispose() {
        searchDebounce.stop();
        cachedProducts.clear();
    }
}
