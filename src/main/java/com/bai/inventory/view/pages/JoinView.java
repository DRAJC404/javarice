package com.bai.inventory.view.pages;

import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.io.InputStream;

public class JoinView extends BorderPane implements Disposable {

    private static final String SVG_CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    private static final String SVG_EYE = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String SVG_EYE_OFF = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27z";
    private static final String SVG_KEY = "M12.65 10C11.83 7.67 9.61 6 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6c2.61 0 4.83-1.67 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z";

    private final AuthService authService;

    private TextField codeField;
    private TextField nameField;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private PasswordField confirmPasswordField;
    private TextField visibleConfirmPasswordField;
    private Button joinButton;
    private ProgressIndicator loadingIndicator;
    private Label errorLabel;

    private Region seg1;
    private Region seg2;
    private Region seg3;
    private Label strengthLabel;

    private Runnable onNavigateLogin;

    public JoinView() {
        this.authService = new AuthService();
        buildUI();
    }

    public void setOnNavigateLogin(Runnable onNavigateLogin) {
        this.onNavigateLogin = onNavigateLogin;
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

        Label badge = new Label("STAFF ONBOARDING");
        badge.getStyleClass().add("hero-badge");

        Label title = new Label("Join Your Store Team with an Invite Code");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label("Your store owner generated a secure 10-minute join code. Enter the 8-character code below along with your name, email, and password to activate your access.");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(12);
        features.getChildren().addAll(
                createFeatureItem("Direct Team Access", "Instantly linked to your organization with role pre-configured by your manager or store owner."),
                createFeatureItem("10-Minute Code Expiry", "Join codes expire in 10 minutes for airtight security. One code, single redemption."),
                createFeatureItem("Real-Time Store Terminal", "Log deductions immediately at retail checkout and run floor-level inventory tracking.")
        );

        content.getChildren().addAll(badge, title, subtitle, features);
        hero.getChildren().add(content);
        heroStack.getChildren().addAll(imageLayer, overlayLayer, hero);

        return heroStack;
    }

    private HBox createFeatureItem(String title, String desc) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        SVGPath check = new SVGPath();
        check.setContent(SVG_CHECK);
        check.getStyleClass().add("hero-feature-icon");

        VBox text = new VBox(2);
        Label t = new Label(title);
        t.getStyleClass().add("hero-feature-title");
        Label d = new Label(desc);
        d.getStyleClass().add("hero-feature-desc");
        d.setWrapText(true);
        text.getChildren().addAll(t, d);

