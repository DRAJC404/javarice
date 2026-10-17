package com.bai.inventory.view.component;

import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public class AppDialog extends VBox {

    private final Label titleLabel;
    private final VBox bodyContainer;
    private final HBox footerContainer;

    private static final String SVG_CLOSE = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";

    public AppDialog(String titleText, double width) {
        super(16);
        getStyleClass().add("dialog-modal-card");
        setMaxWidth(width);
        setPrefWidth(width);

        BorderPane header = new BorderPane();
        header.getStyleClass().add("dialog-header");

        titleLabel = new Label(titleText);
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setMinWidth(Label.USE_PREF_SIZE);
        header.setLeft(titleLabel);

        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("btn-dialog-close");
        SVGPath closeIcon = new SVGPath();
        closeIcon.setContent(SVG_CLOSE);
        closeIcon.getStyleClass().add("dialog-close-icon");
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(e -> ViewNavigator.hideOverlayDialog());
        header.setRight(closeBtn);

        bodyContainer = new VBox(14);
        bodyContainer.getStyleClass().add("dialog-body");

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(bodyContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setMaxHeight(480);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        footerContainer = new HBox(12);
        footerContainer.setAlignment(Pos.CENTER_RIGHT);
        footerContainer.getStyleClass().add("dialog-footer");

        getChildren().addAll(header, scrollPane, footerContainer);
    }

    public void setBody(Node... nodes) {
        bodyContainer.getChildren().setAll(nodes);
    }

    public void addBody(Node... nodes) {
        bodyContainer.getChildren().addAll(nodes);
    }

    public void addFooterButton(Button button) {
        footerContainer.getChildren().add(button);
    }

    public void setFooterButtons(Button... buttons) {
        footerContainer.getChildren().setAll(buttons);
    }

    public VBox getBodyContainer() {
        return bodyContainer;
    }

    public void show() {
        ViewNavigator.showCustomDialog(this);
    }
}
