package com.stockpilot.model;

import java.time.LocalDateTime;

/**
 * A catalogue item. A Product itself carries no quantity — quantity is
 * tracked per-warehouse by StockLevel, since the same SKU can sit in
 * many warehouses at different stock levels simultaneously.
 */
public class Product implements Reorderable {
    private final String sku;
    private final String name;
    private final String category;
    private final double unitPrice;
    private final int reorderLevel;
    private final LocalDateTime createdAt;

    public Product(String sku, String name, String category, double unitPrice, int reorderLevel, LocalDateTime createdAt) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.reorderLevel = reorderLevel;
        this.createdAt = createdAt;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getUnitPrice() { return unitPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public int getReorderLevel() { return reorderLevel; }

    @Override
    public String toString() {
        return String.format("%-10s | %-20s | %-12s | Rs.%8.2f | Reorder @ %d", sku, name, category, unitPrice, reorderLevel);
    }
}
