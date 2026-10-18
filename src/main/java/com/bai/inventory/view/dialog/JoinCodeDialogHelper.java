package com.bai.inventory.view.dialog;

import com.bai.inventory.model.JoinCode;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.JoinCodeService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

public class JoinCodeDialogHelper {

    private static final String SVG_PLUS = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_CLOSE = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";
    private static final String SVG_COPY = "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z";

    private static Timeline currentCountdownTimeline;

    public static void showAddTeamMemberModal(Runnable onSuccess) {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.OWNER) {
            ViewNavigator.showToast("Only the Business Owner can generate team join codes.", true);
            return;
        }

        VBox card = new VBox(16);
        card.getStyleClass().add("dialog-modal-card");
        card.setPrefWidth(560);
        card.setMaxWidth(560);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        BorderPane header = new BorderPane();
        header.getStyleClass().add("dialog-header");

        HBox titleLeft = new HBox(10);
        titleLeft.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = new SVGPath();
        icon.setContent(SVG_PLUS);
        icon.getStyleClass().add("dialog-icon-info");

        Label titleLabel = new Label("Add Team Member");
        titleLabel.getStyleClass().add("dialog-title");
        titleLeft.getChildren().addAll(icon, titleLabel);
        header.setLeft(titleLeft);

        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("btn-dialog-close");
        SVGPath closeIcon = new SVGPath();
        closeIcon.setContent(SVG_CLOSE);
        closeIcon.getStyleClass().add("dialog-close-icon");
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());
        header.setRight(closeBtn);

        VBox body = new VBox(14);
        body.getStyleClass().add("dialog-body");

        Label desc = new Label("Generate a 10-minute short-lived join code. When staff enter this code on the Join screen, their account is instantly activated.");
        desc.setWrapText(true);
        desc.getStyleClass().add("dialog-body-text");

        Label roleHeader = new Label("Select Staff Role");
        roleHeader.getStyleClass().add("text-label");

        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton employeeRadio = new RadioButton("Store Associate (EMPLOYEE)");
        employeeRadio.setToggleGroup(roleGroup);
        employeeRadio.setSelected(true);

        Label empDesc = new Label("Can deduct inventory (Sold, Used, Damaged, Expired) and view inventory grid on the Store Terminal.");
        empDesc.setWrapText(true);
        empDesc.getStyleClass().add("form-helper");

        VBox empBox = new VBox(4, employeeRadio, empDesc);

        RadioButton managerRadio = new RadioButton("Inventory Manager (MANAGER)");
        managerRadio.setToggleGroup(roleGroup);

        Label mgrDesc = new Label("Full access to Dashboard, Product catalog, stock adjustments, par-level alerts, audits, and cost analyses.");
        mgrDesc.setWrapText(true);
        mgrDesc.getStyleClass().add("form-helper");

        VBox mgrBox = new VBox(4, managerRadio, mgrDesc);

        VBox radioBox = new VBox(12, empBox, mgrBox);

        Label notice = new Label("• Codes expire strictly in 10 minutes from generation.\n• Max 5 active codes allowed per business.\n• For security, a business can never generate another OWNER.");
        notice.setWrapText(true);
        notice.getStyleClass().add("form-helper");

        body.getChildren().addAll(desc, roleHeader, radioBox, notice);

        HBox footer = new HBox(12);
        footer.getStyleClass().add("dialog-footer");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(20, 20);
        spinner.setVisible(false);

        Button generateBtn = new Button("Generate Join Code");
        generateBtn.getStyleClass().add("btn-dialog-confirm");
        generateBtn.setOnAction(e -> {
            Role targetRole = employeeRadio.isSelected() ? Role.EMPLOYEE : Role.MANAGER;
            generateBtn.setDisable(true);
            cancelBtn.setDisable(true);
            spinner.setVisible(true);

            AppExecutor.execute(() -> {
                try {
                    JoinCodeService service = new JoinCodeService();
                    JoinCodeService.GeneratedCodeResult result = service.generateCode(currentUser.getId(), targetRole);
                    Platform.runLater(() -> {
                        showGeneratedCodeModal(result.rawCodeFormatted(), result.joinCode(), onSuccess);
                        if (onSuccess != null) onSuccess.run();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        generateBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                        spinner.setVisible(false);
                        ViewNavigator.showToast(ex.getMessage() != null ? ex.getMessage() : "Error generating join code.", true);
                    });
                }
            });
        });

        footer.getChildren().addAll(spinner, cancelBtn, generateBtn);
        card.getChildren().addAll(header, body, footer);

        ViewNavigator.showCustomDialog(card);
        Platform.runLater(generateBtn::requestFocus);
    }

    public static void showGeneratedCodeModal(String formattedCode, JoinCode createdCode, Runnable onCloseCallback) {
        if (currentCountdownTimeline != null) {
            currentCountdownTimeline.stop();
        }

        VBox card = new VBox(18);
        card.getStyleClass().add("dialog-modal-card");
        card.setPrefWidth(480);
        card.setMaxWidth(480);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        BorderPane header = new BorderPane();
        header.getStyleClass().add("dialog-header");

        HBox titleLeft = new HBox(10);
        titleLeft.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Staff Join Code");
        titleLabel.getStyleClass().add("dialog-title");
        titleLeft.getChildren().add(titleLabel);
        header.setLeft(titleLeft);

        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("btn-dialog-close");
        SVGPath closeIcon = new SVGPath();
        closeIcon.setContent(SVG_CLOSE);
        closeIcon.getStyleClass().add("dialog-close-icon");
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(e -> {
            stopActiveCountdown();
            ViewNavigator.hideOverlayDialog();
            if (onCloseCallback != null) onCloseCallback.run();
        });
        header.setRight(closeBtn);

        VBox body = new VBox(14);
        body.getStyleClass().add("dialog-body");
        body.setAlignment(Pos.CENTER);

        VBox codeBox = new VBox(6);
        codeBox.getStyleClass().add("join-code-display-box");

        Label codeLabel = new Label(formattedCode);
        codeLabel.getStyleClass().add("join-code-display-text");

        Label countdownLabel = new Label("Expires in 10:00");
        countdownLabel.getStyleClass().add("join-code-countdown-text");

        codeBox.getChildren().addAll(codeLabel, countdownLabel);

        final int[] secondsRemaining = { 600 };
        currentCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            secondsRemaining[0]--;
            if (secondsRemaining[0] <= 0) {
                countdownLabel.setText("Expired");
                stopActiveCountdown();
            } else {
                int mins = secondsRemaining[0] / 60;
                int secs = secondsRemaining[0] % 60;
                countdownLabel.setText(String.format("Expires in %02d:%02d", mins, secs));
            }
        }));
        currentCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        currentCountdownTimeline.play();

        Label instructionLabel = new Label("Ask your staff to open bAI and click 'Join with a code'.");
        instructionLabel.getStyleClass().add("join-code-instruction-text");

        HBox copyBox = new HBox(10);
        copyBox.setAlignment(Pos.CENTER);

        Button copyBtn = new Button("Copy Code");
        copyBtn.getStyleClass().add("btn-secondary");
        SVGPath copyIcon = new SVGPath();
        copyIcon.setContent(SVG_COPY);
        copyIcon.getStyleClass().add("sidebar-item-icon");
        copyBtn.setGraphic(copyIcon);

        Tooltip copyTooltip = new Tooltip("Copy to clipboard");
        Tooltip.install(copyBtn, copyTooltip);

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(formattedCode);
            clipboard.setContent(content);

            copyBtn.setText("Copied!");
            copyTooltip.setText("Copied!");
            copyTooltip.show(copyBtn, copyBtn.getScene().getWindow().getX() + copyBtn.localToScene(0, 0).getX(),
                    copyBtn.getScene().getWindow().getY() + copyBtn.localToScene(0, 0).getY() - 30);

            Timeline revert = new Timeline(new KeyFrame(Duration.seconds(2), t -> {
                copyTooltip.hide();
                copyTooltip.setText("Copy to clipboard");
            }));
            revert.play();

            // Button resets after 60s, and clipboard is cleared in 60s
            Timeline resetTimeline = new Timeline(new KeyFrame(Duration.seconds(60), t -> {
                copyBtn.setText("Copy Code");
                Clipboard clip = Clipboard.getSystemClipboard();
                if (clip.hasString() && formattedCode.equals(clip.getString())) {
                    clip.clear();
                }
            }));
            resetTimeline.setCycleCount(1);
            resetTimeline.play();
        });

        copyBox.getChildren().add(copyBtn);

        body.getChildren().addAll(codeBox, instructionLabel, copyBox);

        HBox footer = new HBox(12);
        footer.getStyleClass().add("dialog-footer");

        Button doneBtn = new Button("Done");
        doneBtn.getStyleClass().add("btn-dialog-confirm");
        doneBtn.setOnAction(e -> {
            stopActiveCountdown();
            ViewNavigator.hideOverlayDialog();
            if (onCloseCallback != null) onCloseCallback.run();
        });

        footer.getChildren().add(doneBtn);
        card.getChildren().addAll(header, body, footer);

        ViewNavigator.showCustomDialog(card);
        Platform.runLater(doneBtn::requestFocus);
    }

    public static void stopActiveCountdown() {
        if (currentCountdownTimeline != null) {
            currentCountdownTimeline.stop();
            currentCountdownTimeline = null;
        }
    }
}
