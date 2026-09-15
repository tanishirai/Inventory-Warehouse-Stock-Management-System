package com.stockpilot.model;

/** Every kind of stock movement the system can record against a product/warehouse pair. */
public enum MovementType {
    STOCK_IN,
    STOCK_OUT,
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT
}
