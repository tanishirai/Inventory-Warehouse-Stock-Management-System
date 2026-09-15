package com.stockpilot.dao;

import com.stockpilot.model.Warehouse;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data-access object for the warehouses table. */
public class WarehouseDAO {

    public void insert(Warehouse warehouse) throws SQLException {
        String sql = "INSERT INTO warehouses (code, name, location, created_at) VALUES (?, ?, ?, ?)";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouse.getCode());
            ps.setString(2, warehouse.getName());
            ps.setString(3, warehouse.getLocation());
            ps.setString(4, warehouse.getCreatedAt().toString());
            ps.executeUpdate();
        }
    }

    public Optional<Warehouse> findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM warehouses WHERE code = ?";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Warehouse> findAll() throws SQLException {
        String sql = "SELECT * FROM warehouses ORDER BY code";
        Connection conn = DBConnectionManager.getConnection();
        List<Warehouse> result = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public boolean exists(String code) throws SQLException {
        return findByCode(code).isPresent();
    }

    private Warehouse mapRow(ResultSet rs) throws SQLException {
        return new Warehouse(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("location"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
