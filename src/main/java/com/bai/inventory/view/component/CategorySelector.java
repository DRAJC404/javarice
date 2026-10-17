package com.bai.inventory.view.component;

import com.bai.inventory.model.Category;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.service.ProductService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class CategorySelector extends VBox {

    public enum Mode {
        FORM("No category"),
        FILTER("All Categories");

        private final String defaultOptionLabel;
        Mode(String defaultOptionLabel) {
            this.defaultOptionLabel = defaultOptionLabel;
        }
        public String getDefaultOptionLabel() {
            return defaultOptionLabel;
        }
    }

    public record CategoryItem(Integer id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private final Mode mode;
    private final ProductService productService;
    private final User currentUser;
    private final boolean canCreateCategory;

    private final ComboBox<CategoryItem> comboBox;
    private final Button newBtn;
    private final HBox selectorRow;

    private final HBox inlineNewRow;
    private final TextField newNameField;
    private final Button saveBtn;
    private final Button cancelBtn;
    private final Label inlineErrorLabel;

    private final ObjectProperty<CategoryItem> selectedCategoryProperty = new SimpleObjectProperty<>();
    private Consumer<Category> onCategoryCreatedCallback;

    public CategorySelector() {
        this(Mode.FORM);
    }

    public CategorySelector(Mode mode) {
        super(6);
        this.mode = mode;
        this.productService = new ProductService();
        this.currentUser = AuthService.getCurrentUser();
        this.canCreateCategory = (currentUser != null && currentUser.getRole() != null && currentUser.getRole().isManagerOrAbove());

        getStyleClass().add("category-selector");

        selectorRow = new HBox(8);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(selectorRow, Priority.ALWAYS);

        comboBox = new ComboBox<>();
        comboBox.getStyleClass().add("combo-box");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(comboBox, Priority.ALWAYS);

        newBtn = new Button("+ New");
        newBtn.getStyleClass().addAll("btn-secondary", "btn-new-category");
        newBtn.setMinWidth(Button.USE_PREF_SIZE);
        newBtn.setVisible(canCreateCategory);
        newBtn.setManaged(canCreateCategory);
        newBtn.setOnAction(e -> showInlineNewRow(true));

        selectorRow.getChildren().addAll(comboBox, newBtn);

        inlineNewRow = new HBox(8);
        inlineNewRow.setAlignment(Pos.CENTER_LEFT);
        inlineNewRow.setVisible(false);
        inlineNewRow.setManaged(false);
        HBox.setHgrow(inlineNewRow, Priority.ALWAYS);

        newNameField = new TextField();
        newNameField.setPromptText("Category name (max 60 chars)");
        newNameField.getStyleClass().add("text-field");
        HBox.setHgrow(newNameField, Priority.ALWAYS);

        saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMinWidth(Button.USE_PREF_SIZE);
        saveBtn.setOnAction(e -> handleSaveCategory());

        cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setMinWidth(Button.USE_PREF_SIZE);
        cancelBtn.setOnAction(e -> showInlineNewRow(false));

        inlineNewRow.getChildren().addAll(newNameField, saveBtn, cancelBtn);

        inlineErrorLabel = new Label();
        inlineErrorLabel.getStyleClass().add("form-error");
        inlineErrorLabel.setVisible(false);
        inlineErrorLabel.setManaged(false);

        getChildren().addAll(selectorRow, inlineNewRow, inlineErrorLabel);

        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedCategoryProperty.set(newVal);
        });

        refreshCategories(null);
    }

    public void refreshCategories(Integer preselectId) {
        AppExecutor.execute(() -> {
            List<Category> categories = productService.getCategories(currentUser);
            Platform.runLater(() -> {
                CategoryItem current = comboBox.getValue();
                Integer idToSelect = preselectId != null ? preselectId : (current != null ? current.id() : null);

                comboBox.getItems().clear();
                comboBox.getItems().add(new CategoryItem(null, mode.getDefaultOptionLabel()));

                CategoryItem foundTarget = null;
                for (Category c : categories) {
                    CategoryItem item = new CategoryItem(c.getId(), c.getName());
                    comboBox.getItems().add(item);
                    if (idToSelect != null && idToSelect.equals(c.getId())) {
                        foundTarget = item;
                    }
                }

                if (foundTarget != null) {
                    comboBox.setValue(foundTarget);
                } else {
                    comboBox.getSelectionModel().selectFirst();
                }

                if (categories.isEmpty() && mode == Mode.FORM) {
                    comboBox.setPromptText("No categories yet");
                }
            });
        });
    }

    private void showInlineNewRow(boolean show) {
        selectorRow.setVisible(!show);
        selectorRow.setManaged(!show);
        inlineNewRow.setVisible(show);
        inlineNewRow.setManaged(show);
        inlineErrorLabel.setVisible(false);
        inlineErrorLabel.setManaged(false);
        if (show) {
            newNameField.clear();
            newNameField.requestFocus();
        }
    }

    private void handleSaveCategory() {
        String name = newNameField.getText() != null ? newNameField.getText().trim() : "";
        if (name.isEmpty()) {
            inlineErrorLabel.setText("Category name is required.");
            inlineErrorLabel.setVisible(true);
            inlineErrorLabel.setManaged(true);
            return;
        }
        if (name.length() > 60) {
            inlineErrorLabel.setText("Category name cannot exceed 60 characters.");
            inlineErrorLabel.setVisible(true);
            inlineErrorLabel.setManaged(true);
            return;
        }

        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");

        AppExecutor.execute(() -> {
            try {
                Category created = productService.createCategory(name, "", currentUser);
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    saveBtn.setText("Save");
                    showInlineNewRow(false);

                    CategoryItem newItem = new CategoryItem(created.getId(), created.getName());
                    comboBox.getItems().add(newItem);
                    comboBox.setValue(newItem);

                    ViewNavigator.showToast("Category '" + created.getName() + "' created.", false);
                    if (onCategoryCreatedCallback != null) {
                        onCategoryCreatedCallback.accept(created);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    saveBtn.setText("Save");
                    inlineErrorLabel.setText(ex.getMessage());
                    inlineErrorLabel.setVisible(true);
                    inlineErrorLabel.setManaged(true);
                });
            }
        });
    }

    public CategoryItem getSelectedCategory() {
        return comboBox.getValue();
    }

    public Integer getSelectedCategoryId() {
        CategoryItem item = comboBox.getValue();
        return item != null ? item.id() : null;
    }

    public void setSelectedCategoryId(Integer categoryId) {
        if (categoryId == null) {
            comboBox.getSelectionModel().selectFirst();
            return;
        }
        for (CategoryItem item : comboBox.getItems()) {
            if (categoryId.equals(item.id())) {
                comboBox.setValue(item);
                return;
            }
        }
        refreshCategories(categoryId);
    }

    public ObjectProperty<CategoryItem> selectedCategoryProperty() {
        return selectedCategoryProperty;
    }

    public ComboBox<CategoryItem> getComboBox() {
        return comboBox;
    }

    public void setOnCategoryCreated(Consumer<Category> callback) {
        this.onCategoryCreatedCallback = callback;
    }
}