        row.getChildren().addAll(check, text);
        return row;
    }

    private Node createFormPane() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("auth-form-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox container = new VBox();
        container.getStyleClass().add("form-pane");
        container.setAlignment(Pos.CENTER);

        VBox form = new VBox(14);
        form.getStyleClass().add("form-card");
        form.setMaxWidth(440);

        HBox brandRow = new HBox(10);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        try (InputStream is = getClass().getResourceAsStream("/images/bailogo.png")) {
            if (is != null) {
                ImageView logoImg = new ImageView(new Image(is));
                logoImg.setFitHeight(34);
                logoImg.setPreserveRatio(true);
                brandRow.getChildren().add(logoImg);
            }
        } catch (Exception ignored) {}

        Label brandLabel = new Label("bAI");
        brandLabel.getStyleClass().add("form-brand-name");
        Label editionLabel = new Label("STAFF JOIN");
        editionLabel.getStyleClass().add("form-brand-tag");
        brandRow.getChildren().addAll(brandLabel, editionLabel);

        VBox titleBox = new VBox(4);
        Label title = new Label("Join Organization");
        title.getStyleClass().add("form-title");

        Label subtitle = new Label("Enter your 8-character invitation code to join");
        subtitle.getStyleClass().add("form-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Label codeLabel = new Label("Invitation Code *");
        codeLabel.getStyleClass().add("form-field-label");

        codeField = new TextField();
        codeField.setPromptText("e.g. 7K9P-X2M4");
        codeField.getStyleClass().add("auth-input");
        codeField.setStyle("-fx-font-family: monospace; -fx-font-size: 15px; -fx-font-weight: 700; -fx-letter-spacing: 1px;");

        Label nameLabel = new Label("Full Name *");
        nameLabel.getStyleClass().add("form-field-label");

        nameField = new TextField();
        nameField.setPromptText("Maria Santos");
        nameField.getStyleClass().add("auth-input");

        Label emailLabel = new Label("Work Email Address *");
        emailLabel.getStyleClass().add("form-field-label");

        emailField = new TextField();
        emailField.setPromptText("maria@example.com");
        emailField.getStyleClass().add("auth-input");

        Label passwordLabel = new Label("Create Password *");
        passwordLabel.getStyleClass().add("form-field-label");

        StackPane passwordContainer = new StackPane();
        passwordField = new PasswordField();
        passwordField.setPromptText("At least 8 characters");
        passwordField.getStyleClass().add("auth-input");

        visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("At least 8 characters");
        visiblePasswordField.getStyleClass().add("auth-input");
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        Button togglePasswordBtn = createEyeToggleButton(passwordField, visiblePasswordField);
        StackPane.setAlignment(togglePasswordBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(togglePasswordBtn, new Insets(0, 10, 0, 0));
        passwordContainer.getChildren().addAll(passwordField, visiblePasswordField, togglePasswordBtn);

        Node strengthBar = createStrengthIndicator();
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> updateStrengthIndicator(newVal));

        Label confirmPasswordLabel = new Label("Confirm Password *");
        confirmPasswordLabel.getStyleClass().add("form-field-label");

        StackPane confirmPasswordContainer = new StackPane();
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Re-type your password");
        confirmPasswordField.getStyleClass().add("auth-input");

        visibleConfirmPasswordField = new TextField();
        visibleConfirmPasswordField.setPromptText("Re-type your password");
        visibleConfirmPasswordField.getStyleClass().add("auth-input");
        visibleConfirmPasswordField.setVisible(false);
        visibleConfirmPasswordField.setManaged(false);

        confirmPasswordField.textProperty().bindBidirectional(visibleConfirmPasswordField.textProperty());

        Button toggleConfirmBtn = createEyeToggleButton(confirmPasswordField, visibleConfirmPasswordField);
        StackPane.setAlignment(toggleConfirmBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggleConfirmBtn, new Insets(0, 10, 0, 0));
        confirmPasswordContainer.getChildren().addAll(confirmPasswordField, visibleConfirmPasswordField, toggleConfirmBtn);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error-message");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(18, 18);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);

        joinButton = new Button("Join Organization");
        joinButton.getStyleClass().add("btn-auth-submit");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        joinButton.setOnAction(e -> handleJoin());

        HBox loginRow = new HBox(6);
        loginRow.getStyleClass().add("auth-footer");
        loginRow.setAlignment(Pos.CENTER);

        Label alreadyAccount = new Label("Already have an account?");
        alreadyAccount.getStyleClass().add("auth-footer-text");

        Hyperlink loginLink = new Hyperlink("Sign in");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnAction(e -> {
            if (onNavigateLogin != null) onNavigateLogin.run();
        });

        loginRow.getChildren().addAll(alreadyAccount, loginLink);

        VBox codeGroup = new VBox(4, codeLabel, codeField);
        VBox nameGroup = new VBox(4, nameLabel, nameField);
        VBox emailGroup = new VBox(4, emailLabel, emailField);
        VBox passwordGroup = new VBox(4, passwordLabel, passwordContainer, strengthBar);
        VBox confirmPasswordGroup = new VBox(4, confirmPasswordLabel, confirmPasswordContainer);

        form.getChildren().addAll(
                brandRow,
                titleBox,
                codeGroup,
                nameGroup,
                emailGroup,
                passwordGroup,
                confirmPasswordGroup,
                errorLabel,
                joinButton,
                loginRow
        );

        container.getChildren().add(form);
        scrollPane.setContent(container);
        return scrollPane;
    }

    private Node createStrengthIndicator() {
        VBox box = new VBox(4);

        HBox bars = new HBox(4);
        seg1 = new Region();
        seg2 = new Region();
        seg3 = new Region();

        HBox.setHgrow(seg1, Priority.ALWAYS);
        HBox.setHgrow(seg2, Priority.ALWAYS);
        HBox.setHgrow(seg3, Priority.ALWAYS);

        seg1.setPrefHeight(4);
        seg2.setPrefHeight(4);
        seg3.setPrefHeight(4);

        resetSegmentStyles();

        bars.getChildren().addAll(seg1, seg2, seg3);

        strengthLabel = new Label("Must be min 8 chars with letters & numbers");
        strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        box.getChildren().addAll(bars, strengthLabel);
        return box;
    }

    private void resetSegmentStyles() {
        String base = "-fx-background-color: #E2E8F0; -fx-background-radius: 2;";
        seg1.setStyle(base);
        seg2.setStyle(base);
        seg3.setStyle(base);
    }

    private void updateStrengthIndicator(String password) {
        if (password == null || password.isEmpty()) {
            resetSegmentStyles();
            strengthLabel.setText("Must be min 8 chars with letters & numbers");
            strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
            return;
        }

        boolean hasLength = password.length() >= 8;
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");

        int score = 0;
        if (hasLength) score++;
        if (hasLetter && hasNumber) score++;
        if (score == 2 && (password.length() >= 12 || hasSpecial)) score++;

        String bgInactive = "-fx-background-color: #E2E8F0; -fx-background-radius: 2;";
        String bgWeak = "-fx-background-color: #EF4444; -fx-background-radius: 2;";
        String bgFair = "-fx-background-color: #F59E0B; -fx-background-radius: 2;";
        String bgStrong = "-fx-background-color: #10B981; -fx-background-radius: 2;";

        switch (score) {
            case 1 -> {
                seg1.setStyle(bgWeak);
                seg2.setStyle(bgInactive);
                seg3.setStyle(bgInactive);
                strengthLabel.setText("Weak (needs letter and number, 8+ chars)");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444; -fx-font-weight: 600;");
            }
            case 2 -> {
                seg1.setStyle(bgFair);
                seg2.setStyle(bgFair);
                seg3.setStyle(bgInactive);
                strengthLabel.setText("Fair (add symbols or length for stronger password)");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #F59E0B; -fx-font-weight: 600;");
            }
            case 3 -> {
                seg1.setStyle(bgStrong);
                seg2.setStyle(bgStrong);
                seg3.setStyle(bgStrong);
                strengthLabel.setText("Strong password");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981; -fx-font-weight: 700;");
            }
            default -> {
                seg1.setStyle(bgWeak);
                seg2.setStyle(bgInactive);
                seg3.setStyle(bgInactive);
                strengthLabel.setText("Too weak");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444;");
            }
        }
    }

    private Button createEyeToggleButton(PasswordField pwField, TextField visField) {
        Button btn = new Button();
        btn.getStyleClass().add("auth-eye-btn");

        SVGPath eye = new SVGPath();
        eye.setContent(SVG_EYE);
        eye.setStyle("-fx-fill: -text-muted;");
        btn.setGraphic(eye);

        btn.setOnAction(e -> {
            boolean visible = visField.isVisible();
            if (visible) {
                visField.setVisible(false);
                visField.setManaged(false);
                pwField.setVisible(true);
                pwField.setManaged(true);
                eye.setContent(SVG_EYE);
                pwField.requestFocus();
                pwField.positionCaret(pwField.getText().length());
            } else {
                pwField.setVisible(false);
                pwField.setManaged(false);
                visField.setVisible(true);
                visField.setManaged(true);
                eye.setContent(SVG_EYE_OFF);
                visField.requestFocus();
                visField.positionCaret(visField.getText().length());
            }
        });

        return btn;
    }

    private void handleJoin() {
        String code = codeField.getText() != null ? codeField.getText().trim() : "";
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (code.isEmpty()) {
            showError("Invitation code is required.");
            codeField.requestFocus();
            return;
        }

        if (name.isEmpty()) {
            showError("Full name is required.");
            nameField.requestFocus();
            return;
        }

        if (email.isEmpty() || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showError("Please enter a valid work email address.");
            emailField.requestFocus();
            return;
        }

        if (pass == null || pass.length() < 8 || !pass.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            showError("Password must be at least 8 characters long and contain at least one letter and one number.");
            passwordField.requestFocus();
            return;
        }

        if (!pass.equals(confirm)) {
            showError("Passwords do not match.");
            confirmPasswordField.requestFocus();
            return;
        }

        setLoading(true);

        AppExecutor.execute(() -> {
            try {
                User user = authService.redeemCode(code, name, email, pass, confirm);

                Platform.runLater(() -> {
                    setLoading(false);
                    ViewNavigator.showOverlayDialog(
                            "Welcome to the Team!",
                            "Your account (" + user.getEmail() + ") is now activated with role " + user.getRole().name() + ".\n\n"
                                    + "You can now sign in using your credentials.",
                            ViewNavigator.OverlayType.SUCCESS,
                            () -> {
                                if (onNavigateLogin != null) {
                                    onNavigateLogin.run();
                                } else {
                                    ViewNavigator.showLogin();
                                }
                            }
                    );
                });

            } catch (AuthService.ValidationException ve) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(ve.getMessage());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(ex.getMessage() != null ? ex.getMessage() : "Error completing registration.");
                });
            }
        });
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
        joinButton.setDisable(loading);
        codeField.setDisable(loading);
        nameField.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        visiblePasswordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        visibleConfirmPasswordField.setDisable(loading);

        if (loading) {
            joinButton.setText("Activating Access...");
            joinButton.setGraphic(loadingIndicator);
            loadingIndicator.setVisible(true);
            loadingIndicator.setManaged(true);
            clearError();
        } else {
            joinButton.setText("Join Organization");
            joinButton.setGraphic(null);
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
    }

    @Override
    public void dispose() {
        if (passwordField != null) passwordField.clear();
        if (visiblePasswordField != null) visiblePasswordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (visibleConfirmPasswordField != null) visibleConfirmPasswordField.clear();
    }
}
