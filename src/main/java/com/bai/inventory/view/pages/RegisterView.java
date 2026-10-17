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

public class RegisterView extends BorderPane implements Disposable {

    private static final String SVG_CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    private static final String SVG_EYE = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String SVG_EYE_OFF = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27z";

    private final AuthService authService;

    private TextField businessNameField;
    private TextField nameField;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private PasswordField confirmPasswordField;
    private TextField visibleConfirmPasswordField;
    private Button registerButton;
    private ProgressIndicator loadingIndicator;
    private Label errorLabel;

    private Region seg1;
    private Region seg2;
    private Region seg3;
    private Label strengthLabel;

    private Runnable onNavigateLogin;

    public RegisterView() {
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

        Label badge = new Label("BUSINESS OWNER PORTAL");
        badge.getStyleClass().add("hero-badge");

        Label title = new Label("Register Your Retail Business on bAI");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label("Set up your retail organization, manage stock and catalog in Philippine Pesos (₱), and invite staff securely using 10-minute join codes.");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(12);
        features.getChildren().addAll(
                createFeatureItem("Multi-Tenant Store Management", "Complete ownership of store catalog, categories, pricing, and par levels."),
                createFeatureItem("10-Minute Staff Join Codes", "Add employees and managers with short-lived secure codes. No invite links or approval queues."),
                createFeatureItem("Live Velocity & Stock Audits", "Real-time stock deduction tracking and shift reconciliation for total inventory control.")
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

        VBox formOuter = new VBox();
        formOuter.getStyleClass().add("form-pane");
        formOuter.setAlignment(Pos.CENTER);

        VBox formContent = new VBox(14);
        formContent.getStyleClass().add("form-card");
        formContent.setMaxWidth(440);

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

        Label appTitle = new Label("bAI");
        appTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: -primary;");

        Label badge = new Label("OWNER SETUP");
        badge.getStyleClass().add("brand-tag");

        brandRow.getChildren().addAll(appTitle, badge);

        VBox titleBox = new VBox(4);
        Label headerTitle = new Label("Create Business Account");
        headerTitle.getStyleClass().add("auth-title");

        Label headerSubtitle = new Label("Register your store to start tracking stock and inviting staff.");
        headerSubtitle.getStyleClass().add("auth-subtitle");
        headerSubtitle.setWrapText(true);
        titleBox.getChildren().addAll(headerTitle, headerSubtitle);

        Label businessLabel = new Label("Business / Store Name *");
        businessLabel.getStyleClass().add("field-label");

        businessNameField = new TextField();
        businessNameField.setPromptText("e.g. Metro Retail Hub");
        businessNameField.getStyleClass().add("input-box");

        Label nameLabel = new Label("Owner Full Name *");
        nameLabel.getStyleClass().add("field-label");

        nameField = new TextField();
        nameField.setPromptText("e.g. Juan Dela Cruz");
        nameField.getStyleClass().add("input-box");

        Label emailLabel = new Label("Work Email Address *");
        emailLabel.getStyleClass().add("field-label");

        emailField = new TextField();
        emailField.setPromptText("owner@store.com");
        emailField.getStyleClass().add("input-box");

        Label passLabel = new Label("Password (min. 8 characters, letters & numbers) *");
        passLabel.getStyleClass().add("field-label");

        StackPane passWrapper = new StackPane();
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.getStyleClass().add("input-box");

        visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("••••••••");
        visiblePasswordField.getStyleClass().add("input-box");
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordStrength(newVal));

        Button eyeBtn1 = createEyeButton(passwordField, visiblePasswordField);
        StackPane.setAlignment(eyeBtn1, Pos.CENTER_RIGHT);
        StackPane.setMargin(eyeBtn1, new Insets(0, 8, 0, 0));

        passWrapper.getChildren().addAll(passwordField, visiblePasswordField, eyeBtn1);

        Node strengthIndicator = createStrengthIndicator();

        Label confirmLabel = new Label("Confirm Password *");
        confirmLabel.getStyleClass().add("field-label");

        StackPane confirmWrapper = new StackPane();
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("••••••••");
        confirmPasswordField.getStyleClass().add("input-box");

        visibleConfirmPasswordField = new TextField();
        visibleConfirmPasswordField.setPromptText("••••••••");
        visibleConfirmPasswordField.getStyleClass().add("input-box");
        visibleConfirmPasswordField.setVisible(false);
        visibleConfirmPasswordField.setManaged(false);

        confirmPasswordField.textProperty().bindBidirectional(visibleConfirmPasswordField.textProperty());

        Button eyeBtn2 = createEyeButton(confirmPasswordField, visibleConfirmPasswordField);
        StackPane.setAlignment(eyeBtn2, Pos.CENTER_RIGHT);
        StackPane.setMargin(eyeBtn2, new Insets(0, 8, 0, 0));

        confirmWrapper.getChildren().addAll(confirmPasswordField, visibleConfirmPasswordField, eyeBtn2);

        HBox termsRow = new HBox(4);
        termsRow.setAlignment(Pos.CENTER_LEFT);
        Label termsPrefix = new Label("By registering, you agree to the");
        termsPrefix.setStyle("-fx-font-size: 11.5px; -fx-text-fill: -text-muted;");
        Hyperlink termsLink = new Hyperlink("Terms of Service");
        termsLink.setStyle("-fx-font-size: 11.5px; -fx-padding: 0;");
        termsLink.setOnAction(e -> {
            ViewNavigator.showOverlayDialog(
                    "Terms of Service & Privacy Policy",
                    "bAI Retail Assistant Terms of Service:\n\n"
                            + "1. Data Ownership: Your store's inventory, pricing, and movement logs remain your sole property with Row-Level Security tenant isolation.\n\n"
                            + "2. Access Control: As Business Owner, you are responsible for authorized code generation and role assignments.\n\n"
                            + "3. Integrity: All stock deductions, audits, and movements are immutably logged for shift accountability.",
                    ViewNavigator.OverlayType.INFO,
                    null
            );
        });
        termsRow.getChildren().addAll(termsPrefix, termsLink);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);

