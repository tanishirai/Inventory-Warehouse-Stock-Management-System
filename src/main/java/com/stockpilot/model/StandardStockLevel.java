package com.stockpilot.model;

import java.time.LocalDateTime;

/**
 * Standard stock: quantity may never drop below a configured safety
 * buffer (a reserve kept aside for emergencies/priority orders — a
 * buffer of 0 simply means "never allow negative stock").
 */
public class StandardStockLevel extends StockLevel {
    private final int safetyBuffer;

    public StandardStockLevel(String sku, String warehouseCode, int openingQuantity,
                               LocalDateTime updatedAt, int safetyBuffer) {
        super(sku, warehouseCode, openingQuantity, updatedAt);
        this.safetyBuffer = safetyBuffer;
    }

    @Override
    public String getPolicyType() { return "STANDARD"; }

    @Override
    protected int getMinimumAllowedQuantity() { return safetyBuffer; }

    public int getSafetyBuffer() { return safetyBuffer; }
}
