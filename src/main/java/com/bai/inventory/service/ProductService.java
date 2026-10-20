package com.bai.inventory.service;

import com.bai.inventory.dao.ProductDao;
import com.bai.inventory.dao.ProductHistoryDao;
import com.bai.inventory.dao.ProductSubmissionDao;
import com.bai.inventory.dao.ReportDao;
import com.bai.inventory.model.*;
import com.bai.inventory.util.Constants;
import com.bai.inventory.util.ProductImageManager;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final ProductHistoryDao productHistoryDao;
    private final ProductSubmissionDao productSubmissionDao;
    private final ReportDao reportDao;

    public ProductService() {
        this(new ProductDao(), new ProductHistoryDao(), new ProductSubmissionDao(), new ReportDao());
    }

    public ProductService(ProductDao productDao) {
        this(productDao, new ProductHistoryDao(), new ProductSubmissionDao(), new ReportDao());
    }

    public ProductService(ProductDao productDao, ProductHistoryDao productHistoryDao, ProductSubmissionDao productSubmissionDao) {
        this(productDao, productHistoryDao, productSubmissionDao, new ReportDao());
    }

    public ProductService(ProductDao productDao, ProductHistoryDao productHistoryDao,
                          ProductSubmissionDao productSubmissionDao, ReportDao reportDao) {
        this.productDao = productDao;
        this.productHistoryDao = productHistoryDao;
        this.productSubmissionDao = productSubmissionDao;
        this.reportDao = reportDao;
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

        if (product.getImagePath() == null || product.getImagePath().trim().isEmpty() || !isValidProductImage(product.getImagePath())) {
            errors.add(new FieldError("picture", "Please choose a PNG, JPG, or GIF under 5 MB."));
        }

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            errors.add(new FieldError("name", "Product name is required."));
        } else if (product.getName().trim().length() > 120) {
            errors.add(new FieldError("name", "Product name cannot exceed 120 characters."));
        }

        if (product.getCategoryId() == null || product.getCategoryId() <= 0) {
            errors.add(new FieldError("category", "Category is required."));
        } else if (!productDao.categoryExists(product.getCategoryId(), orgId)) {
            errors.add(new FieldError("category", "Selected category does not exist or belongs to another business."));
        }

        product.setUnit(Constants.UNIT);

        if (isNew && product.getStockQuantity() < 0) {
            errors.add(new FieldError("startingQuantity", "Please enter a whole number of pieces."));
        }

        if (product.getSellingPrice() == null || product.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new FieldError("sellingPrice", "Selling price must be 0 or greater."));
        } else if (product.getSellingPrice().scale() > 2) {
            errors.add(new FieldError("sellingPrice", "Selling price cannot have more than 2 decimal places."));
        }

        if (product.getCostPrice() == null || product.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new FieldError("costPrice", "Cost price must be 0 or greater."));
        } else if (product.getCostPrice().scale() > 2) {
            errors.add(new FieldError("costPrice", "Cost price cannot have more than 2 decimal places."));
        }

        if (product.getLowStockThreshold() < 0) {
            errors.add(new FieldError("lowStockThreshold", "Please enter a whole number of pieces."));
        }

        Integer excludeId = isNew ? null : product.getId();
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            errors.add(new FieldError("sku", "SKU is required."));
        } else if (product.getSku().trim().length() > 64) {
            errors.add(new FieldError("sku", "SKU cannot exceed 64 characters."));
        } else if (productDao.isSkuTaken(product.getSku().trim(), excludeId, orgId)) {
            errors.add(new FieldError("sku", "A product with this SKU already exists in your business."));
        }

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
        if (currentUser == null) {
            throw new AccessDeniedException("User must be logged in to create products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        product.setUnit(Constants.UNIT);

        List<FieldError> errors = validateProduct(product, true, orgId);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        boolean isEmployee = (currentUser.getRole() == Role.EMPLOYEE);
        if (isEmployee) {
            product.setApprovalStatus("PENDING");
        } else {
            product.setApprovalStatus("APPROVED");
        }

        try {
            Product created = productDao.createProduct(product, orgId, currentUser.getId());

            if (isEmployee) {
                ProductSubmission sub = new ProductSubmission();
                sub.setOrganizationId(orgId);
                sub.setProductId(created.getId());
                sub.setSubmittedBy(currentUser.getId());
                sub.setSubmissionType("CREATE");
                sub.setStatus("PENDING");
                sub.setName(created.getName());
                sub.setCategoryId(created.getCategoryId());
                sub.setSku(created.getSku());
                sub.setBarcode(created.getBarcode());
                sub.setCostPrice(created.getCostPrice());
                sub.setSellingPrice(created.getSellingPrice());
                sub.setStockQuantity(created.getStockQuantity());
                sub.setLowStockThreshold(created.getLowStockThreshold());
                sub.setImagePath(created.getImagePath());
                productSubmissionDao.createSubmission(sub);

                productHistoryDao.record(created.getId(), "SUBMIT_CREATE", "product", null, created.getName(), currentUser.getId());
            } else {
                productHistoryDao.record(created.getId(), "CREATE", "product", null, created.getName(), currentUser.getId());
            }

            return created;
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
        if (currentUser == null) {
            throw new AccessDeniedException("User must be logged in to edit products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        product.setUnit(Constants.UNIT);

        List<FieldError> errors = validateProduct(product, false, orgId);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Optional<Product> oldProductOpt = productDao.findById(product.getId(), orgId);
        if (oldProductOpt.isEmpty()) {
            throw new ValidationException("Product not found.");
        }
        Product oldP = oldProductOpt.get();

        boolean isEmployee = (currentUser.getRole() == Role.EMPLOYEE);
        if (isEmployee) {
            ProductSubmission sub = new ProductSubmission();
            sub.setOrganizationId(orgId);
            sub.setProductId(product.getId());
            sub.setSubmittedBy(currentUser.getId());
            sub.setSubmissionType("UPDATE");
            sub.setStatus("PENDING");

            sub.setName(product.getName());
            sub.setCategoryId(product.getCategoryId());
            sub.setSku(product.getSku());
            sub.setBarcode(product.getBarcode());
            sub.setCostPrice(product.getCostPrice());
            sub.setSellingPrice(product.getSellingPrice());
            sub.setStockQuantity(product.getStockQuantity());
            sub.setLowStockThreshold(product.getLowStockThreshold());
            sub.setImagePath(product.getImagePath());

            sub.setOldName(oldP.getName());
            sub.setOldCategoryId(oldP.getCategoryId());
            sub.setOldSku(oldP.getSku());
            sub.setOldBarcode(oldP.getBarcode());
            sub.setOldCostPrice(oldP.getCostPrice());
            sub.setOldSellingPrice(oldP.getSellingPrice());
            sub.setOldStockQuantity(oldP.getStockQuantity());
            sub.setOldLowStockThreshold(oldP.getLowStockThreshold());
            sub.setOldImagePath(oldP.getImagePath());

            productSubmissionDao.createSubmission(sub);
            productHistoryDao.record(product.getId(), "SUBMIT_UPDATE", "product", null, "Submitted update for approval", currentUser.getId());
            return true;
        }

        try {
            String oldImagePath = oldP.getImagePath();
            boolean updated = productDao.updateProduct(product, orgId);
            if (updated) {
                if (!Objects.equals(oldP.getName(), product.getName())) {
                    productHistoryDao.record(product.getId(), "UPDATE", "name", oldP.getName(), product.getName(), currentUser.getId());
                }
                if (oldP.getSellingPrice() != null && product.getSellingPrice() != null &&
                        oldP.getSellingPrice().compareTo(product.getSellingPrice()) != 0) {
                    productHistoryDao.record(product.getId(), "UPDATE", "selling_price",
                            String.format("₱%,.2f", oldP.getSellingPrice()),
                            String.format("₱%,.2f", product.getSellingPrice()), currentUser.getId());
                }
                if (oldP.getCostPrice() != null && product.getCostPrice() != null &&
                        oldP.getCostPrice().compareTo(product.getCostPrice()) != 0) {
                    productHistoryDao.record(product.getId(), "UPDATE", "cost_price",
                            String.format("₱%,.2f", oldP.getCostPrice()),
                            String.format("₱%,.2f", product.getCostPrice()), currentUser.getId());
                }
                if (!Objects.equals(oldP.getCategoryId(), product.getCategoryId())) {
                    productHistoryDao.record(product.getId(), "UPDATE", "category_id",
                            String.valueOf(oldP.getCategoryId()), String.valueOf(product.getCategoryId()), currentUser.getId());
                }
                if (oldP.getLowStockThreshold() != product.getLowStockThreshold()) {
                    productHistoryDao.record(product.getId(), "UPDATE", "low_stock_threshold",
                            String.valueOf(oldP.getLowStockThreshold()), String.valueOf(product.getLowStockThreshold()), currentUser.getId());
                }

                if (oldImagePath != null && !oldImagePath.equals(product.getImagePath())) {
                    ProductImageManager.deleteOldImage(oldImagePath, orgId);
                }
            }
            return updated;
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

    public boolean approveSubmission(int submissionId, User currentUser) throws AccessDeniedException, ValidationException, SQLException {
        if (currentUser == null || !currentUser.getRole().isManagerOrAbove()) {
            throw new AccessDeniedException("Only business owners and managers can approve product submissions.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        Optional<ProductSubmission> subOpt = productSubmissionDao.findById(submissionId, orgId);
        if (subOpt.isEmpty()) {
            throw new ValidationException("Submission not found.");
        }
        ProductSubmission sub = subOpt.get();

        if (sub.getSubmittedBy() == currentUser.getId()) {
            throw new AccessDeniedException("You cannot approve your own submission.");
        }
        if (!"PENDING".equalsIgnoreCase(sub.getStatus())) {
            throw new ValidationException("This submission has already been reviewed.");
        }

        if ("CREATE".equalsIgnoreCase(sub.getSubmissionType())) {
            if (sub.getProductId() != null) {
                productDao.updateApprovalStatus(sub.getProductId(), "APPROVED", orgId);
                productHistoryDao.record(sub.getProductId(), "APPROVE", "approval_status", "PENDING", "APPROVED", currentUser.getId());
            }
        } else if ("UPDATE".equalsIgnoreCase(sub.getSubmissionType())) {
            if (sub.getProductId() != null) {
                Optional<Product> prodOpt = productDao.findById(sub.getProductId(), orgId);
                if (prodOpt.isPresent()) {
                    Product p = prodOpt.get();
                    for (ProductSubmission.FieldDiff diff : sub.getFieldDifferences()) {
                        productHistoryDao.record(sub.getProductId(), "APPROVE_UPDATE", diff.fieldName(), diff.oldValue(), diff.newValue(), currentUser.getId());
                    }
                    p.setName(sub.getName());
                    p.setCategoryId(sub.getCategoryId());
                    p.setBarcode(sub.getBarcode());
                    p.setCostPrice(sub.getCostPrice());
                    p.setSellingPrice(sub.getSellingPrice());
                    p.setLowStockThreshold(sub.getLowStockThreshold());
                    p.setImagePath(sub.getImagePath());
                    productDao.updateProduct(p, orgId);
                }
            }
        }

        return productSubmissionDao.updateStatus(submissionId, "APPROVED", null, currentUser.getId(), orgId);
    }

    public boolean rejectSubmission(int submissionId, String reason, User currentUser) throws AccessDeniedException, ValidationException, SQLException {
        if (currentUser == null || !currentUser.getRole().isManagerOrAbove()) {
            throw new AccessDeniedException("Only business owners and managers can reject product submissions.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Please provide a reason for the rejection.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        Optional<ProductSubmission> subOpt = productSubmissionDao.findById(submissionId, orgId);
        if (subOpt.isEmpty()) {
            throw new ValidationException("Submission not found.");
        }
        ProductSubmission sub = subOpt.get();

        if (sub.getSubmittedBy() == currentUser.getId()) {
            throw new AccessDeniedException("You cannot reject your own submission.");
        }
        if (!"PENDING".equalsIgnoreCase(sub.getStatus())) {
            throw new ValidationException("This submission has already been reviewed.");
        }

        if ("CREATE".equalsIgnoreCase(sub.getSubmissionType())) {
            if (sub.getProductId() != null) {
                productDao.updateApprovalStatus(sub.getProductId(), "REJECTED", orgId);
                productHistoryDao.record(sub.getProductId(), "REJECT", "approval_status", "PENDING", "REJECTED", currentUser.getId());
            }
        } else if ("UPDATE".equalsIgnoreCase(sub.getSubmissionType())) {
            if (sub.getProductId() != null) {
                productHistoryDao.record(sub.getProductId(), "REJECT_UPDATE", "rejection_reason", null, reason.trim(), currentUser.getId());
            }
        }

        return productSubmissionDao.updateStatus(submissionId, "REJECTED", reason.trim(), currentUser.getId(), orgId);
    }

    public boolean softDeleteProduct(int productId, User currentUser) throws AccessDeniedException, SQLException {
        if (currentUser == null || !currentUser.getRole().isManagerOrAbove()) {
            throw new AccessDeniedException("Only business owners and managers can delete products.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        boolean deleted = productDao.softDelete(productId, orgId);
        if (deleted) {
            productHistoryDao.record(productId, "DELETE", "is_deleted", "false", "true", currentUser.getId());
        }
        return deleted;
    }

    public List<ProductHistory> getProductHistory(int productId, String actionFilter, LocalDate fromDate, LocalDate toDate) {
        return productHistoryDao.findByProductId(productId, actionFilter, fromDate, toDate);
    }

    public List<ProductSubmission> getPendingSubmissions(User currentUser) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        return productSubmissionDao.findPendingSubmissions(orgId);
    }

    public List<ProductSubmission> getMySubmissions(User currentUser) {
        if (currentUser == null) return List.of();
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        return productSubmissionDao.findSubmissionsByUser(currentUser.getId(), orgId);
    }

    public int getPendingSubmissionsCount(User currentUser) {
        if (currentUser == null) return 0;
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        return productSubmissionDao.countPending(orgId);
    }

    public List<ProductPerformance> getProductPerformance(User currentUser, LocalDateTime from, LocalDateTime to) {
        int orgId = (currentUser != null && currentUser.getOrganizationId() > 0) ? currentUser.getOrganizationId() : 1;
        return reportDao.getProductPerformanceReport(orgId, from, to);
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
            throw new ValidationException("Please enter a category name.");
        }
        String cleanName = name.trim();
        if (cleanName.length() > 60) {
            throw new ValidationException("Category name must be 60 characters or fewer.");
        }
        int orgId = currentUser.getOrganizationId() > 0 ? currentUser.getOrganizationId() : 1;
        if (productDao.isCategoryNameTaken(cleanName, orgId)) {
            throw new ValidationException("A category with this name already exists.");
        }
        try {
            return productDao.createCategory(cleanName, description, orgId);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                throw new ValidationException("A category with this name already exists.");
            }
            throw new SQLException("Something went wrong on our side. Please try again in a moment.", e);
        }
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
        if (f.isAbsolute() && f.exists() && f.isFile()) {
            return ProductImageManager.isValidImageFile(f);
        }

        File resolved = ProductImageManager.resolveImageFile(path, 1);
        if (resolved != null && resolved.exists() && resolved.isFile()) {
            return ProductImageManager.isValidImageFile(resolved);
        }

        return false;
    }
}
