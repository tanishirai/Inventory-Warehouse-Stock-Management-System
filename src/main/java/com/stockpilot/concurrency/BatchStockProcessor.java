package com.stockpilot.concurrency;

import com.stockpilot.io.FileLogger;
import com.stockpilot.service.StockService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Submits a batch of stock movement requests to a fixed thread pool
 * and collects the results. Demonstrates the ExecutorService
 * lifecycle: submit, await completion, shut down. Used to simulate
 * multiple warehouse staff picking/receiving the same SKU concurrently
 * (e.g. several pickers fulfilling different orders for the same
 * product at once).
 */
public class BatchStockProcessor {
    private final StockService stockService;

    public BatchStockProcessor(StockService stockService) {
        this.stockService = stockService;
    }

    public List<StockMovementWorker.Result> processAll(List<StockMovementRequest> requests, int poolSize) {
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        List<Future<StockMovementWorker.Result>> futures = new ArrayList<>();
        try {
            for (StockMovementRequest req : requests) {
                futures.add(pool.submit(new StockMovementWorker(stockService, req)));
            }
            List<StockMovementWorker.Result> results = new ArrayList<>();
            for (Future<StockMovementWorker.Result> f : futures) {
                try {
                    results.add(f.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    results.add(new StockMovementWorker.Result(null, false, "Interrupted while waiting for worker."));
                } catch (ExecutionException e) {
                    results.add(new StockMovementWorker.Result(null, false, "Worker threw: " + e.getCause()));
                }
            }
            FileLogger.log("Batch of " + requests.size() + " stock movements processed with pool size " + poolSize);
            return results;
        } finally {
            pool.shutdown();
        }
    }
}
