package com.bai.inventory.service;

import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class ProductService {

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

    public Product createProduct(Product product, User currentUser) throws AccessDeniedException, ValidationException, SQLException {
        if (currentUser == null || currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Employees are not authorized to create products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;

        Integer catId = product.getCategoryId();
        if (catId != null && catId > 0) {
            if (!productDao.categoryExists(catId, orgId)) {
                throw new ValidationException("Selected category no longer exists.");
            }
        }

        try {
            return productDao.createProduct(product, orgId, currentUser.getId());
        } catch (SQLException e) {
            System.err.println("[ProductService] Error in createProduct: SQLState=" + e.getSQLState() + " Code=" + e.getErrorCode() + " Message=" + e.getMessage());
            e.printStackTrace();
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

        Integer catId = product.getCategoryId();
        if (catId != null && catId > 0) {
            if (!productDao.categoryExists(catId, orgId)) {
                throw new ValidationException("Selected category no longer exists.");
            }
        }

        try {
            return productDao.updateProduct(product, orgId);
        } catch (SQLException e) {
            System.err.println("[ProductService] Error in updateProduct: SQLState=" + e.getSQLState() + " Code=" + e.getErrorCode() + " Message=" + e.getMessage());
            e.printStackTrace();
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

    public static class AccessDeniedException extends Exception {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
