package com.stockpilot.model;

import java.time.LocalDateTime;

/** A physical warehouse/storage location that can hold stock of many products. */
public class Warehouse {
    private final String code;
    private final String name;
    private final String location;
    private final LocalDateTime createdAt;

    public Warehouse(String code, String name, String location, LocalDateTime createdAt) {
        this.code = code;
        this.name = name;
        this.location = location;
        this.createdAt = createdAt;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("%-8s | %-20s | %s", code, name, location);
    }
}
