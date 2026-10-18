package com.bai.inventory.view.pages;

import com.bai.inventory.dao.UserDao;
import com.bai.inventory.model.JoinCode;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.JoinCodeService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.EmptyState;
import com.bai.inventory.view.component.PageHeader;
import com.bai.inventory.view.dialog.JoinCodeDialogHelper;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeamView extends BorderPane implements Disposable {

    private static final String SVG_PLUS = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    private static final String SVG_REFRESH = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_USERS = "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final JoinCodeService joinCodeService;
    private final UserDao userDao;

    private TableView<JoinCode> codesTable;
    private TableView<User> usersTable;
    private ProgressIndicator loadingIndicator;

    public TeamView() {
        this.joinCodeService = new JoinCodeService();
        this.userDao = new UserDao();
        buildUI();
        loadData();
    }

    private void buildUI() {
        getStyleClass().add("content-pane");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox contentBox = new VBox(24);
        contentBox.setPadding(new Insets(32));
        contentBox.setFillWidth(true);

        Node header = buildHeader();
        Node codesSection = buildCodesSection();
        Node usersSection = buildUsersSection();

        contentBox.getChildren().addAll(header, codesSection, usersSection);
        scrollPane.setContent(contentBox);
        setCenter(scrollPane);
    }

    private Node buildHeader() {
        PageHeader header = new PageHeader(
                "Team & Staff Access",
                "Generate 10-minute secure join codes for onboarding and view store staff."
        );

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(20, 20);
        loadingIndicator.setVisible(false);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        SVGPath refIcon = new SVGPath();
        refIcon.setContent(SVG_REFRESH);
        refIcon.getStyleClass().add("sidebar-item-icon");
        refreshBtn.setGraphic(refIcon);
        refreshBtn.setOnAction(e -> loadData());

        Button addMemberBtn = new Button("Add Team Member");
        addMemberBtn.getStyleClass().add("btn-primary");
        SVGPath addIcon = new SVGPath();
        addIcon.setContent(SVG_PLUS);
        addIcon.getStyleClass().add("sidebar-item-icon");
        addMemberBtn.setGraphic(addIcon);
        addMemberBtn.setOnAction(e -> JoinCodeDialogHelper.showAddTeamMemberModal(this::loadData));

        header.addActions(loadingIndicator, refreshBtn, addMemberBtn);
        return header;
    }

    private Node buildCodesSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox sectionHeader = new HBox(12);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        VBox titleCol = new VBox(2);
        Label secTitle = new Label("10-Minute Invitation Codes");
        secTitle.getStyleClass().add("stat-label");

        Label secDesc = new Label("Short-lived single-use onboarding codes. Raw codes are never stored; only active/used status is tracked.");
        secDesc.getStyleClass().add("form-helper");
        titleCol.getChildren().addAll(secTitle, secDesc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label limitBadge = new Label("Max 5 Active Codes");
        limitBadge.getStyleClass().addAll("badge", "badge-info");

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
                    badge.getStyleClass().add("badge");
                    if ("MANAGER".equalsIgnoreCase(roleStr)) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-info");
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
                    badge.getStyleClass().add("badge");
                    switch (status) {
                        case "PENDING" -> badge.getStyleClass().add("badge-success");
                        case "USED" -> badge.getStyleClass().add("badge-neutral");
                        case "EXPIRED" -> badge.getStyleClass().add("badge-neutral");
                        case "CANCELLED" -> badge.getStyleClass().add("badge-danger");
                        default -> badge.getStyleClass().add("badge-neutral");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        statusCol.setPrefWidth(110);

        TableColumn<JoinCode, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel");
            {
                cancelBtn.getStyleClass().add("btn-danger");
                cancelBtn.setOnAction(e -> {
                    JoinCode code = getTableView().getItems().get(getIndex());
                    if (code != null) handleCancelCode(code);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    JoinCode code = getTableView().getItems().get(getIndex());
                    if (code != null && "PENDING".equals(code.getStatusString())) {
                        setGraphic(cancelBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        actionCol.setPrefWidth(100);

        codesTable.getColumns().addAll(idCol, roleCol, createdCol, expiresCol, statusCol, actionCol);
        card.getChildren().addAll(sectionHeader, codesTable);
        return card;
    }

    private Node buildUsersSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        VBox titleCol = new VBox(2);
        Label secTitle = new Label("Organization Team Members");
        secTitle.getStyleClass().add("stat-label");

        Label secDesc = new Label("Active store accounts with terminal and management access.");
        secDesc.getStyleClass().add("form-helper");
        titleCol.getChildren().addAll(secTitle, secDesc);

        usersTable = new TableView<>();
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        usersTable.setPrefHeight(240);

        EmptyState emptyState = new EmptyState(
                SVG_USERS,
                "No team members yet",
                "Generate a join code to invite staff.",
                "Generate join code",
                () -> JoinCodeDialogHelper.showAddTeamMemberModal(this::loadData)
        );
        usersTable.setPlaceholder(emptyState);

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
                    badge.getStyleClass().add("badge");
                    if ("OWNER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) {
                        badge.getStyleClass().add("badge-info");
                    } else if ("MANAGER".equalsIgnoreCase(roleName)) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-neutral");
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
                    badge.getStyleClass().add("badge");
                    if ("ACTIVE".equals(status)) {
                        badge.getStyleClass().add("badge-success");
                    } else {
                        badge.getStyleClass().add("badge-danger");
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
                            joinCodeService.cancelCode(currentUser.getId(), code.getId());
                            Platform.runLater(() -> {
                                ViewNavigator.showToast("Join code cancelled successfully.", false);
                                loadData();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> ViewNavigator.showToast("Failed to cancel code: " + ex.getMessage(), true));
                        }
                    });
                }
        );
    }

    @Override
    public void dispose() {
        JoinCodeDialogHelper.stopActiveCountdown();
    }
}
