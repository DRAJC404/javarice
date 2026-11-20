package com.bai.inventory.view.component;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class FormField extends VBox {

    private final Label label;
    private final Node inputControl;
    private final Label errorLabel;
    private final Label helperLabel;

    public FormField(String labelText, Node inputControl) {
        this(labelText, inputControl, false, null);
    }

    public FormField(String labelText, Node inputControl, boolean required) {
        this(labelText, inputControl, required, null);
    }

    public FormField(String labelText, Node inputControl, boolean required, String helperText) {
        super(4);
        this.inputControl = inputControl;
        getStyleClass().add("form-field");

        javafx.scene.layout.HBox labelBox = new javafx.scene.layout.HBox(2);
        labelBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        label = new Label(labelText);
        label.getStyleClass().add("form-label");
        label.setMinWidth(Label.USE_PREF_SIZE);
        labelBox.getChildren().add(label);

        if (required) {
            Label star = new Label("*");
            star.getStyleClass().addAll("form-label", "form-label-required");
            labelBox.getChildren().add(star);
        }

        errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        getChildren().addAll(labelBox, inputControl, errorLabel);

        if (helperText != null && !helperText.isEmpty()) {
            helperLabel = new Label(helperText);
            helperLabel.getStyleClass().add("form-helper");
            getChildren().add(helperLabel);
        } else {
            helperLabel = null;
        }
    }

    public void setError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            errorLabel.setText(error);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            if (!inputControl.getStyleClass().contains("input-error")) {
                inputControl.getStyleClass().add("input-error");
            }
        } else {
            clearError();
        }
    }

    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        inputControl.getStyleClass().remove("input-error");
    }

    public boolean hasError() {
        return errorLabel.isVisible();
    }

    public void requestFocusOnInput() {
        inputControl.requestFocus();
    }

    public Node getInputControl() {
        return inputControl;
    }
}
