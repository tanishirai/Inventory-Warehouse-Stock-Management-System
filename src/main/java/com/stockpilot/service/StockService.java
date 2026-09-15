package com.stockpilot.service;

import com.stockpilot.dao.StockLevelDAO;
import com.stockpilot.dao.StockTransactionDAO;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.InvalidQuantityException;
import com.stockpilot.exception.ProductNotFoundException;
import com.stockpilot.exception.WarehouseNotFoundException;
import com.stockpilot.io.FileLogger;
import com.stockpilot.model.MovementType;
import com.stockpilot.model.StandardStockLevel;
import com.stockpilot.model.StockLevel;
import com.stockpilot.model.StockTransaction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies stock-in, stock-out, and inter-warehouse transfer movements.
 *
 * Thread-safety design: a fresh StockLevel object read straight from
 * the database on every call would defeat StockLevel's own
 * per-instance synchronized methods, since two concurrent threads
 * would each get a *different* object for the *same* (sku,
 * warehouse_code) row and could race past each other. To make the
 * in-memory lock actually mean something, this service keeps a small
 * ConcurrentHashMap cache so every thread touching the same SKU at
 * the same warehouse synchronizes on the exact same StockLevel
 * instance. Persistence to SQLite happens immediately after each
 * in-memory mutation so the two stay consistent.
 */
public class StockService {
    private final StockLevelDAO stockLevelDAO = new StockLevelDAO();
    private final StockTransactionDAO transactionDAO = new StockTransactionDAO();
    private final Map<String, StockLevel> stockCache = new ConcurrentHashMap<>();

    private static String cacheKey(String sku, String warehouseCode) {
        return sku + "@" + warehouseCode;
    }

    /**
     * Registers a new (sku, warehouse) stock line with an opening
     * quantity, defaulting to a StandardStockLevel with no safety
     * buffer. Call this once per SKU/warehouse pair before any
     * movement against it.
     */
    public StockLevel initializeStock(String sku, String warehouseCode, int openingQuantity, int safetyBuffer)
            throws SQLException {
        StockLevel level = new StandardStockLevel(sku, warehouseCode, openingQuantity, LocalDateTime.now(), safetyBuffer);
        stockLevelDAO.insert(level);
        stockCache.put(cacheKey(sku, warehouseCode), level);
        FileLogger.log(String.format("Initialized stock: %s @ %s, qty=%d, safety buffer=%d",
                sku, warehouseCode, openingQuantity, safetyBuffer));
        return level;
    }

    private StockLevel cachedLevel(String sku, String warehouseCode)
            throws ProductNotFoundException, WarehouseNotFoundException, SQLException {
        String key = cacheKey(sku, warehouseCode);
        StockLevel cached = stockCache.get(key);
        if (cached != null) {
            return cached;
        }
        StockLevel fromDb = stockLevelDAO.find(sku, warehouseCode)
                .orElseThrow(() -> new ProductNotFoundException(
                        "No stock record for SKU " + sku + " at warehouse " + warehouseCode
                                + " (has it been initialized there?)"));
        StockLevel winner = stockCache.putIfAbsent(key, fromDb);
        return winner != null ? winner : fromDb;
    }

    public StockTransaction stockIn(String sku, String warehouseCode, int qty, String reference)
            throws ProductNotFoundException, WarehouseNotFoundException, InvalidQuantityException, SQLException {
        StockLevel level = cachedLevel(sku, warehouseCode);
        synchronized (level) {
            level.stockIn(qty);
            stockLevelDAO.updateQuantity(sku, warehouseCode, level.getQuantity());
            StockTransaction t = transactionDAO.insert(sku, warehouseCode, MovementType.STOCK_IN, qty,
                    level.getQuantity(), reference);
            FileLogger.log(String.format("STOCK_IN %d -> %s@%s (new qty %d)", qty, sku, warehouseCode, level.getQuantity()));
            return t;
        }
    }

    public StockTransaction stockOut(String sku, String warehouseCode, int qty, String reference)
            throws ProductNotFoundException, WarehouseNotFoundException, InvalidQuantityException,
            InsufficientStockException, SQLException {
        StockLevel level = cachedLevel(sku, warehouseCode);
        synchronized (level) {
            level.stockOut(qty);
            stockLevelDAO.updateQuantity(sku, warehouseCode, level.getQuantity());
            StockTransaction t = transactionDAO.insert(sku, warehouseCode, MovementType.STOCK_OUT, qty,
                    level.getQuantity(), reference);
            FileLogger.log(String.format("STOCK_OUT %d <- %s@%s (new qty %d)", qty, sku, warehouseCode, level.getQuantity()));
            return t;
        }
    }

    /**
     * Moves units of one SKU from one warehouse to another, atomically
     * from the caller's point of view. Locks are acquired in a fixed
     * order (lexicographic cache key) regardless of transfer direction
     * — the standard technique for avoiding deadlock when two threads
     * might transfer in opposite directions between the same warehouse
     * pair for the same SKU.
     */
    public void transferStock(String sku, String fromWarehouse, String toWarehouse, int qty, String reference)
            throws ProductNotFoundException, WarehouseNotFoundException, InvalidQuantityException,
            InsufficientStockException, SQLException {
        if (fromWarehouse.equals(toWarehouse)) {
            throw new InvalidQuantityException("Cannot transfer stock to the same warehouse.");
        }
        StockLevel from = cachedLevel(sku, fromWarehouse);
        StockLevel to = cachedLevel(sku, toWarehouse);

        String fromKey = cacheKey(sku, fromWarehouse);
        String toKey = cacheKey(sku, toWarehouse);
        StockLevel first = fromKey.compareTo(toKey) < 0 ? from : to;
        StockLevel second = fromKey.compareTo(toKey) < 0 ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.stockOut(qty);
                to.stockIn(qty);
                stockLevelDAO.updateQuantity(sku, fromWarehouse, from.getQuantity());
                stockLevelDAO.updateQuantity(sku, toWarehouse, to.getQuantity());
                transactionDAO.insert(sku, fromWarehouse, MovementType.TRANSFER_OUT, qty, from.getQuantity(),
                        reference + " (to " + toWarehouse + ")");
                transactionDAO.insert(sku, toWarehouse, MovementType.TRANSFER_IN, qty, to.getQuantity(),
                        reference + " (from " + fromWarehouse + ")");
                FileLogger.log(String.format("TRANSFER %d units of %s: %s -> %s", qty, sku, fromWarehouse, toWarehouse));
            }
        }
    }

    public List<StockTransaction> movementHistory(String sku, String warehouseCode) throws SQLException {
        return transactionDAO.findBySkuAndWarehouse(sku, warehouseCode);
    }

    public List<StockLevel> allStockLevels() throws SQLException {
        return stockLevelDAO.findAll();
    }
}
