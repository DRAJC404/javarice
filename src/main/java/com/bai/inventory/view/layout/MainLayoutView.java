package com.bai.inventory.view.layout;

import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.view.Disposable;
import com.bai.inventory.view.navigation.ViewNavigator;
import com.bai.inventory.view.navigation.ViewType;
import com.bai.inventory.view.pages.AnalyticsView;
import com.bai.inventory.view.pages.DashboardView;
import com.bai.inventory.view.pages.InventoryView;
import com.bai.inventory.view.pages.ProductCatalogView;
import com.bai.inventory.view.pages.TeamView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainLayoutView extends StackPane implements Disposable {

    private static final String SVG_HAMBURGER = "M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z";
    private static final String SVG_DASHBOARD = "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z";
    private static final String SVG_PRODUCTS  = "M20 7l-8-4-8 4 8 4 8-4zm0 6l-8 4-8-4m16 4l-8 4-8-4";
    private static final String SVG_INVENTORY = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z";
    private static final String SVG_ANALYTICS = "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z";
    private static final String SVG_INSIGHTS  = "M12 2L9.5 8.5 3 11l6.5 2.5L12 20l2.5-6.5L21 11l-6.5-2.5z";
    private static final String SVG_REPORTS   = "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";
    private static final String SVG_USERS     = "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z";
    private static final String SVG_LOGOUT    = "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z";
    private static final String SVG_PLUS      = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";

    private final BorderPane mainBorderPane;
    private final BorderPane contentArea;
    private final StackPane centerContainer;
    private final Label pageTitleLabel;

    private final StackPane drawerOverlay;
    private final Region drawerBackdrop;

    private VBox sidebarBox;
    private Label brandTitle;
    private Label tagBadge;
    private Node userDetailsBox;
    private Label logoutTextLabel;
    private Button addTeamNavBtn;
    private Label teamBadgeLabel;
    private final com.bai.inventory.dao.JoinCodeDao joinCodeDao = new com.bai.inventory.dao.JoinCodeDao();

    private final Map<ViewType, Button> navButtons = new LinkedHashMap<>();
    private final Map<ViewType, Label> navLabels = new LinkedHashMap<>();
    private ViewType currentActiveView = ViewType.DASHBOARD;
    private Node currentSubView;

    private boolean userDesktopExpanded = true;
    private boolean isNarrowMode = false;

    public MainLayoutView() {
        this(ViewType.DASHBOARD);
    }

    public MainLayoutView(ViewType initialView) {
        getStyleClass().add("main-layout");

        centerContainer = new StackPane();
        centerContainer.getStyleClass().add("content-pane");
        HBox.setHgrow(centerContainer, Priority.ALWAYS);
        VBox.setVgrow(centerContainer, Priority.ALWAYS);

        pageTitleLabel = new Label("Dashboard");
        pageTitleLabel.getStyleClass().add("top-navbar-title");
        Node topNavbar = buildTopNavbar();

        contentArea = new BorderPane();
        contentArea.setTop(topNavbar);
        contentArea.setCenter(centerContainer);

        sidebarBox = buildSidebar();

        mainBorderPane = new BorderPane();
        mainBorderPane.setLeft(sidebarBox);
        mainBorderPane.setCenter(contentArea);

        drawerBackdrop = new Region();
        drawerBackdrop.getStyleClass().add("drawer-backdrop");
        drawerBackdrop.setOnMouseClicked(e -> closeDrawer());

        drawerOverlay = new StackPane();
        drawerOverlay.setVisible(false);
        drawerOverlay.setManaged(false);

        getChildren().addAll(mainBorderPane, drawerOverlay);

        widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0) {
                handleResponsiveLayout(newW.doubleValue());
            }
        });

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE && drawerOverlay.isVisible()) {
                        closeDrawer();
                        event.consume();
                    }
                });
            }
        });

        selectView(initialView != null ? initialView : ViewType.DASHBOARD);
        refreshTeamBadge();
    }

    private Node buildTopNavbar() {
        BorderPane navbar = new BorderPane();
        navbar.getStyleClass().add("top-navbar");

        HBox leftSection = new HBox(16);
        leftSection.setAlignment(Pos.CENTER_LEFT);

        Button hamburgerBtn = new Button();
        hamburgerBtn.getStyleClass().add("top-navbar-hamburger");
        SVGPath hamburgerIcon = new SVGPath();
        hamburgerIcon.setContent(SVG_HAMBURGER);
        hamburgerIcon.getStyleClass().add("top-navbar-icon");
        hamburgerBtn.setGraphic(hamburgerIcon);
        hamburgerBtn.setTooltip(new Tooltip("Toggle navigation menu"));
        hamburgerBtn.setOnAction(e -> handleHamburgerClick());

        leftSection.getChildren().addAll(hamburgerBtn, pageTitleLabel);
        navbar.setLeft(leftSection);

        User currentUser = AuthService.getCurrentUser();
        HBox rightSection = new HBox(12);
        rightSection.setAlignment(Pos.CENTER_RIGHT);

        if (currentUser != null && currentUser.getRole() == Role.OWNER) {
            addTeamNavBtn = new Button("Add team member");
            addTeamNavBtn.getStyleClass().add("btn-primary");
            SVGPath plusIcon = new SVGPath();
            plusIcon.setContent(SVG_PLUS);
            plusIcon.getStyleClass().add("sidebar-item-icon");
            addTeamNavBtn.setGraphic(plusIcon);
            addTeamNavBtn.setOnAction(e -> com.bai.inventory.view.dialog.JoinCodeDialogHelper.showAddTeamMemberModal(this::refreshTeamBadge));
            rightSection.getChildren().add(addTeamNavBtn);
        }

        Node userMenu = buildUserTopMenu(currentUser);
        rightSection.getChildren().add(userMenu);
        navbar.setRight(rightSection);

        return navbar;
    }

    private Button sidebarLogoutBtn;

    private Node buildUserTopMenu(User user) {
        HBox userBox = new HBox(12);
        userBox.getStyleClass().add("top-navbar-user-box");
        userBox.setAlignment(Pos.CENTER_RIGHT);

        String fullName = (user != null && user.getFullName() != null) ? user.getFullName() : "Administrator";
        String roleTitle = (user != null && user.getRole() != null) ? user.getRole().getDisplayName() : "Admin";

        Label avatar = new Label(extractInitials(fullName));
        avatar.getStyleClass().add("top-navbar-avatar");

        VBox metaBox = new VBox(2);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("top-navbar-user-name");

        Label roleLabel = new Label(roleTitle.toUpperCase());
        roleLabel.getStyleClass().add("top-navbar-user-role");
        metaBox.getChildren().addAll(nameLabel, roleLabel);

        userBox.getChildren().addAll(avatar, metaBox);
        return userBox;
    }

    private VBox buildSidebar() {
        sidebarBox = new VBox();
        sidebarBox.getStyleClass().add("sidebar");
        sidebarBox.setPrefWidth(250);
        sidebarBox.setMinWidth(250);

        VBox brandBox = new VBox(8);
        brandBox.getStyleClass().add("sidebar-brand-box");

        HBox logoRow = new HBox(10);
        logoRow.setAlignment(Pos.CENTER_LEFT);

        try (InputStream is = getClass().getResourceAsStream("/images/bailogo.png")) {
            if (is != null) {
                ImageView logoImg = new ImageView(new Image(is));
                logoImg.setFitHeight(36);
                logoImg.setPreserveRatio(true);
                logoRow.getChildren().add(logoImg);
            }
        } catch (Exception ignored) {}

        brandTitle = new Label("bAI");
        brandTitle.getStyleClass().add("sidebar-logo");

        tagBadge = new Label("Inventory");
        tagBadge.getStyleClass().add("sidebar-logo-tag");

        logoRow.getChildren().addAll(brandTitle, tagBadge);
        brandBox.getChildren().add(logoRow);

        VBox navContainer = new VBox(6);
        navContainer.getStyleClass().add("sidebar-nav-container");

        User currentUser = AuthService.getCurrentUser();
        boolean isOwner = (currentUser != null && currentUser.getRole() == Role.OWNER);

        addNavItem(navContainer, ViewType.DASHBOARD, "Dashboard", SVG_DASHBOARD);
        addNavItem(navContainer, ViewType.PRODUCT, "Products", SVG_PRODUCTS);
        addNavItem(navContainer, ViewType.INVENTORY, "Inventory", SVG_INVENTORY);

        if (isOwner) {
            teamBadgeLabel = new Label();
            teamBadgeLabel.getStyleClass().add("join-code-badge");
            teamBadgeLabel.setVisible(false);
            teamBadgeLabel.setManaged(false);
            addNavItem(navContainer, ViewType.USERS, "Team", SVG_USERS, teamBadgeLabel);
        }

        addNavItem(navContainer, ViewType.ANALYTICS, "Analytics", SVG_ANALYTICS);
        addNavItem(navContainer, ViewType.AI_INSIGHTS, "AI Insights", SVG_INSIGHTS);
        addNavItem(navContainer, ViewType.REPORT, "Reports", SVG_REPORTS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Node userFooter = buildUserFooter(currentUser);

        sidebarBox.getChildren().addAll(brandBox, navContainer, spacer, userFooter);
        return sidebarBox;
    }

    private void addNavItem(VBox container, ViewType type, String labelText, String svgPath) {
        addNavItem(container, type, labelText, svgPath, null);
    }

    private void addNavItem(VBox container, ViewType type, String labelText, String svgPath, Node badge) {
        Button btn = new Button();
        btn.getStyleClass().add("sidebar-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("sidebar-icon-box");

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("sidebar-item-icon");

        iconBox.getChildren().add(icon);

        Label label = new Label(labelText);
        label.getStyleClass().add("sidebar-item-label");

        row.getChildren().addAll(iconBox, label);

        if (badge != null) {
            Region rowSpacer = new Region();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);
            row.getChildren().addAll(rowSpacer, badge);
        }

        btn.setGraphic(row);

        btn.setOnAction(e -> {
            selectView(type);
            if (isNarrowMode) {
                closeDrawer();
            }
        });

        navButtons.put(type, btn);
        navLabels.put(type, label);
        container.getChildren().add(btn);
    }

    private Node buildUserFooter(User user) {
        VBox footer = new VBox(12);
        footer.getStyleClass().add("sidebar-footer");

        HBox profileRow = new HBox(12);
        profileRow.setAlignment(Pos.CENTER_LEFT);

        String fullName = (user != null && user.getFullName() != null) ? user.getFullName() : "Administrator";
        String roleTitle = (user != null && user.getRole() != null) ? user.getRole().getDisplayName() : "Admin";

        Label avatar = new Label(extractInitials(fullName));
        avatar.getStyleClass().add("sidebar-user-avatar");

        VBox userDetails = new VBox(2);
        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("sidebar-user-name");

        Label roleLabel = new Label(roleTitle.toUpperCase());
        roleLabel.getStyleClass().add("sidebar-user-role");

        userDetails.getChildren().addAll(nameLabel, roleLabel);
        HBox.setHgrow(userDetails, Priority.ALWAYS);
        this.userDetailsBox = userDetails;

        profileRow.getChildren().addAll(avatar, userDetails);

        Button logoutBtn = new Button();
        logoutBtn.getStyleClass().add("sidebar-logout-btn");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        this.sidebarLogoutBtn = logoutBtn;

        HBox logoutContent = new HBox(8);
        logoutContent.setAlignment(Pos.CENTER);

        SVGPath logoutIcon = new SVGPath();
        logoutIcon.setContent(SVG_LOGOUT);
        logoutIcon.getStyleClass().add("dialog-close-icon");

        logoutTextLabel = new Label("Sign Out");
        logoutTextLabel.getStyleClass().add("sidebar-user-role");

        logoutContent.getChildren().addAll(logoutIcon, logoutTextLabel);
        logoutBtn.setGraphic(logoutContent);
        logoutBtn.setOnAction(e -> ViewNavigator.showConfirmDialog(
                "Sign out?",
                "You will need to sign in again to continue.",
                "Sign out",
                ViewNavigator::logout
        ));

        footer.getChildren().addAll(profileRow, logoutBtn);
        return footer;
    }

    private void handleHamburgerClick() {
        if (isNarrowMode) {
            if (drawerOverlay.isVisible()) {
                closeDrawer();
            } else {
                openDrawer();
            }
        } else {
            userDesktopExpanded = !userDesktopExpanded;
            applyDesktopSidebarState(userDesktopExpanded);
        }
    }

    private void handleResponsiveLayout(double width) {
        if (addTeamNavBtn != null) {
            if (width < 800) {
                addTeamNavBtn.setText("");
                addTeamNavBtn.setTooltip(new Tooltip("Add team member"));
            } else {
                addTeamNavBtn.setText("Add team member");
                addTeamNavBtn.setTooltip(null);
            }
        }

        if (width < 1180) {
            isNarrowMode = true;
            if (mainBorderPane.getLeft() == sidebarBox) {
                mainBorderPane.setLeft(null);
            }
            if (sidebarBox.getParent() != drawerOverlay) {
                if (sidebarBox.getParent() instanceof Pane parentPane) {
                    parentPane.getChildren().remove(sidebarBox);
                }
                drawerOverlay.getChildren().setAll(drawerBackdrop, sidebarBox);
                StackPane.setAlignment(sidebarBox, Pos.CENTER_LEFT);
            }
            applyDesktopSidebarState(true);
        } else {
            isNarrowMode = false;
            closeDrawer();
            if (sidebarBox.getParent() != mainBorderPane) {
                if (sidebarBox.getParent() instanceof Pane parentPane) {
                    parentPane.getChildren().remove(sidebarBox);
                }
                mainBorderPane.setLeft(sidebarBox);
            }
            applyDesktopSidebarState(userDesktopExpanded);
        }
    }

    private void openDrawer() {
        if (sidebarBox.getParent() != drawerOverlay) {
            if (sidebarBox.getParent() instanceof Pane parentPane) {
                parentPane.getChildren().remove(sidebarBox);
            }
            drawerOverlay.getChildren().setAll(drawerBackdrop, sidebarBox);
            StackPane.setAlignment(sidebarBox, Pos.CENTER_LEFT);
        }
        applyDesktopSidebarState(true);
        drawerOverlay.setVisible(true);
        drawerOverlay.setManaged(true);
        drawerOverlay.requestFocus();
    }

    private void closeDrawer() {
        drawerOverlay.setVisible(false);
        drawerOverlay.setManaged(false);
    }

    private void applyDesktopSidebarState(boolean expanded) {
        if (expanded) {
            sidebarBox.setPrefWidth(250);
            sidebarBox.setMinWidth(250);
            sidebarBox.setMaxWidth(250);
            sidebarBox.getStyleClass().remove("sidebar-collapsed");

            if (brandTitle != null) { brandTitle.setVisible(true); brandTitle.setManaged(true); }
            if (tagBadge != null) { tagBadge.setVisible(true); tagBadge.setManaged(true); }
            navLabels.values().forEach(lbl -> { lbl.setVisible(true); lbl.setManaged(true); });
            if (userDetailsBox != null) { userDetailsBox.setVisible(true); userDetailsBox.setManaged(true); }
            if (logoutTextLabel != null) { logoutTextLabel.setVisible(true); logoutTextLabel.setManaged(true); }
            if (teamBadgeLabel != null) {
                boolean show = teamBadgeLabel.getText() != null && !teamBadgeLabel.getText().isEmpty();
                teamBadgeLabel.setVisible(show);
                teamBadgeLabel.setManaged(show);
            }

            navButtons.forEach((type, btn) -> btn.setTooltip(null));
            if (sidebarLogoutBtn != null) sidebarLogoutBtn.setTooltip(null);
        } else {
            sidebarBox.setPrefWidth(72);
            sidebarBox.setMinWidth(72);
            sidebarBox.setMaxWidth(72);
            if (!sidebarBox.getStyleClass().contains("sidebar-collapsed")) {
                sidebarBox.getStyleClass().add("sidebar-collapsed");
            }

            if (brandTitle != null) { brandTitle.setVisible(false); brandTitle.setManaged(false); }
            if (tagBadge != null) { tagBadge.setVisible(false); tagBadge.setManaged(false); }
            navLabels.values().forEach(lbl -> { lbl.setVisible(false); lbl.setManaged(false); });
            if (userDetailsBox != null) { userDetailsBox.setVisible(false); userDetailsBox.setManaged(false); }
            if (logoutTextLabel != null) { logoutTextLabel.setVisible(false); logoutTextLabel.setManaged(false); }
            if (teamBadgeLabel != null) {
                teamBadgeLabel.setVisible(false);
                teamBadgeLabel.setManaged(false);
            }

            navButtons.forEach((type, btn) -> {
                Label label = navLabels.get(type);
                if (label != null) {
                    btn.setTooltip(new Tooltip(label.getText()));
                }
            });
            if (sidebarLogoutBtn != null) sidebarLogoutBtn.setTooltip(new Tooltip("Sign Out"));
        }
    }

    public void refreshTeamBadge() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.OWNER || teamBadgeLabel == null) return;
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        com.bai.inventory.util.AppExecutor.execute(() -> {
            int active = joinCodeDao.countActiveCodes(orgId);
            Platform.runLater(() -> {
                if (active > 0) {
                    teamBadgeLabel.setText(active + " active");
                    boolean show = !sidebarBox.getStyleClass().contains("sidebar-collapsed");
                    teamBadgeLabel.setVisible(show);
                    teamBadgeLabel.setManaged(show);
                } else {
                    teamBadgeLabel.setText("");
                    teamBadgeLabel.setVisible(false);
                    teamBadgeLabel.setManaged(false);
                }
            });
        });
    }

    public void selectView(ViewType type) {
        User currentUser = AuthService.getCurrentUser();
        if (type == ViewType.USERS) {
            if (currentUser == null || currentUser.getRole() != Role.OWNER) {
                ViewNavigator.showToast("Access Denied: Business Owner role required.", true);
                return;
            }
        }

        if (currentSubView instanceof Disposable disposable) {
            try {
                disposable.dispose();
            } catch (Exception ex) {
                System.err.println("[MainLayoutView] Error disposing subview: " + ex.getMessage());
            }
        }

        this.currentActiveView = type;
        pageTitleLabel.setText(getViewTitle(type));

        for (Map.Entry<ViewType, Button> entry : navButtons.entrySet()) {
            Button btn = entry.getValue();
            boolean isActive = (entry.getKey() == type);
            if (isActive) {
                if (!btn.getStyleClass().contains("sidebar-item-active")) {
                    btn.getStyleClass().add("sidebar-item-active");
                }
            } else {
                btn.getStyleClass().remove("sidebar-item-active");
            }
        }

        Node viewNode = createViewNode(type);
        this.currentSubView = viewNode;
        centerContainer.getChildren().setAll(viewNode);
    }

    private String getViewTitle(ViewType type) {
        return switch (type) {
            case DASHBOARD -> "Dashboard";
            case PRODUCT -> "Products";
            case INVENTORY -> "Inventory Control";
            case ANALYTICS -> "Stock Analytics";
            case AI_INSIGHTS -> "bAI Insights";
            case REPORT -> "Reports & Export";
            case USERS -> "Team Members";
            default -> "Dashboard";
        };
    }

    private Node createViewNode(ViewType type) {
        return switch (type) {
            case DASHBOARD -> new DashboardView();
            case PRODUCT -> new ProductCatalogView();
            case INVENTORY -> new InventoryView();
            case ANALYTICS -> new AnalyticsView();
            case AI_INSIGHTS -> new com.bai.inventory.view.pages.AIInsightsView();
            case REPORT -> new com.bai.inventory.view.pages.ReportsView();
            case USERS -> new TeamView();
            default -> new DashboardView();
        };
    }

    private Node createSectionPlaceholder(String title, String description, String svgPath) {
        VBox outer = new VBox(24);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(40));

        VBox card = new VBox(20);
        card.setMaxWidth(620);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 40; -fx-border-color: -border-subtle; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.06), 16, 0, 0, 4);");

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.setStyle("-fx-fill: -primary;");
        icon.setScaleX(2.0);
        icon.setScaleY(2.0);

        Label badge = new Label("PAGE READY FOR IMPLEMENTATION");
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -primary; -fx-background-color: -primary-light; -fx-padding: 4 10; -fx-background-radius: 6;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-muted; -fx-line-spacing: 4px;");

        card.getChildren().addAll(icon, badge, titleLabel, descLabel);
        outer.getChildren().add(card);
        return outer;
    }

    private String extractInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public ViewType getCurrentActiveView() {
        return currentActiveView;
    }

    @Override
    public void dispose() {
        if (currentSubView instanceof Disposable disposable) {
            try {
                disposable.dispose();
            } catch (Exception ex) {
                System.err.println("[MainLayoutView] Error disposing subview on layout teardown: " + ex.getMessage());
            }
        }
    }
}
