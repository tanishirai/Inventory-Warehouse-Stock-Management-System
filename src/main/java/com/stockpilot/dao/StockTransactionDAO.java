package com.stockpilot.dao;

import com.stockpilot.model.MovementType;
import com.stockpilot.model.StockTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data-access object for the stock_transactions (movement ledger) table. */
public class StockTransactionDAO {

    public StockTransaction insert(String sku, String warehouseCode, MovementType type, int quantity,
                                    int quantityAfter, String reference) throws SQLException {
        String sql = """
            INSERT INTO stock_transactions (sku, warehouse_code, type, quantity, quantity_after, timestamp, reference)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";
        Connection conn = DBConnectionManager.getConnection();
        String timestamp = LocalDateTime.now().toString();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sku);
            ps.setString(2, warehouseCode);
            ps.setString(3, type.name());
            ps.setInt(4, quantity);
            ps.setInt(5, quantityAfter);
            ps.setString(6, timestamp);
            ps.setString(7, reference);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                return new StockTransaction(id, sku, warehouseCode, type, quantity, quantityAfter,
                        LocalDateTime.parse(timestamp), reference);
            }
        }
    }

    public List<StockTransaction> findBySkuAndWarehouse(String sku, String warehouseCode) throws SQLException {
        String sql = "SELECT * FROM stock_transactions WHERE sku = ? AND warehouse_code = ? ORDER BY id";
        Connection conn = DBConnectionManager.getConnection();
        List<StockTransaction> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.setString(2, warehouseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    private StockTransaction mapRow(ResultSet rs) throws SQLException {
        return new StockTransaction(
                rs.getLong("id"),
                rs.getString("sku"),
                rs.getString("warehouse_code"),
                MovementType.valueOf(rs.getString("type")),
                rs.getInt("quantity"),
                rs.getInt("quantity_after"),
                LocalDateTime.parse(rs.getString("timestamp")),
                rs.getString("reference")
        );
    }
}
