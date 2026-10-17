package com.bai.inventory.view.navigation;

public enum ViewType {
    LOGIN("Login"),
    REGISTER("Register"),
    JOIN("Join Organization"),
    STORE_TERMINAL("Store Terminal"),
    DASHBOARD("Dashboard"),
    PRODUCT("Product"),
    INVENTORY("Inventory"),
    ANALYTICS("Analytics"),
    REPORT("Report"),
    AI_INSIGHTS("AI Insights"),
    USERS("Users");

    private final String title;

    ViewType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
