package com.bai.inventory.view.navigation;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.layout.MainLayoutView;
import com.bai.inventory.view.pages.JoinView;
import com.bai.inventory.view.pages.LoginView;
import com.bai.inventory.view.pages.RegisterView;
import com.bai.inventory.view.pages.StoreTerminalView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ViewNavigator {

    private static Stage primaryStage;
    private static Scene currentScene;
    private static Parent currentPage;

    private static StackPane rootContainer;
    private static StackPane contentPane;
    private static StackPane dialogOverlay;
    private static VBox toastContainer;
    private static boolean isDialogOpen = false;

    private static long lastActivityTimestamp = System.currentTimeMillis();
    private static boolean isIdleCountingDown = false;
    private static int idleCountdownSeconds = 30;
    private static HBox idleCountdownToast;
    private static Label idleCountdownLabel;
    private static Timeline idleCheckTimeline;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);

        primaryStage.setFullScreenExitHint("Press F11 to exit full screen");
        if (DatabaseConfig.isKioskModeEnabled()) {
            primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static Parent getCurrentPage() {
        return currentPage;
    }

    public static void routeByRole(User user) {
        if (user == null) {
            showLogin();
            return;
        }

        if (user.getRole() == Role.EMPLOYEE) {
            showStoreTerminal();
        } else {
            showMainLayout(ViewType.DASHBOARD);
        }
    }

    public static void navigateTo(ViewType viewType) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage not initialized in ViewNavigator");
        }

        User user = AuthService.getCurrentUser();

        if (user != null && user.getRole() == Role.EMPLOYEE) {
            if (viewType != ViewType.STORE_TERMINAL && viewType != ViewType.LOGIN) {
                System.out.println("[ViewNavigator] Access denied: Employee restricted to Store Terminal.");
                showStoreTerminal();
                return;
            }
        }

        switch (viewType) {
            case LOGIN -> showLogin();
            case REGISTER -> showRegister();
            case JOIN -> showJoin();
            case STORE_TERMINAL -> showStoreTerminal();
            case USERS -> {
                if (user == null || !user.getRole().isOwnerOrAdmin()) {
                    showToast("Access Denied: Business Owner or Administrator role required.", true);
                    return;
                }
                openInMainLayout(viewType);
            }
            case DASHBOARD, PRODUCT, INVENTORY, ANALYTICS, AI_INSIGHTS, REPORT -> openInMainLayout(viewType);
        }
    }

    private static void openInMainLayout(ViewType viewType) {
        if (currentPage instanceof MainLayoutView mainLayout) {
            mainLayout.selectView(viewType);
            primaryStage.setTitle("bAI - " + viewType.getTitle());
        } else {
            showMainLayout(viewType);
        }
    }

    public static void showMainLayout(ViewType initialView) {
        MainLayoutView layoutView = new MainLayoutView(initialView);
        setRoot(layoutView, "bAI - " + (initialView != null ? initialView.getTitle() : "Dashboard"));
    }

    public static void showSplash() {
        VBox splash = new VBox(20);
        splash.setAlignment(Pos.CENTER);
        splash.getStyleClass().add("splash-view");

        VBox brandBox = new VBox(10);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.getStyleClass().add("splash-brand-box");

        try (java.io.InputStream is = ViewNavigator.class.getResourceAsStream("/images/bailogo.png")) {
            if (is != null) {
                javafx.scene.image.ImageView logoImg = new javafx.scene.image.ImageView(new javafx.scene.image.Image(is));
                logoImg.setFitHeight(72);
                logoImg.setPreserveRatio(true);
                brandBox.getChildren().add(logoImg);
            }
        } catch (Exception ignored) {}

        Label logoLabel = new Label("bAI");
        logoLabel.getStyleClass().add("splash-logo");

        Label appName = new Label("AI-Powered Inventory Assistant");
        appName.getStyleClass().add("splash-subtitle");

        brandBox.getChildren().addAll(logoLabel, appName);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        spinner.getStyleClass().add("splash-spinner");

        Label status = new Label("Starting bAI...");
        status.getStyleClass().add("splash-status");

        splash.getChildren().addAll(brandBox, spinner, status);
        setRoot(splash, "bAI - Starting...");
    }

    public static void showLogin() {
        LoginView loginView = new LoginView();
        loginView.setOnLoginSuccess(ViewNavigator::routeByRole);
        loginView.setOnNavigateRegister(() -> navigateTo(ViewType.REGISTER));
        loginView.setOnNavigateJoin(() -> navigateTo(ViewType.JOIN));
        setRoot(loginView, "bAI - Login");
    }

    public static void showStoreTerminal() {
        StoreTerminalView terminalView = new StoreTerminalView();
        setRoot(terminalView, "bAI - Store Inventory Terminal");
    }

    public static void showDashboard() {
        openInMainLayout(ViewType.DASHBOARD);
    }

    public static void showRegister() {
        RegisterView registerView = new RegisterView();
        registerView.setOnNavigateLogin(ViewNavigator::showLogin);
        setRoot(registerView, "bAI - Register Business Owner");
    }

    public static void showJoin() {
        JoinView joinView = new JoinView();
        joinView.setOnNavigateLogin(ViewNavigator::showLogin);
        setRoot(joinView, "bAI - Join Organization");
    }

    private static void showPlaceholder(ViewType type) {
        showOverlayDialog(
                type.getTitle(),
                type.getTitle() + " view will be rendered here.",
                OverlayType.INFO,
                null
        );
    }

    public static void logout() {
        hideAllOverlayDialogs();
        resetIdleCountdown();
        new AuthService().logout();
        showLogin();
    }

    public static void setRoot(Parent root, String title) {
        hideAllOverlayDialogs();
        if (currentPage instanceof Disposable disposable) {
            try {
                disposable.dispose();
            } catch (Exception ex) {
                System.err.println("[ViewNavigator] Error disposing page: " + ex.getMessage());
            }
        }
        currentPage = root;

        if (currentScene == null) {
            contentPane = new StackPane();
            contentPane.getStyleClass().add("app-content-pane");

            dialogOverlay = new StackPane();
            dialogOverlay.getStyleClass().add("app-dialog-overlay");
            dialogOverlay.setVisible(false);
            dialogOverlay.setManaged(false);
            dialogOverlay.setOnMouseClicked(e -> {
                if (e.getTarget() == dialogOverlay && !operationInProgress.get()) {
                    hideOverlayDialog();
                }
            });

            toastContainer = new VBox(8);
            toastContainer.setAlignment(Pos.BOTTOM_CENTER);
            toastContainer.setPickOnBounds(false);
            toastContainer.setPadding(new Insets(0, 0, 24, 0));
            StackPane.setAlignment(toastContainer, Pos.BOTTOM_CENTER);

            rootContainer = new StackPane(contentPane, dialogOverlay, toastContainer);

            currentScene = new Scene(rootContainer);
            initStylesheets(currentScene);

            currentScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) {
                    primaryStage.setFullScreen(!primaryStage.isFullScreen());
                    event.consume();
                }
            });

            currentScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (isDialogOpen && event.getCode() == KeyCode.ESCAPE && !operationInProgress.get()) {
                    hideOverlayDialog();
                    event.consume();
                }
            });

            currentScene.addEventFilter(InputEvent.ANY, event -> onUserActivity());

            startIdleMonitor();

            primaryStage.setScene(currentScene);
        }

        contentPane.getChildren().setAll(root);
        primaryStage.setTitle(title);
    }

    public enum OverlayType {
        INFO, SUCCESS, WARNING, ERROR
    }

    
    private static final String SVG_CLOSE = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";
    private static final String SVG_SUCCESS_CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    private static final String SVG_WARNING_TRIANGLE = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
    private static final String SVG_ERROR_ALERT = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z";
    private static final String SVG_INFO_CIRCLE = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";

    public static void showOverlayDialog(String title, String message, OverlayType type, Runnable onConfirm) {
        showOverlayDialog(title, message, type, "Understood", onConfirm);
    }

    private static void pushDialogNode(Node node) {
        if (contentPane != null) {
            contentPane.setDisable(true);
        }
        isDialogOpen = true;

        if (dialogOverlay.getChildren().isEmpty()) {
            dialogOverlay.getChildren().add(node);
        } else {
            Node currentTop = dialogOverlay.getChildren().get(dialogOverlay.getChildren().size() - 1);
            currentTop.setDisable(true);

            StackPane modalLayer = new StackPane(node);
            modalLayer.setStyle("-fx-background-color: rgba(15, 23, 42, 0.45); -fx-alignment: center;");
            modalLayer.setOnMouseClicked(e -> {
                if (e.getTarget() == modalLayer && !operationInProgress.get()) {
                    hideOverlayDialog(node);
                }
            });
            dialogOverlay.getChildren().add(modalLayer);
        }

        dialogOverlay.setVisible(true);
        dialogOverlay.setManaged(true);
    }

    public static void showOverlayDialog(String title, String message, OverlayType type, String buttonText, Runnable onConfirm) {
        Platform.runLater(() -> {
            if (dialogOverlay == null) return;

            VBox card = new VBox(16);
            card.getStyleClass().add("dialog-modal-card");
            card.setMaxWidth(440);
            card.setPrefWidth(440);
            card.setMaxHeight(Region.USE_PREF_SIZE);

            BorderPane header = new BorderPane();
            header.getStyleClass().add("dialog-header");

            HBox titleLeft = new HBox(10);
            titleLeft.setAlignment(Pos.CENTER_LEFT);

            SVGPath icon = new SVGPath();
            switch (type) {
                case SUCCESS -> {
                    icon.setContent(SVG_SUCCESS_CHECK);
                    icon.getStyleClass().add("dialog-icon-success");
                }
                case WARNING -> {
                    icon.setContent(SVG_WARNING_TRIANGLE);
                    icon.getStyleClass().add("dialog-icon-warning");
                }
                case ERROR -> {
                    icon.setContent(SVG_ERROR_ALERT);
                    icon.getStyleClass().add("dialog-icon-danger");
                }
                case INFO -> {
                    icon.setContent(SVG_INFO_CIRCLE);
                    icon.getStyleClass().add("dialog-icon-info");
                }
            }

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("dialog-title");
            titleLeft.getChildren().addAll(icon, titleLabel);
            header.setLeft(titleLeft);

            Button closeBtn = new Button();
            closeBtn.getStyleClass().add("btn-dialog-close");
            SVGPath closeIcon = new SVGPath();
            closeIcon.setContent(SVG_CLOSE);
            closeIcon.getStyleClass().add("dialog-close-icon");
            closeBtn.setGraphic(closeIcon);
            closeBtn.setOnAction(e -> hideOverlayDialog(card));
            header.setRight(closeBtn);

            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageLabel.getStyleClass().add("dialog-body-text");

            HBox buttonRow = new HBox(10);
            buttonRow.setAlignment(Pos.CENTER_RIGHT);

            Button okBtn = new Button(buttonText != null ? buttonText : "Understood");
            okBtn.getStyleClass().add("btn-dialog-confirm");
            okBtn.setOnAction(e -> {
                hideOverlayDialog(card);
                if (onConfirm != null) onConfirm.run();
            });

            buttonRow.getChildren().add(okBtn);
            card.getChildren().addAll(header, messageLabel, buttonRow);

            StackPane.setAlignment(card, Pos.CENTER);
            pushDialogNode(card);
            Platform.runLater(okBtn::requestFocus);
        });
    }

    public static void showConfirmDialog(String title, String message, String confirmText, Runnable onConfirm) {
        Platform.runLater(() -> {
            if (dialogOverlay == null) return;

            VBox card = new VBox(16);
            card.getStyleClass().add("dialog-modal-card");
            card.setMaxWidth(440);
            card.setPrefWidth(440);
            card.setMaxHeight(Region.USE_PREF_SIZE);

            BorderPane header = new BorderPane();
            header.getStyleClass().add("dialog-header");

            HBox titleLeft = new HBox(10);
            titleLeft.setAlignment(Pos.CENTER_LEFT);

            SVGPath icon = new SVGPath();
            icon.setContent(SVG_WARNING_TRIANGLE);
            icon.getStyleClass().add("dialog-icon-warning");

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("dialog-title");
            titleLeft.getChildren().addAll(icon, titleLabel);
            header.setLeft(titleLeft);

            Button closeBtn = new Button();
            closeBtn.getStyleClass().add("btn-dialog-close");
            SVGPath closeIcon = new SVGPath();
            closeIcon.setContent(SVG_CLOSE);
            closeIcon.getStyleClass().add("dialog-close-icon");
            closeBtn.setGraphic(closeIcon);
            closeBtn.setOnAction(e -> hideOverlayDialog(card));
            header.setRight(closeBtn);

            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageLabel.getStyleClass().add("dialog-body-text");

            HBox buttonRow = new HBox(10);
            buttonRow.setAlignment(Pos.CENTER_RIGHT);

            Button cancelBtn = new Button("Cancel");
            cancelBtn.getStyleClass().add("btn-dialog-cancel");
            cancelBtn.setOnAction(e -> hideOverlayDialog(card));

            Button confirmBtn = new Button(confirmText != null ? confirmText : "Confirm");
            confirmBtn.getStyleClass().add("btn-dialog-confirm");
            confirmBtn.setOnAction(e -> {
                hideOverlayDialog(card);
                if (onConfirm != null) onConfirm.run();
            });

            buttonRow.getChildren().addAll(cancelBtn, confirmBtn);
            card.getChildren().addAll(header, messageLabel, buttonRow);

            StackPane.setAlignment(card, Pos.CENTER);
            pushDialogNode(card);
            Platform.runLater(confirmBtn::requestFocus);
        });
    }

    public static void showCustomDialog(Node content) {
        Platform.runLater(() -> {
            if (dialogOverlay == null) return;

            for (Node existing : dialogOverlay.getChildren()) {
                if (existing == content || (existing instanceof Pane p && p.getChildren().contains(content))) {
                    return;
                }
            }

            if (content instanceof Region reg) {
                reg.setMaxHeight(Region.USE_PREF_SIZE);
            }
            StackPane.setAlignment(content, Pos.CENTER);
            pushDialogNode(content);
        });
    }

    public static void hideOverlayDialog() {
        hideOverlayDialog(null);
    }

    public static void hideOverlayDialog(Node specificDialog) {
        Platform.runLater(() -> {
            if (dialogOverlay == null || dialogOverlay.getChildren().isEmpty()) {
                if (dialogOverlay != null) {
                    dialogOverlay.setVisible(false);
                    dialogOverlay.setManaged(false);
                }
                if (contentPane != null) {
                    contentPane.setDisable(false);
                }
                isDialogOpen = false;
                return;
            }

            if (specificDialog == null) {
                int lastIdx = dialogOverlay.getChildren().size() - 1;
                dialogOverlay.getChildren().remove(lastIdx);
            } else {
                Node toRemove = null;
                for (Node child : dialogOverlay.getChildren()) {
                    if (child == specificDialog) {
                        toRemove = child;
                        break;
                    }
                    if (child instanceof Pane pane && pane.getChildren().contains(specificDialog)) {
                        toRemove = child;
                        break;
                    }
                }
                if (toRemove != null) {
                    dialogOverlay.getChildren().remove(toRemove);
                } else {
                    int lastIdx = dialogOverlay.getChildren().size() - 1;
                    dialogOverlay.getChildren().remove(lastIdx);
                }
            }

            if (dialogOverlay.getChildren().isEmpty()) {
                dialogOverlay.setVisible(false);
                dialogOverlay.setManaged(false);
                if (contentPane != null) {
                    contentPane.setDisable(false);
                }
                isDialogOpen = false;
            } else {
                Node top = dialogOverlay.getChildren().get(dialogOverlay.getChildren().size() - 1);
                top.setDisable(false);
                isDialogOpen = true;
                Platform.runLater(top::requestFocus);
            }
        });
    }

    public static void hideAllOverlayDialogs() {
        Platform.runLater(() -> {
            if (dialogOverlay != null) {
                dialogOverlay.getChildren().clear();
                dialogOverlay.setVisible(false);
                dialogOverlay.setManaged(false);
            }
            if (contentPane != null) {
                contentPane.setDisable(false);
            }
            isDialogOpen = false;
        });
    }

    private static final java.util.concurrent.atomic.AtomicBoolean operationInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);

    public static void setOperationInProgress(boolean inProgress) {
        operationInProgress.set(inProgress);
        if (inProgress) {
            lastActivityTimestamp = System.currentTimeMillis();
            if (isIdleCountingDown) {
                resetIdleCountdown();
            }
        }
    }

    
    public static void showToast(String message, boolean isError) {
        Platform.runLater(() -> {
            if (toastContainer == null) return;

            long activeRegularToasts = toastContainer.getChildren().stream()
                    .filter(node -> node != idleCountdownToast)
                    .count();
            while (activeRegularToasts >= 3) {
                for (int i = 0; i < toastContainer.getChildren().size(); i++) {
                    javafx.scene.Node n = toastContainer.getChildren().get(i);
                    if (n != idleCountdownToast) {
                        toastContainer.getChildren().remove(i);
                        activeRegularToasts--;
                        break;
                    }
                }
            }

            HBox toast = new HBox(12);
            toast.getStyleClass().add(isError ? "global-toast-error" : "global-toast-success");
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.setMaxWidth(480);

            Label msgLabel = new Label(message);
            msgLabel.getStyleClass().add("toast-msg-text");
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(440);
            HBox.setHgrow(msgLabel, Priority.ALWAYS);

            toast.getChildren().add(msgLabel);
            toastContainer.getChildren().add(toast);

            double durationSeconds = isError ? 6.0 : 3.5;
            Timeline timer = new Timeline(new KeyFrame(Duration.seconds(durationSeconds), e -> {
                toastContainer.getChildren().remove(toast);
            }));
            timer.play();
        });
    }

    private static void onUserActivity() {
        lastActivityTimestamp = System.currentTimeMillis();
        if (isIdleCountingDown) {
            resetIdleCountdown();
        }
    }

    private static void startIdleMonitor() {
        if (idleCheckTimeline != null) idleCheckTimeline.stop();

        idleCheckTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> checkIdleState()));
        idleCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        idleCheckTimeline.play();
    }

    private static void checkIdleState() {
        int timeoutMinutes = DatabaseConfig.getIdleTimeoutMinutes();
        if (timeoutMinutes <= 0) return;

        if (operationInProgress.get()) {
            lastActivityTimestamp = System.currentTimeMillis();
            return;
        }

        if (AuthService.getCurrentUser() == null) {
            lastActivityTimestamp = System.currentTimeMillis();
            if (isIdleCountingDown) resetIdleCountdown();
            return;
        }

        if (isIdleCountingDown) {
            idleCountdownSeconds--;
            if (idleCountdownLabel != null) {
                idleCountdownLabel.setText("Inactivity detected: Logging out in " + idleCountdownSeconds + "s... (Touch or move mouse to stay logged in)");
            }
            if (idleCountdownSeconds <= 0) {
                resetIdleCountdown();
                System.out.println("[ViewNavigator] Inactivity timeout reached. Auto-logging out...");
                logout();
            }
        } else {
            long idleSeconds = (System.currentTimeMillis() - lastActivityTimestamp) / 1000;
            if (idleSeconds >= (timeoutMinutes * 60L)) {
                startIdleCountdown();
            }
        }
    }

    private static void startIdleCountdown() {
        isIdleCountingDown = true;
        idleCountdownSeconds = 30;

        if (toastContainer != null) {
            idleCountdownToast = new HBox(12);
            idleCountdownToast.getStyleClass().add("global-toast-warning");
            idleCountdownToast.setAlignment(Pos.CENTER_LEFT);
            idleCountdownToast.setMaxWidth(520);

            idleCountdownLabel = new Label("Inactivity detected: Logging out in 30s... (Touch or move mouse to stay logged in)");
            idleCountdownLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13.5px; -fx-font-weight: 700;");
            HBox.setHgrow(idleCountdownLabel, Priority.ALWAYS);

            idleCountdownToast.getChildren().add(idleCountdownLabel);
            toastContainer.getChildren().add(idleCountdownToast);
        }
    }

    private static void resetIdleCountdown() {
        isIdleCountingDown = false;
        idleCountdownSeconds = 30;
        lastActivityTimestamp = System.currentTimeMillis();
        if (idleCountdownToast != null && toastContainer != null) {
            toastContainer.getChildren().remove(idleCountdownToast);
            idleCountdownToast = null;
            idleCountdownLabel = null;
        }
    }

    private static void initStylesheets(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        addStylesheetIfPresent(scene, "/css/main.css");
        addStylesheetIfPresent(scene, "/css/login.css");
        addStylesheetIfPresent(scene, "/css/storeterminal.css");
    }

    private static void addStylesheetIfPresent(Scene scene, String resourcePath) {
        if (ViewNavigator.class.getResource(resourcePath) != null) {
            scene.getStylesheets().add(ViewNavigator.class.getResource(resourcePath).toExternalForm());
        }
    }
}
