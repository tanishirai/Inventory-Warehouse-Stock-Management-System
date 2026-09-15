package com.stockpilot.exception;

/** Thrown when attempting to create a SKU, warehouse code, or username that already exists. */
public class DuplicateSkuException extends Exception {
    public DuplicateSkuException(String message) {
        super(message);
    }
}
