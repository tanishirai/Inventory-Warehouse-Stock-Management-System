package com.stockpilot.model;

/**
 * Contract for anything that has a reorder threshold. Uses a default
 * method so implementers only need to supply the threshold itself —
 * the comparison logic is shared and can't drift between implementers.
 */
public interface Reorderable {
    int getReorderLevel();

    default boolean needsReorder(int currentQuantity) {
        return currentQuantity <= getReorderLevel();
    }
}
