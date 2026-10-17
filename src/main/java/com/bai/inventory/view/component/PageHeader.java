package com.bai.inventory.view.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PageHeader extends BorderPane {

    private final Label titleLabel;
    private final Label subtitleLabel;
    private final HBox actionsContainer;

    public PageHeader(String titleText, String subtitleText) {
        getStyleClass().add("page-header");

        VBox titleBox = new VBox(4);
        titleLabel = new Label(titleText);
        titleLabel.getStyleClass().add("page-header-title");

        subtitleLabel = new Label(subtitleText);
        subtitleLabel.getStyleClass().add("page-header-subtitle");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        setLeft(titleBox);

        actionsContainer = new HBox(10);
        actionsContainer.setAlignment(Pos.CENTER_RIGHT);
        actionsContainer.getStyleClass().add("page-header-actions");
        setRight(actionsContainer);
    }

    public void addAction(Node actionNode) {
        actionsContainer.getChildren().add(actionNode);
    }

    public void addActions(Node... actionNodes) {
        actionsContainer.getChildren().addAll(actionNodes);
    }

    public void setTitle(String text) {
        titleLabel.setText(text);
    }

    public void setSubtitle(String text) {
        subtitleLabel.setText(text);
    }

    public HBox getActionsContainer() {
        return actionsContainer;
    }
}
