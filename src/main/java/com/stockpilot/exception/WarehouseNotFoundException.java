package com.stockpilot.exception;

/** Thrown when an operation references a warehouse code that does not exist. */
public class WarehouseNotFoundException extends Exception {
    public WarehouseNotFoundException(String message) {
        super(message);
    }
}
