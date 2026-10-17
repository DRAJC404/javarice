package com.bai.inventory.view.pages;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.InventoryService;
import com.bai.inventory.service.ProductService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class StoreTerminalView extends BorderPane implements Disposable {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final User currentUser;

    private TextField searchField;
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));
    private final AtomicLong searchRequestId = new AtomicLong(0);
    private final ChangeListener<Number> scrollWidthListener = (obs, oldW, newW) -> relayoutProductGrid();

    private GridPane productGrid;
    private ScrollPane productScrollPane;
    private List<Product> currentProducts = new ArrayList<>();
    private final Map<Integer, Node> productCardMap = new HashMap<>();

    private Integer selectedCategoryId = 0; 
    private VBox categoryContainer;
    private ScrollPane categoryScrollPane;

    private final StringBuilder qtyBuffer = new StringBuilder("1");
    private boolean freshEntry = true;
    private int selectedQuantity = 1;
    private Label qtyDisplayLabel;
    private Label maxStockIndicator;
    private String selectedReason = "SOLD";

    private VBox recentDeductionsBox;
    private ScrollPane recentScrollPane;
    private final List<StockMovement> sessionDeductions = new ArrayList<>();
    private final Map<Integer, HBox> recentRowMap = new HashMap<>();

    private StackPane rootOverlay;
    private HBox toastBox;
    private Label toastMessageLabel;
    private Button toastUndoButton;
    private Timeline toastTimer;
    private StockMovement lastMovementForUndo;

    public StoreTerminalView() {
        this.productService = new ProductService();
        this.inventoryService = new InventoryService();
        this.currentUser = AuthService.getCurrentUser();
        buildUI();
        loadCategoriesAndProducts();
    }

    private void buildUI() {
        getStyleClass().add("terminal-root");

        if (DatabaseConfig.isDevModeEnabled()) {
            HBox devBanner = new HBox(8);
            devBanner.setAlignment(Pos.CENTER);
            devBanner.setStyle("-fx-background-color: #FEF3C7; -fx-padding: 8 16; -fx-border-color: #FCD34D; -fx-border-width: 0 0 1 0;");
            Label devLabel = new Label("⚠️ DEMO MODE ACTIVE (bai.dev.mode=true) — Deductions run in-memory and are not committed to PostgreSQL.");
            devLabel.setStyle("-fx-text-fill: #92400E; -fx-font-size: 12px; -fx-font-weight: 700;");
            devBanner.getChildren().add(devLabel);
            setTop(devBanner);
        }

        Node leftPane = createLeftPane();
        Node rightPane = createRightPane();

        BorderPane mainContent = new BorderPane();
        mainContent.setCenter(leftPane);
        mainContent.setRight(rightPane);

        rootOverlay = new StackPane(mainContent);
        setupToast();

        setCenter(rootOverlay);
    }

    private Node createLeftPane() {
        VBox left = new VBox(16);
        left.getStyleClass().add("terminal-left-pane");
        VBox.setVgrow(left, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Scan barcode or search product name / SKU... (Press Enter to deduct)");
        searchField.getStyleClass().add("terminal-search-bar");

        searchDebounce.setOnFinished(e -> executeDebouncedSearch());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });

        searchField.setOnAction(e -> handleSearchEnter());

        productGrid = new GridPane();
        productGrid.getStyleClass().add("product-grid");
        productGrid.setHgap(16);
        productGrid.setVgap(16);
        productGrid.setAlignment(Pos.TOP_LEFT);

        productScrollPane = new ScrollPane(productGrid);
        productScrollPane.getStyleClass().add("product-scroll-pane");
        productScrollPane.setFitToWidth(true);
        VBox.setVgrow(productScrollPane, Priority.ALWAYS);

        productScrollPane.widthProperty().addListener(scrollWidthListener);

        left.getChildren().addAll(searchField, productScrollPane);
        return left;
    }

    private Node createRightPane() {
        VBox right = new VBox(14);
        right.getStyleClass().add("terminal-right-pane");
        right.setMinWidth(360);
        right.setPrefWidth(380);
        right.setMaxWidth(420);

        HBox userBox = new HBox(12);
        userBox.getStyleClass().add("terminal-user-box");
        userBox.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(currentUser != null ? currentUser.getFullName().substring(0, 1).toUpperCase() : "E");
        avatar.getStyleClass().add("terminal-user-avatar");

        VBox userDetails = new VBox(2);
        Label userName = new Label(currentUser != null ? currentUser.getFullName() : "Employee");
        userName.getStyleClass().add("terminal-user-name");

        Label userRole = new Label(currentUser != null ? currentUser.getRole().getDisplayName() : "Store Associate");
        userRole.getStyleClass().add("terminal-user-role");
        userDetails.getChildren().addAll(userName, userRole);
        HBox.setHgrow(userDetails, Priority.ALWAYS);

        Button logoutBtn = new Button("Log out");
        logoutBtn.getStyleClass().add("btn-terminal-logout");
        logoutBtn.setOnAction(e -> ViewNavigator.logout());

        userBox.getChildren().addAll(avatar, userDetails, logoutBtn);

        VBox catSection = new VBox(6);
        HBox catHeader = new HBox(8);
        catHeader.setAlignment(Pos.CENTER_LEFT);
        Label catTitle = new Label("CATEGORIES");
        catTitle.getStyleClass().add("category-header-title");
        HBox.setHgrow(catTitle, Priority.ALWAYS);

        Button arrowUp = new Button("▲");
        arrowUp.getStyleClass().add("btn-scroll-arrow");
        arrowUp.setOnAction(e -> categoryScrollPane.setVvalue(Math.max(0.0, categoryScrollPane.getVvalue() - 0.35)));

        Button arrowDown = new Button("▼");
        arrowDown.getStyleClass().add("btn-scroll-arrow");
        arrowDown.setOnAction(e -> categoryScrollPane.setVvalue(Math.min(1.0, categoryScrollPane.getVvalue() + 0.35)));

        catHeader.getChildren().addAll(catTitle, arrowUp, arrowDown);

        categoryContainer = new VBox(6);
        categoryContainer.getStyleClass().add("category-pill-box");

        categoryScrollPane = new ScrollPane(categoryContainer);
        categoryScrollPane.getStyleClass().add("category-scroll-container");
        categoryScrollPane.setFitToWidth(true);
        categoryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryScrollPane.setPrefHeight(100);
        categoryScrollPane.setMinHeight(80);
        categoryScrollPane.setMaxHeight(130);

        catSection.getChildren().addAll(catHeader, categoryScrollPane);

        VBox reasonSection = new VBox(6);
        Label reasonTitle = new Label("REASON");
        reasonTitle.getStyleClass().add("category-header-title");

        HBox reasonBox = new HBox(6);
        reasonBox.getStyleClass().add("reason-box");
        String[] reasons = {"Sold", "Used", "Damaged", "Expired"};
        for (String r : reasons) {
            Button chip = new Button(r);
            chip.getStyleClass().add("reason-chip");
            if (r.equalsIgnoreCase(selectedReason)) chip.getStyleClass().add("selected");
            chip.setOnAction(e -> {
                selectedReason = r.toUpperCase();
                for (Node n : reasonBox.getChildren()) n.getStyleClass().remove("selected");
                chip.getStyleClass().add("selected");
            });
            reasonBox.getChildren().add(chip);
        }
        reasonSection.getChildren().addAll(reasonTitle, reasonBox);

        VBox qtySection = new VBox(6);
        HBox qtyDisplayBox = new HBox(8);
        qtyDisplayBox.getStyleClass().add("keypad-display-box");
        qtyDisplayBox.setAlignment(Pos.CENTER_RIGHT);

        Label qtyLabel = new Label("Quantity:");
        qtyLabel.getStyleClass().add("keypad-qty-label");
        HBox.setHgrow(qtyLabel, Priority.ALWAYS);

        maxStockIndicator = new Label("");
        maxStockIndicator.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444; -fx-font-weight: 700;");
        maxStockIndicator.setVisible(false);

        qtyDisplayLabel = new Label("1");
        qtyDisplayLabel.getStyleClass().add("keypad-qty-value");
        qtyDisplayBox.getChildren().addAll(qtyLabel, maxStockIndicator, qtyDisplayLabel);

        GridPane keypad = createKeypad();
        qtySection.getChildren().addAll(qtyDisplayBox, keypad);

        VBox recentSection = new VBox(6);
        VBox.setVgrow(recentSection, Priority.ALWAYS);

        Label recentTitle = new Label("RECENT SESSION DEDUCTIONS");
        recentTitle.getStyleClass().add("recent-title");

        recentDeductionsBox = new VBox(6);
        recentDeductionsBox.getStyleClass().add("recent-deductions-box");

        recentScrollPane = new ScrollPane(recentDeductionsBox);
        recentScrollPane.getStyleClass().add("recent-scroll-pane");
        recentScrollPane.setFitToWidth(true);
        recentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(recentScrollPane, Priority.ALWAYS);

        recentSection.getChildren().addAll(recentTitle, recentScrollPane);

        right.getChildren().addAll(userBox, catSection, reasonSection, qtySection, recentSection);
        return right;
    }

    private GridPane createKeypad() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("keypad-grid");
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);

        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.333);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        String[][] keys = {
                {"7", "8", "9"},
                {"4", "5", "6"},
                {"1", "2", "3"},
                {"C", "0", "⌫"}
        };

        for (int r = 0; r < keys.length; r++) {
            for (int c = 0; c < keys[r].length; c++) {
                String keyText = keys[r][c];
                Button btn = new Button(keyText);
                btn.getStyleClass().add("btn-keypad");
                btn.setMaxWidth(Double.MAX_VALUE);

                if (keyText.equals("C") || keyText.equals("⌫")) {
                    btn.getStyleClass().add("btn-keypad-action");
                }

                btn.setOnAction(e -> handleKeypadPress(keyText));
                grid.add(btn, c, r);
            }
        }
        return grid;
    }

    private void handleKeypadPress(String key) {
        maxStockIndicator.setVisible(false);

        if (key.equals("C")) {
            qtyBuffer.setLength(0);
            qtyBuffer.append("1");
            freshEntry = true;
            selectedQuantity = 1;
        } else if (key.equals("⌫")) {
            if (qtyBuffer.length() > 1) {
                qtyBuffer.deleteCharAt(qtyBuffer.length() - 1);
                selectedQuantity = Integer.parseInt(qtyBuffer.toString());
            } else {
                qtyBuffer.setLength(0);
                qtyBuffer.append("1");
                freshEntry = true;
                selectedQuantity = 1;
            }
        } else {
            if (freshEntry) {
                if (!key.equals("0")) {
                    qtyBuffer.setLength(0);
                    qtyBuffer.append(key);
                    freshEntry = false;
                }
            } else {
                if (qtyBuffer.length() < 4) {
                    qtyBuffer.append(key);
                } else {
                    maxStockIndicator.setText("Max 4 digits (9,999)");
                    maxStockIndicator.setVisible(true);
                }
            }
            try {
                selectedQuantity = Integer.parseInt(qtyBuffer.toString());
                if (selectedQuantity == 0) selectedQuantity = 1;
            } catch (NumberFormatException ignored) {
                selectedQuantity = 1;
            }
        }
        qtyDisplayLabel.setText(String.valueOf(selectedQuantity));
    }

    private void resetKeypadQuantity() {
        qtyBuffer.setLength(0);
        qtyBuffer.append("1");
        freshEntry = true;
        selectedQuantity = 1;
        qtyDisplayLabel.setText("1");
        maxStockIndicator.setVisible(false);
    }

    private void loadCategoriesAndProducts() {
        AppExecutor.execute(() -> {
            try {
                List<Category> cats = productService.getCategories(currentUser);
                Platform.runLater(() -> populateCategoryButtons(cats));
            } catch (Exception ex) {
                System.err.println("[StoreTerminalView] Could not load categories: " + ex.getMessage());
            }
        });

        executeDebouncedSearch();
    }

    private void populateCategoryButtons(List<Category> categories) {
        categoryContainer.getChildren().clear();

        Button allBtn = new Button("All Items");
        allBtn.getStyleClass().addAll("btn-category-item", "active");
        allBtn.setMaxWidth(Double.MAX_VALUE);
        allBtn.setOnAction(e -> {
            selectedCategoryId = 0;
            highlightCategory(allBtn);
            executeDebouncedSearch();
        });
        categoryContainer.getChildren().add(allBtn);

        for (Category c : categories) {
            Button btn = new Button(c.getName());
            btn.getStyleClass().add("btn-category-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                selectedCategoryId = c.getId();
                highlightCategory(btn);
                executeDebouncedSearch();
            });
            categoryContainer.getChildren().add(btn);
        }
    }

    private void highlightCategory(Button selected) {
        for (Node n : categoryContainer.getChildren()) {
            n.getStyleClass().remove("active");
        }
        selected.getStyleClass().add("active");
    }

    private void executeDebouncedSearch() {
        long reqId = searchRequestId.incrementAndGet();
        String query = searchField != null && searchField.getText() != null ? searchField.getText().trim() : "";
        Integer catId = selectedCategoryId;

        AppExecutor.execute(() -> {
            try {
                List<Product> products = productService.getProducts(query, catId, currentUser);
                if (reqId == searchRequestId.get()) {
                    Platform.runLater(() -> {
                        currentProducts = products;
                        renderProductCards();
                    });
                }
            } catch (Exception ex) {
                System.err.println("[StoreTerminalView] Search error: " + ex.getMessage());
            }
        });
    }

    private void handleSearchEnter() {
        String query = searchField.getText() != null ? searchField.getText().trim() : "";
        if (query.isEmpty()) return;

        searchDebounce.stop();

        AppExecutor.execute(() -> {
            try {
                List<Product> matches = productService.getProducts(query, 0, currentUser);
                Platform.runLater(() -> {
                    Product targetProduct = null;

                    for (Product p : matches) {
                        if (query.equalsIgnoreCase(p.getSku()) || query.equalsIgnoreCase(p.getBarcode())) {
                            targetProduct = p;
                            break;
                        }
                    }

                    if (targetProduct == null && matches.size() == 1) {
                        targetProduct = matches.get(0);
                    }

                    if (targetProduct != null) {
                        if (targetProduct.isOutOfStock()) {
                            ViewNavigator.showToast(targetProduct.getName() + " is Out of Stock.", true);
                        } else {
                            executeDeduction(targetProduct);
                            searchField.clear();
                            executeDebouncedSearch();
                        }
                    } else if (matches.isEmpty()) {
                        ViewNavigator.showToast("No product found matching: " + query, true);
                    } else {
                        currentProducts = matches;
                        renderProductCards();
                        ViewNavigator.showToast(matches.size() + " matches found. Select product.", false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> ViewNavigator.showToast("Search failed: " + ex.getMessage(), true));
            }
        });
    }

    private void renderProductCards() {
        productCardMap.clear();

        if (currentProducts.isEmpty()) {
            productGrid.getChildren().clear();
            productGrid.getColumnConstraints().clear();
            Label empty = new Label("No matching products found in stock.");
            empty.setStyle("-fx-font-size: 15px; -fx-text-fill: #94A3B8; -fx-padding: 30;");
            productGrid.add(empty, 0, 0);
            return;
        }

        for (Product p : currentProducts) {
            Node card = createProductCard(p);
            productCardMap.put(p.getId(), card);
        }

        relayoutProductGrid();
    }

    private void relayoutProductGrid() {
        if (currentProducts.isEmpty()) return;

        double availableWidth = productScrollPane.getWidth() - 32;
        if (availableWidth <= 200) availableWidth = 800; // Sensible default before layout pass

        int minCardWidth = 200;
        int hgap = 16;
        int cols = Math.max(1, (int) Math.floor((availableWidth + hgap) / (minCardWidth + hgap)));
        if (cols < 2 && availableWidth > 450) cols = 2;
        if (cols > 5) cols = 5;

        productGrid.getColumnConstraints().clear();
        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            productGrid.getColumnConstraints().add(cc);
        }

        productGrid.getChildren().clear();
        for (int i = 0; i < currentProducts.size(); i++) {
            Product p = currentProducts.get(i);
            Node card = productCardMap.get(p.getId());
            if (card != null) {
                int row = i / cols;
                int col = i % cols;
                productGrid.add(card, col, row);
            }
        }
    }

    private Node createProductCard(Product p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("terminal-product-card");
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(card, true);

        boolean isOut = p.isOutOfStock();
        if (isOut) card.getStyleClass().add("disabled");

        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-card-image-box");

        if (p.getImagePath() != null && !p.getImagePath().trim().isEmpty()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(100);
            iv.setFitHeight(75);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            AppExecutor.execute(() -> {
                Image img = AppExecutor.getCachedImage(p.getImagePath());
                if (img != null) {
                    Platform.runLater(() -> {
                        imgBox.getChildren().clear();
                        iv.setImage(img);
                        imgBox.getChildren().add(iv);
                    });
                }
            });
            imgBox.getChildren().add(createDefaultPlaceholderIcon());
        } else {
            imgBox.getChildren().add(createDefaultPlaceholderIcon());
        }

        Label name = new Label(p.getName());
        name.getStyleClass().add("product-card-name");
        name.setWrapText(true);

        Label sku = new Label("SKU: " + p.getSku());
        sku.getStyleClass().add("product-card-sku");

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        Label price = new Label("₱" + String.format("%.2f", p.getSellingPrice()));
        price.getStyleClass().add("product-card-price");
        HBox.setHgrow(price, Priority.ALWAYS);

        Label badge = new Label(isOut ? "Out of Stock" : (p.isLowStock() ? "Low Stock" : "In Stock"));
        badge.getStyleClass().addAll("badge", isOut ? "badge-danger" : (p.isLowStock() ? "badge-warning" : "badge-success"));

        priceRow.getChildren().addAll(price, badge);

        Label stock = new Label("Stock: " + p.getStockQuantity() + " " + p.getUnit());
        stock.getStyleClass().add("product-card-stock");

        Button sellBtn = new Button(isOut ? "Unavailable" : "Sell");
        sellBtn.getStyleClass().add("btn-sell");
        sellBtn.setMaxWidth(Double.MAX_VALUE);
        sellBtn.setDisable(isOut);

        sellBtn.setOnAction(e -> executeDeduction(p));

        card.getChildren().addAll(imgBox, name, sku, priceRow, stock, sellBtn);
        return card;
    }

    private Node createDefaultPlaceholderIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z");
        icon.setStyle("-fx-fill: #3B82F6;");
        return icon;
    }

    private void executeDeduction(Product p) {
        int qtyToDeduct = selectedQuantity;
        String reason = selectedReason;

        if (p.getStockQuantity() < qtyToDeduct) {
            maxStockIndicator.setText("Available: " + p.getStockQuantity());
            maxStockIndicator.setVisible(true);
            ViewNavigator.showToast("Cannot deduct " + qtyToDeduct + ". Available stock is only " + p.getStockQuantity() + " " + p.getUnit(), true);
            return;
        }

        setCardButtonsDisabled(p.getId(), true, "Deducting...");
        ViewNavigator.setOperationInProgress(true);

        AppExecutor.execute(() -> {
            try {
                StockMovement movement = inventoryService.deductStock(p.getId(), qtyToDeduct, reason, currentUser);

                Platform.runLater(() -> {
                    p.setStockQuantity(movement.getNewStock());
                    resetKeypadQuantity();
                    addSessionDeduction(movement);
                    showToast("Deducted " + qtyToDeduct + " x " + p.getName(), movement);

                    Node card = productCardMap.get(p.getId());
                    if (card != null) {
                        Node newCard = createProductCard(p);
                        productCardMap.put(p.getId(), newCard);
                        relayoutProductGrid();
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setCardButtonsDisabled(p.getId(), false, p.isOutOfStock() ? "Unavailable" : "Sell");
                    ViewNavigator.showToast("Deduction error: " + ex.getMessage(), true);
                });
            } finally {
                ViewNavigator.setOperationInProgress(false);
            }
        });
    }

    private void setCardButtonsDisabled(int productId, boolean disabled, String buttonText) {
        Node card = productCardMap.get(productId);
        if (card instanceof VBox vb) {
            for (Node child : vb.getChildren()) {
                if (child instanceof Button btn && btn.getStyleClass().contains("btn-sell")) {
                    btn.setDisable(disabled);
                    btn.setText(buttonText);
                }
            }
        }
    }

    private void addSessionDeduction(StockMovement m) {
        sessionDeductions.add(0, m);
        if (sessionDeductions.size() > 8) {
            sessionDeductions.remove(sessionDeductions.size() - 1);
        }
        renderRecentDeductions();
    }

    private void renderRecentDeductions() {
        recentDeductionsBox.getChildren().clear();
        recentRowMap.clear();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (StockMovement item : sessionDeductions) {
            HBox row = new HBox(8);
            row.getStyleClass().add("recent-item-row");
            row.setAlignment(Pos.CENTER_LEFT);

            boolean isUndone = item.isUndone();
            if (isUndone) {
                row.getStyleClass().add("undone");
            }

            String timeStr = item.getCreatedAt() != null ? item.getCreatedAt().format(timeFmt) : "Just now";
            Label time = new Label(timeStr);
            time.getStyleClass().add("recent-item-time");

            Label text = new Label(item.getQuantity() + "x " + item.getProductName() + " (" + item.getReason() + ")");
            text.getStyleClass().add("recent-item-text");
            if (isUndone) {
                text.setStyle("-fx-strikethrough: true; -fx-text-fill: #94A3B8;");
            }
            HBox.setHgrow(text, Priority.ALWAYS);

            Button undoBtn = new Button(isUndone ? "Undone" : "Undo");
            undoBtn.getStyleClass().add("btn-item-undo");
            undoBtn.setDisable(isUndone);

            undoBtn.setOnAction(e -> handleIndividualUndo(item, undoBtn, row, text));

            row.getChildren().addAll(time, text, undoBtn);
            recentDeductionsBox.getChildren().add(row);
            recentRowMap.put(item.getId(), row);
        }
    }

    private void handleIndividualUndo(StockMovement item, Button undoBtn, HBox row, Label text) {
        undoBtn.setDisable(true);
        undoBtn.setText("Reverting...");
        ViewNavigator.setOperationInProgress(true);

        AppExecutor.execute(() -> {
            try {
                StockMovement undoResult = inventoryService.undoDeduction(item.getId(), currentUser);
                Platform.runLater(() -> {
                    item.setUndoneByMovementId(undoResult.getId());
                    row.getStyleClass().add("undone");
                    text.setStyle("-fx-strikethrough: true; -fx-text-fill: #94A3B8;");
                    undoBtn.setText("Undone");
                    undoBtn.setDisable(true);

                    if (lastMovementForUndo != null && lastMovementForUndo.getId() == item.getId()) {
                        hideToast();
                    }

                    executeDebouncedSearch();
                    ViewNavigator.showToast("Restored " + item.getQuantity() + "x " + item.getProductName() + " back to stock.", false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    undoBtn.setDisable(false);
                    undoBtn.setText("Undo");
                    ViewNavigator.showToast("Could not undo: " + ex.getMessage(), true);
                });
            } finally {
                ViewNavigator.setOperationInProgress(false);
            }
        });
    }

    private void setupToast() {
        toastBox = new HBox(16);
        toastBox.getStyleClass().add("toast-container");
        toastBox.setAlignment(Pos.CENTER_LEFT);
        toastBox.setMaxWidth(460);
        toastBox.setVisible(false);
        toastBox.setManaged(false);

        toastMessageLabel = new Label();
        toastMessageLabel.getStyleClass().add("toast-message");
        HBox.setHgrow(toastMessageLabel, Priority.ALWAYS);

        toastUndoButton = new Button("Undo");
        toastUndoButton.getStyleClass().add("btn-toast-undo");
        toastUndoButton.setOnAction(e -> {
            if (lastMovementForUndo != null) {
                StockMovement target = lastMovementForUndo;
                HBox row = recentRowMap.get(target.getId());
                if (row != null) {
                    for (Node n : row.getChildren()) {
                        if (n instanceof Button btn && btn.getStyleClass().contains("btn-item-undo")) {
                            for (Node t : row.getChildren()) {
                                if (t instanceof Label lbl && lbl.getStyleClass().contains("recent-item-text")) {
                                    handleIndividualUndo(target, btn, row, lbl);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                } else {
                    handleQuickToastUndo();
                }
            }
        });

        toastBox.getChildren().addAll(toastMessageLabel, toastUndoButton);

        StackPane.setAlignment(toastBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastBox, new Insets(0, 0, 24, 0));

        rootOverlay.getChildren().add(toastBox);
    }

    private void showToast(String message, StockMovement movement) {
        this.lastMovementForUndo = movement;
        toastMessageLabel.setText(message);
        toastBox.setVisible(true);
        toastBox.setManaged(true);

        if (toastTimer != null) toastTimer.stop();

        toastTimer = new Timeline(new KeyFrame(Duration.seconds(10), e -> hideToast()));
        toastTimer.play();
    }

    private void hideToast() {
        toastBox.setVisible(false);
        toastBox.setManaged(false);
        lastMovementForUndo = null;
    }

    private void handleQuickToastUndo() {
        if (lastMovementForUndo == null) return;
        StockMovement mov = lastMovementForUndo;
        hideToast();
        ViewNavigator.setOperationInProgress(true);

        AppExecutor.execute(() -> {
            try {
                StockMovement undoResult = inventoryService.undoDeduction(mov.getId(), currentUser);
                Platform.runLater(() -> {
                    mov.setUndoneByMovementId(undoResult.getId());
                    renderRecentDeductions();
                    executeDebouncedSearch();
                    ViewNavigator.showToast("Restored " + mov.getQuantity() + "x " + mov.getProductName() + " back to stock.", false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> ViewNavigator.showToast("Undo failed: " + ex.getMessage(), true));
            } finally {
                ViewNavigator.setOperationInProgress(false);
            }
        });
    }

    @Override
    public void dispose() {
        if (toastTimer != null) {
            toastTimer.stop();
        }
        searchDebounce.stop();
        if (productScrollPane != null && scrollWidthListener != null) {
            productScrollPane.widthProperty().removeListener(scrollWidthListener);
        }
        sessionDeductions.clear();
        productCardMap.clear();
    }
}
