package com.bai.inventory.view.pages;

import com.bai.inventory.dao.UserDao;
import com.bai.inventory.model.JoinCode;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.JoinCodeService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeamView extends BorderPane implements Disposable {

    private static final String SVG_PLUS = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_REFRESH = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_COPY = "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z";
    private static final String SVG_CANCEL = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final JoinCodeService joinCodeService;
    private final UserDao userDao;

    private TableView<JoinCode> codesTable;
    private TableView<User> usersTable;
    private ProgressIndicator loadingIndicator;

    private Timeline activeCountdownTimeline;

    public TeamView() {
        this.joinCodeService = new JoinCodeService();
        this.userDao = new UserDao();
        buildUI();
        loadData();
    }

    private void buildUI() {
        getStyleClass().add("dashboard-root");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("dashboard-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox contentBox = new VBox(24);
        contentBox.setPadding(new Insets(28));
        contentBox.setFillWidth(true);

        Node header = buildHeader();

        Node codesSection = buildCodesSection();

        Node usersSection = buildUsersSection();

        contentBox.getChildren().addAll(header, codesSection, usersSection);
        scrollPane.setContent(contentBox);
        setCenter(scrollPane);
    }

    private Node buildHeader() {
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Team & Staff Access");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: -text-main;");

        Label subtitle = new Label("Generate 10-minute secure join codes for onboarding and view store staff.");
        subtitle.setStyle("-fx-font-size: 13.5px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(20, 20);
        loadingIndicator.setVisible(false);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        SVGPath refIcon = new SVGPath();
        refIcon.setContent(SVG_REFRESH);
        refIcon.setStyle("-fx-fill: -text-muted;");
        refreshBtn.setGraphic(refIcon);
        refreshBtn.setOnAction(e -> loadData());

        Button addMemberBtn = new Button("Add Team Member");
        addMemberBtn.getStyleClass().add("btn-primary");
        SVGPath addIcon = new SVGPath();
        addIcon.setContent(SVG_PLUS);
        addIcon.setStyle("-fx-fill: white;");
        addMemberBtn.setGraphic(addIcon);
        addMemberBtn.setOnAction(e -> showAddTeamMemberModal());

        headerRow.getChildren().addAll(titleBox, spacer, loadingIndicator, refreshBtn, addMemberBtn);
        return headerRow;
    }

    private Node buildCodesSection() {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: -border-subtle; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.04), 12, 0, 0, 3);");

        HBox sectionHeader = new HBox(12);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        VBox titleCol = new VBox(2);
        Label secTitle = new Label("10-Minute Invitation Codes");
        secTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        Label secDesc = new Label("Short-lived single-use onboarding codes. Raw codes are never stored; only active/used status is tracked.");
        secDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        titleCol.getChildren().addAll(secTitle, secDesc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label limitBadge = new Label("Max 5 Active Codes");
        limitBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -primary; -fx-background-color: -primary-light; -fx-padding: 3 8; -fx-background-radius: 6;");

        sectionHeader.getChildren().addAll(titleCol, spacer, limitBadge);

        codesTable = new TableView<>();
        codesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        codesTable.setPrefHeight(220);
        codesTable.setPlaceholder(new Label("No join codes generated yet. Click 'Add Team Member' to create one."));

        TableColumn<JoinCode, String> idCol = new TableColumn<>("Code Ref");
        idCol.setCellValueFactory(data -> new SimpleStringProperty("#JC-" + data.getValue().getId()));
        idCol.setPrefWidth(100);

        TableColumn<JoinCode, String> roleCol = new TableColumn<>("Role Assigned");
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String roleStr, boolean empty) {
                super.updateItem(roleStr, empty);
                if (empty || roleStr == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(roleStr);
                    if ("MANAGER".equalsIgnoreCase(roleStr)) {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #B45309; -fx-background-color: #FEF3C7; -fx-padding: 2 8; -fx-background-radius: 4;");
                    } else {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #1D4ED8; -fx-background-color: #DBEAFE; -fx-padding: 2 8; -fx-background-radius: 4;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        roleCol.setPrefWidth(130);

        TableColumn<JoinCode, String> createdCol = new TableColumn<>("Created At");
        createdCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null ? data.getValue().getCreatedAt().format(TIME_FMT) : "-"
        ));
        createdCol.setPrefWidth(160);

        TableColumn<JoinCode, String> expiresCol = new TableColumn<>("Expires At");
        expiresCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getExpiresAt() != null ? data.getValue().getExpiresAt().format(TIME_FMT) : "-"
        ));
        expiresCol.setPrefWidth(160);

        TableColumn<JoinCode, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusString()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    switch (status) {
                        case "PENDING" -> badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #047857; -fx-background-color: #D1FAE5; -fx-padding: 2 8; -fx-background-radius: 4;");
                        case "USED" -> badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #4B5563; -fx-background-color: #E5E7EB; -fx-padding: 2 8; -fx-background-radius: 4;");
                        case "EXPIRED" -> badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #9CA3AF; -fx-background-color: #F3F4F6; -fx-padding: 2 8; -fx-background-radius: 4;");
                        case "CANCELLED" -> badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 2 8; -fx-background-radius: 4;");
                        default -> badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #4B5563;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        statusCol.setPrefWidth(120);

        TableColumn<JoinCode, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel Code");
            {
                cancelBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
                cancelBtn.setOnAction(e -> {
                    JoinCode code = getTableView().getItems().get(getIndex());
                    if (code != null) {
                        handleCancelCode(code);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    JoinCode code = getTableView().getItems().get(getIndex());
                    if (code != null && code.isPending()) {
                        setGraphic(cancelBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        actionCol.setPrefWidth(110);

        codesTable.getColumns().addAll(idCol, roleCol, createdCol, expiresCol, statusCol, actionCol);
        card.getChildren().addAll(sectionHeader, codesTable);
        return card;
    }

    private Node buildUsersSection() {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: -border-subtle; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.04), 12, 0, 0, 3);");

        VBox titleCol = new VBox(2);
        Label secTitle = new Label("Organization Team Members");
        secTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        Label secDesc = new Label("Active store accounts with terminal and management access.");
        secDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        titleCol.getChildren().addAll(secTitle, secDesc);

        usersTable = new TableView<>();
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        usersTable.setPrefHeight(240);
        usersTable.setPlaceholder(new Label("No members found."));

        TableColumn<User, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        idCol.setPrefWidth(60);

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        nameCol.setPrefWidth(180);

        TableColumn<User, String> emailCol = new TableColumn<>("Email Address");
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        emailCol.setPrefWidth(220);

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole() != null ? data.getValue().getRole().name() : "EMPLOYEE"));
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String roleName, boolean empty) {
                super.updateItem(roleName, empty);
                if (empty || roleName == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(roleName);
                    if ("OWNER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #6B21A8; -fx-background-color: #F3E8FF; -fx-padding: 2 8; -fx-background-radius: 4;");
                    } else if ("MANAGER".equalsIgnoreCase(roleName)) {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #B45309; -fx-background-color: #FEF3C7; -fx-padding: 2 8; -fx-background-radius: 4;");
                    } else {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #1D4ED8; -fx-background-color: #DBEAFE; -fx-padding: 2 8; -fx-background-radius: 4;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        roleCol.setPrefWidth(120);

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "ACTIVE" : "INACTIVE"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    if ("ACTIVE".equals(status)) {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #047857; -fx-background-color: #D1FAE5; -fx-padding: 2 8; -fx-background-radius: 4;");
                    } else {
                        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 2 8; -fx-background-radius: 4;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        statusCol.setPrefWidth(100);

        usersTable.getColumns().addAll(idCol, nameCol, emailCol, roleCol, statusCol);
        card.getChildren().addAll(titleCol, usersTable);
        return card;
    }

    private void loadData() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null) return;

        loadingIndicator.setVisible(true);

        AppExecutor.execute(() -> {
            try {
                List<JoinCode> codes = joinCodeService.listCodeHistory(currentUser.getId(), 20);
                List<User> users = userDao.findByOrganization(currentUser.getOrganizationId());

                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    codesTable.getItems().setAll(codes);
                    usersTable.getItems().setAll(users);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    ViewNavigator.showToast("Failed to load team data: " + ex.getMessage(), true);
                });
            }
        });
    }

    private void handleCancelCode(JoinCode code) {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null) return;

        ViewNavigator.showConfirmDialog(
                "Cancel Join Code",
                "Are you sure you want to cancel invitation code #JC-" + code.getId() + "? It will immediately become unusable.",
                "Cancel Code",
                () -> {
                    AppExecutor.execute(() -> {
                        try {
                            boolean ok = joinCodeService.cancelCode(currentUser.getId(), code.getId());
                            Platform.runLater(() -> {
                                if (ok) {
                                    ViewNavigator.showToast("Join code #JC-" + code.getId() + " has been cancelled.", false);
                                    loadData();
                                } else {
                                    ViewNavigator.showToast("Failed to cancel code.", true);
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> ViewNavigator.showToast("Error cancelling code: " + ex.getMessage(), true));
                        }
                    });
                }
        );
    }


    private void showAddTeamMemberModal() {
        VBox card = new VBox(18);
        card.getStyleClass().add("dialog-modal-card");
        card.setMaxWidth(480);
        card.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = new SVGPath();
        icon.setContent(SVG_PLUS);
        icon.setStyle("-fx-fill: -primary;");

        Label title = new Label("Add Team Member");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        header.getChildren().addAll(icon, title);

        Label desc = new Label("Generate a 10-minute short-lived join code. When staff enter this code on the Join page, their account is instantly activated.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-line-spacing: 2px;");

        Label roleHeader = new Label("Select Staff Role:");
        roleHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        ToggleGroup roleGroup = new ToggleGroup();

        RadioButton employeeRadio = new RadioRadioButtonItem(
                "Store Associate (EMPLOYEE)",
                "Can log stock deductions (Sold, Used, Damaged, Expired) and view inventory grid on the Store Terminal.",
                Role.EMPLOYEE,
                roleGroup
        );
        employeeRadio.setSelected(true);

        RadioButton managerRadio = new RadioRadioButtonItem(
                "Inventory Manager (MANAGER)",
                "Full access to Dashboard, Product catalog, stock adjustments, par-level alerts, audits, and cost analyses.",
                Role.MANAGER,
                roleGroup
        );

        VBox radioBox = new VBox(10, employeeRadio, managerRadio);
        radioBox.setStyle("-fx-padding: 8 0;");

        Label limitNotice = new Label("• Codes expire strictly in 10 minutes from generation.\n• Max 5 active codes allowed at any time.\n• For security, a business can never generate another OWNER.");
        limitNotice.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B; -fx-line-spacing: 3px; -fx-background-color: #F8FAFC; -fx-padding: 10 12; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-width: 1;");

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());

        ProgressIndicator modalSpinner = new ProgressIndicator();
        modalSpinner.setMaxSize(18, 18);
        modalSpinner.setVisible(false);

        Button generateBtn = new Button("Generate 10-Minute Code");
        generateBtn.getStyleClass().add("btn-dialog-confirm");
        generateBtn.setOnAction(e -> {
            Role targetRole = employeeRadio.isSelected() ? Role.EMPLOYEE : Role.MANAGER;
            generateBtn.setDisable(true);
            cancelBtn.setDisable(true);
            modalSpinner.setVisible(true);

            User currentUser = AuthService.getCurrentUser();
            if (currentUser == null) return;

            AppExecutor.execute(() -> {
                try {
                    JoinCodeService.GeneratedCodeResult result = joinCodeService.generateCode(currentUser.getId(), targetRole);
                    Platform.runLater(() -> {
                        showGeneratedCodeModal(result.rawCodeFormatted(), result.joinCode());
                        loadData();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        generateBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                        modalSpinner.setVisible(false);
                        ViewNavigator.showToast(ex.getMessage() != null ? ex.getMessage() : "Error generating code.", true);
                    });
                }
            });
        });

        buttonRow.getChildren().addAll(modalSpinner, cancelBtn, generateBtn);
        card.getChildren().addAll(header, desc, roleHeader, radioBox, limitNotice, buttonRow);

        ViewNavigator.showCustomDialog(card);
    }

    private void showGeneratedCodeModal(String formattedCode, JoinCode createdCode) {
        stopActiveCountdown();

        VBox card = new VBox(20);
        card.getStyleClass().add("dialog-modal-card");
        card.setMaxWidth(520);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 28; -fx-border-color: #E2E8F0; -fx-border-width: 1.5;");

        Label badge = new Label("ONE-TIME INVITATION CODE");
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #1D4ED8; -fx-background-color: #DBEAFE; -fx-padding: 4 10; -fx-background-radius: 6;");

        Label codeLabel = new Label(formattedCode);
        codeLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #0F172A; -fx-background-color: #F1F5F9; -fx-padding: 12 24; -fx-background-radius: 10; -fx-border-color: #CBD5E1; -fx-border-width: 2;");

        Label countdownLabel = new Label("Expires in: 10:00");
        countdownLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #B45309;");

        final int[] secondsRemaining = { 600 };
        activeCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            secondsRemaining[0]--;
            if (secondsRemaining[0] <= 0) {
                countdownLabel.setText("EXPIRED");
                countdownLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #B91C1C;");
                stopActiveCountdown();
            } else {
                int mins = secondsRemaining[0] / 60;
                int secs = secondsRemaining[0] % 60;
                countdownLabel.setText(String.format("Expires in: %02d:%02d", mins, secs));
            }
        }));
        activeCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        activeCountdownTimeline.play();

        HBox alertBox = new HBox(10);
        alertBox.setAlignment(Pos.CENTER_LEFT);
        alertBox.setStyle("-fx-background-color: #FEF3C7; -fx-border-color: #FCD34D; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;");
        Label alertIcon = new Label("⚠️");
        Label alertText = new Label("This code is shown ONCE. When this dialog is closed, the raw code can NEVER be retrieved again. Share it with your staff member now.");
        alertText.setWrapText(true);
        alertText.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400E; -fx-font-weight: 600; -fx-line-spacing: 2px;");
        HBox.setHgrow(alertText, Priority.ALWAYS);
        alertBox.getChildren().addAll(alertIcon, alertText);

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER);

        Button copyBtn = new Button("Copy Code");
        copyBtn.getStyleClass().add("btn-secondary");
        SVGPath copyIcon = new SVGPath();
        copyIcon.setContent(SVG_COPY);
        copyIcon.setStyle("-fx-fill: -text-main;");
        copyBtn.setGraphic(copyIcon);

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(formattedCode);
            clipboard.setContent(content);

            copyBtn.setText("Copied!");
            copyBtn.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #047857; -fx-font-weight: 700;");
            Timeline revert = new Timeline(new KeyFrame(Duration.seconds(2), t -> {
                copyBtn.setText("Copy Code");
                copyBtn.setStyle("");
            }));
            revert.play();

            Timeline clipboardClearer = new Timeline(new KeyFrame(Duration.seconds(60), t -> {
                Clipboard clip = Clipboard.getSystemClipboard();
                if (clip.hasString() && formattedCode.equals(clip.getString())) {
                    clip.clear();
                }
            }));
            clipboardClearer.setCycleCount(1);
            clipboardClearer.play();
        });

        Button cancelCodeBtn = new Button("Cancel Code");
        cancelCodeBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelCodeBtn.setOnAction(e -> {
            stopActiveCountdown();
            User currentUser = AuthService.getCurrentUser();
            if (currentUser != null) {
                AppExecutor.execute(() -> {
                    try {
                        joinCodeService.cancelCode(currentUser.getId(), createdCode.getId());
                        Platform.runLater(() -> {
                            ViewNavigator.hideOverlayDialog();
                            ViewNavigator.showToast("Join code cancelled.", false);
                            loadData();
                        });
                    } catch (Exception ignored) {
                        Platform.runLater(ViewNavigator::hideOverlayDialog);
                    }
                });
            } else {
                ViewNavigator.hideOverlayDialog();
            }
        });

        Button doneBtn = new Button("Done & Close");
        doneBtn.getStyleClass().add("btn-primary");
        doneBtn.setOnAction(e -> {
            stopActiveCountdown();
            ViewNavigator.hideOverlayDialog();
            loadData();
        });

        buttonRow.getChildren().addAll(copyBtn, cancelCodeBtn, doneBtn);

        card.getChildren().addAll(badge, codeLabel, countdownLabel, alertBox, buttonRow);
        ViewNavigator.showCustomDialog(card);
    }

    private void stopActiveCountdown() {
        if (activeCountdownTimeline != null) {
            activeCountdownTimeline.stop();
            activeCountdownTimeline = null;
        }
    }

    private static class RadioRadioButtonItem extends RadioButton {
        RadioRadioButtonItem(String title, String desc, Role role, ToggleGroup group) {
            super();
            setToggleGroup(group);

            VBox box = new VBox(2);
            Label t = new Label(title);
            t.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
            Label d = new Label(desc);
            d.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
            d.setWrapText(true);
            box.getChildren().addAll(t, d);

            setGraphic(box);
            setStyle("-fx-padding: 4 0;");
        }
    }

    @Override
    public void dispose() {
        stopActiveCountdown();
    }
}
