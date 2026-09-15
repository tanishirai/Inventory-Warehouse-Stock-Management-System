# Problem Statement

Most introductory inventory-management coursework stops at holding a single stock count per product in memory — nothing survives a restart, nothing accounts for stock existing in more than one location, and error conditions are handled with a generic try/catch that swallows the specifics. That's a missed opportunity in a course whose syllabus explicitly covers exception handling, multithreading, and JDBC persistence: a simulator that skips all three doesn't actually exercise what the course teaches, and real warehouse systems are inherently multi-location and concurrent (multiple staff picking and receiving stock for the same product at the same time).

StockPilot is a command-line inventory and multi-warehouse stock management system that takes those requirements seriously. It models real warehousing primitives — a product catalogue, multiple named warehouses, per-(product, warehouse) stock levels with different floor policies (safety-stock buffers vs. backorder limits), stock-in, stock-out, and inter-warehouse transfers — persists every staff user, product, warehouse, stock level, and movement to a real embedded SQL database via hand-written JDBC, handles every predictable failure mode through a dedicated custom exception (insufficient stock, unknown SKU, unknown warehouse, invalid quantity, duplicate SKU, bad credentials), and demonstrates two different styles of concurrent processing (a `Thread` subclass and an `ExecutorService`-driven thread pool) applied safely to shared, mutable stock-level state.

## Scope

**In scope:**
- Staff registration and login (password-hashed, not plaintext), with ADMIN/STAFF roles
- A product catalogue and a set of named warehouses
- Per-(SKU, warehouse) stock levels built on a shared abstract class, with two floor policies (safety buffer, backorder limit)
- Stock-in, stock-out, and inter-warehouse transfer operations, fully validated
- Movement history per SKU/warehouse, with CSV export and in-app aggregate reporting (totals by type, net change)
- Thread-safe concurrent stock movement processing via a bounded thread pool
- Background reorder-level checking across all stock via a dedicated thread
- Append-only application logging to a file
- Full JDBC persistence to an embedded SQLite database, created and migrated automatically on first run

**Out of scope (see README's Future Enhancements):**
- A GUI or web front-end — the brief specifically requires CLI executability
- Supplier/purchase-order tracking beyond a free-text reference field
- Per-user salted password hashing / a production-grade KDF
- Real scheduled (cron-like) reorder checking — it's triggered on demand from the CLI so a grader can observe it deterministically

## Target Users

- **Primary**: the course evaluator, running the project from a clean clone via `mvn clean package && java -jar target/stockpilot.jar` to verify functional and non-functional requirements
- **Secondary**: a hypothetical warehouse operations team using the CLI to track stock across multiple locations, move inventory between them, and monitor reorder thresholds — the persona the feature set is actually designed around

## High-Level Features

1. **Product & warehouse management** — registration, login, product catalogue, warehouse registry
2. **Stock movement processing** — stock-in, stock-out, inter-warehouse transfer, with full exception-based validation and thread-safe concurrent execution
3. **Reporting & persistence** — per-SKU/warehouse movement history, CSV export, aggregate totals, reorder-level alerts, and durable storage of every entity via JDBC
