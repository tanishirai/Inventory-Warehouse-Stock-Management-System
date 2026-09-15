package com.stockpilot.model;

import java.time.LocalDateTime;

/**
 * Backorder-enabled stock: quantity is permitted to go negative, up to
 * a configured limit, representing units already committed to
 * customers but not yet physically received into the warehouse.
 */
public class BackorderStockLevel extends StockLevel {
    private final int backorderLimit;

    public BackorderStockLevel(String sku, String warehouseCode, int openingQuantity,
                                LocalDateTime updatedAt, int backorderLimit) {
        super(sku, warehouseCode, openingQuantity, updatedAt);
        this.backorderLimit = backorderLimit;
    }

    @Override
    public String getPolicyType() { return "BACKORDER"; }

    @Override
    protected int getMinimumAllowedQuantity() { return -backorderLimit; }

    public int getBackorderLimit() { return backorderLimit; }
}
