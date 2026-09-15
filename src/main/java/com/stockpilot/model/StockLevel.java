package com.stockpilot.model;

import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.InvalidQuantityException;
import java.time.LocalDateTime;

/**
 * Tracks how many units of one SKU sit in one warehouse. This is the
 * mutable resource every stock movement ultimately changes, so — like
 * a bank account balance — every mutation is validated and
 * synchronized here rather than left to callers to get right.
 *
 * Concrete subclasses decide how far quantity is allowed to drop
 * below zero: a StandardStockLevel enforces a safety-stock floor at
 * or above zero, while a BackorderStockLevel permits going negative
 * up to a configured backorder limit (units already promised to a
 * customer but not yet physically in the warehouse).
 */
public abstract class StockLevel {
    private final String sku;
    private final String warehouseCode;
    private int quantity;
    private LocalDateTime updatedAt;

    protected StockLevel(String sku, String warehouseCode, int openingQuantity, LocalDateTime updatedAt) {
        this.sku = sku;
        this.warehouseCode = warehouseCode;
        this.quantity = openingQuantity;
        this.updatedAt = updatedAt;
    }

    public String getSku() { return sku; }
    public String getWarehouseCode() { return warehouseCode; }
    public synchronized int getQuantity() { return quantity; }
    public synchronized LocalDateTime getUpdatedAt() { return updatedAt; }

    /** Short label such as "STANDARD" or "BACKORDER", used in listings and persistence. */
    public abstract String getPolicyType();

    /**
     * Subclasses decide how far quantity is allowed to drop below zero
     * (a non-negative safety buffer for standard stock, a negative
     * backorder limit for backorder-enabled stock).
     */
    protected abstract int getMinimumAllowedQuantity();

    public synchronized void stockIn(int qty) throws InvalidQuantityException {
        if (qty <= 0) {
            throw new InvalidQuantityException("Stock-in quantity must be positive, got: " + qty);
        }
        quantity += qty;
        updatedAt = LocalDateTime.now();
    }

    public synchronized void stockOut(int qty) throws InvalidQuantityException, InsufficientStockException {
        if (qty <= 0) {
            throw new InvalidQuantityException("Stock-out quantity must be positive, got: " + qty);
        }
        int projected = quantity - qty;
        if (projected < getMinimumAllowedQuantity()) {
            throw new InsufficientStockException(String.format(
                    "SKU %s @ %s: quantity %d cannot cover stock-out of %d (floor: %d)",
                    sku, warehouseCode, quantity, qty, getMinimumAllowedQuantity()));
        }
        quantity -= qty;
        updatedAt = LocalDateTime.now();
    }

    /** Package-private/protected hook used only by StockLevelDAO when hydrating from the database. */
    protected synchronized void setQuantityFromStorage(int storedQuantity) {
        this.quantity = storedQuantity;
    }

    @Override
    public String toString() {
        return String.format("%-10s @ %-8s | %-10s | Qty: %6d | Updated: %s",
                sku, warehouseCode, getPolicyType(), quantity, updatedAt.toLocalDate());
    }
}
