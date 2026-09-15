package com.stockpilot.dao;

import com.stockpilot.model.Product;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data-access object for the products table. */
public class ProductDAO {

    public void insert(Product product) throws SQLException {
        String sql = """
            INSERT INTO products (sku, name, category, unit_price, reorder_level, created_at)
            VALUES (?, ?, ?, ?, ?, ?)""";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getSku());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setDouble(4, product.getUnitPrice());
            ps.setInt(5, product.getReorderLevel());
            ps.setString(6, product.getCreatedAt().toString());
            ps.executeUpdate();
        }
    }

    public Optional<Product> findBySku(String sku) throws SQLException {
        String sql = "SELECT * FROM products WHERE sku = ?";
        Connection conn = DBConnectionManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Product> findAll() throws SQLException {
        String sql = "SELECT * FROM products ORDER BY sku";
        Connection conn = DBConnectionManager.getConnection();
        List<Product> result = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public boolean exists(String sku) throws SQLException {
        return findBySku(sku).isPresent();
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getDouble("unit_price"),
                rs.getInt("reorder_level"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
