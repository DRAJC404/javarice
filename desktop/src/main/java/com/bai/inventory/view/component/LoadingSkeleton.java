package com.bai.inventory.view.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class LoadingSkeleton extends VBox {

    private final ProgressIndicator spinner;
    private final Label loadingText;

    public LoadingSkeleton() {
        this("Loading data...");
    }

    public LoadingSkeleton(String message) {
        super(12);
        setAlignment(Pos.CENTER);
        getStyleClass().add("loading-skeleton");

        spinner = new ProgressIndicator();
        spinner.getStyleClass().add("loading-spinner");
        spinner.setMaxSize(40, 40);

        loadingText = new Label(message);
        loadingText.getStyleClass().add("loading-text");

        getChildren().addAll(spinner, loadingText);
    }

    public void setMessage(String message) {
        loadingText.setText(message);
    }
}
