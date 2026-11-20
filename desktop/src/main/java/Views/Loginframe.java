package Views;

import Models.User;
import Utils.ValidationUtils;

import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.SwingUtilities;
import java.util.regex.Pattern;

public class Loginframe extends Application {

    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass OFF = PseudoClass.getPseudoClass("off");
    private static final PseudoClass SHOWN = PseudoClass.getPseudoClass("shown");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    private Stage stage;

    private TextField emailField;
    private PasswordField passwordField;
    private TextField visibleField;
    private Label errorLabel;

    public static void open() {
        Platform.runLater(() -> new Loginframe().start(new Stage()));
    }

    @Override
    public void start(Stage primaryStage) {

        Platform.setImplicitExit(false);

        this.stage = primaryStage;

        stage.setOnCloseRequest(e -> Platform.exit());

        HBox root = new HBox(buildImagePane(), buildLoginPane());
        root.getStyleClass().add("root-box");

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(
                getClass().getResource("/css/login.css").toExternalForm());

        stage.setTitle("bAI");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildLoginPane() {

        VBox pane = new VBox();
        pane.getStyleClass().add("login-pane");
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.setPrefWidth(450);
        pane.setMinWidth(450);
        pane.setMaxWidth(450);

        Region logo = new Region();
        logo.getStyleClass().add("logo");
        VBox.setMargin(logo, new Insets(0, 0, 0, 0));

        Label title = new Label("HI, welcome to bAI");
        title.getStyleClass().add("title");
        VBox.setMargin(title, new Insets(0, 0, 8, 0));

        Label subtitle = new Label("Enter your credentials to access your account");
        subtitle.getStyleClass().add("subtitle");
        VBox.setMargin(subtitle, new Insets(0, 0, 30, 0));

        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("field-label");
        VBox.setMargin(emailLabel, new Insets(0, 0, 8, 0));

        emailField = new TextField();
        emailField.setPromptText("you@example.com");
        emailField.getStyleClass().add("input");
        VBox.setMargin(emailField, new Insets(0, 0, 18, 0));

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("field-label");
        VBox.setMargin(passwordLabel, new Insets(0, 0, 8, 0));

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("plain-field");

        visibleField = new TextField();
        visibleField.setPromptText("Enter your password");
        visibleField.getStyleClass().add("plain-field");
        visibleField.setVisible(false);
        visibleField.setManaged(false);

        visibleField.textProperty().bindBidirectional(passwordField.textProperty());

        StackPane fieldStack = new StackPane(passwordField, visibleField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        Region eyeIcon = new Region();
        eyeIcon.getStyleClass().add("eye-icon");

        Tooltip eyeTip = new Tooltip("Show password");

        Button eyeButton = new Button();
        eyeButton.setGraphic(eyeIcon);
        eyeButton.getStyleClass().add("eye-button");
        eyeButton.setFocusTraversable(false);
        eyeButton.setTooltip(eyeTip);
        HBox.setMargin(eyeButton, new Insets(0, 6, 0, 0));

        HBox passwordBox = new HBox(fieldStack, eyeButton);
        passwordBox.getStyleClass().add("field-box");
        passwordBox.setAlignment(Pos.CENTER_LEFT);

        eyeButton.setOnAction(e -> {
            boolean show = !visibleField.isVisible();

            visibleField.setVisible(show);
            visibleField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);

            eyeIcon.pseudoClassStateChanged(OFF, show);
            eyeButton.pseudoClassStateChanged(SHOWN, show);
            eyeTip.setText(show ? "Hide password" : "Show password");

            ScaleTransition pop = new ScaleTransition(Duration.millis(150), eyeIcon);
            pop.setFromX(0.65);
            pop.setFromY(0.65);
            pop.setToX(1.0);
            pop.setToY(1.0);
            pop.play();

            TextField active = show ? visibleField : passwordField;
            active.requestFocus();
            active.positionCaret(active.getText().length());
        });

        Runnable updateFocus = () -> passwordBox.pseudoClassStateChanged(
                ACTIVE, passwordField.isFocused() || visibleField.isFocused());
        passwordField.focusedProperty().addListener((o, a, b) -> updateFocus.run());
        visibleField.focusedProperty().addListener((o, a, b) -> updateFocus.run());

        errorLabel = new Label(" ");
        errorLabel.getStyleClass().add("error");
        VBox.setMargin(errorLabel, new Insets(8, 0, 8, 0));

        Button loginButton = new Button("Sign In");
        loginButton.getStyleClass().add("sign-in");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> login());

        emailField.setOnAction(e -> focusPassword());
        passwordField.setOnAction(e -> login());
        visibleField.setOnAction(e -> login());

        Label noAccount = new Label("Don't have an account?");
        noAccount.getStyleClass().add("muted-text");

        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.getStyleClass().add("register-link");
        registerLink.setFocusTraversable(false);
        registerLink.setOnAction(e -> openRegister());

        HBox registerRow = new HBox(2, noAccount, registerLink);
        registerRow.setAlignment(Pos.CENTER);
        VBox.setMargin(registerRow, new Insets(18, 0, 0, 0));

        pane.getChildren().addAll(
                logo,
                title, subtitle,
                emailLabel, emailField,
                passwordLabel, passwordBox,
                errorLabel,
                loginButton,
                registerRow);

        return pane;
    }

    private StackPane buildImagePane() {

        Region tint = new Region();
        tint.getStyleClass().add("image-tint");

        Region vignette = new Region();
        vignette.getStyleClass().add("image-vignette");

        StackPane pane = new StackPane(tint, vignette);
        pane.getStyleClass().add("image-pane");
        pane.setPrefWidth(450);
        HBox.setHgrow(pane, Priority.ALWAYS);

        return pane;
    }

    private void openRegister() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Register");
        alert.setHeaderText(null);
        alert.setContentText("The registration screen is coming soon.");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void login() {

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (ValidationUtils.isEmpty(email)) {
            showError("Email is required.");
            emailField.requestFocus();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            emailField.requestFocus();
            return;
        }

        if (ValidationUtils.isEmpty(password)) {
            showError("Password is required.");
            focusPassword();
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showError("Password must be at least 4 characters.");
            focusPassword();
            return;
        }

        User user = new User(
                "admin@gmail.com",
                "1234",
                "Administrator");

        if (email.equalsIgnoreCase(user.getUsername())
                && password.equals(user.getPassword())) {

            errorLabel.setText(" ");

            SwingUtilities.invokeLater(() -> new DashboardFrame(user).setVisible(true));

            stage.hide();

        } else {

            showError("Invalid email or password.");
            passwordField.clear();
            focusPassword();
        }
    }

    private void focusPassword() {
        (visibleField.isVisible() ? visibleField : passwordField).requestFocus();
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}