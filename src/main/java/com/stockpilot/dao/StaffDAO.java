package com.stockpilot.dao;

import com.stockpilot.model.StaffUser;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/** Data-access object for the staff table. */
public class StaffDAO {

    public StaffUser insert(String username, String passwordHash, String fullName, String role) throws SQLException {
        String sql = "INSERT INTO staff (username, password_hash, full_name, role, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DBConnectionManager.getConnection();
        String createdAt = LocalDateTime.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.setString(5, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new StaffUser(id, username, fullName, role, LocalDateTime.parse(createdAt));
            }
        }
    }

    public Optional<StaffUser> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM staff WHERE username = ?";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<String> findPasswordHash(String username) throws SQLException {
        String sql = "SELECT password_hash FROM staff WHERE username = ?";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("password_hash")) : Optional.empty();
            }
        }
    }

    private StaffUser mapRow(ResultSet rs) throws SQLException {
        return new StaffUser(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("role"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
