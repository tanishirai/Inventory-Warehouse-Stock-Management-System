-- StockPilot schema (SQLite)
-- Applied automatically at startup by DBConnectionManager; kept here
-- as the authoritative, human-readable reference for the report's
-- database design section.

CREATE TABLE IF NOT EXISTS staff (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,          -- SHA-256 hex digest, never plaintext
    full_name     TEXT NOT NULL,
    role          TEXT NOT NULL,          -- 'ADMIN' | 'STAFF'
    created_at    TEXT NOT NULL           -- ISO-8601 LocalDateTime string
);

CREATE TABLE IF NOT EXISTS warehouses (
    code       TEXT PRIMARY KEY,          -- e.g. WH-A
    name       TEXT NOT NULL,
    location   TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    sku           TEXT PRIMARY KEY,       -- e.g. WID-001
    name          TEXT NOT NULL,
    category      TEXT NOT NULL,
    unit_price    REAL NOT NULL,
    reorder_level INTEGER NOT NULL,       -- alert threshold, shared across all warehouses
    created_at    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS stock_levels (
    sku             TEXT NOT NULL,
    warehouse_code  TEXT NOT NULL,
    policy_type     TEXT NOT NULL,        -- 'STANDARD' | 'BACKORDER'
    safety_buffer   INTEGER,              -- set for STANDARD, NULL for BACKORDER
    backorder_limit INTEGER,              -- set for BACKORDER, NULL for STANDARD
    quantity        INTEGER NOT NULL,
    updated_at      TEXT NOT NULL,
    PRIMARY KEY (sku, warehouse_code),    -- composite key: one row per SKU per warehouse
    FOREIGN KEY (sku) REFERENCES products(sku),
    FOREIGN KEY (warehouse_code) REFERENCES warehouses(code)
);

CREATE TABLE IF NOT EXISTS stock_transactions (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    sku            TEXT NOT NULL,
    warehouse_code TEXT NOT NULL,
    type           TEXT NOT NULL,         -- STOCK_IN | STOCK_OUT | TRANSFER_IN | TRANSFER_OUT | ADJUSTMENT
    quantity       INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    timestamp      TEXT NOT NULL,
    reference      TEXT,
    FOREIGN KEY (sku) REFERENCES products(sku),
    FOREIGN KEY (warehouse_code) REFERENCES warehouses(code)
);

-- Entity relationships:
--   products (1) ----< stock_levels (many)        one product can sit in many warehouses
--   warehouses (1) ----< stock_levels (many)       one warehouse holds many products
--   stock_levels (1) ----< stock_transactions (many, via sku+warehouse_code)   one stock line has many movements
