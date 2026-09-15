package com.stockpilot.concurrency;

import com.stockpilot.service.StockService;

import java.util.concurrent.Callable;

/**
 * Wraps a single StockMovementRequest so it can be submitted to an
 * ExecutorService. This is the Runnable/Callable style of concurrency
 * (as opposed to ReorderCheckThread's direct Thread subclassing), used
 * by BatchStockProcessor to simulate many warehouse staff picking and
 * receiving stock against potentially-overlapping SKUs at the same
 * time — safety comes from every worker sharing the same StockService
 * instance, whose stock-level cache + synchronized blocks serialize
 * access per (sku, warehouse) pair (see StockService for the full
 * rationale).
 */
public class StockMovementWorker implements Callable<StockMovementWorker.Result> {
    private final StockService stockService;
    private final StockMovementRequest request;

    public StockMovementWorker(StockService stockService, StockMovementRequest request) {
        this.stockService = stockService;
        this.request = request;
    }

    @Override
    public Result call() {
        try {
            switch (request.type().toUpperCase()) {
                case "IN" -> stockService.stockIn(request.sku(), request.warehouseCode(), request.quantity(), request.reference());
                case "OUT" -> stockService.stockOut(request.sku(), request.warehouseCode(), request.quantity(), request.reference());
                default -> {
                    return new Result(request, false, "Unknown movement type: " + request.type());
                }
            }
            return new Result(request, true, null);
        } catch (Exception e) {
            return new Result(request, false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Outcome of one worker's attempt, reported back to the caller instead of thrown across threads. */
    public record Result(StockMovementRequest request, boolean success, String errorMessage) { }
}
