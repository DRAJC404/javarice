package com.bai.inventory.view.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.SVGPath;

public class ErrorBanner extends HBox {

    private final Label messageLabel;
    private final Button retryButton;

    private static final String SVG_ERROR = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z";

    public ErrorBanner(String message, Runnable onRetry) {
        super(12);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("error-banner");

        SVGPath icon = new SVGPath();
        icon.setContent(SVG_ERROR);
        icon.getStyleClass().add("error-banner-icon");

        messageLabel = new Label(message);
        messageLabel.getStyleClass().add("error-banner-text");
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        getChildren().addAll(icon, messageLabel);

        if (onRetry != null) {
            retryButton = new Button("Retry");
            retryButton.getStyleClass().add("btn-error-retry");
            retryButton.setOnAction(e -> onRetry.run());
            getChildren().add(retryButton);
        } else {
            retryButton = null;
        }
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}
