package com.stockpilot.model;

import java.time.LocalDateTime;

/** Represents a warehouse staff account. Passwords are hashed before persistence (see AuthService). */
public class StaffUser {
    private final int id;
    private final String username;
    private final String fullName;
    private final String role; // "ADMIN" or "STAFF"
    private final LocalDateTime createdAt;

    public StaffUser(int id, String username, String fullName, String role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("Staff#%d [%s] (%s, %s)", id, username, fullName, role);
    }
}
