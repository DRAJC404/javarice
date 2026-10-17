package com.bai.inventory.view.pages;

import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.view.Disposable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

public class DashboardPlaceholderView extends ScrollPane implements Disposable {

    public DashboardPlaceholderView() {
        buildUI();
    }

    private void buildUI() {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        VBox root = new VBox(24);
        root.setPadding(new Insets(10, 20, 30, 20));

        User user = AuthService.getCurrentUser();
        String roleStr = (user != null) ? user.getRole().getDisplayName() : "Manager";
        String userName = (user != null) ? user.getFullName() : "Administrator";

        VBox header = new VBox(6);
        header.getStyleClass().add("page-header");

        Label title = new Label("Dashboard Overview");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Welcome back, " + userName + " (" + roleStr + "). Here is your real-time store inventory summary.");
        subtitle.getStyleClass().add("page-subtitle");

        header.getChildren().addAll(title, subtitle);

        HBox statsGrid = new HBox(16);
        statsGrid.setAlignment(Pos.CENTER_LEFT);

        statsGrid.getChildren().addAll(
                createStatCard("TOTAL PRODUCTS", "10", "Active retail catalog SKUs", "#3B82F6", "M20 7l-8-4-8 4 8 4 8-4zm0 6l-8 4-8-4m16 4l-8 4-8-4"),
                createStatCard("LOW STOCK ITEMS", "4", "Requires immediate restock", "#F59E0B", "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"),
                createStatCard("OUT OF STOCK", "1", "Critically depleted inventory", "#EF4444", "M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"),
                createStatCard("AI RESTOCK ALERTS", "3", "Proactive recommendations", "#10B981", "M13 10V3L4 14h7v7l9-11h-7z")
        );

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36));

        SVGPath icon = new SVGPath();
        icon.setContent("M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z");
        icon.setStyle("-fx-fill: -primary;");
        icon.setScaleX(1.8);
        icon.setScaleY(1.8);

        Label badge = new Label("PAGE 3 MAIN LAYOUT & NAVIGATION READY");
        badge.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #10B981; -fx-background-color: #ECFDF5; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: #A7F3D0; -fx-border-radius: 6;");

        Label cardTitle = new Label("Management Shell & Responsive Navigation Active");
        cardTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        Label desc = new Label("The full management navigation shell is active with responsive sidebar, admin-exclusive Users tab, "
                + "and swappable workspace views.\n\n"
                + "Next up: Page 4 (Dashboard Analytics & AI Insights).");
        desc.setWrapText(true);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-muted; -fx-line-spacing: 4px;");

        card.getChildren().addAll(icon, badge, cardTitle, desc);

        root.getChildren().addAll(header, statsGrid, card);
        setContent(root);
    }

    private Node createStatCard(String label, String value, String subtext, String accentColor, String svgPath) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.setStyle("-fx-fill: " + accentColor + ";");

        top.getChildren().addAll(lbl, icon);

        Label val = new Label(value);
        val.getStyleClass().add("stat-value");

        Label sub = new Label(subtext);
        sub.getStyleClass().add("stat-subtext");

        card.getChildren().addAll(top, val, sub);
        return card;
    }

    @Override
    public void dispose() {
    }
}
