package com.bai.inventory.view.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StatCard extends VBox {

    private final Label titleLabel;
    private final Label valueLabel;
    private final Label subLabel;

    public enum StatType {
        PRIMARY("stat-primary"),
        SUCCESS("stat-success"),
        WARNING("stat-warning"),
        DANGER("stat-danger"),
        INFO("stat-info");

        private final String cssClass;
        StatType(String cssClass) { this.cssClass = cssClass; }
        public String getCssClass() { return cssClass; }
    }

    public StatCard(String title, String initialValue, StatType type) {
        this(title, initialValue, null, type);
    }

    public StatCard(String title, String initialValue, String subtitle, StatType type) {
        super(6);
        getStyleClass().addAll("stat-card", type.getCssClass());
        HBox.setHgrow(this, Priority.ALWAYS);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-card-title");

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("stat-card-value");

        getChildren().addAll(titleLabel, valueLabel);

        subLabel = new Label(subtitle != null ? subtitle : "");
        subLabel.getStyleClass().add("stat-card-sub");
        if (subtitle != null && !subtitle.isEmpty()) {
            getChildren().add(subLabel);
        }
    }

    public void setValue(String val) {
        valueLabel.setText(val);
    }

    public void setSubtitle(String sub) {
        subLabel.setText(sub);
        if (!getChildren().contains(subLabel) && sub != null && !sub.isEmpty()) {
            getChildren().add(subLabel);
        }
    }

    public Label getValueLabel() {
        return valueLabel;
    }
}
