package com.stockpilot.service;

import com.stockpilot.dao.ProductDAO;
import com.stockpilot.dao.StockTransactionDAO;
import com.stockpilot.io.StockReportExporter;
import com.stockpilot.model.MovementType;
import com.stockpilot.model.Product;
import com.stockpilot.model.StockLevel;
import com.stockpilot.model.StockTransaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Produces movement reports and low-stock analytics. Uses the Java
 * Collections Framework (EnumMap, List, Streams) to aggregate
 * transaction history rather than re-querying the database for each
 * figure.
 */
public class ReportService {
    private final StockTransactionDAO transactionDAO = new StockTransactionDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public List<StockTransaction> movementHistory(String sku, String warehouseCode) throws SQLException {
        return transactionDAO.findBySkuAndWarehouse(sku, warehouseCode);
    }

    public String exportMovementsCsv(String sku, String warehouseCode) throws SQLException, IOException {
        List<StockTransaction> history = movementHistory(sku, warehouseCode);
        return StockReportExporter.exportMovementsToCsv(sku, warehouseCode, history);
    }

    /** Sums movement quantities grouped by type — e.g. total received vs. dispatched vs. transferred. */
    public Map<MovementType, Integer> totalsByType(List<StockTransaction> movements) {
        Map<MovementType, Integer> totals = new EnumMap<>(MovementType.class);
        for (MovementType type : MovementType.values()) {
            totals.put(type, 0);
        }
        for (StockTransaction t : movements) {
            totals.merge(t.getType(), t.getQuantity(), Integer::sum);
        }
        return totals;
    }

    public int netChange(List<StockTransaction> movements) {
        int net = 0;
        for (StockTransaction t : movements) {
            net += switch (t.getType()) {
                case STOCK_IN, TRANSFER_IN -> t.getQuantity();
                case STOCK_OUT, TRANSFER_OUT -> -t.getQuantity();
                case ADJUSTMENT -> 0;
            };
        }
        return net;
    }

    /**
     * Cross-references every stock level against its product's reorder
     * threshold (via the Reorderable interface's default method) and
     * returns the ones that need restocking.
     */
    public List<String> lowStockAlerts(List<StockLevel> allLevels) throws SQLException {
        List<String> alerts = new ArrayList<>();
        for (StockLevel level : allLevels) {
            Product product = productDAO.findBySku(level.getSku()).orElse(null);
            if (product != null && product.needsReorder(level.getQuantity())) {
                alerts.add(String.format("%-10s @ %-8s | qty %d <= reorder level %d",
                        level.getSku(), level.getWarehouseCode(), level.getQuantity(), product.getReorderLevel()));
            }
        }
        return alerts;
    }
}
