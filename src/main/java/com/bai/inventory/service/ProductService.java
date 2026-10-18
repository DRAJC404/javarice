package com.bai.inventory.service;

import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProductService {

    public record FieldError(String field, String message) {}

    public static class AccessDeniedException extends Exception {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends Exception {
        private final List<FieldError> errors;

        public ValidationException(String message) {
            super(message);
            this.errors = List.of(new FieldError("general", message));
        }

        public ValidationException(List<FieldError> errors) {
            super(errors.isEmpty() ? "Validation failed" : errors.get(0).message());
            this.errors = errors;
        }

        public List<FieldError> getErrors() {
            return errors;
        }

        public boolean hasFieldError(String field) {
            return errors.stream().anyMatch(e -> e.field().equalsIgnoreCase(field));
        }
    }

    private final ProductDao productDao;

    public ProductService() {
        this.productDao = new ProductDao();
    }

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    public List<Category> getCategories() {
        return getCategories(AuthService.getCurrentUser());
    }

    public List<Category> getCategories(User currentUser) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        return productDao.findAllCategories(orgId);
    }

    public List<Product> getProducts(String query, Integer categoryId, User currentUser) {
        return getProducts(query, categoryId, currentUser, false);
    }

    public List<Product> getProducts(String query, Integer categoryId, User currentUser, boolean includeArchived) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        List<Product> products = productDao.findAll(query, categoryId, orgId, includeArchived);

        if (currentUser != null && currentUser.getRole() == Role.EMPLOYEE) {
            for (Product p : products) {
                p.setCostPrice(BigDecimal.ZERO);
            }
        }
        return products;
    }

    public List<FieldError> validateProduct(Product product, boolean isNew, int orgId) {
        List<FieldError> errors = new ArrayList<>();

        // 1. Picture required (png/jpg/jpeg/gif, max 5 MB, decodes as an image)
        if (product.getImagePath() == null || product.getImagePath().trim().isEmpty() || !isValidProductImage(product.getImagePath())) {
            errors.add(new FieldError("picture", "Product picture is required (PNG, JPG, or GIF, max 5 MB)."));
        }

        // 2. Product name 1-120 chars
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            errors.add(new FieldError("name", "Product name is required."));
        } else if (product.getName().trim().length() > 120) {
            errors.add(new FieldError("name", "Product name cannot exceed 120 characters."));
        }

        // 3. Category must be selected and belong to business
        if (product.getCategoryId() == null || product.getCategoryId() <= 0) {
            errors.add(new FieldError("category", "Category is required."));
        } else if (!productDao.categoryExists(product.getCategoryId(), orgId)) {
            errors.add(new FieldError("category", "Selected category does not exist or belongs to another business."));
        }

        // 4. Unit selected
        if (product.getUnit() == null || product.getUnit().trim().isEmpty()) {
            errors.add(new FieldError("unit", "Unit is required."));
        }

        // 5. Starting quantity whole number >= 0 (add only)
        if (isNew && product.getStockQuantity() < 0) {
            errors.add(new FieldError("startingQuantity", "Starting quantity must be 0 or greater."));
        }

        // 6. Selling price >= 0 with at most 2 decimals
        if (product.getSellingPrice() == null || product.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new FieldError("sellingPrice", "Selling price must be 0 or greater."));
        } else if (product.getSellingPrice().scale() > 2) {
            errors.add(new FieldError("sellingPrice", "Selling price cannot have more than 2 decimal places."));
        }

        // 7. Cost price >= 0 with at most 2 decimals
        if (product.getCostPrice() == null || product.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new FieldError("costPrice", "Cost price must be 0 or greater."));
        } else if (product.getCostPrice().scale() > 2) {
            errors.add(new FieldError("costPrice", "Cost price cannot have more than 2 decimal places."));
        }

        // 8. Low-stock level whole number >= 0
        if (product.getLowStockThreshold() < 0) {
            errors.add(new FieldError("lowStockThreshold", "Low-stock level must be 0 or greater."));
        }

        // 9. SKU 1-64 chars, unique per business
        Integer excludeId = isNew ? null : product.getId();
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            errors.add(new FieldError("sku", "SKU is required."));
        } else if (product.getSku().trim().length() > 64) {
            errors.add(new FieldError("sku", "SKU cannot exceed 64 characters."));
        } else if (productDao.isSkuTaken(product.getSku().trim(), excludeId, orgId)) {
            errors.add(new FieldError("sku", "A product with this SKU already exists in your business."));
        }

        // 10. Barcode 4-64 chars, unique per business
        if (product.getBarcode() == null || product.getBarcode().trim().isEmpty()) {
            errors.add(new FieldError("barcode", "Barcode is required."));
        } else if (product.getBarcode().trim().length() < 4 || product.getBarcode().trim().length() > 64) {
            errors.add(new FieldError("barcode", "Barcode must be between 4 and 64 characters."));
        } else if (productDao.isBarcodeTaken(product.getBarcode().trim(), excludeId, orgId)) {
            errors.add(new FieldError("barcode", "A product with this barcode already exists in your business."));
        }

        return errors;
    }

    public Product createProduct(Product product, User currentUser) throws AccessDeniedException, ValidationException, SQLException {
        if (currentUser == null || currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Employees are not authorized to create products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;

        List<FieldError> errors = validateProduct(product, true, orgId);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        try {
            return productDao.createProduct(product, orgId, currentUser.getId());
        } catch (SQLException e) {
            System.err.println("[ProductService] Error in createProduct: SQLState=" + e.getSQLState() + " Message=" + e.getMessage());
            if ("23505".equals(e.getSQLState())) {
                throw new ValidationException("A product with this SKU or Barcode already exists.");
            } else if ("23503".equals(e.getSQLState())) {
                throw new ValidationException("Selected category no longer exists or belongs to another business.");
            }
            throw e;
        }
    }

    public boolean updateProduct(Product product, User currentUser) throws AccessDeniedException, ValidationException, SQLException {
        if (currentUser == null || currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Employees are not authorized to edit products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;

        List<FieldError> errors = validateProduct(product, false, orgId);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        try {
            return productDao.updateProduct(product, orgId);
        } catch (SQLException e) {
            System.err.println("[ProductService] Error in updateProduct: SQLState=" + e.getSQLState() + " Message=" + e.getMessage());
            if ("23505".equals(e.getSQLState())) {
                throw new ValidationException("A product with this SKU or Barcode already exists.");
            } else if ("23503".equals(e.getSQLState())) {
                throw new ValidationException("Selected category no longer exists or belongs to another business.");
            }
            throw e;
        }
    }

    public boolean archiveProduct(int productId, User currentUser) throws AccessDeniedException, SQLException {
        if (currentUser == null || currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Employees are not authorized to archive products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        return productDao.archiveProduct(productId, orgId);
    }

    public boolean unarchiveProduct(int productId, User currentUser) throws AccessDeniedException, SQLException {
        if (currentUser == null || currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Employees are not authorized to restore archived products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        return productDao.unarchiveProduct(productId, orgId);
    }

    public Category createCategory(String name, String description, User currentUser)
            throws AccessDeniedException, ValidationException, SQLException {
        if (currentUser == null || currentUser.getRole() == null || !currentUser.getRole().isManagerOrAbove()) {
            throw new AccessDeniedException("Only business owners and managers can create categories.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name is required.");
        }
        String cleanName = name.trim();
        if (cleanName.length() > 60) {
            throw new ValidationException("Category name cannot exceed 60 characters.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        return productDao.createCategory(cleanName, description, orgId);
    }

    public String generateUniqueSku(User currentUser, String categoryName) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        String prefix = "PRD";
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            String clean = categoryName.trim().replaceAll("[^a-zA-Z]", "").toUpperCase();
            if (clean.length() >= 3) {
                prefix = clean.substring(0, 3);
            }
        }
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            String candidate = prefix + "-" + (100000 + rnd.nextInt(900000));
            if (!productDao.isSkuTaken(candidate, null, orgId)) {
                return candidate;
            }
        }
        return prefix + "-" + (System.currentTimeMillis() % 1000000);
    }

    public String generateUniqueBarcode(User currentUser) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            long num = 890000000000L + (long)(rnd.nextDouble() * 999999999L);
            String candidate = String.valueOf(num);
            if (!productDao.isBarcodeTaken(candidate, null, orgId)) {
                return candidate;
            }
        }
        return "890" + (System.currentTimeMillis() % 1000000000L);
    }

    public static boolean isValidProductImage(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        if (path.startsWith("demo:") || path.startsWith("/images/") || path.startsWith("http")) return true;

        File f = new File(path);
        if (!f.exists() || !f.isFile()) return false;
        if (f.length() > 5 * 1024 * 1024) return false;

        String lower = f.getName().toLowerCase();
        if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".gif")) {
            return false;
        }

        try {
            return javax.imageio.ImageIO.read(f) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
