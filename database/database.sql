
DROP TRIGGER IF EXISTS trg_update_product_status ON products CASCADE;
DROP FUNCTION IF EXISTS update_product_status() CASCADE;

DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS join_codes CASCADE;
DROP TABLE IF EXISTS ai_insights CASCADE;
DROP TABLE IF EXISTS stock_counts CASCADE;
DROP TABLE IF EXISTS stock_movements CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS organizations CASCADE;

CREATE TABLE organizations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_organizations_slug ON organizations(slug);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'DISABLED')),
    remember_token_hash VARCHAR(64),
    remember_expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_org ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_approval ON users(approval_status);
CREATE INDEX idx_users_remember_token ON users(remember_token_hash);

CREATE TABLE join_codes (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('EMPLOYEE', 'MANAGER')),
    created_by INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '10 minutes'),
    used_at TIMESTAMP WITH TIME ZONE,
    used_by INT REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_join_codes_hash ON join_codes(code_hash);
CREATE INDEX idx_join_codes_org ON join_codes(organization_id);

ALTER TABLE join_codes ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_join_codes ON join_codes
    FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    organization_id INT REFERENCES organizations(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_log_org ON audit_log(organization_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_audit_log ON audit_log
    FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

CREATE OR REPLACE FUNCTION sync_user_active_status()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.is_active IS DISTINCT FROM OLD.is_active AND NEW.approval_status IS NOT DISTINCT FROM OLD.approval_status THEN
        IF NEW.is_active = TRUE THEN
            NEW.approval_status := 'APPROVED';
        ELSE
            NEW.approval_status := 'DISABLED';
        END IF;
    ELSE
        IF NEW.approval_status = 'APPROVED' THEN
            NEW.is_active := TRUE;
        ELSE
            NEW.is_active := FALSE;
        END IF;
    END IF;
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_user_active_status
BEFORE INSERT OR UPDATE OF approval_status, is_active ON users
FOR EACH ROW
EXECUTE FUNCTION sync_user_active_status();

CREATE TABLE join_attempt_rate_limits (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(128) NOT NULL,
    attempt_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_rate_limits_device_time ON join_attempt_rate_limits(device_id, attempt_time);

CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL DEFAULT 1 REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (organization_id, name),
    UNIQUE (organization_id, id)
);
CREATE INDEX idx_categories_org ON categories(organization_id);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL DEFAULT 1 REFERENCES organizations(id) ON DELETE CASCADE,
    category_id INT REFERENCES categories(id) ON DELETE SET NULL,
    sku VARCHAR(64) NOT NULL,
    barcode VARCHAR(64),
    name VARCHAR(150) NOT NULL,
    unit VARCHAR(30) NOT NULL DEFAULT 'pcs',
    cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (cost_price >= 0),
    selling_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (selling_price >= 0),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    low_stock_threshold INT NOT NULL DEFAULT 10 CHECK (low_stock_threshold >= 0),
    image_path VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK' CHECK (status IN ('IN_STOCK', 'LOW_STOCK', 'OUT_OF_STOCK', 'DISCONTINUED')),
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (organization_id, sku),
    CONSTRAINT fk_products_org_category FOREIGN KEY (organization_id, category_id) REFERENCES categories(organization_id, id) ON DELETE SET NULL
);
CREATE INDEX idx_products_org ON products(organization_id);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_archived ON products(is_archived);
CREATE INDEX idx_products_name ON products(name);

CREATE OR REPLACE FUNCTION update_product_status()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.stock_quantity <= 0 THEN
        NEW.status := 'OUT_OF_STOCK';
    ELSIF NEW.stock_quantity <= NEW.low_stock_threshold THEN
        NEW.status := 'LOW_STOCK';
    ELSE
        NEW.status := 'IN_STOCK';
    END IF;
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_product_status
BEFORE INSERT OR UPDATE OF stock_quantity, low_stock_threshold ON products
FOR EACH ROW
EXECUTE FUNCTION update_product_status();

CREATE TABLE stock_movements (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL DEFAULT 1 REFERENCES organizations(id) ON DELETE CASCADE,
    reference_no VARCHAR(60) NOT NULL UNIQUE,
    product_id INT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    movement_type VARCHAR(20) NOT NULL CHECK (movement_type IN ('IN', 'OUT', 'ADJUSTMENT')),
    quantity INT NOT NULL CHECK (quantity > 0),
    reason VARCHAR(40) NOT NULL CHECK (reason IN ('SOLD', 'USED', 'DAMAGED', 'EXPIRED', 'RESTOCK', 'AUDIT_CORRECTION', 'OTHER', 'UNDO', 'OPENING_STOCK')),
    previous_stock INT NOT NULL CHECK (previous_stock >= 0),
    new_stock INT NOT NULL CHECK (new_stock >= 0),
    undone_by_movement_id INT REFERENCES stock_movements(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_movements_org ON stock_movements(organization_id);
CREATE INDEX idx_movements_ref ON stock_movements(reference_no);
CREATE INDEX idx_movements_prod ON stock_movements(product_id);
CREATE INDEX idx_movements_user ON stock_movements(user_id);
CREATE INDEX idx_movements_type ON stock_movements(movement_type);
CREATE INDEX idx_movements_reason ON stock_movements(reason);
CREATE INDEX idx_movements_undone ON stock_movements(undone_by_movement_id);
CREATE INDEX idx_movements_created ON stock_movements(created_at);

CREATE TABLE stock_counts (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL DEFAULT 1 REFERENCES organizations(id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    product_id INT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    system_stock INT NOT NULL CHECK (system_stock >= 0),
    physical_count INT NOT NULL CHECK (physical_count >= 0),
    difference INT GENERATED ALWAYS AS (physical_count - system_stock) STORED,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED' CHECK (status IN ('PENDING', 'APPLIED', 'REJECTED')),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_counts_org ON stock_counts(organization_id);
CREATE INDEX idx_counts_prod ON stock_counts(product_id);
CREATE INDEX idx_counts_created ON stock_counts(created_at);

CREATE TABLE ai_insights (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL DEFAULT 1 REFERENCES organizations(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL CHECK (category IN ('FAST_MOVING', 'DEMAND_FORECAST', 'LOW_STOCK', 'OVERSTOCK', 'SLOW_MOVING', 'EXPIRY_RISK', 'ANOMALY', 'RESTOCK_RECOMMENDATION')),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'OPPORTUNITY')),
    metric_value VARCHAR(50),
    action_label VARCHAR(80),
    action_command VARCHAR(100),
    is_resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_ai_insights_org ON ai_insights(organization_id);
CREATE INDEX idx_ai_insights_cat ON ai_insights(category);
CREATE INDEX idx_ai_insights_severity ON ai_insights(severity);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_users ON users FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_categories ON categories FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE products FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_products ON products FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movements FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_stock_movements ON stock_movements FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

ALTER TABLE stock_counts ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_counts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_stock_counts ON stock_counts FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

ALTER TABLE ai_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_insights FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_ai_insights ON ai_insights FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

CREATE TABLE IF NOT EXISTS ai_usage_tracking (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    call_count INT NOT NULL DEFAULT 1,
    last_called_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ai_usage_org_date UNIQUE (organization_id, usage_date)
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_org ON ai_usage_tracking(organization_id);

ALTER TABLE ai_usage_tracking ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage_tracking FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_ai_usage_tracking ON ai_usage_tracking FOR ALL
    USING (organization_id = NULLIF(current_setting('app.current_organization_id', true), '')::INT);

ALTER TABLE join_codes FORCE ROW LEVEL SECURITY;

ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION fn_find_user_by_email(p_email TEXT)
RETURNS TABLE (
    id INT,
    organization_id INT,
    role_id INT,
    full_name VARCHAR,
    email VARCHAR,
    password_hash VARCHAR,
    approval_status VARCHAR,
    remember_token_hash VARCHAR,
    remember_expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN
)
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN QUERY
    SELECT u.id, u.organization_id, u.role_id, u.full_name, u.email,
           u.password_hash, u.approval_status, u.remember_token_hash,
           u.remember_expires_at, u.is_active
    FROM users u
    WHERE LOWER(u.email) = LOWER(p_email)
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_email_exists(p_email TEXT)
RETURNS BOOLEAN
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM users u WHERE LOWER(u.email) = LOWER(p_email)
    );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_find_user_by_remember_token(p_token_hash TEXT)
RETURNS TABLE (
    id INT,
    organization_id INT,
    role_id INT,
    full_name VARCHAR,
    email VARCHAR,
    password_hash VARCHAR,
    approval_status VARCHAR,
    remember_token_hash VARCHAR,
    remember_expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN
)
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN QUERY
    SELECT u.id, u.organization_id, u.role_id, u.full_name, u.email,
           u.password_hash, u.approval_status, u.remember_token_hash,
           u.remember_expires_at, u.is_active
    FROM users u
    WHERE u.remember_token_hash = p_token_hash
      AND u.remember_expires_at > CURRENT_TIMESTAMP
      AND u.is_active = true
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_lock_active_join_code(p_code_hash TEXT)
RETURNS TABLE (
    id INT,
    organization_id INT,
    role VARCHAR,
    org_active BOOLEAN
)
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN QUERY
    SELECT jc.id, jc.organization_id, jc.role, o.is_active AS org_active
    FROM join_codes jc
    JOIN organizations o ON jc.organization_id = o.id
    WHERE jc.code_hash = p_code_hash
      AND jc.used_at IS NULL
      AND jc.cancelled_at IS NULL
      AND jc.expires_at > CURRENT_TIMESTAMP
    FOR UPDATE OF jc;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'bai_app_user') THEN
        CREATE ROLE bai_app_user WITH LOGIN PASSWORD 'bai_password' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
    END IF;
END $$;
GRANT CONNECT ON DATABASE bai_pos TO bai_app_user;
GRANT USAGE ON SCHEMA public TO bai_app_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bai_app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bai_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO bai_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO bai_app_user;

INSERT INTO organizations (id, name, slug, is_active) VALUES
(1, 'bAI Retail Manila Store', 'bai-manila', true);

INSERT INTO roles (id, name, description) VALUES
(1, 'OWNER', 'Business Owner / System Setup'),
(2, 'MANAGER', 'Inventory Manager / Audits & Adjustments'),
(3, 'EMPLOYEE', 'Store Associate / Quick Deductions & Restocks');

INSERT INTO users (id, organization_id, role_id, full_name, email, password_hash, approval_status, is_active) VALUES
(1, 1, 1, 'Store Administrator (Owner)', 'admin@gmail.com', '$2a$12$s0Hn2VYxfAcBRGor2.vDauP3/SA1niIuG64FYo8tKBk3C9As1F3Qi', 'APPROVED', true),
(2, 1, 2, 'Jane Santos (Manager)', 'manager@bai.com', '$2a$12$s0Hn2VYxfAcBRGor2.vDauP3/SA1niIuG64FYo8tKBk3C9As1F3Qi', 'APPROVED', true),
(3, 1, 3, 'Alex Cruz (Associate)', 'employee@bai.com', '$2a$12$s0Hn2VYxfAcBRGor2.vDauP3/SA1niIuG64FYo8tKBk3C9As1F3Qi', 'APPROVED', true);

INSERT INTO categories (id, name, description) VALUES
(1, 'Beverages', 'Cold brew, espresso beans, teas, syrups'),
(2, 'Bakery', 'Croissants, bagels, muffins, pastries'),
(3, 'Fresh & Dairy', 'Fresh whole milk, oat milk, almond milk, butter'),
(4, 'Packaged Snacks', 'Sandwiches, salads, wraps, protein packs'),
(5, 'Store Supplies & Packaging', 'Paper cups, napkins, coffee lids, carry bags');

INSERT INTO products (id, category_id, sku, barcode, name, unit, cost_price, selling_price, stock_quantity, low_stock_threshold) VALUES
(1, 1, 'BEV-001', '8901001', 'Barako Arabica Beans 1kg', 'bag', 380.00, 650.00, 42, 15),
(2, 1, 'BEV-002', '8901002', 'Benguet Cold Brew Keg 20L', 'keg', 1200.00, 2400.00, 3, 4),
(3, 1, 'BEV-003', '8901003', 'Ceremonial Matcha Tin 100g', 'tin', 450.00, 850.00, 6, 10),
(4, 2, 'BAK-001', '8902001', 'Butter Croissant (Box of 6)', 'box', 160.00, 320.00, 8, 12),
(5, 2, 'BAK-002', '8902002', 'Special Ensaymada (4pk)', 'box', 140.00, 280.00, 18, 10),
(6, 2, 'BAK-003', '8902003', 'Almond Danish Pack (4pk)', 'box', 150.00, 300.00, 0, 6),
(7, 3, 'DAI-001', '8904001', 'Fresh Whole Milk 1L', 'carton', 65.00, 110.00, 7, 20),
(8, 3, 'DAI-002', '8904002', 'Barista Oat Milk 1L', 'carton', 95.00, 175.00, 35, 15),
(9, 4, 'SNK-001', '8903001', 'Roast Chicken Pesto Ciabatta', 'pcs', 90.00, 185.00, 14, 10),
(10, 5, 'SUP-001', '8905001', 'Eco Paper Cups 12oz (50s)', 'sleeve', 75.00, 140.00, 55, 20);

INSERT INTO stock_movements (id, reference_no, product_id, user_id, movement_type, quantity, reason, previous_stock, new_stock, notes, created_at) VALUES
(1, 'SM-INIT-0001', 1, 1, 'IN', 60, 'RESTOCK', 0, 60, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(2, 'SM-DED-0002', 1, 3, 'OUT', 1, 'SOLD', 60, 59, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 13 hours'),
(3, 'SM-DED-0003', 1, 3, 'OUT', 2, 'USED', 59, 57, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 11 hours'),
(4, 'SM-DED-0004', 1, 3, 'OUT', 6, 'SOLD', 57, 51, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 18 hours'),
(5, 'SM-DED-0005', 1, 3, 'OUT', 1, 'SOLD', 51, 50, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 10 hours'),
(6, 'SM-DED-0006', 1, 3, 'OUT', 1, 'USED', 50, 49, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 13 hours'),
(7, 'SM-DED-0007', 1, 3, 'OUT', 5, 'SOLD', 49, 44, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 18 hours'),
(8, 'SM-DED-0008', 1, 3, 'OUT', 2, 'SOLD', 44, 42, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 18 hours'),
(9, 'SM-DED-0009', 1, 3, 'OUT', 4, 'SOLD', 42, 38, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 14 hours'),
(10, 'SM-DED-0010', 1, 3, 'OUT', 1, 'SOLD', 38, 37, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 12 hours'),
(11, 'SM-DED-0011', 1, 3, 'OUT', 6, 'SOLD', 37, 31, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 14 hours'),
(12, 'SM-DED-0012', 1, 3, 'OUT', 2, 'SOLD', 31, 29, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 15 hours'),
(13, 'SM-DED-0013', 1, 3, 'OUT', 1, 'USED', 29, 28, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 11 hours'),
(14, 'SM-DED-0014', 1, 3, 'OUT', 3, 'SOLD', 28, 25, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 14 hours'),
(15, 'SM-DED-0015', 1, 3, 'OUT', 1, 'SOLD', 25, 24, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 18 hours'),
(16, 'SM-DED-0016', 1, 3, 'OUT', 1, 'SOLD', 24, 23, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 16 hours'),
(17, 'SM-DED-0017', 1, 3, 'OUT', 1, 'SOLD', 23, 22, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 15 hours'),
(18, 'SM-DED-0018', 1, 3, 'OUT', 5, 'EXPIRED', 22, 17, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 11 hours'),
(19, 'SM-DED-0019', 1, 3, 'OUT', 1, 'SOLD', 17, 16, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 14 hours'),
(20, 'SM-DED-0020', 1, 3, 'OUT', 1, 'SOLD', 16, 15, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 11 hours'),
(21, 'SM-DED-0021', 1, 3, 'OUT', 4, 'SOLD', 15, 11, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 15 hours'),
(22, 'SM-DED-0022', 1, 3, 'OUT', 1, 'SOLD', 11, 10, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 13 hours'),
(23, 'SM-DED-0023', 1, 3, 'OUT', 3, 'SOLD', 10, 7, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 11 hours'),
(24, 'SM-RESTOCK-0024', 1, 2, 'IN', 35, 'RESTOCK', 7, 42, 'Recent scheduled inventory shipment', CURRENT_TIMESTAMP - INTERVAL '1 days 10 hours'),
(25, 'SM-INIT-0025', 2, 1, 'IN', 23, 'RESTOCK', 0, 23, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(26, 'SM-DED-0026', 2, 3, 'OUT', 5, 'SOLD', 23, 18, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 12 hours'),
(27, 'SM-DED-0027', 2, 3, 'OUT', 4, 'SOLD', 18, 14, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 18 hours'),
(28, 'SM-DED-0028', 2, 3, 'OUT', 2, 'SOLD', 14, 12, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 10 hours'),
(29, 'SM-DED-0029', 2, 3, 'OUT', 2, 'SOLD', 12, 10, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 15 hours'),
(30, 'SM-DED-0030', 2, 3, 'OUT', 2, 'SOLD', 10, 8, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 13 hours'),
(31, 'SM-DED-0031', 2, 3, 'OUT', 2, 'SOLD', 8, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 17 hours'),
(32, 'SM-DED-0032', 2, 3, 'OUT', 2, 'SOLD', 6, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 17 hours'),
(33, 'SM-DED-0033', 2, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 13 hours'),
(34, 'SM-DED-0034', 2, 3, 'OUT', 1, 'SOLD', 3, 2, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 16 hours'),
(35, 'SM-RESTOCK-0035', 2, 2, 'IN', 26, 'RESTOCK', 2, 28, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '26 days 9 hours'),
(36, 'SM-DED-0036', 2, 3, 'OUT', 1, 'SOLD', 28, 27, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 12 hours'),
(37, 'SM-DED-0037', 2, 3, 'OUT', 5, 'SOLD', 27, 22, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 10 hours'),
(38, 'SM-DED-0038', 2, 3, 'OUT', 1, 'EXPIRED', 22, 21, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 12 hours'),
(39, 'SM-DED-0039', 2, 3, 'OUT', 6, 'SOLD', 21, 15, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 11 hours'),
(40, 'SM-DED-0040', 2, 3, 'OUT', 4, 'SOLD', 15, 11, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 17 hours'),
(41, 'SM-DED-0041', 2, 3, 'OUT', 3, 'SOLD', 11, 8, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 18 hours'),
(42, 'SM-DED-0042', 2, 3, 'OUT', 1, 'SOLD', 8, 7, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 11 hours'),
(43, 'SM-DED-0043', 2, 3, 'OUT', 2, 'SOLD', 7, 5, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 15 hours'),
(44, 'SM-DED-0044', 2, 3, 'OUT', 1, 'SOLD', 5, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 12 hours'),
(45, 'SM-DED-0045', 2, 3, 'OUT', 1, 'USED', 4, 3, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 14 hours'),
(46, 'SM-DED-0046', 2, 3, 'OUT', 1, 'SOLD', 3, 2, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 11 hours'),
(47, 'SM-RESTOCK-0047', 2, 2, 'IN', 21, 'RESTOCK', 2, 23, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '4 days 9 hours'),
(48, 'SM-DED-0048', 2, 3, 'OUT', 1, 'EXPIRED', 23, 22, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 12 hours'),
(49, 'SM-DED-0049', 2, 3, 'OUT', 5, 'SOLD', 22, 17, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 18 hours'),
(50, 'SM-DED-0050', 2, 3, 'OUT', 14, 'SOLD', 17, 3, 'Recent shift counter deduction', CURRENT_TIMESTAMP - INTERVAL '0 days 2 hours'),
(51, 'SM-INIT-0051', 3, 1, 'IN', 21, 'RESTOCK', 0, 21, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(52, 'SM-DED-0052', 3, 3, 'OUT', 5, 'SOLD', 21, 16, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 10 hours'),
(53, 'SM-DED-0053', 3, 3, 'OUT', 1, 'SOLD', 16, 15, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 14 hours'),
(54, 'SM-DED-0054', 3, 3, 'OUT', 2, 'USED', 15, 13, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 11 hours'),
(55, 'SM-DED-0055', 3, 3, 'OUT', 1, 'SOLD', 13, 12, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 11 hours'),
(56, 'SM-DED-0056', 3, 3, 'OUT', 2, 'USED', 12, 10, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 17 hours'),
(57, 'SM-DED-0057', 3, 3, 'OUT', 3, 'EXPIRED', 10, 7, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 18 hours'),
(58, 'SM-DED-0058', 3, 3, 'OUT', 2, 'SOLD', 7, 5, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 18 hours'),
(59, 'SM-DED-0059', 3, 3, 'OUT', 1, 'SOLD', 5, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 16 hours'),
(60, 'SM-DED-0060', 3, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 18 hours'),
(61, 'SM-DED-0061', 3, 3, 'OUT', 1, 'USED', 3, 2, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 13 hours'),
(62, 'SM-RESTOCK-0062', 3, 2, 'IN', 25, 'RESTOCK', 2, 27, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '24 days 9 hours'),
(63, 'SM-DED-0063', 3, 3, 'OUT', 1, 'USED', 27, 26, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 18 hours'),
(64, 'SM-DED-0064', 3, 3, 'OUT', 2, 'SOLD', 26, 24, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 10 hours'),
(65, 'SM-DED-0065', 3, 3, 'OUT', 1, 'SOLD', 24, 23, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 10 hours'),
(66, 'SM-DED-0066', 3, 3, 'OUT', 2, 'USED', 23, 21, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 10 hours'),
(67, 'SM-DED-0067', 3, 3, 'OUT', 3, 'USED', 21, 18, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 13 hours'),
(68, 'SM-DED-0068', 3, 3, 'OUT', 3, 'SOLD', 18, 15, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 13 hours'),
(69, 'SM-DED-0069', 3, 3, 'OUT', 5, 'USED', 15, 10, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 17 hours'),
(70, 'SM-DED-0070', 3, 3, 'OUT', 1, 'SOLD', 10, 9, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 16 hours'),
(71, 'SM-DED-0071', 3, 3, 'OUT', 1, 'USED', 9, 8, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 16 hours'),
(72, 'SM-DED-0072', 3, 3, 'OUT', 2, 'SOLD', 8, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 17 hours'),
(73, 'SM-DED-0073', 3, 3, 'OUT', 1, 'SOLD', 6, 5, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 11 hours'),
(74, 'SM-DED-0074', 3, 3, 'OUT', 1, 'SOLD', 5, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 15 hours'),
(75, 'SM-RESTOCK-0075', 3, 2, 'IN', 2, 'RESTOCK', 4, 6, 'Recent scheduled inventory shipment', CURRENT_TIMESTAMP - INTERVAL '1 days 10 hours'),
(76, 'SM-INIT-0076', 4, 1, 'IN', 26, 'RESTOCK', 0, 26, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(77, 'SM-DED-0077', 4, 3, 'OUT', 2, 'EXPIRED', 26, 24, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 18 hours'),
(78, 'SM-DED-0078', 4, 3, 'OUT', 4, 'USED', 24, 20, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 12 hours'),
(79, 'SM-DED-0079', 4, 3, 'OUT', 3, 'SOLD', 20, 17, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 11 hours'),
(80, 'SM-DED-0080', 4, 3, 'OUT', 4, 'SOLD', 17, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 18 hours'),
(81, 'SM-DED-0081', 4, 3, 'OUT', 1, 'USED', 13, 12, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 18 hours'),
(82, 'SM-DED-0082', 4, 3, 'OUT', 1, 'SOLD', 12, 11, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 13 hours'),
(83, 'SM-DED-0083', 4, 3, 'OUT', 1, 'SOLD', 11, 10, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 17 hours'),
(84, 'SM-DED-0084', 4, 3, 'OUT', 1, 'SOLD', 10, 9, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 10 hours'),
(85, 'SM-DED-0085', 4, 3, 'OUT', 1, 'SOLD', 9, 8, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 16 hours'),
(86, 'SM-DED-0086', 4, 3, 'OUT', 2, 'SOLD', 8, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 17 hours'),
(87, 'SM-DED-0087', 4, 3, 'OUT', 2, 'SOLD', 6, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 18 hours'),
(88, 'SM-DED-0088', 4, 3, 'OUT', 1, 'EXPIRED', 4, 3, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 14 hours'),
(89, 'SM-DED-0089', 4, 3, 'OUT', 1, 'SOLD', 3, 2, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 18 hours'),
(90, 'SM-RESTOCK-0090', 4, 2, 'IN', 25, 'RESTOCK', 2, 27, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '18 days 9 hours'),
(91, 'SM-DED-0091', 4, 3, 'OUT', 1, 'USED', 27, 26, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 17 hours'),
(92, 'SM-DED-0092', 4, 3, 'OUT', 5, 'SOLD', 26, 21, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 18 hours'),
(93, 'SM-DED-0093', 4, 3, 'OUT', 2, 'USED', 21, 19, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 18 hours'),
(94, 'SM-DED-0094', 4, 3, 'OUT', 1, 'SOLD', 19, 18, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 11 hours'),
(95, 'SM-DED-0095', 4, 3, 'OUT', 5, 'USED', 18, 13, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 13 hours'),
(96, 'SM-DED-0096', 4, 3, 'OUT', 4, 'USED', 13, 9, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 13 hours'),
(97, 'SM-DED-0097', 4, 3, 'OUT', 3, 'SOLD', 9, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 11 hours'),
(98, 'SM-DED-0098', 4, 3, 'OUT', 2, 'SOLD', 6, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 18 hours'),
(99, 'SM-DED-0099', 4, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 13 hours'),
(100, 'SM-RESTOCK-0100', 4, 2, 'IN', 5, 'RESTOCK', 3, 8, 'Recent scheduled inventory shipment', CURRENT_TIMESTAMP - INTERVAL '1 days 10 hours'),
(101, 'SM-INIT-0101', 5, 1, 'IN', 43, 'RESTOCK', 0, 43, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(102, 'SM-DED-0102', 5, 3, 'OUT', 2, 'SOLD', 43, 41, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 12 hours'),
(103, 'SM-DED-0103', 5, 3, 'OUT', 6, 'SOLD', 41, 35, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 17 hours'),
(104, 'SM-DED-0104', 5, 3, 'OUT', 3, 'SOLD', 35, 32, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 11 hours'),
(105, 'SM-DED-0105', 5, 3, 'OUT', 1, 'SOLD', 32, 31, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 11 hours'),
(106, 'SM-DED-0106', 5, 3, 'OUT', 1, 'SOLD', 31, 30, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 18 hours'),
(107, 'SM-DED-0107', 5, 3, 'OUT', 3, 'USED', 30, 27, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 15 hours'),
(108, 'SM-DED-0108', 5, 3, 'OUT', 1, 'SOLD', 27, 26, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 15 hours'),
(109, 'SM-DED-0109', 5, 3, 'OUT', 3, 'EXPIRED', 26, 23, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 18 hours'),
(110, 'SM-DED-0110', 5, 3, 'OUT', 6, 'SOLD', 23, 17, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 18 hours'),
(111, 'SM-DED-0111', 5, 3, 'OUT', 1, 'SOLD', 17, 16, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 18 hours'),
(112, 'SM-DED-0112', 5, 3, 'OUT', 3, 'SOLD', 16, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 11 hours'),
(113, 'SM-DED-0113', 5, 3, 'OUT', 2, 'SOLD', 13, 11, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 11 hours'),
(114, 'SM-DED-0114', 5, 3, 'OUT', 3, 'SOLD', 11, 8, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 14 hours'),
(115, 'SM-DED-0115', 5, 3, 'OUT', 2, 'SOLD', 8, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 15 hours'),
(116, 'SM-DED-0116', 5, 3, 'OUT', 1, 'SOLD', 6, 5, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 14 hours'),
(117, 'SM-DED-0117', 5, 3, 'OUT', 1, 'SOLD', 5, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 10 hours'),
(118, 'SM-DED-0118', 5, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 14 hours'),
(119, 'SM-DED-0119', 5, 3, 'OUT', 1, 'USED', 3, 2, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 12 hours'),
(120, 'SM-RESTOCK-0120', 5, 2, 'IN', 20, 'RESTOCK', 2, 22, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '8 days 9 hours'),
(121, 'SM-DED-0121', 5, 3, 'OUT', 1, 'SOLD', 22, 21, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 18 hours'),
(122, 'SM-DED-0122', 5, 3, 'OUT', 6, 'SOLD', 21, 15, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 10 hours'),
(123, 'SM-DED-0123', 5, 3, 'OUT', 1, 'USED', 15, 14, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 12 hours'),
(124, 'SM-DED-0124', 5, 3, 'OUT', 1, 'SOLD', 14, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 18 hours'),
(125, 'SM-RESTOCK-0125', 5, 2, 'IN', 5, 'RESTOCK', 13, 18, 'Recent scheduled inventory shipment', CURRENT_TIMESTAMP - INTERVAL '1 days 10 hours'),
(126, 'SM-INIT-0126', 6, 1, 'IN', 19, 'RESTOCK', 0, 19, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(127, 'SM-DED-0127', 6, 3, 'OUT', 4, 'USED', 19, 15, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 14 hours'),
(128, 'SM-DED-0128', 6, 3, 'OUT', 3, 'SOLD', 15, 12, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 10 hours'),
(129, 'SM-DED-0129', 6, 3, 'OUT', 3, 'SOLD', 12, 9, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 13 hours'),
(130, 'SM-DED-0130', 6, 3, 'OUT', 3, 'USED', 9, 6, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 18 hours'),
(131, 'SM-DED-0131', 6, 3, 'OUT', 2, 'SOLD', 6, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 12 hours'),
(132, 'SM-DED-0132', 6, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 12 hours'),
(133, 'SM-DED-0133', 6, 3, 'OUT', 1, 'USED', 3, 2, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 15 hours'),
(134, 'SM-RESTOCK-0134', 6, 2, 'IN', 22, 'RESTOCK', 2, 24, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '30 days 9 hours'),
(135, 'SM-DED-0135', 6, 3, 'OUT', 1, 'SOLD', 24, 23, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 11 hours'),
(136, 'SM-DED-0136', 6, 3, 'OUT', 4, 'SOLD', 23, 19, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 17 hours'),
(137, 'SM-DED-0137', 6, 3, 'OUT', 2, 'EXPIRED', 19, 17, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 17 hours'),
(138, 'SM-DED-0138', 6, 3, 'OUT', 3, 'SOLD', 17, 14, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 13 hours'),
(139, 'SM-DED-0139', 6, 3, 'OUT', 2, 'USED', 14, 12, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 13 hours'),
(140, 'SM-DED-0140', 6, 3, 'OUT', 4, 'SOLD', 12, 8, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 11 hours'),
(141, 'SM-DED-0141', 6, 3, 'OUT', 2, 'SOLD', 8, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 18 hours'),
(142, 'SM-DED-0142', 6, 3, 'OUT', 2, 'SOLD', 6, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 18 hours'),
(143, 'SM-DED-0143', 6, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 11 hours'),
(144, 'SM-DED-0144', 6, 3, 'OUT', 1, 'EXPIRED', 3, 2, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 14 hours'),
(145, 'SM-RESTOCK-0145', 6, 2, 'IN', 18, 'RESTOCK', 2, 20, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '10 days 9 hours'),
(146, 'SM-DED-0146', 6, 3, 'OUT', 1, 'SOLD', 20, 19, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 15 hours'),
(147, 'SM-DED-0147', 6, 3, 'OUT', 6, 'SOLD', 19, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 16 hours'),
(148, 'SM-DED-0148', 6, 3, 'OUT', 1, 'SOLD', 13, 12, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 13 hours'),
(149, 'SM-DED-0149', 6, 3, 'OUT', 3, 'USED', 12, 9, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 16 hours'),
(150, 'SM-DED-0150', 6, 3, 'OUT', 1, 'SOLD', 9, 8, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 18 hours'),
(151, 'SM-DED-0151', 6, 3, 'OUT', 8, 'DAMAGED', 8, 0, 'Recent shift counter deduction', CURRENT_TIMESTAMP - INTERVAL '0 days 2 hours'),
(152, 'SM-INIT-0152', 7, 1, 'IN', 28, 'RESTOCK', 0, 28, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(153, 'SM-DED-0153', 7, 3, 'OUT', 6, 'USED', 28, 22, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 15 hours'),
(154, 'SM-DED-0154', 7, 3, 'OUT', 5, 'SOLD', 22, 17, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 11 hours'),
(155, 'SM-DED-0155', 7, 3, 'OUT', 7, 'SOLD', 17, 10, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 16 hours'),
(156, 'SM-DED-0156', 7, 3, 'OUT', 6, 'SOLD', 10, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 18 hours'),
(157, 'SM-RESTOCK-0157', 7, 2, 'IN', 28, 'RESTOCK', 4, 32, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '36 days 9 hours'),
(158, 'SM-DED-0158', 7, 3, 'OUT', 4, 'SOLD', 32, 28, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 16 hours'),
(159, 'SM-DED-0159', 7, 3, 'OUT', 4, 'SOLD', 28, 24, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 14 hours'),
(160, 'SM-DED-0160', 7, 3, 'OUT', 7, 'SOLD', 24, 17, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 14 hours'),
(161, 'SM-DED-0161', 7, 3, 'OUT', 4, 'SOLD', 17, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 15 hours'),
(162, 'SM-DED-0162', 7, 3, 'OUT', 6, 'SOLD', 13, 7, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 13 hours'),
(163, 'SM-DED-0163', 7, 3, 'OUT', 4, 'SOLD', 7, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 14 hours'),
(164, 'SM-RESTOCK-0164', 7, 2, 'IN', 22, 'RESTOCK', 3, 25, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '24 days 9 hours'),
(165, 'SM-DED-0165', 7, 3, 'OUT', 3, 'SOLD', 25, 22, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 13 hours'),
(166, 'SM-DED-0166', 7, 3, 'OUT', 4, 'USED', 22, 18, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 13 hours'),
(167, 'SM-DED-0167', 7, 3, 'OUT', 7, 'SOLD', 18, 11, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 11 hours'),
(168, 'SM-DED-0168', 7, 3, 'OUT', 6, 'SOLD', 11, 5, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 13 hours'),
(169, 'SM-RESTOCK-0169', 7, 2, 'IN', 27, 'RESTOCK', 5, 32, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '16 days 9 hours'),
(170, 'SM-DED-0170', 7, 3, 'OUT', 6, 'SOLD', 32, 26, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 10 hours'),
(171, 'SM-DED-0171', 7, 3, 'OUT', 6, 'SOLD', 26, 20, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 18 hours'),
(172, 'SM-DED-0172', 7, 3, 'OUT', 3, 'SOLD', 20, 17, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 11 hours'),
(173, 'SM-DED-0173', 7, 3, 'OUT', 4, 'SOLD', 17, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 18 hours'),
(174, 'SM-DED-0174', 7, 3, 'OUT', 6, 'SOLD', 13, 7, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 18 hours'),
(175, 'SM-RESTOCK-0175', 7, 2, 'IN', 29, 'RESTOCK', 7, 36, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '6 days 9 hours'),
(176, 'SM-DED-0176', 7, 3, 'OUT', 7, 'SOLD', 36, 29, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 17 hours'),
(177, 'SM-DED-0177', 7, 3, 'OUT', 5, 'SOLD', 29, 24, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 14 hours'),
(178, 'SM-DED-0178', 7, 3, 'OUT', 6, 'SOLD', 24, 18, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 14 hours'),
(179, 'SM-DED-0179', 7, 3, 'OUT', 11, 'SOLD', 18, 7, 'Recent shift counter deduction', CURRENT_TIMESTAMP - INTERVAL '0 days 2 hours'),
(180, 'SM-INIT-0180', 8, 1, 'IN', 64, 'RESTOCK', 0, 64, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(181, 'SM-DED-0181', 8, 3, 'OUT', 1, 'SOLD', 64, 63, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 13 hours'),
(182, 'SM-DED-0182', 8, 3, 'OUT', 3, 'SOLD', 63, 60, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 18 hours'),
(183, 'SM-DED-0183', 8, 3, 'OUT', 1, 'USED', 60, 59, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 13 hours'),
(184, 'SM-DED-0184', 8, 3, 'OUT', 4, 'SOLD', 59, 55, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 13 hours'),
(185, 'SM-DED-0185', 8, 3, 'OUT', 1, 'SOLD', 55, 54, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 15 hours'),
(186, 'SM-DED-0186', 8, 3, 'OUT', 5, 'SOLD', 54, 49, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 10 hours'),
(187, 'SM-DED-0187', 8, 3, 'OUT', 2, 'SOLD', 49, 47, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 16 hours'),
(188, 'SM-DED-0188', 8, 3, 'OUT', 5, 'SOLD', 47, 42, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 10 hours'),
(189, 'SM-DED-0189', 8, 3, 'OUT', 5, 'SOLD', 42, 37, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 10 hours'),
(190, 'SM-DED-0190', 8, 3, 'OUT', 3, 'SOLD', 37, 34, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 16 hours'),
(191, 'SM-DED-0191', 8, 3, 'OUT', 4, 'SOLD', 34, 30, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 18 hours'),
(192, 'SM-DED-0192', 8, 3, 'OUT', 5, 'SOLD', 30, 25, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 17 hours'),
(193, 'SM-DED-0193', 8, 3, 'OUT', 2, 'SOLD', 25, 23, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 17 hours'),
(194, 'SM-DED-0194', 8, 3, 'OUT', 1, 'SOLD', 23, 22, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 16 hours'),
(195, 'SM-DED-0195', 8, 3, 'OUT', 6, 'EXPIRED', 22, 16, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 17 hours'),
(196, 'SM-DED-0196', 8, 3, 'OUT', 2, 'SOLD', 16, 14, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 18 hours'),
(197, 'SM-DED-0197', 8, 3, 'OUT', 1, 'SOLD', 14, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 10 hours'),
(198, 'SM-DED-0198', 8, 3, 'OUT', 1, 'SOLD', 13, 12, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 12 hours'),
(199, 'SM-DED-0199', 8, 3, 'OUT', 4, 'EXPIRED', 12, 8, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 14 hours'),
(200, 'SM-DED-0200', 8, 3, 'OUT', 2, 'SOLD', 8, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 17 hours'),
(201, 'SM-DED-0201', 8, 3, 'OUT', 2, 'SOLD', 6, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 16 hours'),
(202, 'SM-DED-0202', 8, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 16 hours'),
(203, 'SM-RESTOCK-0203', 8, 2, 'IN', 32, 'RESTOCK', 3, 35, 'Recent scheduled inventory shipment', CURRENT_TIMESTAMP - INTERVAL '1 days 10 hours'),
(204, 'SM-INIT-0204', 9, 1, 'IN', 37, 'RESTOCK', 0, 37, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(205, 'SM-DED-0205', 9, 3, 'OUT', 1, 'SOLD', 37, 36, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 18 hours'),
(206, 'SM-DED-0206', 9, 3, 'OUT', 1, 'SOLD', 36, 35, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 15 hours'),
(207, 'SM-DED-0207', 9, 3, 'OUT', 2, 'SOLD', 35, 33, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 10 hours'),
(208, 'SM-DED-0208', 9, 3, 'OUT', 1, 'SOLD', 33, 32, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 13 hours'),
(209, 'SM-DED-0209', 9, 3, 'OUT', 1, 'SOLD', 32, 31, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 13 hours'),
(210, 'SM-DED-0210', 9, 3, 'OUT', 2, 'SOLD', 31, 29, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 11 hours'),
(211, 'SM-DED-0211', 9, 3, 'OUT', 5, 'SOLD', 29, 24, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 17 hours'),
(212, 'SM-DED-0212', 9, 3, 'OUT', 6, 'SOLD', 24, 18, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 15 hours'),
(213, 'SM-DED-0213', 9, 3, 'OUT', 2, 'SOLD', 18, 16, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 11 hours'),
(214, 'SM-DED-0214', 9, 3, 'OUT', 2, 'SOLD', 16, 14, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 11 hours'),
(215, 'SM-DED-0215', 9, 3, 'OUT', 1, 'SOLD', 14, 13, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 16 hours'),
(216, 'SM-DED-0216', 9, 3, 'OUT', 4, 'SOLD', 13, 9, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 13 hours'),
(217, 'SM-DED-0217', 9, 3, 'OUT', 1, 'SOLD', 9, 8, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 13 hours'),
(218, 'SM-DED-0218', 9, 3, 'OUT', 1, 'SOLD', 8, 7, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 14 hours'),
(219, 'SM-DED-0219', 9, 3, 'OUT', 1, 'SOLD', 7, 6, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 10 hours'),
(220, 'SM-DED-0220', 9, 3, 'OUT', 2, 'SOLD', 6, 4, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 15 hours'),
(221, 'SM-DED-0221', 9, 3, 'OUT', 1, 'SOLD', 4, 3, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 15 hours'),
(222, 'SM-DED-0222', 9, 3, 'OUT', 1, 'SOLD', 3, 2, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 17 hours'),
(223, 'SM-RESTOCK-0223', 9, 2, 'IN', 28, 'RESTOCK', 2, 30, 'Mid-month supplier restock shipment', CURRENT_TIMESTAMP - INTERVAL '8 days 9 hours'),
(224, 'SM-DED-0224', 9, 3, 'OUT', 1, 'SOLD', 30, 29, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 17 hours'),
(225, 'SM-DED-0225', 9, 3, 'OUT', 6, 'EXPIRED', 29, 23, 'Regular store expired deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 12 hours'),
(226, 'SM-DED-0226', 9, 3, 'OUT', 6, 'SOLD', 23, 17, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 14 hours'),
(227, 'SM-DED-0227', 9, 3, 'OUT', 5, 'SOLD', 17, 12, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 18 hours'),
(228, 'SM-RESTOCK-0228', 9, 2, 'IN', 2, 'RESTOCK', 12, 14, 'Recent scheduled inventory shipment', CURRENT_TIMESTAMP - INTERVAL '1 days 10 hours'),
(229, 'SM-INIT-0229', 10, 1, 'IN', 85, 'RESTOCK', 0, 85, 'Opening inventory base', CURRENT_TIMESTAMP - INTERVAL '45 days 8 hours'),
(230, 'SM-DED-0230', 10, 3, 'OUT', 4, 'SOLD', 85, 81, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '44 days 14 hours'),
(231, 'SM-DED-0231', 10, 3, 'OUT', 3, 'SOLD', 81, 78, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '42 days 11 hours'),
(232, 'SM-DED-0232', 10, 3, 'OUT', 3, 'SOLD', 78, 75, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '40 days 13 hours'),
(233, 'SM-DED-0233', 10, 3, 'OUT', 4, 'SOLD', 75, 71, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '38 days 16 hours'),
(234, 'SM-DED-0234', 10, 3, 'OUT', 3, 'USED', 71, 68, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '36 days 15 hours'),
(235, 'SM-DED-0235', 10, 3, 'OUT', 2, 'SOLD', 68, 66, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '34 days 15 hours'),
(236, 'SM-DED-0236', 10, 3, 'OUT', 3, 'SOLD', 66, 63, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '32 days 14 hours'),
(237, 'SM-DED-0237', 10, 3, 'OUT', 5, 'USED', 63, 58, 'Regular store used deduction', CURRENT_TIMESTAMP - INTERVAL '30 days 13 hours'),
(238, 'SM-DED-0238', 10, 3, 'OUT', 1, 'SOLD', 58, 57, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '28 days 16 hours'),
(239, 'SM-DED-0239', 10, 3, 'OUT', 4, 'SOLD', 57, 53, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '26 days 13 hours'),
(240, 'SM-DED-0240', 10, 3, 'OUT', 6, 'SOLD', 53, 47, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '24 days 17 hours'),
(241, 'SM-DED-0241', 10, 3, 'OUT', 4, 'SOLD', 47, 43, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '22 days 11 hours'),
(242, 'SM-DED-0242', 10, 3, 'OUT', 3, 'SOLD', 43, 40, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '20 days 13 hours'),
(243, 'SM-DED-0243', 10, 3, 'OUT', 3, 'SOLD', 40, 37, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '18 days 15 hours'),
(244, 'SM-DED-0244', 10, 3, 'OUT', 4, 'SOLD', 37, 33, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '16 days 15 hours'),
(245, 'SM-DED-0245', 10, 3, 'OUT', 4, 'SOLD', 33, 29, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '14 days 18 hours'),
(246, 'SM-DED-0246', 10, 3, 'OUT', 3, 'SOLD', 29, 26, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '12 days 17 hours'),
(247, 'SM-DED-0247', 10, 3, 'OUT', 3, 'SOLD', 26, 23, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '10 days 13 hours'),
(248, 'SM-DED-0248', 10, 3, 'OUT', 1, 'SOLD', 23, 22, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '8 days 15 hours'),
(249, 'SM-DED-0249', 10, 3, 'OUT', 1, 'SOLD', 22, 21, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '6 days 12 hours'),
(250, 'SM-DED-0250', 10, 3, 'OUT', 2, 'SOLD', 21, 19, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '4 days 17 hours'),
(251, 'SM-DED-0251', 10, 3, 'OUT', 3, 'SOLD', 19, 16, 'Regular store sold deduction', CURRENT_TIMESTAMP - INTERVAL '2 days 18 hours'),
(252, 'SM-RESTOCK-0252', 10, 2, 'IN', 39, 'RESTOCK', 16, 55, 'Recent scheduled inventory shipment', CURRENT_TIMESTAMP - INTERVAL '1 days 10 hours');

INSERT INTO stock_counts (user_id, product_id, system_stock, physical_count, status, notes) VALUES
(2, 1, 44, 42, 'APPLIED', 'Bi-weekly bean weight audit - 2 units adjustment confirmed'),
(2, 8, 35, 35, 'APPLIED', 'Oat milk physical carton spot check - 100% matched');

INSERT INTO ai_insights (category, title, description, severity, metric_value, action_label, action_command) VALUES
('FAST_MOVING', 'Fresh Whole Milk 1L deduction velocity peaked at 5.4 units/day', 'Velocity over the last 14 days has surged 22%. At current stock (7 cartons), stockout will occur in ~1.3 days. Recommend ordering 40 cartons immediately.', 'WARNING', '1.3 days remaining', 'Quick Restock', 'OPEN_RESTOCK_MODAL'),
('LOW_STOCK', '4 products are currently below safety thresholds', 'Fresh Whole Milk (7/20), Matcha Tin (6/10), Croissant Box (8/12), and Cold Brew Keg (3/4) are below their configured thresholds.', 'WARNING', '4 Low-Stock', 'Review Low Stock', 'NAVIGATE_LOW_STOCK'),
('SLOW_MOVING', 'Almond Danish Pack has zero stock and zero velocity', 'Depleted 2 days ago due to damage write-off with no restock scheduled. Review supplier delivery schedule or delist item.', 'INFO', '0 units remaining', 'Review Catalog', 'NAVIGATE_PRODUCT'),
('EXPIRY_RISK', 'Butter Croissants: 4 boxes recorded expired this month', 'Expiration deductions account for 12% of total Croissant removals. Consider reducing standing order size by 15%.', 'WARNING', '4 boxes expired', 'Adjust Par Level', 'OPEN_THRESHOLD_MODAL'),
('ANOMALY', 'Unusual Damaged write-off on Almond Danish Pack', 'Full 3 units written off as damaged on SM-DED-0158. Discrepancy logged for shelf handling review.', 'INFO', '3 units lost', 'View Audit Log', 'NAVIGATE_INVENTORY_LOG'),
('RESTOCK_RECOMMENDATION', 'Recommended Reorder Batch for Week 43', 'Suggested purchase order: Fresh Whole Milk (40 pcs), Ceremonial Matcha (15 tins), Butter Croissant (20 boxes). Total estimated cost: ₱8,600.00.', 'OPPORTUNITY', '₱8,600.00 Batch', 'Export Reorder List', 'EXPORT_REORDER_PDF');

SELECT setval('organizations_id_seq', (SELECT MAX(id) FROM organizations));
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('stock_movements_id_seq', (SELECT MAX(id) FROM stock_movements));
SELECT setval('stock_counts_id_seq', (SELECT MAX(id) FROM stock_counts));
SELECT setval('ai_insights_id_seq', (SELECT MAX(id) FROM ai_insights));
SELECT setval('join_codes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM join_codes));
SELECT setval('audit_log_id_seq', (SELECT COALESCE(MAX(id), 1) FROM audit_log));
