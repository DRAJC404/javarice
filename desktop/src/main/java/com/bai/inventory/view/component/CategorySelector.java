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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

    private final ObjectProperty<CategoryItem> selectedCategoryProperty = new SimpleObjectProperty<>();
    private Consumer<Category> onCategoryCreatedCallback;

    public CategorySelector() {
        this(Mode.FORM);
    }

    public CategorySelector(Mode mode) {
        super(4);
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
        boolean showNew = (mode == Mode.FORM && canCreateCategory);
        newBtn.setVisible(showNew);
        newBtn.setManaged(showNew);
        newBtn.setOnAction(e -> openNewCategoryDialog());

        selectorRow.getChildren().addAll(comboBox, newBtn);

        getChildren().add(selectorRow);

        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedCategoryProperty.set(newVal);
        });

        refreshCategories(null);
    }

    public void refreshCategories(Integer preselectId) {
        refreshCategories(preselectId, null);
    }

    public void refreshCategories(Integer preselectId, Runnable onComplete) {
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

                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    public void openNewCategoryDialog() {
        AppDialog dialog = new AppDialog("New Category", 440);

        TextField nameField = new TextField();
        nameField.setId("categoryNameInput");
        nameField.setPromptText("Category name");
        nameField.getStyleClass().add("text-field");

        FormField field = new FormField("Category name", nameField, true, "Up to 60 characters");
        field.setId("categoryNameFormField");
        dialog.setBody(field);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setId("btnCancelCategory");
        cancelBtn.getStyleClass().add("btn-dialog-cancel");
        cancelBtn.setOnAction(e -> {
            e.consume();
            dialog.close();
            comboBox.requestFocus();
        });

        Button createBtn = new Button("Create");
        createBtn.setId("btnCreateCategory");
        createBtn.getStyleClass().add("btn-dialog-confirm");

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (field.hasError()) {
                field.clearError();
            }
        });

        Runnable doCreate = () -> {
            String rawText = nameField.getText();
            String cleanName = rawText != null ? rawText.trim() : "";

            if (cleanName.isEmpty()) {
                field.setError("Please enter a category name.");
                nameField.requestFocus();
                return;
            }

            if (cleanName.length() > 60) {
                field.setError("Category name must be 60 characters or fewer.");
                nameField.requestFocus();
                return;
            }

            field.clearError();
            createBtn.setDisable(true);
            createBtn.setText("Creating...");

            AppExecutor.execute(() -> {
                try {
                    Category created = productService.createCategory(cleanName, "", currentUser);
                    Platform.runLater(() -> {
                        dialog.close();
                        ViewNavigator.showToast("Category '" + created.getName() + "' created.", false);
                        refreshCategories(created.getId(), () -> {
                            if (onCategoryCreatedCallback != null) {
                                onCategoryCreatedCallback.accept(created);
                            } else {
                                comboBox.requestFocus();
                            }
                        });
                    });
                } catch (ProductService.ValidationException ve) {
                    Platform.runLater(() -> {
                        createBtn.setDisable(false);
                        createBtn.setText("Create");
                        field.setError(ve.getMessage());
                        nameField.requestFocus();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        createBtn.setDisable(false);
                        createBtn.setText("Create");
                        String msg = ex.getMessage();
                        if (msg != null && msg.contains("already exists")) {
                            field.setError("A category with this name already exists.");
                        } else {
                            field.setError("Something went wrong on our side. Please try again in a moment.");
                        }
                        nameField.requestFocus();
                    });
                }
            });
        };

        createBtn.setOnAction(e -> {
            e.consume();
            if (!createBtn.isDisabled()) {
                doCreate.run();
            }
        });

        nameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                if (!createBtn.isDisabled()) {
                    doCreate.run();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                dialog.close();
                comboBox.requestFocus();
            }
        });

        dialog.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                dialog.close();
                comboBox.requestFocus();
            }
        });

        dialog.setFooterButtons(cancelBtn, createBtn);
        dialog.setInitialFocusNode(nameField);
        dialog.show();
        Platform.runLater(nameField::requestFocus);
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
