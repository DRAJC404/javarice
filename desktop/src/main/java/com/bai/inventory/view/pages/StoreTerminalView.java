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
import com.bai.inventory.util.CalculatorEvaluator;
import com.bai.inventory.util.Constants;
import com.bai.inventory.util.ProductImageManager;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.math.BigDecimal;
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

    private final StringBuilder calcBuffer = new StringBuilder();
    private boolean isResultMode = false;
    private Label calcExpressionLabel;
    private Label calcTotalLabel;
    private Label calcErrorLabel;
    private javafx.event.EventHandler<KeyEvent> keyEventFilter;

    private VBox recentDeductionsBox;
    private ScrollPane recentScrollPane;
    private final List<StockMovement> sessionDeductions = new ArrayList<>();

    private StackPane rootOverlay;
    private HBox toastBox;
    private Label toastMessageLabel;
    private FadeTransition toastFadeIn;
    private PauseTransition toastStay;
    private FadeTransition toastFadeOut;

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

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && keyEventFilter != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
            }
            if (newScene != null) {
                keyEventFilter = this::handleGlobalKeyPress;
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
            }
        });
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
        logoutBtn.setOnAction(e -> ViewNavigator.showConfirmDialog(
                "Sign out?",
                "You will need to sign in again to continue.",
                "Sign out",
                ViewNavigator::logout
        ));

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

        VBox calcSection = new VBox(6);

        VBox calcDisplayBox = new VBox(2);
        calcDisplayBox.getStyleClass().add("keypad-display-box");
        calcDisplayBox.setAlignment(Pos.CENTER_RIGHT);

        calcExpressionLabel = new Label("");
        calcExpressionLabel.getStyleClass().add("keypad-expression-label");

        HBox totalRow = new HBox(8);
        totalRow.setAlignment(Pos.CENTER_RIGHT);

        Label totalTitleLabel = new Label("Total:");
        totalTitleLabel.getStyleClass().add("keypad-qty-label");
        HBox.setHgrow(totalTitleLabel, Priority.ALWAYS);

        calcTotalLabel = new Label("₱0.00");
        calcTotalLabel.getStyleClass().add("keypad-qty-value");

        totalRow.getChildren().addAll(totalTitleLabel, calcTotalLabel);
        calcDisplayBox.getChildren().addAll(calcExpressionLabel, totalRow);

        calcErrorLabel = new Label("");
        calcErrorLabel.getStyleClass().add("keypad-error-label");
        calcErrorLabel.setVisible(false);
        calcErrorLabel.setManaged(false);

        GridPane keypad = createKeypad();
        calcSection.getChildren().addAll(calcDisplayBox, calcErrorLabel, keypad);

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

        right.getChildren().addAll(userBox, catSection, calcSection, recentSection);
        return right;
    }

    private GridPane createKeypad() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("keypad-grid");
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25.0);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        String[][] layout = {
                {"7", "8", "9", "÷"},
                {"4", "5", "6", "×"},
                {"1", "2", "3", "-"},
                {"C", "0", ".", "+"}
        };

        for (int r = 0; r < layout.length; r++) {
            for (int c = 0; c < layout[r].length; c++) {
                String keyText = layout[r][c];
                Button btn = new Button(keyText);
                btn.getStyleClass().add("btn-keypad");
                btn.setMaxWidth(Double.MAX_VALUE);

                if (keyText.equals("C")) {
                    btn.getStyleClass().add("btn-keypad-action");
                } else if (keyText.equals("+") || keyText.equals("-") || keyText.equals("×") || keyText.equals("÷")) {
                    btn.getStyleClass().add("btn-keypad-operator");
                }

                btn.setOnAction(e -> handleKeypadPress(keyText));
                grid.add(btn, c, r);
            }
        }

        Button backspaceBtn = new Button("⌫");
        backspaceBtn.getStyleClass().addAll("btn-keypad", "btn-keypad-action");
        backspaceBtn.setMaxWidth(Double.MAX_VALUE);
        backspaceBtn.setOnAction(e -> handleKeypadPress("⌫"));
        grid.add(backspaceBtn, 0, 4, 2, 1);

        Button equalsBtn = new Button("=");
        equalsBtn.getStyleClass().addAll("btn-keypad", "btn-keypad-equals");
        equalsBtn.setMaxWidth(Double.MAX_VALUE);
        equalsBtn.setOnAction(e -> handleKeypadPress("="));
        grid.add(equalsBtn, 2, 4, 2, 1);

        return grid;
    }

    private void clearCalculatorError() {
        if (calcErrorLabel != null) {
            calcErrorLabel.setVisible(false);
            calcErrorLabel.setManaged(false);
        }
    }

    private void showCalculatorError(String msg) {
        if (calcErrorLabel != null) {
            calcErrorLabel.setText(msg);
            calcErrorLabel.setVisible(true);
            calcErrorLabel.setManaged(true);
        }
    }

    private void resetCalculator() {
        calcBuffer.setLength(0);
        isResultMode = false;
        clearCalculatorError();
        updateCalculatorDisplay();
    }

    private void addPriceToCalculator(BigDecimal price) {
        if (price == null) return;
        clearCalculatorError();

        if (isResultMode) {
            isResultMode = false;
        }

        String priceStr = price.stripTrailingZeros().scale() <= 0
                ? price.setScale(0).toPlainString()
                : price.stripTrailingZeros().toPlainString();

        String cur = calcBuffer.toString().trim();
        if (cur.isEmpty()) {
            calcBuffer.append(priceStr);
        } else if (cur.endsWith("+") || cur.endsWith("-") || cur.endsWith("×") || cur.endsWith("÷")) {
            calcBuffer.append(priceStr);
        } else {
            calcBuffer.append(" + ").append(priceStr);
        }

        updateCalculatorDisplay();
    }

    private void handleKeypadPress(String key) {
        clearCalculatorError();

        if ("C".equals(key)) {
            resetCalculator();
            return;
        }

        if ("⌫".equals(key)) {
            if (isResultMode) {
                resetCalculator();
                return;
            }
            if (calcBuffer.length() > 0) {
                String s = calcBuffer.toString();
                if (s.endsWith(" ")) {
                    int lastNonSpace = s.trim().length();
                    int prevSpace = s.substring(0, lastNonSpace).lastIndexOf(' ');
                    if (prevSpace >= 0) {
                        calcBuffer.setLength(prevSpace);
                    } else {
                        calcBuffer.setLength(0);
                    }
                } else {
                    calcBuffer.deleteCharAt(calcBuffer.length() - 1);
                }
            }
            updateCalculatorDisplay();
            return;
        }

        if ("=".equals(key)) {
            String trimmed = calcBuffer.toString().trim();
            if (trimmed.isEmpty()) {
                return;
            }
            CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate(trimmed);
            if (!res.isSuccess()) {
                showCalculatorError(res.error() != null ? res.error() : "Error");
            } else {
                BigDecimal val = res.val();
                calcExpressionLabel.setText(trimmed + " =");
                calcTotalLabel.setText(formatCurrency(val));
                calcBuffer.setLength(0);
                String plain = val.stripTrailingZeros().scale() <= 0
                        ? val.setScale(0).toPlainString()
                        : val.stripTrailingZeros().toPlainString();
                calcBuffer.append(plain);
                isResultMode = true;
            }
            return;
        }

        if ("+".equals(key) || "-".equals(key) || "×".equals(key) || "÷".equals(key) || "*".equals(key) || "/".equals(key)) {
            String op = key.replace('*', '×').replace('/', '÷');
            isResultMode = false;

            String cur = calcBuffer.toString().trim();
            if (cur.isEmpty()) {
                return; 
            }

            if (calcBuffer.length() >= 50) {
                showCalculatorError("Max expression length reached");
                return;
            }

            if (cur.endsWith("+") || cur.endsWith("-") || cur.endsWith("×") || cur.endsWith("÷")) {
                int lastOpIdx = cur.lastIndexOf(' ');
                if (lastOpIdx >= 0) {
                    calcBuffer.setLength(lastOpIdx);
                    calcBuffer.append(" ").append(op).append(" ");
                } else {
                    calcBuffer.setLength(0);
                    calcBuffer.append(op).append(" ");
                }
            } else {
                calcBuffer.append(" ").append(op).append(" ");
            }

            updateCalculatorDisplay();
            return;
        }

        if (".".equals(key)) {
            if (isResultMode) {
                calcBuffer.setLength(0);
                isResultMode = false;
            }

            String s = calcBuffer.toString();
            int lastSpace = s.lastIndexOf(' ');
            String currentOperand = (lastSpace >= 0) ? s.substring(lastSpace + 1) : s;

            if (currentOperand.contains(".")) {
                return; 
            }

            if (currentOperand.isEmpty()) {
                calcBuffer.append("0.");
            } else {
                calcBuffer.append(".");
            }

            updateCalculatorDisplay();
            return;
        }

        if (Character.isDigit(key.charAt(0))) {
            if (isResultMode) {
                calcBuffer.setLength(0);
                isResultMode = false;
            }

            if (calcBuffer.length() >= 50) {
                showCalculatorError("Max expression length reached");
                return;
            }

            String s = calcBuffer.toString();
            int lastSpace = s.lastIndexOf(' ');
            String currentOperand = (lastSpace >= 0) ? s.substring(lastSpace + 1) : s;

            if (currentOperand.contains(".")) {
                int dotIndex = currentOperand.indexOf('.');
                String decimals = currentOperand.substring(dotIndex + 1);
                if (decimals.length() >= 2) {
                    return; 
                }
            }

            long digitCount = currentOperand.chars().filter(Character::isDigit).count();
            if (digitCount >= 9) {
                showCalculatorError("Max 9 digits per number");
                return;
            }

            calcBuffer.append(key);
            updateCalculatorDisplay();
        }
    }

    private void handleGlobalKeyPress(KeyEvent event) {
        if (searchField != null && searchField.isFocused()) {
            if (event.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
                resetCalculator();
                event.consume();
            }
            return;
        }

        KeyCode code = event.getCode();
        String text = event.getText();

        if (code == KeyCode.ENTER) {
            handleKeypadPress("=");
            event.consume();
        } else if (code == KeyCode.BACK_SPACE) {
            handleKeypadPress("⌫");
            event.consume();
        } else if (code == KeyCode.ESCAPE) {
            handleKeypadPress("C");
            event.consume();
        } else if (code == KeyCode.ADD || "+".equals(text)) {
            handleKeypadPress("+");
            event.consume();
        } else if (code == KeyCode.SUBTRACT || "-".equals(text)) {
            handleKeypadPress("-");
            event.consume();
        } else if (code == KeyCode.MULTIPLY || "*".equals(text)) {
            handleKeypadPress("×");
            event.consume();
        } else if (code == KeyCode.DIVIDE || "/".equals(text)) {
            handleKeypadPress("÷");
            event.consume();
        } else if (code == KeyCode.PERIOD || code == KeyCode.DECIMAL || ".".equals(text)) {
            handleKeypadPress(".");
            event.consume();
        } else if (code == KeyCode.EQUALS || "=".equals(text)) {
            handleKeypadPress("=");
            event.consume();
        } else if (text != null && text.length() == 1 && Character.isDigit(text.charAt(0))) {
            handleKeypadPress(text);
            event.consume();
        }
    }

    private void updateCalculatorDisplay() {
        String expr = calcBuffer.toString().trim();
        if (expr.isEmpty()) {
            calcExpressionLabel.setText("");
            calcTotalLabel.setText("₱0.00");
            return;
        }

        calcExpressionLabel.setText(expr);

        CalculatorEvaluator.EvaluationResult res = CalculatorEvaluator.evaluate(expr);
        if (res.isSuccess()) {
            calcTotalLabel.setText(formatCurrency(res.val()));
        }
    }

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₱0.00";
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return String.format(Locale.US, "-₱%,.2f", amount.abs());
        } else {
            return String.format(Locale.US, "₱%,.2f", amount);
        }
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
        if (availableWidth <= 200) availableWidth = 800; 

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

            Rectangle clip = new Rectangle(100, 75);
            clip.setArcWidth(8);
            clip.setArcHeight(8);
            iv.setClip(clip);

            AppExecutor.execute(() -> {
                Image img = ProductImageManager.getImage(p.getImagePath(), p.getOrganizationId(), 100, 75);
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
        price.setTooltip(new Tooltip("Add price to calculator"));
        price.setOnMouseClicked(e -> addPriceToCalculator(p.getSellingPrice()));
        HBox.setHgrow(price, Priority.ALWAYS);

        Label badge = new Label(isOut ? "Out of Stock" : (p.isLowStock() ? "Low Stock" : "In Stock"));
        badge.getStyleClass().addAll("badge", isOut ? "badge-danger" : (p.isLowStock() ? "badge-warning" : "badge-success"));

        priceRow.getChildren().addAll(price, badge);

        Label stock = new Label("Stock: " + p.getStockQuantity() + " " + Constants.UNIT);
        stock.getStyleClass().add("product-card-stock");

        Button sellBtn = new Button(isOut ? "Out of stock" : "Sell");
        sellBtn.getStyleClass().add("btn-sell");
        sellBtn.setMaxWidth(Double.MAX_VALUE);
        sellBtn.setDisable(isOut);

        sellBtn.setOnAction(e -> executeDeduction(p, 1));

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
        executeDeduction(p, 1);
    }

    private void executeDeduction(Product p, int qtyToDeduct) {
        if (p.isOutOfStock() || p.getStockQuantity() < qtyToDeduct) {
            ViewNavigator.showToast(p.getName() + " is Out of Stock.", true);
            return;
        }

        setCardButtonState(p.getId(), true, "Deducting...");
        ViewNavigator.setOperationInProgress(true);

        AppExecutor.execute(() -> {
            try {
                StockMovement movement = inventoryService.deductPosSale(p.getId(), qtyToDeduct, currentUser);

                Platform.runLater(() -> {
                    p.setStockQuantity(movement.getNewStock());
                    addSessionDeduction(movement);
                    showToast("Deducted " + qtyToDeduct + " x " + p.getName());

                    Node card = productCardMap.get(p.getId());
                    if (card != null) {
                        Node newCard = createProductCard(p);
                        productCardMap.put(p.getId(), newCard);
                        relayoutProductGrid();
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setCardButtonState(p.getId(), p.isOutOfStock(), p.isOutOfStock() ? "Out of stock" : "Sell");
                    ViewNavigator.showToast("Deduction error: " + ex.getMessage(), true);
                });
            } finally {
                ViewNavigator.setOperationInProgress(false);
            }
        });
    }

    private void setCardButtonState(int productId, boolean disabled, String buttonText) {
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

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (StockMovement item : sessionDeductions) {
            HBox row = new HBox(8);
            row.getStyleClass().add("recent-item-row");
            row.setAlignment(Pos.CENTER_LEFT);

            String timeStr = item.getCreatedAt() != null ? item.getCreatedAt().format(timeFmt) : "Just now";
            Label time = new Label(timeStr);
            time.getStyleClass().add("recent-item-time");

            Label text = new Label(item.getQuantity() + "x " + item.getProductName() + " (" + item.getReason() + ")");
            text.getStyleClass().add("recent-item-text");
            HBox.setHgrow(text, Priority.ALWAYS);

            row.getChildren().addAll(time, text);
            recentDeductionsBox.getChildren().add(row);
        }
    }

    private void setupToast() {
        toastBox = new HBox();
        toastBox.getStyleClass().add("toast-container");
        toastBox.setAlignment(Pos.CENTER);
        toastBox.setMaxWidth(360);
        toastBox.setMaxHeight(Region.USE_PREF_SIZE);
        toastBox.setVisible(false);
        toastBox.setManaged(false);
        toastBox.setMouseTransparent(true);

        toastMessageLabel = new Label();
        toastMessageLabel.getStyleClass().add("toast-message");
        toastMessageLabel.setAlignment(Pos.CENTER);

        toastBox.getChildren().add(toastMessageLabel);

        StackPane.setAlignment(toastBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastBox, new Insets(0, 0, 24, 0));

        rootOverlay.getChildren().add(toastBox);
    }

    private void showToast(String message) {
        if (toastFadeIn != null) toastFadeIn.stop();
        if (toastStay != null) toastStay.stop();
        if (toastFadeOut != null) toastFadeOut.stop();

        toastMessageLabel.setText(message);
        toastBox.setOpacity(0.0);
        toastBox.setVisible(true);
        toastBox.setManaged(true);

        toastFadeIn = new FadeTransition(Duration.millis(150), toastBox);
        toastFadeIn.setFromValue(0.0);
        toastFadeIn.setToValue(1.0);

        toastStay = new PauseTransition(Duration.seconds(2));

        toastFadeOut = new FadeTransition(Duration.millis(250), toastBox);
        toastFadeOut.setFromValue(1.0);
        toastFadeOut.setToValue(0.0);
        toastFadeOut.setOnFinished(e -> {
            toastBox.setVisible(false);
            toastBox.setManaged(false);
        });

        toastFadeIn.setOnFinished(e -> toastStay.play());
        toastStay.setOnFinished(e -> toastFadeOut.play());

        toastFadeIn.play();
    }

    private void hideToast() {
        if (toastFadeIn != null) toastFadeIn.stop();
        if (toastStay != null) toastStay.stop();
        if (toastFadeOut != null) toastFadeOut.stop();
        toastBox.setVisible(false);
        toastBox.setManaged(false);
    }

    @Override
    public void dispose() {
        hideToast();
        searchDebounce.stop();
        if (productScrollPane != null && scrollWidthListener != null) {
            productScrollPane.widthProperty().removeListener(scrollWidthListener);
        }
        if (getScene() != null && keyEventFilter != null) {
            getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyEventFilter);
        }
        sessionDeductions.clear();
        productCardMap.clear();
    }
}
