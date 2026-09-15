package com.stockpilot.service;

import com.stockpilot.dao.ProductDAO;
import com.stockpilot.dao.WarehouseDAO;
import com.stockpilot.exception.DuplicateSkuException;
import com.stockpilot.exception.ProductNotFoundException;
import com.stockpilot.exception.WarehouseNotFoundException;
import com.stockpilot.io.FileLogger;
import com.stockpilot.model.Product;
import com.stockpilot.model.Warehouse;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Manages the product catalogue and the set of known warehouses. */
public class ProductService {
    private final ProductDAO productDAO = new ProductDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();

    public Product addProduct(String sku, String name, String category, double unitPrice, int reorderLevel)
            throws DuplicateSkuException, SQLException {
        if (productDAO.exists(sku)) {
            throw new DuplicateSkuException("SKU '" + sku + "' is already registered.");
        }
        Product product = new Product(sku, name, category, unitPrice, reorderLevel, LocalDateTime.now());
        productDAO.insert(product);
        FileLogger.log("Added product " + sku + " (" + name + ")");
        return product;
    }

    public Warehouse addWarehouse(String code, String name, String location)
            throws DuplicateSkuException, SQLException {
        if (warehouseDAO.exists(code)) {
            throw new DuplicateSkuException("Warehouse code '" + code + "' is already registered.");
        }
        Warehouse warehouse = new Warehouse(code, name, location, LocalDateTime.now());
        warehouseDAO.insert(warehouse);
        FileLogger.log("Added warehouse " + code + " (" + name + ")");
        return warehouse;
    }

    public Product findProduct(String sku) throws ProductNotFoundException, SQLException {
        return productDAO.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException("No product found with SKU: " + sku));
    }

    public Warehouse findWarehouse(String code) throws WarehouseNotFoundException, SQLException {
        return warehouseDAO.findByCode(code)
                .orElseThrow(() -> new WarehouseNotFoundException("No warehouse found with code: " + code));
    }

    public List<Product> listProducts() throws SQLException {
        return productDAO.findAll();
    }

    public List<Warehouse> listWarehouses() throws SQLException {
        return warehouseDAO.findAll();
    }
}
