package com.bai.inventory.view.component;

import javafx.scene.control.Label;

public class Badge extends Label {

    public enum BadgeVariant {
        SUCCESS("badge-success"),
        WARNING("badge-warning"),
        DANGER("badge-danger"),
        INFO("badge-info"),
        NEUTRAL("badge-neutral");

        private final String cssClass;
        BadgeVariant(String cssClass) { this.cssClass = cssClass; }
        public String getCssClass() { return cssClass; }
    }

    public Badge(String text, BadgeVariant variant) {
        super(text);
        getStyleClass().addAll("badge", variant.getCssClass());
    }

    public static Badge success(String text) {
        return new Badge(text, BadgeVariant.SUCCESS);
    }

    public static Badge warning(String text) {
        return new Badge(text, BadgeVariant.WARNING);
    }

    public static Badge danger(String text) {
        return new Badge(text, BadgeVariant.DANGER);
    }

    public static Badge info(String text) {
        return new Badge(text, BadgeVariant.INFO);
    }

    public static Badge neutral(String text) {
        return new Badge(text, BadgeVariant.NEUTRAL);
    }

    public void setVariant(BadgeVariant variant) {
        getStyleClass().removeIf(c -> c.startsWith("badge-"));
        getStyleClass().add(variant.getCssClass());
    }
}
