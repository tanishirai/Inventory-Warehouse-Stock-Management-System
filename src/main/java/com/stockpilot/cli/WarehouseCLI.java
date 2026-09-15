package com.stockpilot.cli;

import com.stockpilot.concurrency.BatchStockProcessor;
import com.stockpilot.concurrency.ReorderCheckThread;
import com.stockpilot.concurrency.StockMovementRequest;
import com.stockpilot.concurrency.StockMovementWorker;
import com.stockpilot.exception.*;
import com.stockpilot.model.Product;
import com.stockpilot.model.StaffUser;
import com.stockpilot.model.StockLevel;
import com.stockpilot.model.StockTransaction;
import com.stockpilot.model.Warehouse;
import com.stockpilot.service.AuthService;
import com.stockpilot.service.ProductService;
import com.stockpilot.service.ReportService;
import com.stockpilot.service.StockService;
import com.stockpilot.util.InputValidator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Text menu that drives the whole application. Kept deliberately thin:
 * every real decision (validation, persistence, locking) lives in the
 * service layer, so this class is mostly input collection, dispatch,
 * and printing — which also means every exception it needs to handle
 * is one thrown deliberately by a service, not a surprise.
 */
public class WarehouseCLI {
    private final Scanner scanner = new Scanner(System.in);
    private final AuthService authService = new AuthService();
    private final ProductService productService = new ProductService();
    private final StockService stockService = new StockService();
    private final ReportService reportService = new ReportService();

    private StaffUser currentUser;

    public void start() {
        System.out.println("=========================================");
        System.out.println("  StockPilot - CLI Warehouse & Inventory Manager");
        System.out.println("=========================================");
        boolean running = true;
        while (running) {
            running = currentUser == null ? loggedOutMenu() : loggedInMenu();
        }
        System.out.println("Goodbye.");
    }

    // ---------------------------------------------------------------
    // Logged-out menu
    // ---------------------------------------------------------------

    private boolean loggedOutMenu() {
        System.out.println("\n[1] Register  [2] Login  [0] Exit");
        String choice = prompt("Choose an option: ");
        switch (choice) {
            case "1" -> handleRegister();
            case "2" -> handleLogin();
            case "0" -> { return false; }
            default -> System.out.println("Unrecognised option.");
        }
        return true;
    }

    private void handleRegister() {
        try {
            String username = prompt("Choose a username: ");
            String password = prompt("Choose a password: ");
            String fullName = prompt("Full name: ");
            String role = prompt("Role (ADMIN or STAFF): ");
            StaffUser user = authService.register(username, password, fullName, role);
            System.out.println("Registered successfully as " + user.getUsername() + " (" + user.getRole() + "). Please log in.");
        } catch (DuplicateSkuException | IllegalArgumentException e) {
            System.out.println("Registration failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A database error occurred during registration: " + e.getMessage());
        }
    }

