ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_id_fkey;

ALTER TABLE products ALTER COLUMN category_id DROP NOT NULL;

ALTER TABLE categories DROP CONSTRAINT IF EXISTS uq_categories_org_id;
ALTER TABLE categories ADD CONSTRAINT uq_categories_org_id UNIQUE (organization_id, id);

ALTER TABLE products ADD CONSTRAINT products_category_id_fkey 
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

ALTER TABLE products DROP CONSTRAINT IF EXISTS fk_products_org_category;
ALTER TABLE products ADD CONSTRAINT fk_products_org_category 
    FOREIGN KEY (organization_id, category_id) REFERENCES categories(organization_id, id) ON DELETE SET NULL;
