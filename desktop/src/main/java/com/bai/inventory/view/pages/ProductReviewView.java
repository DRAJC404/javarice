package com.bai.inventory.view.pages;

import com.bai.inventory.model.ProductPerformance;
import com.bai.inventory.model.ProductSubmission;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.ProductService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.util.Constants;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.component.AppDialog;
import com.bai.inventory.view.component.EmptyState;
import com.bai.inventory.view.component.PageHeader;
import com.bai.inventory.view.layout.MainLayoutView;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductReviewView extends BorderPane implements Disposable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final ProductService productService;
    private volatile boolean isDisposed = false;

    private ToggleButton pendingTabBtn;
    private ToggleButton mySubmissionsTabBtn;
    private ToggleButton performanceTabBtn;
    private StackPane contentStack;

    private VBox pendingListBox;
    private VBox mySubmissionsListBox;
    private Label pendingCountChip;

    private ComboBox<String> dateRangeCombo;
    private CheckBox needsAttentionOnlyCheck;
    private TextField searchField;
    private TableView<ProductPerformance> performanceTable;
    private Label perfTotalUnitsLabel;
    private Label perfTotalRevLabel;
    private Label perfTotalProfitLabel;
    private List<ProductPerformance> allPerformanceData = new ArrayList<>();

    public ProductReviewView() {
        this.productService = new ProductService();
        buildUI();
        loadPendingSubmissions();
        loadPerformanceData();
    }

    private void buildUI() {
        getStyleClass().add("content-pane");

        VBox rootBox = new VBox(20);
        rootBox.setPadding(new Insets(28));
        rootBox.setFillWidth(true);

        PageHeader header = new PageHeader(
                "Product Review & Performance",
                "Review employee product submissions and evaluate product sales performance"
        );

        HBox tabToolbar = buildTabBar();

        contentStack = new StackPane();
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        Node pendingView = buildPendingView();
        Node mySubmissionsView = buildMySubmissionsView();
        Node performanceView = buildPerformanceView();

        contentStack.getChildren().addAll(pendingView, mySubmissionsView, performanceView);
        mySubmissionsView.setVisible(false);
        performanceView.setVisible(false);

        rootBox.getChildren().addAll(header, tabToolbar, contentStack);
        setCenter(rootBox);
    }

    private HBox buildTabBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup group = new ToggleGroup();

        pendingTabBtn = new ToggleButton("Pending Submissions");
        pendingTabBtn.setToggleGroup(group);
        pendingTabBtn.setSelected(true);
        pendingTabBtn.getStyleClass().add("btn-secondary");

        pendingCountChip = new Label("0");
        pendingCountChip.setStyle("-fx-background-color: -danger; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 2 7; -fx-background-radius: 12;");
        pendingCountChip.setVisible(false);
        pendingCountChip.setManaged(false);

        HBox pendingBtnContent = new HBox(8, new Label("Pending Review"), pendingCountChip);
        pendingBtnContent.setAlignment(Pos.CENTER);
        pendingTabBtn.setGraphic(pendingBtnContent);

        mySubmissionsTabBtn = new ToggleButton("My Submissions");
        mySubmissionsTabBtn.setToggleGroup(group);
        mySubmissionsTabBtn.getStyleClass().add("btn-secondary");

        performanceTabBtn = new ToggleButton("Performance Evaluation");
        performanceTabBtn.setToggleGroup(group);
        performanceTabBtn.getStyleClass().add("btn-secondary");

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                if (oldVal != null) oldVal.setSelected(true);
                return;
            }
            contentStack.getChildren().get(0).setVisible(newVal == pendingTabBtn);
            contentStack.getChildren().get(1).setVisible(newVal == mySubmissionsTabBtn);
            contentStack.getChildren().get(2).setVisible(newVal == performanceTabBtn);

            if (newVal == pendingTabBtn) {
                loadPendingSubmissions();
            } else if (newVal == mySubmissionsTabBtn) {
                loadMySubmissions();
            } else if (newVal == performanceTabBtn) {
                loadPerformanceData();
            }
        });

        bar.getChildren().addAll(pendingTabBtn, mySubmissionsTabBtn, performanceTabBtn);
        return bar;
    }

    private Node buildPendingView() {
        VBox container = new VBox(16);
        VBox.setVgrow(container, Priority.ALWAYS);

        pendingListBox = new VBox(12);
        pendingListBox.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(pendingListBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        container.getChildren().add(scroll);
        return container;
    }

    private Node buildMySubmissionsView() {
        VBox container = new VBox(16);
        VBox.setVgrow(container, Priority.ALWAYS);

        mySubmissionsListBox = new VBox(12);
        mySubmissionsListBox.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(mySubmissionsListBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        container.getChildren().add(scroll);
        return container;
    }

    private Node buildPerformanceView() {
        VBox container = new VBox(16);
        VBox.setVgrow(container, Priority.ALWAYS);

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label rangeLbl = new Label("Period:");
        rangeLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -text-primary;");

        dateRangeCombo = new ComboBox<>();
        dateRangeCombo.getItems().addAll("Last 7 Days", "Last 30 Days", "This Month", "All Time");
        dateRangeCombo.setValue("Last 30 Days");
        dateRangeCombo.setOnAction(e -> loadPerformanceData());

        needsAttentionOnlyCheck = new CheckBox("Show Needs Attention Only");
        needsAttentionOnlyCheck.setStyle("-fx-font-weight: 600; -fx-text-fill: -danger;");
        needsAttentionOnlyCheck.setOnAction(e -> filterPerformanceTable());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Search products...");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, oldV, newV) -> filterPerformanceTable());

        toolbar.getChildren().addAll(rangeLbl, dateRangeCombo, needsAttentionOnlyCheck, spacer, searchField);

        HBox kpiBar = buildPerformanceKpiBar();

        performanceTable = new TableView<>();
        performanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(performanceTable, Priority.ALWAYS);

        TableColumn<ProductPerformance, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));

        TableColumn<ProductPerformance, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName() != null ? data.getValue().getCategoryName() : "—"));

        TableColumn<ProductPerformance, String> soldCol = new TableColumn<>("Units Sold");
        soldCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitsSold() + " " + Constants.UNIT));

        TableColumn<ProductPerformance, String> stockCol = new TableColumn<>("Stock Left");
        stockCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStockLeft() + " " + Constants.UNIT));

        TableColumn<ProductPerformance, String> turnoverCol = new TableColumn<>("Turnover");
        turnoverCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedTurnover()));

        TableColumn<ProductPerformance, String> revCol = new TableColumn<>("Revenue");
        revCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedRevenue()));

        TableColumn<ProductPerformance, String> profitCol = new TableColumn<>("Profit");
        profitCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedProfit()));

        TableColumn<ProductPerformance, String> tagCol = new TableColumn<>("Status");
        tagCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusTag()));
        tagCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tag, boolean empty) {
                super.updateItem(tag, empty);
                if (empty || tag == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(tag);
                    badge.getStyleClass().add("status-badge");
                    if ("Top Seller".equalsIgnoreCase(tag)) {
                        badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 12;");
                    } else if ("Needs Attention".equalsIgnoreCase(tag)) {
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 12;");
                    } else {
                        badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-padding: 3 8; -fx-background-radius: 12;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<ProductPerformance, String> reasonCol = new TableColumn<>("Evaluation Note");
        reasonCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAttentionReason() != null ? data.getValue().getAttentionReason() : ""));

        performanceTable.getColumns().addAll(List.of(nameCol, catCol, soldCol, stockCol, turnoverCol, revCol, profitCol, tagCol, reasonCol));

        container.getChildren().addAll(toolbar, kpiBar, performanceTable);
        return container;
    }

    private HBox buildPerformanceKpiBar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: -border-subtle; -fx-border-width: 1;");
        bar.setAlignment(Pos.CENTER_LEFT);

        perfTotalUnitsLabel = new Label("0 pcs");
        perfTotalUnitsLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: -text-primary;");

        perfTotalRevLabel = new Label("₱0.00");
        perfTotalRevLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: -primary;");

        perfTotalProfitLabel = new Label("₱0.00");
        perfTotalProfitLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: -success;");

        bar.getChildren().addAll(
                new HBox(8, new Label("Units Sold:"), perfTotalUnitsLabel),
                new Separator(javafx.geometry.Orientation.VERTICAL),
                new HBox(8, new Label("Total Revenue:"), perfTotalRevLabel),
                new Separator(javafx.geometry.Orientation.VERTICAL),
                new HBox(8, new Label("Total Profit:"), perfTotalProfitLabel)
        );
        return bar;
    }

    private void loadPendingSubmissions() {
        User currentUser = AuthService.getCurrentUser();
        AppExecutor.execute(() -> {
            List<ProductSubmission> list = productService.getPendingSubmissions(currentUser);
            Platform.runLater(() -> {
                if (isDisposed) return;
                renderPendingList(list);
                int count = list.size();
                if (count > 0) {
                    pendingCountChip.setText(String.valueOf(count));
                    pendingCountChip.setVisible(true);
                    pendingCountChip.setManaged(true);
                } else {
                    pendingCountChip.setVisible(false);
                    pendingCountChip.setManaged(false);
                }
            });
        });
    }

    private void renderPendingList(List<ProductSubmission> list) {
        pendingListBox.getChildren().clear();
        if (list.isEmpty()) {
            EmptyState empty = new EmptyState(
                    null,
                    "All Caught Up!",
                    "No pending product submissions waiting for manager review."
            );
            pendingListBox.getChildren().add(empty);
            return;
        }

        for (ProductSubmission sub : list) {
            pendingListBox.getChildren().add(buildSubmissionCard(sub, true));
        }
    }

    private void loadMySubmissions() {
        User currentUser = AuthService.getCurrentUser();
        AppExecutor.execute(() -> {
            List<ProductSubmission> list = productService.getMySubmissions(currentUser);
            Platform.runLater(() -> {
                if (isDisposed) return;
                renderMySubmissionsList(list);
            });
        });
    }

    private void renderMySubmissionsList(List<ProductSubmission> list) {
        mySubmissionsListBox.getChildren().clear();
        if (list.isEmpty()) {
            EmptyState empty = new EmptyState(
                    null,
                    "No Submissions Found",
                    "You haven't submitted any product changes yet."
            );
            mySubmissionsListBox.getChildren().add(empty);
            return;
        }

        for (ProductSubmission sub : list) {
            mySubmissionsListBox.getChildren().add(buildSubmissionCard(sub, false));
        }
    }

    private Node buildSubmissionCard(ProductSubmission sub, boolean canReview) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: -border-subtle; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(sub.getSubmissionType() != null ? sub.getSubmissionType() : "SUBMISSION");
        typeBadge.setStyle("-fx-background-color: " + ("CREATE".equalsIgnoreCase(sub.getSubmissionType()) ? "#dbeafe; -fx-text-fill: #1e40af;" : "#fef3c7; -fx-text-fill: #92400e;") + " -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");

        Label statusBadge = new Label(sub.getStatus() != null ? sub.getStatus() : "PENDING");
        if ("APPROVED".equalsIgnoreCase(sub.getStatus())) {
            statusBadge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
        } else if ("REJECTED".equalsIgnoreCase(sub.getStatus())) {
            statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
        } else {
            statusBadge.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
        }

        Label submitterLabel = new Label("Submitted by " + sub.getSubmitterName() + " (" + (sub.getSubmitterRole() != null ? sub.getSubmitterRole() : "Staff") + ")");
        submitterLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -text-secondary;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String dateStr = sub.getCreatedAt() != null ? sub.getCreatedAt().format(TIME_FMT) : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 12px;");

        topRow.getChildren().addAll(typeBadge, statusBadge, submitterLabel, spacer, dateLabel);

        Label prodTitle = new Label(sub.getName() + " (SKU: " + sub.getSku() + ")");
        prodTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        VBox diffBox = new VBox(6);
        diffBox.setPadding(new Insets(8, 12, 8, 12));
        diffBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

        for (ProductSubmission.FieldDiff diff : sub.getFieldDifferences()) {
            HBox diffRow = new HBox(12);
            diffRow.setAlignment(Pos.CENTER_LEFT);

            Label fName = new Label(diff.label() + ":");
            fName.setPrefWidth(120);
            fName.setStyle("-fx-font-weight: 600; -fx-text-fill: -text-secondary;");

            Label oldV = new Label(diff.oldValue() != null ? diff.oldValue() : "—");
            oldV.setStyle("-fx-text-fill: -danger; -fx-font-weight: 500;");

            Label arrow = new Label("➔");
            arrow.setStyle("-fx-text-fill: -text-muted;");

            Label newV = new Label(diff.newValue() != null ? diff.newValue() : "—");
            newV.setStyle("-fx-text-fill: -success; -fx-font-weight: 600;");

            diffRow.getChildren().addAll(fName, oldV, arrow, newV);
            diffBox.getChildren().add(diffRow);
        }

        card.getChildren().addAll(topRow, prodTitle, diffBox);

        if ("REJECTED".equalsIgnoreCase(sub.getStatus()) && sub.getRejectionReason() != null) {
            HBox rejectBox = new HBox(8);
            rejectBox.setStyle("-fx-background-color: #fef2f2; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #fca5a5; -fx-border-width: 1;");
            Label rejectReason = new Label("Rejection Reason: " + sub.getRejectionReason());
            rejectReason.setStyle("-fx-text-fill: #991b1b; -fx-font-weight: 600;");
            rejectBox.getChildren().add(rejectReason);
            card.getChildren().add(rejectBox);
        }

        if (canReview && "PENDING".equalsIgnoreCase(sub.getStatus())) {
            User currentUser = AuthService.getCurrentUser();
            boolean isSelf = (currentUser != null && sub.getSubmittedBy() == currentUser.getId());

            HBox actions = new HBox(12);
            actions.setAlignment(Pos.CENTER_RIGHT);

            if (isSelf) {
                Label selfNote = new Label("Cannot approve your own submission");
                selfNote.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-style: italic;");
                actions.getChildren().add(selfNote);
            } else {
                Button rejectBtn = new Button("Reject");
                rejectBtn.getStyleClass().add("btn-danger");
                rejectBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: 600; -fx-background-radius: 6;");
                rejectBtn.setOnAction(e -> promptRejectSubmission(sub));

                Button approveBtn = new Button("Approve");
                approveBtn.getStyleClass().add("btn-primary");
                approveBtn.setOnAction(e -> doApproveSubmission(sub));

                actions.getChildren().addAll(rejectBtn, approveBtn);
            }
            card.getChildren().add(actions);
        }

        return card;
    }

    private void doApproveSubmission(ProductSubmission sub) {
        User currentUser = AuthService.getCurrentUser();
        AppExecutor.execute(() -> {
            try {
                productService.approveSubmission(sub.getId(), currentUser);
                Platform.runLater(() -> {
                    ViewNavigator.showToast("Product submission approved successfully!", false);
                    loadPendingSubmissions();
                    refreshSidebarBadge();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> ViewNavigator.showToast("Approval failed: " + ex.getMessage(), true));
            }
        });
    }

    private void promptRejectSubmission(ProductSubmission sub) {
        User currentUser = AuthService.getCurrentUser();

        VBox dialogContent = new VBox(12);
        dialogContent.setPrefWidth(380);

        Label label = new Label("Please enter a short reason for rejecting this change:");
        label.setStyle("-fx-font-weight: 600; -fx-text-fill: -text-primary;");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("e.g. Incorrect cost price, duplicate product, etc.");
        reasonArea.setPrefRowCount(3);
        reasonArea.setWrapText(true);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -danger; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        dialogContent.getChildren().addAll(label, reasonArea, errorLabel);

        AppDialog dialog = new AppDialog("Reject Submission", 420);
        dialog.setBody(dialogContent);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmRejectBtn = new Button("Reject Submission");
        confirmRejectBtn.getStyleClass().add("btn-danger");
        confirmRejectBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: 700;");
        confirmRejectBtn.setOnAction(e -> {
            String reason = reasonArea.getText() != null ? reasonArea.getText().trim() : "";
            if (reason.isEmpty()) {
                errorLabel.setText("Rejection reason is required.");
                errorLabel.setVisible(true);
                return;
            }
            dialog.close();
            AppExecutor.execute(() -> {
                try {
                    productService.rejectSubmission(sub.getId(), reason, currentUser);
                    Platform.runLater(() -> {
                        ViewNavigator.showToast("Submission rejected.", false);
                        loadPendingSubmissions();
                        refreshSidebarBadge();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> ViewNavigator.showToast("Rejection failed: " + ex.getMessage(), true));
                }
            });
        });

        dialog.setFooterButtons(cancelBtn, confirmRejectBtn);
        dialog.show();
    }

    private void loadPerformanceData() {
        User currentUser = AuthService.getCurrentUser();
        String range = dateRangeCombo != null ? dateRangeCombo.getValue() : "Last 30 Days";

        LocalDateTime from = null;
        LocalDateTime to = LocalDateTime.now();

        if ("Last 7 Days".equals(range)) {
            from = LocalDateTime.now().minusDays(7);
        } else if ("Last 30 Days".equals(range)) {
            from = LocalDateTime.now().minusDays(30);
        } else if ("This Month".equals(range)) {
            from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }

        final LocalDateTime finalFrom = from;
        AppExecutor.execute(() -> {
            List<ProductPerformance> data = productService.getProductPerformance(currentUser, finalFrom, to);
            Platform.runLater(() -> {
                if (isDisposed) return;
                this.allPerformanceData = data;
                filterPerformanceTable();
            });
        });
    }

    private void filterPerformanceTable() {
        if (allPerformanceData == null) return;

        boolean needsAttOnly = needsAttentionOnlyCheck != null && needsAttentionOnlyCheck.isSelected();
        String q = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        List<ProductPerformance> filtered = allPerformanceData.stream()
                .filter(p -> !needsAttOnly || p.isNeedsAttention())
                .filter(p -> q.isEmpty() || p.getProductName().toLowerCase().contains(q) || p.getSku().toLowerCase().contains(q))
                .collect(Collectors.toList());

        performanceTable.getItems().setAll(filtered);

        int totalUnits = 0;
        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (ProductPerformance p : filtered) {
            totalUnits += p.getUnitsSold();
            totalRev = totalRev.add(p.getRevenue());
            totalProfit = totalProfit.add(p.getProfit());
        }

        perfTotalUnitsLabel.setText(totalUnits + " " + Constants.UNIT);
        perfTotalRevLabel.setText(String.format("₱%,.2f", totalRev));
        perfTotalProfitLabel.setText(String.format("₱%,.2f", totalProfit));
    }

    private void refreshSidebarBadge() {
        if (ViewNavigator.getCurrentPage() instanceof MainLayoutView mainLayout) {
            mainLayout.refreshReviewBadge();
        }
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }
}