    private void handleLogin() {
        try {
            String username = prompt("Username: ");
            String password = prompt("Password: ");
            currentUser = authService.login(username, password);
            System.out.println("Welcome back, " + currentUser.getFullName() + "!");
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A database error occurred during login: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Logged-in menu
    // ---------------------------------------------------------------

    private boolean loggedInMenu() {
        System.out.println("\nLogged in as " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        System.out.println("[1] Add Product            [2] Add Warehouse");
        System.out.println("[3] Initialize Stock       [4] Stock In");
        System.out.println("[5] Stock Out              [6] Transfer Stock");
        System.out.println("[7] View All Stock Levels  [8] Movement History");
        System.out.println("[9] Export Movements (CSV) [10] Run Batch Movements");
        System.out.println("[11] Run Reorder Check     [12] Logout");
        System.out.println("[0] Exit");
        String choice = prompt("Choose an option: ");
        switch (choice) {
            case "1" -> handleAddProduct();
            case "2" -> handleAddWarehouse();
            case "3" -> handleInitializeStock();
            case "4" -> handleStockIn();
            case "5" -> handleStockOut();
            case "6" -> handleTransfer();
            case "7" -> handleViewStockLevels();
            case "8" -> handleMovementHistory();
            case "9" -> handleExportMovements();
            case "10" -> handleBatchDemo();
            case "11" -> handleReorderCheck();
            case "12" -> { currentUser = null; System.out.println("Logged out."); }
            case "0" -> { return false; }
            default -> System.out.println("Unrecognised option.");
        }
        return true;
    }

    private void handleAddProduct() {
        try {
            String sku = prompt("SKU: ");
            String name = prompt("Product name: ");
            String category = prompt("Category: ");
            double price = Double.parseDouble(prompt("Unit price: "));
            int reorderLevel = Integer.parseInt(prompt("Reorder level (alert when qty drops to/below this): "));
            Product product = productService.addProduct(sku, name, category, price, reorderLevel);
            System.out.println("Added: " + product);
        } catch (DuplicateSkuException e) {
            System.out.println("Could not add product: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number entered: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A system error occurred: " + e.getMessage());
        }
    }

    private void handleAddWarehouse() {
        try {
            String code = prompt("Warehouse code: ");
            String name = prompt("Warehouse name: ");
            String location = prompt("Location: ");
            Warehouse warehouse = productService.addWarehouse(code, name, location);
            System.out.println("Added: " + warehouse);
        } catch (DuplicateSkuException e) {
            System.out.println("Could not add warehouse: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A system error occurred: " + e.getMessage());
        }
    }

    private void handleInitializeStock() {
        try {
            String sku = prompt("SKU: ");
            String warehouseCode = prompt("Warehouse code: ");
            productService.findProduct(sku);
            productService.findWarehouse(warehouseCode);
            int opening = InputValidator.parseQuantity(prompt("Opening quantity: "));
            int safetyBuffer = Integer.parseInt(prompt("Safety buffer (minimum reserve, e.g. 0): "));
            StockLevel level = stockService.initializeStock(sku, warehouseCode, opening, safetyBuffer);
            System.out.println("Initialized: " + level);
        } catch (ProductNotFoundException | WarehouseNotFoundException | InvalidQuantityException e) {
            System.out.println("Could not initialize stock: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number entered: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A system error occurred: " + e.getMessage());
        }
    }

    private void handleStockIn() {
        try {
            String sku = prompt("SKU: ");
            String warehouseCode = prompt("Warehouse code: ");
            int qty = InputValidator.parseQuantity(prompt("Quantity to receive: "));
            String ref = prompt("Reference (e.g. PO number, optional): ");
            StockTransaction t = stockService.stockIn(sku, warehouseCode, qty, ref.isBlank() ? "Stock in" : ref);
            System.out.println("Done. New quantity: " + t.getQuantityAfter());
        } catch (ProductNotFoundException | WarehouseNotFoundException | InvalidQuantityException e) {
            System.out.println("Stock-in failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A database error occurred: " + e.getMessage());
        }
    }

    private void handleStockOut() {
        try {
            String sku = prompt("SKU: ");
            String warehouseCode = prompt("Warehouse code: ");
            int qty = InputValidator.parseQuantity(prompt("Quantity to dispatch: "));
            String ref = prompt("Reference (e.g. order number, optional): ");
            StockTransaction t = stockService.stockOut(sku, warehouseCode, qty, ref.isBlank() ? "Stock out" : ref);
            System.out.println("Done. New quantity: " + t.getQuantityAfter());
        } catch (ProductNotFoundException | WarehouseNotFoundException | InvalidQuantityException | InsufficientStockException e) {
            System.out.println("Stock-out failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A database error occurred: " + e.getMessage());
        }
    }

    private void handleTransfer() {
        try {
            String sku = prompt("SKU: ");
            String from = prompt("From warehouse code: ");
            String to = prompt("To warehouse code: ");
            int qty = InputValidator.parseQuantity(prompt("Quantity to transfer: "));
            String ref = prompt("Reference (optional): ");
            stockService.transferStock(sku, from, to, qty, ref.isBlank() ? "Transfer" : ref);
            System.out.println("Transfer complete.");
        } catch (ProductNotFoundException | WarehouseNotFoundException | InvalidQuantityException | InsufficientStockException e) {
            System.out.println("Transfer failed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("A database error occurred: " + e.getMessage());
        }
    }

    private void handleViewStockLevels() {
        try {
            List<StockLevel> levels = stockService.allStockLevels();
            if (levels.isEmpty()) {
                System.out.println("No stock levels recorded yet.");
                return;
            }
            levels.forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println("A database error occurred: " + e.getMessage());
        }
    }

    private void handleMovementHistory() {
        try {
            String sku = prompt("SKU: ");
            String warehouseCode = prompt("Warehouse code: ");
            List<StockTransaction> history = reportService.movementHistory(sku, warehouseCode);
            if (history.isEmpty()) {
                System.out.println("No movements for this SKU/warehouse yet.");
                return;
            }
            history.forEach(t -> System.out.println(t.toReportLine()));
            Map<?, Integer> totals = reportService.totalsByType(history);
            System.out.println("--- Totals by type: " + totals);
            System.out.println("--- Net change: " + reportService.netChange(history));
        } catch (SQLException e) {
            System.out.println("A database error occurred: " + e.getMessage());
        }
    }

    private void handleExportMovements() {
        try {
            String sku = prompt("SKU: ");
            String warehouseCode = prompt("Warehouse code: ");
            String path = reportService.exportMovementsCsv(sku, warehouseCode);
            System.out.println("Exported to: " + path);
        } catch (SQLException e) {
            System.out.println("A database error occurred: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.out.println("Could not write export file: " + e.getMessage());
        }
    }

    private void handleBatchDemo() {
        System.out.println("This demonstrates concurrent stock movement processing on a thread pool.");
        String sku = prompt("SKU to run a concurrent in/out batch against: ");
        String warehouseCode = prompt("Warehouse code: ");
        int count = Integer.parseInt(prompt("How many concurrent movements to simulate (e.g. 10): "));
        List<StockMovementRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String type = (i % 2 == 0) ? "IN" : "OUT";
            requests.add(new StockMovementRequest(sku, warehouseCode, type, 5, "Batch demo #" + i));
        }
        BatchStockProcessor processor = new BatchStockProcessor(stockService);
        List<StockMovementWorker.Result> results = processor.processAll(requests, 4);
        long succeeded = results.stream().filter(StockMovementWorker.Result::success).count();
        System.out.println("Batch complete: " + succeeded + "/" + results.size() + " succeeded.");
        results.stream().filter(r -> !r.success()).forEach(r -> System.out.println("  Failed: " + r.errorMessage()));
    }

    private void handleReorderCheck() {
        System.out.println("Running reorder-level check across all stock on a background thread...");
        ReorderCheckThread thread = new ReorderCheckThread();
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (thread.getFailure() != null) {
            System.out.println("Reorder check failed: " + thread.getFailure().getMessage());
        } else if (thread.getAlerts().isEmpty()) {
            System.out.println("Reorder check complete. Nothing needs restocking.");
        } else {
            System.out.println("Reorder check complete. " + thread.getAlerts().size() + " alert(s):");
            thread.getAlerts().forEach(a -> System.out.println("  " + a));
        }
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }
}
