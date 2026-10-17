package com.bai.inventory.view.pages;

import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.io.InputStream;
import com.bai.inventory.view.Disposable;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LoginView extends BorderPane implements Disposable {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String SVG_CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    private static final String SVG_EYE = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String SVG_EYE_OFF = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27z";

    private final AuthService authService;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private CheckBox rememberMeBox;
    private Button signInButton;
    private ProgressIndicator loadingIndicator;
    private Label errorLabel;

    private Consumer<User> onLoginSuccess;
    private Runnable onNavigateRegister;
    private Runnable onNavigateJoin;

    public LoginView() {
        this.authService = new AuthService();
        buildUI();
    }

    public LoginView(AuthService authService) {
        this.authService = authService;
        buildUI();
    }

    public void setOnLoginSuccess(Consumer<User> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public void setOnNavigateRegister(Runnable onNavigateRegister) {
        this.onNavigateRegister = onNavigateRegister;
    }

    public void setOnNavigateJoin(Runnable onNavigateJoin) {
        this.onNavigateJoin = onNavigateJoin;
    }

    private void buildUI() {
        getStyleClass().add("auth-root");

        HBox splitBox = new HBox();
        splitBox.setAlignment(Pos.CENTER);

        Node heroPane = createHeroPane();
        Node formPane = createFormPane();

        HBox.setHgrow(heroPane, Priority.ALWAYS);
        HBox.setHgrow(formPane, Priority.NEVER);

        splitBox.getChildren().addAll(heroPane, formPane);
        setCenter(splitBox);
    }

    private Node createHeroPane() {
        StackPane heroStack = new StackPane();
        heroStack.getStyleClass().add("hero-stack");

        Region imageLayer = new Region();
        imageLayer.getStyleClass().add("hero-image-layer");

        Region overlayLayer = new Region();
        overlayLayer.getStyleClass().add("hero-overlay-layer");

        VBox hero = new VBox();
        hero.getStyleClass().add("hero-pane");
        hero.setAlignment(Pos.CENTER);

        VBox content = new VBox(24);
        content.getStyleClass().add("hero-content");
        content.setMaxWidth(460);

        Label badge = new Label("AI-POWERED INVENTORY ASSISTANT");
        badge.getStyleClass().add("hero-badge");

        Label title = new Label("Smarter Stock Tracking,\nZero Manual Counting");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label("bAI automates real-time deductions, tracks item velocity, and alerts you before stock runs out.");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(12);
        features.getChildren().addAll(
                createFeaturePill("Real-Time Stock Deductions (Sold, Used, Damaged, Expired)"),
                createFeaturePill("Predictive Restock Alerts & AI Demand Forecast"),
                createFeaturePill("High-Precision Inventory Audits & Spot Checks")
        );

        content.getChildren().addAll(badge, title, subtitle, features);
        hero.getChildren().add(content);

        heroStack.getChildren().addAll(imageLayer, overlayLayer, hero);
        return heroStack;
    }

    private Node createFeaturePill(String text) {
        HBox pill = new HBox(12);
        pill.getStyleClass().add("hero-feature-pill");
        pill.setAlignment(Pos.CENTER_LEFT);

        SVGPath checkIcon = new SVGPath();
        checkIcon.setContent(SVG_CHECK);
        checkIcon.setStyle("-fx-fill: #93C5FD;");

        Label label = new Label(text);
        label.getStyleClass().add("hero-feature-text");

        pill.getChildren().addAll(checkIcon, label);
        return pill;
    }

    private Node createFormPane() {
        VBox form = new VBox(18);
        form.getStyleClass().add("auth-pane");
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPrefWidth(480);
        form.setMinWidth(420);
        form.setMaxWidth(520);

        HBox brandRow = new HBox(10);
        brandRow.getStyleClass().add("brand-row");
        brandRow.setAlignment(Pos.CENTER_LEFT);

        ImageView logoView = new ImageView();
        try (InputStream is = getClass().getResourceAsStream("/images/bailogo.png")) {
            if (is != null) {
                Image logoImg = new Image(is);
                logoView.setImage(logoImg);
                logoView.setFitHeight(38);
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);
            }
        } catch (Exception ignored) {}

        Label tag = new Label("Inventory Assistant");
        tag.getStyleClass().add("brand-tag");

        brandRow.getChildren().addAll(logoView, tag);

        VBox titleBox = new VBox(4);
        Label title = new Label("Hi, welcome to bAI");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Enter your credentials to access your account");
        subtitle.getStyleClass().add("auth-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("field-label");

        emailField = new TextField();
        emailField.setPromptText("you@example.com");
        emailField.getStyleClass().addAll("input-box", "text-input-field");
        emailField.setOnAction(e -> (visiblePasswordField.isVisible() ? visiblePasswordField : passwordField).requestFocus());

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("field-label");

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("text-input-field");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Enter your password");
        visiblePasswordField.getStyleClass().add("text-input-field");
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        HBox.setHgrow(visiblePasswordField, Priority.ALWAYS);

        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        Button eyeToggle = new Button();
        eyeToggle.getStyleClass().add("eye-button");
        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent(SVG_EYE);
        eyeIcon.setStyle("-fx-fill: #64748B;");
        eyeToggle.setGraphic(eyeIcon);

        eyeToggle.setOnAction(e -> {
            boolean show = !visiblePasswordField.isVisible();
            visiblePasswordField.setVisible(show);
            visiblePasswordField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);

            if (show) {
                eyeIcon.setContent(SVG_EYE_OFF);
                visiblePasswordField.requestFocus();
                visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
            } else {
                eyeIcon.setContent(SVG_EYE);
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });

        HBox passwordContainer = new HBox(passwordField, visiblePasswordField, eyeToggle);
        passwordContainer.getStyleClass().add("input-box");
        passwordContainer.setAlignment(Pos.CENTER_LEFT);

        passwordField.setOnAction(e -> handleLogin());
        visiblePasswordField.setOnAction(e -> handleLogin());

        rememberMeBox = new CheckBox("Remember me for 30 days");
        rememberMeBox.setSelected(false);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setVisible(false);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(18, 18);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);

        signInButton = new Button("Sign In");
        signInButton.getStyleClass().add("btn-auth-submit");
        signInButton.setMaxWidth(Double.MAX_VALUE);
        signInButton.setOnAction(e -> handleLogin());

        VBox footerBox = new VBox(6);
        footerBox.getStyleClass().add("auth-footer");
        footerBox.setAlignment(Pos.CENTER);

        HBox ownerRow = new HBox(4);
        ownerRow.setAlignment(Pos.CENTER);
        Label ownerLabel = new Label("Business owner?");
        ownerLabel.getStyleClass().add("auth-footer-text");
        Hyperlink ownerLink = new Hyperlink("Create a business account");
        ownerLink.getStyleClass().add("auth-link");
        ownerLink.setOnAction(e -> {
            if (onNavigateRegister != null) onNavigateRegister.run();
        });
        ownerRow.getChildren().addAll(ownerLabel, ownerLink);

        HBox joinRow = new HBox(4);
        joinRow.setAlignment(Pos.CENTER);
        Label staffLabel = new Label("Staff member?");
        staffLabel.getStyleClass().add("auth-footer-text");
        Hyperlink joinLink = new Hyperlink("Join with a code");
        joinLink.getStyleClass().add("auth-link");
        joinLink.setOnAction(e -> {
            if (onNavigateJoin != null) onNavigateJoin.run();
        });
        joinRow.getChildren().addAll(staffLabel, joinLink);

        footerBox.getChildren().addAll(ownerRow, joinRow);

        form.getChildren().addAll(
                brandRow,
                titleBox,
                emailLabel,
                emailField,
                passwordLabel,
                passwordContainer,
                rememberMeBox,
                errorLabel,
                signInButton,
                footerBox
        );

        return form;
    }

    private void handleLogin() {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showError("Email address is required.");
            emailField.requestFocus();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            emailField.requestFocus();
            return;
        }

        if (password == null || password.isEmpty()) {
            showError("Password is required.");
            (visiblePasswordField.isVisible() ? visiblePasswordField : passwordField).requestFocus();
            return;
        }

        clearError();
        setLoading(true);

        boolean rememberMe = rememberMeBox.isSelected();

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return authService.authenticate(email, password, rememberMe);
            }
        };

        loginTask.setOnSucceeded(e -> {
            setLoading(false);
            User user = loginTask.getValue();
            if (onLoginSuccess != null) {
                onLoginSuccess.accept(user);
            }
        });

        loginTask.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = loginTask.getException();
            String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Authentication failed.";
            showError(msg);
            passwordField.clear();
            visiblePasswordField.clear();
        });

        new Thread(loginTask).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        signInButton.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        visiblePasswordField.setDisable(loading);
        rememberMeBox.setDisable(loading);

        if (loading) {
            signInButton.setText("Signing in...");
            signInButton.setGraphic(loadingIndicator);
            loadingIndicator.setVisible(true);
            loadingIndicator.setManaged(true);
        } else {
            signInButton.setText("Sign In");
            signInButton.setGraphic(null);
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
    }

    @Override
    public void dispose() {
        passwordField.clear();
        visiblePasswordField.clear();
    }
}
