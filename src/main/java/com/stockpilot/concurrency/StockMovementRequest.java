package com.stockpilot.concurrency;

/**
 * Describes one stock movement to be processed by the worker pool.
 * type is "IN" or "OUT" — kept as a String rather than MovementType
 * here because a batch file may also contain rows the parser rejects
 * before they ever become a real transaction.
 */
public record StockMovementRequest(String sku, String warehouseCode, String type, int quantity, String reference) {
}
