-- ============================================================================
-- Migration: Add product approval status, soft delete, history, and submissions
-- ============================================================================

-- 1. Add approval_status and is_deleted columns to products
ALTER TABLE products 
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure existing records have default values
UPDATE products SET approval_status = 'APPROVED' WHERE approval_status IS NULL;
UPDATE products SET is_deleted = FALSE WHERE is_deleted IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_approval_status ON products(approval_status);
CREATE INDEX IF NOT EXISTS idx_products_is_deleted ON products(is_deleted);

-- 2. Create product_history table
CREATE TABLE IF NOT EXISTS product_history (
    history_id SERIAL PRIMARY KEY,
    product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    field VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prod_history_prod ON product_history(product_id);
CREATE INDEX IF NOT EXISTS idx_prod_history_action ON product_history(action);
CREATE INDEX IF NOT EXISTS idx_prod_history_date ON product_history(date);

-- 3. Create product_submissions table for employee changes & manager reviews
CREATE TABLE IF NOT EXISTS product_submissions (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    submitted_by INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submission_type VARCHAR(20) NOT NULL CHECK (submission_type IN ('CREATE', 'UPDATE')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason TEXT,
    reviewed_by INT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Submitted product fields
    name VARCHAR(150) NOT NULL,
    category_id INT REFERENCES categories(id) ON DELETE SET NULL,
    sku VARCHAR(64) NOT NULL,
    barcode VARCHAR(64),
    cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    selling_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    stock_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 10,
    image_path VARCHAR(255),

    -- Original values snapshot (for UPDATE submissions)
    old_name VARCHAR(150),
    old_category_id INT,
    old_sku VARCHAR(64),
    old_barcode VARCHAR(64),
    old_cost_price NUMERIC(12, 2),
    old_selling_price NUMERIC(12, 2),
    old_stock_quantity INT,
    old_low_stock_threshold INT,
    old_image_path VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_prod_submissions_org ON product_submissions(organization_id);
CREATE INDEX IF NOT EXISTS idx_prod_submissions_status ON product_submissions(status);
CREATE INDEX IF NOT EXISTS idx_prod_submissions_submitter ON product_submissions(submitted_by);
