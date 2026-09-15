package com.stockpilot.util;

import com.stockpilot.exception.InvalidQuantityException;

/** Static helpers for validating and parsing user-supplied input from the CLI. */
public final class InputValidator {

    private InputValidator() { }

    public static int parseQuantity(String raw) throws InvalidQuantityException {
        if (raw == null || raw.isBlank()) {
            throw new InvalidQuantityException("Quantity cannot be empty.");
        }
        int qty;
        try {
            qty = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new InvalidQuantityException("'" + raw + "' is not a valid whole number.");
        }
        if (qty <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero.");
        }
        return qty;
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
