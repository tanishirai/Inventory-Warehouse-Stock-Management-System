package com.stockpilot.exception;

/** Thrown when a quantity fails validation (negative, zero where not allowed, non-numeric, etc.). */
public class InvalidQuantityException extends Exception {
    public InvalidQuantityException(String message) {
        super(message);
    }
}
