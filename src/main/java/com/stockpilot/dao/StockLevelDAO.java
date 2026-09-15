package com.stockpilot.dao;

import com.stockpilot.model.BackorderStockLevel;
import com.stockpilot.model.StandardStockLevel;
import com.stockpilot.model.StockLevel;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the stock_levels table, keyed by the
 * composite (sku, warehouse_code) pair. Responsible for the
 * object-relational mapping between rows and the polymorphic
 * StockLevel hierarchy: the policy_type column decides which concrete
 * subclass to instantiate when reading a row back.
 */
public class StockLevelDAO {

    public void insert(StockLevel level) throws SQLException {
        String sql = """
            INSERT INTO stock_levels (sku, warehouse_code, policy_type, safety_buffer,
                                       backorder_limit, quantity, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, level.getSku());
            ps.setString(2, level.getWarehouseCode());
            ps.setString(3, level.getPolicyType());
            if (level instanceof StandardStockLevel std) {
                ps.setInt(4, std.getSafetyBuffer());
                ps.setNull(5, Types.INTEGER);
            } else if (level instanceof BackorderStockLevel bo) {
                ps.setNull(4, Types.INTEGER);
                ps.setInt(5, bo.getBackorderLimit());
            } else {
                ps.setNull(4, Types.INTEGER);
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, level.getQuantity());
            ps.setString(7, level.getUpdatedAt().toString());
            ps.executeUpdate();
        }
    }

    public Optional<StockLevel> find(String sku, String warehouseCode) throws SQLException {
        String sql = "SELECT * FROM stock_levels WHERE sku = ? AND warehouse_code = ?";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.setString(2, warehouseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<StockLevel> findAll() throws SQLException {
        String sql = "SELECT * FROM stock_levels ORDER BY sku, warehouse_code";
        Connection conn = DBConnectionManager.getConnection();
        List<StockLevel> result = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<StockLevel> findBySku(String sku) throws SQLException {
        String sql = "SELECT * FROM stock_levels WHERE sku = ? ORDER BY warehouse_code";
        Connection conn = DBConnectionManager.getConnection();
        List<StockLevel> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public void updateQuantity(String sku, String warehouseCode, int newQuantity) throws SQLException {
        String sql = "UPDATE stock_levels SET quantity = ?, updated_at = ? WHERE sku = ? AND warehouse_code = ?";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, sku);
            ps.setString(4, warehouseCode);
            ps.executeUpdate();
        }
    }

    public boolean exists(String sku, String warehouseCode) throws SQLException {
        return find(sku, warehouseCode).isPresent();
    }

    /** Reconstructs the correct polymorphic StockLevel subclass from a row. */
    private StockLevel mapRow(ResultSet rs) throws SQLException {
        String policy = rs.getString("policy_type");
        String sku = rs.getString("sku");
        String warehouseCode = rs.getString("warehouse_code");
        int quantity = rs.getInt("quantity");
        LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));

        if ("STANDARD".equals(policy)) {
            int buffer = rs.getInt("safety_buffer");
            return new StandardStockLevel(sku, warehouseCode, quantity, updatedAt, buffer);
        } else if ("BACKORDER".equals(policy)) {
            int limit = rs.getInt("backorder_limit");
            return new BackorderStockLevel(sku, warehouseCode, quantity, updatedAt, limit);
        }
        throw new IllegalStateException("Unknown policy_type in storage: " + policy);
    }
}
