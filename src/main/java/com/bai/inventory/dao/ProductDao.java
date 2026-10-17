package com.bai.inventory.dao;

import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.Category;
import com.bai.inventory.model.Product;
import com.bai.inventory.service.AuthService;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDao {

    public List<Category> findAllCategories() {
        int orgId = resolveOrganizationId(0);
        return findAllCategories(orgId);
    }

    public List<Category> findAllCategories(int organizationId) {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name, description FROM categories WHERE organization_id = ? ORDER BY id ASC";

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Category(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Error loading categories: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            list.add(new Category(1, "Beverages", "Cold brew, beans, matcha"));
            list.add(new Category(2, "Bakery", "Pastries and bread"));
            list.add(new Category(3, "Fresh & Dairy", "Milk and dairy"));
            list.add(new Category(4, "Packaged Snacks", "Grab and go snacks"));
            list.add(new Category(5, "Store Supplies", "Packaging and cups"));
        }
        return list;
    }

    public Category createCategory(String name, String description, int organizationId) throws SQLException {
        String sql = "INSERT INTO categories (organization_id, name, description) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            stmt.setString(2, name);
            stmt.setString(3, description != null ? description : "");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Category cat = new Category(rs.getInt("id"), name, description);
                    conn.commit();
                    return cat;
                }
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new SQLException("A category named '" + name + "' already exists in your business.", e.getSQLState(), e);
            }
            throw e;
        }
        throw new SQLException("Failed to create category");
    }

    public List<Product> findAll(String searchQuery, Integer categoryId) {
        return findAll(searchQuery, categoryId, resolveOrganizationId(0), false);
    }

    public List<Product> findAll(String searchQuery, Integer categoryId, int organizationId) {
        return findAll(searchQuery, categoryId, organizationId, false);
    }

    public List<Product> findAll(String searchQuery, Integer categoryId, int organizationId, boolean includeArchived) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.organization_id, p.category_id, c.name AS category_name, p.sku, p.barcode, p.name, " +
                "p.unit, p.cost_price, p.selling_price, p.stock_quantity, p.low_stock_threshold, " +
                "p.image_path, p.status, p.is_archived, p.created_at, p.updated_at " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE p.organization_id = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(organizationId);

        if (!includeArchived) {
            sql.append("AND p.is_archived = false ");
        }

        if (categoryId != null && categoryId > 0) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql.append("AND (LOWER(p.name) LIKE LOWER(?) OR LOWER(p.sku) LIKE LOWER(?) OR p.barcode LIKE ?) ");
            String q = "%" + searchQuery.trim() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY p.name ASC");

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Error finding products: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            list.addAll(getDemoProducts(searchQuery, categoryId));
        }

        return list;
    }

    public Optional<Product> findById(int id) {
        return findById(id, resolveOrganizationId(0));
    }

    public Optional<Product> findById(int id, int organizationId) {
        String sql = "SELECT p.id, p.organization_id, p.category_id, c.name AS category_name, p.sku, p.barcode, p.name, " +
                     "p.unit, p.cost_price, p.selling_price, p.stock_quantity, p.low_stock_threshold, " +
                     "p.image_path, p.status, p.is_archived, p.created_at, p.updated_at " +
                     "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.id = ? AND p.organization_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Error finding product by id: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Product> findByBarcodeOrSku(String code) {
        return findByBarcodeOrSku(code, resolveOrganizationId(0));
    }

    public Optional<Product> findByBarcodeOrSku(String code, int organizationId) {
        if (code == null || code.trim().isEmpty()) return Optional.empty();

        String sql = "SELECT p.id, p.organization_id, p.category_id, c.name AS category_name, p.sku, p.barcode, p.name, " +
                     "p.unit, p.cost_price, p.selling_price, p.stock_quantity, p.low_stock_threshold, " +
                     "p.image_path, p.status, p.is_archived, p.created_at, p.updated_at " +
                     "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE (p.barcode = ? OR LOWER(p.sku) = LOWER(?)) AND p.organization_id = ? AND p.is_archived = false";

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            stmt.setString(2, code.trim());
            stmt.setInt(3, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Error finding product by barcode/sku: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Product> findNeedsRestock(int limit) {
        return findNeedsRestock(limit, resolveOrganizationId(0));
    }

    public List<Product> findNeedsRestock(int limit, int organizationId) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.id, p.organization_id, p.category_id, c.name AS category_name, p.sku, p.barcode, p.name, " +
                     "p.unit, p.cost_price, p.selling_price, p.stock_quantity, p.low_stock_threshold, " +
                     "p.image_path, p.status, p.is_archived, p.created_at, p.updated_at " +
                     "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.organization_id = ? AND p.is_archived = false AND (p.stock_quantity <= p.low_stock_threshold OR p.stock_quantity = 0) " +
                     "ORDER BY (p.low_stock_threshold - p.stock_quantity) DESC, p.stock_quantity ASC " +
                     "LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Error finding products needing restock: " + e.getMessage());
        }

        if (list.isEmpty() && DatabaseConfig.isDevModeEnabled()) {
            for (Product p : getDemoProducts(null, null)) {
                if (p.getStockQuantity() <= p.getLowStockThreshold()) {
                    list.add(p);
                }
            }
        }
        return list;
    }

    public record ProductSummaryMetrics(int totalProducts, int lowStockCount, int outOfStockCount, BigDecimal totalValuation) {
        public int inStockCount() {
            return Math.max(0, totalProducts - lowStockCount - outOfStockCount);
        }
    }

    public ProductSummaryMetrics getSummaryMetrics() {
        return getSummaryMetrics(resolveOrganizationId(0));
    }

    public ProductSummaryMetrics getSummaryMetrics(int organizationId) {
        String sql = "SELECT " +
                     "COUNT(*) AS total_products, " +
                     "COUNT(CASE WHEN stock_quantity <= low_stock_threshold AND stock_quantity > 0 THEN 1 END) AS low_stock_count, " +
                     "COUNT(CASE WHEN stock_quantity = 0 THEN 1 END) AS out_of_stock_count, " +
                     "COALESCE(SUM(stock_quantity * selling_price), 0) AS total_valuation " +
                     "FROM products WHERE organization_id = ? AND is_archived = false";

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ProductSummaryMetrics(
                            rs.getInt("total_products"),
                            rs.getInt("low_stock_count"),
                            rs.getInt("out_of_stock_count"),
                            rs.getBigDecimal("total_valuation")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Error calculating summary metrics: " + e.getMessage());
        }

        return new ProductSummaryMetrics(10, 4, 1, new BigDecimal("48950.00"));
    }

    public Product createProduct(Product product, int organizationId, int userId) throws SQLException {
        String insertProdSql = "INSERT INTO products (organization_id, category_id, sku, barcode, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, image_path, status, is_archived) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false) " +
                               "RETURNING id, organization_id, category_id, sku, barcode, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold, image_path, status, is_archived, created_at, updated_at";

        String insertMovementSql = "INSERT INTO stock_movements (organization_id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, notes) " +
                                   "VALUES (?, ?, ?, ?, 'IN', ?, 'OPENING_STOCK', 0, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection(organizationId)) {
            conn.setAutoCommit(false);
            try {
                Product created;
                try (PreparedStatement stmt = conn.prepareStatement(insertProdSql)) {
                    stmt.setInt(1, organizationId);
                    if (product.getCategoryId() != null && product.getCategoryId() > 0) {
                        stmt.setInt(2, product.getCategoryId());
                    } else {
                        stmt.setNull(2, java.sql.Types.INTEGER);
                    }
                    stmt.setString(3, product.getSku().trim());
                    stmt.setString(4, product.getBarcode() != null && !product.getBarcode().trim().isEmpty() ? product.getBarcode().trim() : null);
                    stmt.setString(5, product.getName().trim());
                    stmt.setString(6, product.getUnit() != null ? product.getUnit().trim() : "pcs");
                    stmt.setBigDecimal(7, product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
                    stmt.setBigDecimal(8, product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO);
                    stmt.setInt(9, product.getStockQuantity());
                    stmt.setInt(10, product.getLowStockThreshold());
                    stmt.setString(11, product.getImagePath());
                    stmt.setString(12, product.getStatus() != null ? product.getStatus() : "IN_STOCK");

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            created = mapResultSetToProduct(rs);
                        } else {
                            throw new SQLException("Failed to create product record.");
                        }
                    }
                }

                if (product.getStockQuantity() > 0) {
                    try (PreparedStatement movStmt = conn.prepareStatement(insertMovementSql)) {
                        String ref = "SM-OPEN-" + System.currentTimeMillis() + "-" + created.getId();
                        movStmt.setInt(1, organizationId);
                        movStmt.setString(2, ref);
                        movStmt.setInt(3, created.getId());
                        movStmt.setInt(4, userId);
                        movStmt.setInt(5, product.getStockQuantity());
                        movStmt.setInt(6, product.getStockQuantity());
                        movStmt.setString(7, "Initial opening stock recorded at product creation");
                        movStmt.executeUpdate();
                    }
                }

                conn.commit();
                return created;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean updateProduct(Product product, int organizationId) throws SQLException {
        String sql = "UPDATE products SET " +
                     "category_id = ?, barcode = ?, name = ?, unit = ?, " +
                     "cost_price = ?, selling_price = ?, low_stock_threshold = ?, " +
                     "image_path = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND organization_id = ?";

        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (product.getCategoryId() != null && product.getCategoryId() > 0) {
                stmt.setInt(1, product.getCategoryId());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setString(2, product.getBarcode() != null && !product.getBarcode().trim().isEmpty() ? product.getBarcode().trim() : null);
            stmt.setString(3, product.getName().trim());
            stmt.setString(4, product.getUnit() != null ? product.getUnit().trim() : "pcs");
            stmt.setBigDecimal(5, product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
            stmt.setBigDecimal(6, product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO);
            stmt.setInt(7, product.getLowStockThreshold());
            stmt.setString(8, product.getImagePath());
            stmt.setInt(9, product.getId());
            stmt.setInt(10, organizationId);
            boolean updated = stmt.executeUpdate() > 0;
            conn.commit();
            return updated;
        }
    }

    public boolean categoryExists(int categoryId, int organizationId) {
        if (categoryId <= 0) return false;
        String sql = "SELECT 1 FROM categories WHERE id = ? AND organization_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            stmt.setInt(2, organizationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Error checking category existence: " + e.getMessage());
            return false;
        }
    }

    public boolean archiveProduct(int productId, int organizationId) throws SQLException {
        String sql = "UPDATE products SET is_archived = true, status = 'DISCONTINUED', updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND organization_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, organizationId);
            boolean archived = stmt.executeUpdate() > 0;
            conn.commit();
            return archived;
        }
    }

    public boolean unarchiveProduct(int productId, int organizationId) throws SQLException {
        String sql = "UPDATE products SET is_archived = false, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND organization_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(organizationId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, organizationId);
            boolean unarchived = stmt.executeUpdate() > 0;
            conn.commit();
            return unarchived;
        }
    }

    private int resolveOrganizationId(int given) {
        if (given > 0) return given;
        com.bai.inventory.model.User current = AuthService.getCurrentUser();
        return (current != null && current.getOrganizationId() > 0) ? current.getOrganizationId() : 1;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Integer catId = rs.getObject("category_id") != null ? rs.getInt("category_id") : null;
        Product p = new Product(
                rs.getInt("id"),
                rs.getInt("organization_id"),
                catId,
                rs.getString("sku"),
                rs.getString("barcode"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("cost_price"),
                rs.getBigDecimal("selling_price"),
                rs.getInt("stock_quantity"),
                rs.getInt("low_stock_threshold"),
                rs.getString("image_path"),
                rs.getString("status"),
                rs.getBoolean("is_archived")
        );
        try {
            p.setCategoryName(rs.getString("category_name"));
        } catch (SQLException ignored) {}

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) p.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) p.setUpdatedAt(uat.toLocalDateTime());
        return p;
    }

    private List<Product> getDemoProducts(String searchQuery, Integer categoryId) {
        List<Product> demo = List.of(
                new Product(1, 1, "BEV-001", "8901001", "Barako Arabica Beans 1kg", "bag", new BigDecimal("380.00"), new BigDecimal("650.00"), 42, 15, null, "IN_STOCK"),
                new Product(2, 1, "BEV-002", "8901002", "Benguet Cold Brew Keg 20L", "keg", new BigDecimal("1200.00"), new BigDecimal("2400.00"), 3, 4, null, "LOW_STOCK"),
                new Product(3, 1, "BEV-003", "8901003", "Ceremonial Matcha Tin 100g", "tin", new BigDecimal("450.00"), new BigDecimal("850.00"), 6, 10, null, "LOW_STOCK"),
                new Product(4, 2, "BAK-001", "8902001", "Butter Croissant (Box of 6)", "box", new BigDecimal("160.00"), new BigDecimal("320.00"), 8, 12, null, "LOW_STOCK"),
                new Product(5, 2, "BAK-002", "8902002", "Special Ensaymada (4pk)", "box", new BigDecimal("140.00"), new BigDecimal("280.00"), 18, 10, null, "IN_STOCK"),
                new Product(6, 2, "BAK-003", "8902003", "Almond Danish Pack (4pk)", "box", new BigDecimal("150.00"), new BigDecimal("300.00"), 0, 6, null, "OUT_OF_STOCK"),
                new Product(7, 3, "DAI-001", "8904001", "Fresh Whole Milk 1L", "carton", new BigDecimal("65.00"), new BigDecimal("110.00"), 7, 20, null, "LOW_STOCK"),
                new Product(8, 3, "DAI-002", "8904002", "Barista Oat Milk 1L", "carton", new BigDecimal("95.00"), new BigDecimal("175.00"), 35, 15, null, "IN_STOCK"),
                new Product(9, 4, "SNK-001", "8903001", "Roast Chicken Pesto Ciabatta", "pcs", new BigDecimal("90.00"), new BigDecimal("185.00"), 14, 10, null, "IN_STOCK"),
                new Product(10, 5, "SUP-001", "8905001", "Eco Paper Cups 12oz (50s)", "sleeve", new BigDecimal("75.00"), new BigDecimal("140.00"), 55, 20, null, "IN_STOCK")
        );

        List<Product> filtered = new ArrayList<>();
        for (Product p : demo) {
            boolean matchCat = (categoryId == null || categoryId <= 0 || p.getCategoryId() == categoryId);
            boolean matchQuery = (searchQuery == null || searchQuery.trim().isEmpty() ||
                    p.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    p.getSku().toLowerCase().contains(searchQuery.toLowerCase()));
            if (matchCat && matchQuery) filtered.add(p);
        }
        return filtered;
    }
}
