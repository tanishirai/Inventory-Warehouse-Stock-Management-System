package com.stockpilot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Immutable record of a single stock movement against one SKU at one warehouse. */
public class StockTransaction implements Reportable {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final long id;
    private final String sku;
    private final String warehouseCode;
    private final MovementType type;
    private final int quantity;
    private final int quantityAfter;
    private final LocalDateTime timestamp;
    private final String reference;

    public StockTransaction(long id, String sku, String warehouseCode, MovementType type, int quantity,
                             int quantityAfter, LocalDateTime timestamp, String reference) {
        this.id = id;
        this.sku = sku;
        this.warehouseCode = warehouseCode;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
        this.timestamp = timestamp;
        this.reference = reference;
    }

    public long getId() { return id; }
    public String getSku() { return sku; }
    public String getWarehouseCode() { return warehouseCode; }
    public MovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public int getQuantityAfter() { return quantityAfter; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getReference() { return reference; }

    @Override
    public String toReportLine() {
        return String.format("%-19s | %-12s | %5s%6d | Qty: %6d | %s",
                timestamp.format(FMT), type, (isInbound() ? "+" : "-"), quantity, quantityAfter, reference);
    }

    private boolean isInbound() {
        return type == MovementType.STOCK_IN || type == MovementType.TRANSFER_IN;
    }
}
