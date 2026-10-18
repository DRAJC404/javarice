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

    private StackPane strengthTrack;
    private Region strengthFill;
    private Label strengthStatusLabel;
    private Label strengthHintLabel;

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

        Label badge = new Label("BUSINESS OWNER PORTAL");
        badge.getStyleClass().add("hero-badge");

        Label title = new Label("Register Your Retail Business on bAI");
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);

        Label subtitle = new Label("Set up your retail organization, manage stock and catalog in Philippine Pesos (₱), and invite staff securely using 10-minute join codes.");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);

        VBox features = new VBox(16);
        features.getChildren().addAll(
                createFeatureItem("Multi-Tenant Store Management", "Complete ownership of store catalog, categories, pricing, and par levels."),
                createFeatureItem("10-Minute Staff Join Codes", "Add employees and managers with short-lived secure codes. No invite links or approval queues."),
                createFeatureItem("Live Velocity & Stock Audits", "Real-time stock deduction tracking and shift reconciliation for total inventory control.")
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

        VBox formContent = new VBox(14);
        formContent.setMaxWidth(408);
        formContent.setAlignment(Pos.CENTER_LEFT);

        HBox brandRow = new HBox(10);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        try (InputStream is = getClass().getResourceAsStream("/images/bailogo.png")) {
            if (is != null) {
                ImageView logoImg = new ImageView(new Image(is));
                logoImg.setFitHeight(40);
                logoImg.setPreserveRatio(true);
                brandRow.getChildren().add(logoImg);
            }
        } catch (Exception ignored) {}

        Label appTitle = new Label("bAI");
        appTitle.getStyleClass().add("brand-wordmark");

        brandRow.getChildren().add(appTitle);

        VBox titleBox = new VBox(4);
        Label headerTitle = new Label("Create Business Account");
        headerTitle.getStyleClass().add("auth-title");

        Label headerSubtitle = new Label("Register your store to start tracking stock and inviting staff.");
        headerSubtitle.getStyleClass().add("auth-subtitle");
        headerSubtitle.setWrapText(true);
        titleBox.getChildren().addAll(headerTitle, headerSubtitle);

        HBox businessLabel = createFieldLabel("Business / Store Name", true);

        businessNameField = new TextField();
        businessNameField.setPromptText("e.g. Metro Retail Hub");
        businessNameField.getStyleClass().add("input-box");

        HBox nameLabel = createFieldLabel("Owner Full Name", true);

        nameField = new TextField();
        nameField.setPromptText("e.g. Juan Dela Cruz");
        nameField.getStyleClass().add("input-box");

        HBox emailLabel = createFieldLabel("Work Email Address", true);

        emailField = new TextField();
        emailField.setPromptText("owner@store.com");
        emailField.getStyleClass().add("input-box");

        HBox passLabel = createFieldLabel("Password (min. 8 characters, letters & numbers)", true);

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

        HBox confirmLabel = createFieldLabel("Confirm Password", true);

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
        termsPrefix.getStyleClass().add("auth-footer-text");
        Hyperlink termsLink = new Hyperlink("Terms of Service");
        termsLink.getStyleClass().add("auth-link");
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

        VBox businessGroup = new VBox(4, businessLabel, businessNameField);
        VBox nameGroup = new VBox(4, nameLabel, nameField);
        VBox emailGroup = new VBox(4, emailLabel, emailField);
        VBox passGroup = new VBox(4, passLabel, passWrapper, strengthIndicator);
        VBox confirmGroup = new VBox(4, confirmLabel, confirmWrapper);

        formContent.getChildren().addAll(
                brandRow,
                titleBox,
                businessGroup,
                nameGroup,
                emailGroup,
                passGroup,
                confirmGroup,
                termsRow,
                errorLabel,
                submitStack,
                footerBox
        );

        formOuter.getChildren().add(formContent);
        scrollPane.setContent(formOuter);
        return scrollPane;
    }

    private HBox createFieldLabel(String text, boolean required) {
        HBox row = new HBox(2);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(text);
        lbl.getStyleClass().add("text-label");
        row.getChildren().add(lbl);
        if (required) {
            Label star = new Label(" *");
            star.getStyleClass().addAll("text-label", "form-label-required");
            row.getChildren().add(star);
        }
        return row;
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
        VBox box = new VBox(6);

        strengthTrack = new StackPane();
        strengthTrack.getStyleClass().add("strength-track");
        strengthTrack.setAlignment(Pos.CENTER_LEFT);

        strengthFill = new Region();
        strengthFill.getStyleClass().add("strength-fill");
        strengthFill.setPrefWidth(0);
        strengthTrack.getChildren().add(strengthFill);

        HBox labelRow = new HBox(8);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        strengthStatusLabel = new Label("");
        strengthStatusLabel.getStyleClass().add("strength-status");

        strengthHintLabel = new Label("Min. 8 characters with letters & numbers");
        strengthHintLabel.getStyleClass().add("strength-hint");
        strengthHintLabel.setWrapText(false);

        labelRow.getChildren().addAll(strengthStatusLabel, strengthHintLabel);
        box.getChildren().addAll(strengthTrack, labelRow);
        return box;
    }

    private void updatePasswordStrength(String password) {
        if (strengthFill == null || strengthTrack == null) return;
        strengthFill.prefWidthProperty().unbind();

        if (password == null || password.isEmpty()) {
            strengthFill.setPrefWidth(0);
            strengthFill.getStyleClass().removeAll("strength-fill-weak", "strength-fill-good", "strength-fill-strong");
            strengthStatusLabel.setText("");
            strengthStatusLabel.getStyleClass().removeAll("strength-weak-text", "strength-good-text", "strength-strong-text");
            strengthHintLabel.setText("Min. 8 characters with letters & numbers");
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

        strengthFill.getStyleClass().removeAll("strength-fill-weak", "strength-fill-good", "strength-fill-strong");
        strengthStatusLabel.getStyleClass().removeAll("strength-weak-text", "strength-good-text", "strength-strong-text");

        switch (score) {
            case 1 -> {
                strengthFill.prefWidthProperty().bind(strengthTrack.widthProperty().multiply(0.33));
                strengthFill.getStyleClass().add("strength-fill-weak");
                strengthStatusLabel.setText("Weak");
                strengthStatusLabel.getStyleClass().add("strength-weak-text");
                strengthHintLabel.setText("Must have at least 8 characters, letter and number.");
            }
            case 2 -> {
                strengthFill.prefWidthProperty().bind(strengthTrack.widthProperty().multiply(0.66));
                strengthFill.getStyleClass().add("strength-fill-good");
                strengthStatusLabel.setText("Good");
                strengthStatusLabel.getStyleClass().add("strength-good-text");
                strengthHintLabel.setText("Add special characters or more length for Strong.");
            }
            case 3 -> {
                strengthFill.prefWidthProperty().bind(strengthTrack.widthProperty());
                strengthFill.getStyleClass().add("strength-fill-strong");
                strengthStatusLabel.setText("Strong");
                strengthStatusLabel.getStyleClass().add("strength-strong-text");
                strengthHintLabel.setText("Strong password.");
            }
            default -> {
                strengthFill.prefWidthProperty().bind(strengthTrack.widthProperty().multiply(0.20));
                strengthFill.getStyleClass().add("strength-fill-weak");
                strengthStatusLabel.setText("Weak");
                strengthStatusLabel.getStyleClass().add("strength-weak-text");
                strengthHintLabel.setText("Too short.");
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
                            "Business Registered Successfully",
                            "Your business account (" + email + ") is ready. You have full access to manage products and inventory, and you can add your team with 10-minute join codes.",
                            ViewNavigator.OverlayType.SUCCESS,
                            "Continue to sign in",
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
