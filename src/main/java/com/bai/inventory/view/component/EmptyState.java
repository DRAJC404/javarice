package com.bai.inventory.view.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public class EmptyState extends VBox {

    private final SVGPath iconPath;
    private final Label titleLabel;
    private final Label descLabel;
    private final Button actionButton;

    public EmptyState(String svgIcon, String title, String description) {
        this(svgIcon, title, description, null, null);
    }

    public EmptyState(String svgIcon, String title, String description, String actionText, Runnable onAction) {
        super(12);
        setAlignment(Pos.CENTER);
        getStyleClass().add("empty-state");

        iconPath = new SVGPath();
        iconPath.setContent(svgIcon != null ? svgIcon : "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z");
        iconPath.getStyleClass().add("empty-state-icon");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");

        descLabel = new Label(description);
        descLabel.getStyleClass().add("empty-state-desc");
        descLabel.setWrapText(true);

        getChildren().addAll(iconPath, titleLabel, descLabel);

        if (actionText != null && onAction != null) {
            actionButton = new Button(actionText);
            actionButton.getStyleClass().add("empty-state-btn");
            actionButton.setOnAction(e -> onAction.run());
            getChildren().add(actionButton);
        } else {
            actionButton = null;
        }
    }
}
