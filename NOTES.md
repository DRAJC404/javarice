# bAI Implementation Notes & Architectural Assumptions

This document records the design assumptions, architectural decisions, and safe defaults implemented across the bAI platform.

---

### 1. Product Field Validations & Uniqueness Rules
* **Mandatory Fields**: Product picture, name, category, unit, starting quantity (new only), selling price, cost price, low-stock threshold, SKU, and barcode are all strictly required in both the UI (`ProductCatalogView`) and service layer (`ProductService`).
* **Image Validation**: Images must exist, be <= 5 MB, match extensions (`.png`, `.jpg`, `.jpeg`, `.gif`), and decode as a valid image via `ImageIO.read()`. Synthetic/demo paths starting with `/images/`, `demo:`, or `http` are permitted for seed data and headless unit tests.
* **Database vs Service Category Constraint**: `products.category_id` remains nullable in the PostgreSQL database with `ON DELETE SET NULL` to preserve existing product records if a category is deleted. However, `ProductService.createProduct` and `updateProduct` enforce that every product must be assigned an existing category belonging to the current business tenant.
* **Initial Stock Movement (`OPENING_STOCK`)**:
  - Creating a product automatically records an atomic `OPENING_STOCK` stock movement in `stock_movements`.
  - Movement properties: `movement_type = 'IN'`, `reason = 'OPENING_STOCK'`, `previous_stock = 0`, `new_stock = startingQuantity`.
  - The database check constraint `stock_movements_quantity_check` was relaxed to `quantity >= 0` to permit products starting with 0 inventory without constraint violations.

---

### 2. Staff Join Code & Onboarding Prominence
* **Monospace Code Display & Live Countdown**:
  - Raw 8-character codes (`XXXX-XXXX`) are displayed in a monospace block.
  - Live countdown timer tracks the 10:00 lifetime.
  - "Copy Code" button copies to the system clipboard, changes tooltip to "Copied!", reverts label after 2s, and resets completely after 60s.
  - **Shared Workstation Safeguard**: To prevent credential leakage, a background timeline automatically purges the clipboard 60 seconds after copying if the raw code is still present.
* **Navigation & Empty States**:
  - **Top Navbar**: Added an "Add team member" button (`.btn-primary` on wide screens, icon-only on narrow screens with tooltip) visible exclusively to the `OWNER`.
  - **Dashboard**: Added an "Invite your team" banner card at the top when the business has 0 employees or managers (`staffCount == 0`). Once a staff member joins, this banner automatically disappears.
  - **Sidebar**: Moved `Team` navigation item immediately after `Inventory`. Includes a dynamic badge showing active unexpired codes (`X active`).
  - **Team Page**: Cleaned of inline styles, equipped with `PageHeader` "+ Add Team Member", and renders an `EmptyState` when no staff have joined.
  - **Login Page**: Added a secondary `.btn-secondary` button "Join with a code" below "Sign In".

---

### 3. Page 8: Inventory Reports (`ReportsView.java`)
* **Four Dedicated Tabs**:
  1. **Stock Movement**: Historical deduction and addition audit trail scoped by date period (Today, Last 7 Days, Last 30 Days, Last 90 Days, All Time).
  2. **Inventory Valuation**: Current on-hand stock quantities, selling prices, and computed valuations.
  3. **Product Catalog**: Complete catalog with SKU, barcode, category, unit, cost, price, and par levels.
  4. **Low Stock Alerts**: Critical products at or below low-stock threshold with deficit quantities.
* **Multi-Format Export**:
  - **CSV Export**: Standard comma-separated values with escaped quotes and UTF-8 encoding.
  - **OpenPDF PDF Export**: Landscape A4 format with corporate blue header, alternating zebra striping, cell padding, metadata timestamps, and footer records.
  - **Print Direct**: Uses JavaFX `PrinterJob` to render the active report table directly to the user's printer.
* **Tenant Isolation & Concurrency**:
  - All queries strictly enforce `organization_id = ?` and execute off the JavaFX Application Thread via `AppExecutor.execute`.

---

### 4. Page 9: bAI Smart Insights (`AIInsightsView.java`)
* **Dual-Engine Architecture**:
  - Real LLM calls via `AIInsightService` with 20s connection timeout and 1 automatic retry.
  - **Rule-Based Fallback**: If `AI_API_KEY` is absent or the store has < 7 days of movement data, the application switches to deterministic rule-based algorithms with an informative status badge (`Rule-Based Mode`).
  - **Daily AI Quota**: Max 20 calls/day per business tracked in `ai_usage_tracking`.
* **Demand Forecast Chart**:
  - Compares the past 7 days actual units deducted against the next 7 days projected demand.
  - NumberAxis uses integer-only ticks (`createIntegerNumberAxis`).
* **Ask bAI Copilot**:
  - Includes interactive prompt chips ("What should I reorder?", "Show high-waste products", "Forecast next week's sales", "How is stock turnover?").
  - 200-character input validation and strict plain-text response rendering.