        registerButton = new Button("Register Business");
        registerButton.getStyleClass().add("btn-auth-submit");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setOnAction(e -> handleRegister());

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(24, 24);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);

        StackPane submitStack = new StackPane(registerButton, loadingIndicator);

        VBox footerBox = new VBox(6);
        footerBox.getStyleClass().add("auth-footer");
        footerBox.setAlignment(Pos.CENTER);

        HBox loginRow = new HBox(4);
        loginRow.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Already have an account?");
        hasAccount.getStyleClass().add("auth-footer-text");
        Hyperlink signinLink = new Hyperlink("Sign In");
        signinLink.getStyleClass().add("auth-link");
        signinLink.setOnAction(e -> {
            if (onNavigateLogin != null) onNavigateLogin.run();
            else ViewNavigator.showLogin();
        });
        loginRow.getChildren().addAll(hasAccount, signinLink);

        HBox joinRow = new HBox(4);
        joinRow.setAlignment(Pos.CENTER);
        Label staffLabel = new Label("Staff member?");
        staffLabel.getStyleClass().add("auth-footer-text");
        Hyperlink joinLink = new Hyperlink("Join with a code");
        joinLink.getStyleClass().add("auth-link");
        joinLink.setOnAction(e -> ViewNavigator.showJoin());
        joinRow.getChildren().addAll(staffLabel, joinLink);

        footerBox.getChildren().addAll(loginRow, joinRow);

        formContent.getChildren().addAll(
                brandRow,
                titleBox,
                businessLabel, businessNameField,
                nameLabel, nameField,
                emailLabel, emailField,
                passLabel, passWrapper,
                strengthIndicator,
                confirmLabel, confirmWrapper,
                termsRow,
                errorLabel,
                submitStack,
                footerBox
        );

        formOuter.getChildren().add(formContent);
        scrollPane.setContent(formOuter);
        return scrollPane;
    }

    private Button createEyeButton(PasswordField pf, TextField tf) {
        Button btn = new Button();
        btn.getStyleClass().add("eye-button");
        SVGPath icon = new SVGPath();
        icon.setContent(SVG_EYE);
        icon.setStyle("-fx-fill: -text-muted;");
        btn.setGraphic(icon);

        btn.setOnAction(e -> {
            boolean visible = tf.isVisible();
            if (visible) {
                tf.setVisible(false);
                tf.setManaged(false);
                pf.setVisible(true);
                pf.setManaged(true);
                icon.setContent(SVG_EYE);
                pf.requestFocus();
                pf.positionCaret(pf.getText().length());
            } else {
                pf.setVisible(false);
                pf.setManaged(false);
                tf.setVisible(true);
                tf.setManaged(true);
                icon.setContent(SVG_EYE_OFF);
                tf.requestFocus();
                tf.positionCaret(tf.getText().length());
            }
        });
        return btn;
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

        strengthLabel = new Label("Password must contain at least 8 characters, a letter and a number.");
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

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            resetSegmentStyles();
            strengthLabel.setText("Password must contain at least 8 characters, a letter and a number.");
            strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
            return;
        }

        boolean hasMinLength = password.length() >= 8;
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");

        int score = 0;
        if (hasMinLength) score++;
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
                strengthLabel.setText("Weak - Must have at least 8 characters, letter and number.");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444; -fx-font-weight: 700;");
            }
            case 2 -> {
                seg1.setStyle(bgFair);
                seg2.setStyle(bgFair);
                seg3.setStyle(bgInactive);
                strengthLabel.setText("Good - Add special characters or more length for Strong.");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #F59E0B; -fx-font-weight: 700;");
            }
            case 3 -> {
                seg1.setStyle(bgStrong);
                seg2.setStyle(bgStrong);
                seg3.setStyle(bgStrong);
                strengthLabel.setText("Strong password.");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981; -fx-font-weight: 700;");
            }
            default -> {
                seg1.setStyle(bgWeak);
                seg2.setStyle(bgInactive);
                seg3.setStyle(bgInactive);
                strengthLabel.setText("Too short.");
                strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444;");
            }
        }
    }

    private void handleRegister() {
        String businessName = businessNameField.getText() != null ? businessNameField.getText().trim() : "";
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (businessName.isEmpty()) {
            showError("Business / Store name is required.");
            businessNameField.requestFocus();
            return;
        }
        if (name.isEmpty()) {
            showError("Owner full name is required.");
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
                authService.registerBusinessOwner(businessName, name, email, pass, confirm, true);

                Platform.runLater(() -> {
                    setLoading(false);
                    ViewNavigator.showOverlayDialog(
                            "Business Registered Successfully!",
                            "Your business account (" + email + ") is now ready with role OWNER.\n\n"
                                    + "You have full administrator privileges to configure inventory and invite staff using 10-minute join codes.",
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
                    showError(ex.getMessage() != null ? ex.getMessage() : "Error creating business account.");
                });
            }
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void setLoading(boolean loading) {
        registerButton.setDisable(loading);
        businessNameField.setDisable(loading);
        nameField.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        visiblePasswordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        visibleConfirmPasswordField.setDisable(loading);

        if (loading) {
            registerButton.setText("Creating Business Account...");
            loadingIndicator.setVisible(true);
            loadingIndicator.setManaged(true);
            clearError();
        } else {
            registerButton.setText("Register Business");
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
