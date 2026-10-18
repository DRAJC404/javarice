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
        getStyleClass().add("form-field");

        this.inputControl = inputControl;

        label = new Label(labelText + (required ? " *" : ""));
        label.getStyleClass().add("form-label");
        label.setMinWidth(Label.USE_PREF_SIZE);
        if (required) {
            label.getStyleClass().add("form-label-required");
        }

        errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        getChildren().addAll(label, inputControl, errorLabel);

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
            inputControl.getStyleClass().add("input-error");
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

    public Node getInputControl() {
        return inputControl;
    }
}
