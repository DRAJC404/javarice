package com.bai.inventory.view.pages;

import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
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
    private Label emailErrorLabel;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private HBox passwordContainer;
    private Label passwordErrorLabel;
    private CheckBox rememberMeBox;
    private Button signInButton;
    private ProgressIndicator loadingIndicator;
    private Label errorLabel;

    private boolean isNavigating = false;
    private boolean emailDirty = false;
    private boolean passwordDirty = false;

    private javafx.beans.value.ChangeListener<Boolean> emailFocusListener;
    private javafx.beans.value.ChangeListener<Boolean> passwordFocusListener;
    private javafx.beans.value.ChangeListener<Boolean> visiblePasswordFocusListener;

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

        widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            if (w > 0) {
                if (w < 900) {
                    heroPane.setVisible(false);
                    heroPane.setManaged(false);
                    HBox.setHgrow(formPane, Priority.ALWAYS);
                } else {
                    heroPane.setVisible(true);
                    heroPane.setManaged(true);
                    HBox.setHgrow(heroPane, Priority.ALWAYS);
                    HBox.setHgrow(formPane, Priority.NEVER);
                }
            }
        });
    }

    private Node createHeroPane() {
        StackPane heroStack = new StackPane();
        heroStack.getStyleClass().add("hero-stack");

        ImageView bgImageView = new ImageView();
        try (InputStream is = getClass().getResourceAsStream("/images/logo.jpeg")) {
            if (is != null) {
                bgImageView.setImage(new Image(is));
                bgImageView.fitWidthProperty().bind(heroStack.widthProperty());
                bgImageView.fitHeightProperty().bind(heroStack.heightProperty());
                bgImageView.setPreserveRatio(false);
                bgImageView.setOpacity(0.18);
            }
        } catch (Exception ignored) {}

        Region overlayLayer = new Region();
        overlayLayer.getStyleClass().add("hero-overlay-layer");

        VBox hero = new VBox();
        hero.getStyleClass().add("hero-pane");
        hero.setAlignment(Pos.CENTER);

        VBox content = new VBox(24);
        content.getStyleClass().add("hero-content");
        content.setMaxWidth(480);

        Label badge = new Label("AI-POWERED INVENTORY ASSISTANT");
        badge.getStyleClass().add("hero-badge");

        Label title = new Label("Smarter Stock Tracking, Zero Manual Counting");
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);

        Label subtitle = new Label("bAI automates real-time deductions, tracks item velocity, and alerts you before stock runs out.");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(16);
        features.getChildren().addAll(
                createFeatureItem("Real-Time Stock Deductions", "Automated tracking for sold, used, damaged, and expired goods."),
                createFeatureItem("Predictive Restock Alerts", "Smart velocity monitoring and AI demand forecasting to prevent stockouts."),
                createFeatureItem("High-Precision Inventory Audits", "Rapid shift reconciliation and multi-location spot checks.")
        );

        content.getChildren().addAll(badge, title, subtitle, features);
        hero.getChildren().add(content);

        heroStack.getChildren().addAll(bgImageView, overlayLayer, hero);
        return heroStack;
    }

    private HBox createFeatureItem(String title, String desc) {
        HBox row = new HBox(14);
        row.getStyleClass().add("hero-feature-row");
        row.setAlignment(Pos.TOP_LEFT);

        StackPane circle = new StackPane();
        circle.getStyleClass().add("hero-check-circle");
        SVGPath check = new SVGPath();
        check.setContent(SVG_CHECK);
        check.getStyleClass().add("hero-check-icon");
        circle.getChildren().add(check);

        VBox text = new VBox(3);
        Label t = new Label(title);
        t.getStyleClass().add("hero-feature-title");
        Label d = new Label(desc);
        d.getStyleClass().add("hero-feature-desc");
        d.setWrapText(true);
        text.getChildren().addAll(t, d);

        row.getChildren().addAll(circle, text);
        return row;
    }

    private Node createFormPane() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("auth-form-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox formOuter = new VBox();
        formOuter.getStyleClass().add("auth-pane");
        formOuter.setAlignment(Pos.CENTER);
        formOuter.setFillWidth(true);

        VBox form = new VBox(18);
        form.setMaxWidth(408);
        form.setAlignment(Pos.CENTER_LEFT);

        HBox brandRow = new HBox();
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.setPadding(new Insets(0, 0, 6, 0));

        try (InputStream is = getClass().getResourceAsStream("/images/bailogo.png")) {
            if (is != null) {
                ImageView logoView = new ImageView(new Image(is));
                logoView.setFitHeight(46);
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);
                brandRow.getChildren().add(logoView);
            }
        } catch (Exception ignored) {}

        VBox titleBox = new VBox(4);
        Label title = new Label("Hi, welcome to bAI");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Enter your credentials to access your account");
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        titleBox.getChildren().addAll(title, subtitle);

        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("text-label");

        emailField = new TextField();
        emailField.setPromptText("you@example.com");
        emailField.getStyleClass().addAll("input-box", "text-input-field");
        emailField.setOnAction(e -> (visiblePasswordField.isVisible() ? visiblePasswordField : passwordField).requestFocus());

        emailErrorLabel = new Label();
        emailErrorLabel.getStyleClass().add("field-error-text");
        emailErrorLabel.setVisible(false);
        emailErrorLabel.setManaged(false);

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            emailDirty = true;
            clearFieldError(emailField, emailErrorLabel);
        });
        emailFocusListener = (obs, wasFocused, isFocused) -> {
            if (isNavigating) return;
            if (!isFocused && emailDirty) validateEmailField();
        };
        emailField.focusedProperty().addListener(emailFocusListener);

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("text-label");

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

        passwordContainer = new HBox(passwordField, visiblePasswordField, eyeToggle);
        passwordContainer.getStyleClass().add("input-box");
        passwordContainer.setAlignment(Pos.CENTER_LEFT);

        passwordErrorLabel = new Label();
        passwordErrorLabel.getStyleClass().add("field-error-text");
        passwordErrorLabel.setVisible(false);
        passwordErrorLabel.setManaged(false);

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            passwordDirty = true;
            clearFieldError(passwordContainer, passwordErrorLabel);
        });
        passwordFocusListener = (obs, wasFocused, isFocused) -> {
            if (isNavigating) return;
            if (!isFocused && !visiblePasswordField.isFocused() && passwordDirty) validatePasswordField();
        };
        passwordField.focusedProperty().addListener(passwordFocusListener);

        visiblePasswordFocusListener = (obs, wasFocused, isFocused) -> {
            if (isNavigating) return;
            if (!isFocused && !passwordField.isFocused() && passwordDirty) validatePasswordField();
        };
        visiblePasswordField.focusedProperty().addListener(visiblePasswordFocusListener);

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

        Button joinButton = new Button("Join with a code");
        joinButton.getStyleClass().add("btn-secondary");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        joinButton.setOnAction(e -> {
            isNavigating = true;
            resetForm();
            if (onNavigateJoin != null) onNavigateJoin.run();
        });

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
            isNavigating = true;
            resetForm();
            if (onNavigateRegister != null) onNavigateRegister.run();
        });
        ownerRow.getChildren().addAll(ownerLabel, ownerLink);

        footerBox.getChildren().add(ownerRow);

        VBox emailGroup = new VBox(4, emailLabel, emailField, emailErrorLabel);
        VBox passwordGroup = new VBox(4, passwordLabel, passwordContainer, passwordErrorLabel);

        form.getChildren().addAll(
                brandRow,
                titleBox,
                emailGroup,
                passwordGroup,
                rememberMeBox,
                errorLabel,
                signInButton,
                joinButton,
                footerBox
        );

        formOuter.getChildren().add(form);
        scrollPane.setContent(formOuter);
        return scrollPane;
    }

    private void showFieldError(Node control, Label label, String msg) {
        if (control != null && !control.getStyleClass().contains("input-error")) {
            control.getStyleClass().add("input-error");
        }
        if (label != null) {
            label.setText(msg);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void clearFieldError(Node control, Label label) {
        if (control != null) {
            control.getStyleClass().remove("input-error");
        }
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void showSingleFieldError(Node control, Label label, String msg) {
        clearAllErrors();
        showFieldError(control, label, msg);
    }

    public void clearAllErrors() {
        clearFieldError(emailField, emailErrorLabel);
        clearFieldError(passwordContainer, passwordErrorLabel);
        clearError();
    }

    public void resetForm() {
        emailDirty = false;
        passwordDirty = false;
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (visiblePasswordField != null) visiblePasswordField.clear();
        if (rememberMeBox != null) rememberMeBox.setSelected(false);
        clearAllErrors();
    }

    private void removeFocusListeners() {
        if (emailField != null && emailFocusListener != null) {
            emailField.focusedProperty().removeListener(emailFocusListener);
        }
        if (passwordField != null && passwordFocusListener != null) {
            passwordField.focusedProperty().removeListener(passwordFocusListener);
        }
        if (visiblePasswordField != null && visiblePasswordFocusListener != null) {
            visiblePasswordField.focusedProperty().removeListener(visiblePasswordFocusListener);
        }
    }

    private boolean validateEmailField() {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        if (email.isEmpty()) {
            showSingleFieldError(emailField, emailErrorLabel, "Please enter your work email address.");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showSingleFieldError(emailField, emailErrorLabel, "That email doesn't look right. Try something like name@company.com.");
            return false;
        }
        clearFieldError(emailField, emailErrorLabel);
        return true;
    }

    private boolean validatePasswordField() {
        String password = passwordField.getText();
        if (password == null || password.isEmpty()) {
            showSingleFieldError(passwordContainer, passwordErrorLabel, "Please enter your password.");
            return false;
        }
        clearFieldError(passwordContainer, passwordErrorLabel);
        return true;
    }

    private void handleLogin() {
        clearAllErrors();

        if (!validateEmailField()) {
            emailField.requestFocus();
            return;
        }
        if (!validatePasswordField()) {
            (visiblePasswordField.isVisible() ? visiblePasswordField : passwordField).requestFocus();
            return;
        }

        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField.getText();

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
            clearAllErrors();
            Throwable ex = loginTask.getException();
            String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Authentication failed.";
            if (msg.contains("Invalid email or password")) {
                showError("Invalid email or password.");
            } else if (msg.contains("Connection") || msg.contains("database") || msg.contains("SQLException") || msg.contains("Something went wrong")) {
                showError("Something went wrong on our side. Please try again in a moment.");
            } else {
                showError(msg);
            }
            passwordField.clear();
            visiblePasswordField.clear();
        });

        new Thread(loginTask).start();
    }

    private void showError(String msg) {
        clearAllErrors();
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
        isNavigating = true;
        removeFocusListeners();
        resetForm();
    }
}
