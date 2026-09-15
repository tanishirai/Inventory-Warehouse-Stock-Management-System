package com.stockpilot.io;

import com.stockpilot.model.StockTransaction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes a stock movement history out to a CSV file staff can open in
 * a spreadsheet. Separate from FileLogger because it produces a
 * user-facing deliverable rather than an internal diagnostic trail.
 */
public final class StockReportExporter {
    private static final String EXPORT_DIR = "exports";

    private StockReportExporter() { }

    public static String exportMovementsToCsv(String sku, String warehouseCode, List<StockTransaction> movements)
            throws IOException {
        Files.createDirectories(Path.of(EXPORT_DIR));
        String filename = EXPORT_DIR + "/" + sku + "_" + warehouseCode + "_movements.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("id,sku,warehouse_code,type,quantity,quantity_after,timestamp,reference");
            writer.newLine();
            for (StockTransaction t : movements) {
                writer.write(String.format("%d,%s,%s,%s,%d,%d,%s,%s",
                        t.getId(), t.getSku(), t.getWarehouseCode(), t.getType(), t.getQuantity(),
                        t.getQuantityAfter(), t.getTimestamp(), sanitize(t.getReference())));
                writer.newLine();
            }
        }
        return filename;
    }

    private static String sanitize(String reference) {
        return reference == null ? "" : reference.replace(",", ";");
    }
}
