package com.stockpilot.exception;

/** Thrown when an operation references a SKU that does not exist in the catalogue. */
public class ProductNotFoundException extends Exception {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
