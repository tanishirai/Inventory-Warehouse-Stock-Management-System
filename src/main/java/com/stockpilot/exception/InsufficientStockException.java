package com.stockpilot.exception;

/** Thrown when a stock-out or transfer would breach a stock level's allowed floor. */
public class InsufficientStockException extends Exception {
    public InsufficientStockException(String message) {
        super(message);
    }
}
